package dc.pay.business.zhihuitong;

/**
 * ************************
 * @author tony 3556239829
 */

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayRequestHandler;
import dc.pay.business.RequestPayResult;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.*;
import dc.pay.utils.qr.QRCodeUtil;
import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestPayHandler("ZHIHUITONG")
public final class ZhiHuiTongPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(ZhiHuiTongPayRequestHandler.class);

     private static final  String	merchantno="merchantno";             //商户编号   是	 	商户编号
     private static final  String	customno="customno";                 //商户订单号 是	 	商户订单号
     private static final  String	productname="productname";           //产品名称   是	 	产品名称
     private static final  String	iphoneX="iphoneX";                  //产品名称   是	 	产品名称 .....智慧通商户通道调整-2058
     private static final  String	money="money";                       //支付金额   是	 	支付金额,单位（元）注意：请输入个位不为零的整数或两位小数
     private static final  String	stype="stype";                       //收款方式   是	 	参考附录4.2收款编码
     private static final  String	timestamp="timestamp";               //时间戳     是	 	例如：1512475188571
     private static final  String	notifyurl="notifyurl";               //通知地址   是	 	通知回调地址
     private static final  String	buyerip="buyerip";                   //用户IP     是
     private static final  String	sign="sign";                         //签名串     是	 	签名结果
     private static final  String	vpnIP="52.175.24.192";




    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(merchantno,channelWrapper.getAPI_MEMBERID());
            payParam.put(customno,channelWrapper.getAPI_ORDER_ID());
            payParam.put(productname,iphoneX);
            payParam.put(money,HandlerUtil.getYuan( String.valueOf(Long.parseLong(channelWrapper.getAPI_AMOUNT())-((long)(HandlerUtil.getRandomIntBetweenAA(1,99)))) )); //智慧通商户通道调整-2058，随机小数金额
            payParam.put(stype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(timestamp,System.currentTimeMillis()+"");
            payParam.put(notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            if(vpnIP.equalsIgnoreCase(channelWrapper.getAPI_Client_IP())){ //为本地测试不要验证。
              payParam.put(buyerip,"123.123.123.".concat(HandlerUtil.getRandomNumber(2,250)+""));
            }else{
              payParam.put(buyerip,channelWrapper.getAPI_Client_IP());
            }
        }

        log.debug("[智慧通]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
       // origin＝merchantno+"|"+customno+"|"+ stype+"|"+notifyurl+"|"+money+"|"+ timestamp+"|"+buyerip+"|"+md5key;
        // merchantno|customno|stype|notifyurl|money|timestamp|buyerip|md5key

        String paramsStr = String.format("%s|%s|%s|%s|%s|%s|%s|%s",
                params.get(merchantno),  params.get(customno), params.get(stype), params.get(notifyurl),
                params.get(money), params.get(timestamp), params.get(buyerip), channelWrapper.getAPI_KEY()
        );

        String pay_md5sign = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[智慧通]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)  ||( HandlerUtil.isWapOrApp(channelWrapper) )  ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
                payResultList.add(result);
            }else{
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
                JSONObject jsonResultStr = JSON.parseObject(resultStr);
            if(1==1 ) {  //扫码（银联扫码&微信&支付宝&QQ）
                    if(null!=jsonResultStr && jsonResultStr.containsKey("success") && "true".equalsIgnoreCase(jsonResultStr.getString("success")) && jsonResultStr.containsKey("data")){
                            if(StringUtils.isNotBlank(jsonResultStr.getString("data")) && StringUtils.isNotBlank(jsonResultStr.getJSONObject("data").getString("scanurl"))){

                                String qrJumpUrl = jsonResultStr.getJSONObject("data").getString("scanurl");
                                Result resultSecond = HttpUtil.get(qrJumpUrl, null, Maps.newHashMap());
                                String imgSrc =  Jsoup.parse(resultSecond.getBody()).getElementsByTag("body").first().select("div#qrcode img ").first().attr("src");
                                if(StringUtils.isBlank(imgSrc) || !imgSrc.toLowerCase().startsWith("data:image")){
                                    log.error("[智慧通]-[请求支付]-3.发送支付请求，及获取支付请求结果失败：二维码出错:{}",HandlerUtil.UrlDecode(imgSrc));
                                    throw new PayException("[[智慧通]-[请求支付]-3.发送支付请求，及获取支付请求结果失败：二维码出错:"+HandlerUtil.UrlDecode(imgSrc));
                                }
                                String rqContent = QRCodeUtil.decodeByBase64(imgSrc);
                                result.put(QRCONTEXT, rqContent); //智慧通只有1个webwapapp通道，只支持二维码
                                payResultList.add(result);
                            }
                    }else {
                        throw new PayException(resultStr);
                    }

                } else if(1==2){//H5

                }else {
                    log.error("[智慧通]3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(JSON.toJSONString(resultStr));
                }
            }
        } catch (Exception e) {
             log.error("[智慧通]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[智慧通]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
        return payResultList;
    }


    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (null != resultListMap && !resultListMap.isEmpty()) {
            if (resultListMap.size() == 1) {
                Map<String, String> resultMap = resultListMap.get(0);
                requestPayResult = buildResult(resultMap,channelWrapper,requestPayResult);
            }
            if (ValidateUtil.requestesultValdata(requestPayResult)) {
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            } else {
                throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[智慧通]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}