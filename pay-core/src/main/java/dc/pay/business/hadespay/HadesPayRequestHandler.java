package dc.pay.business.hadespay;

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
import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestPayHandler("HADESPAY")
public final class HadesPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(HadesPayRequestHandler.class);


    private static final String    	usercode = "usercode";    //	       用户编号    是	String	用户编号
    private static final String    	customno = "customno";    //	       商户订单号    是	String	商户订单号
    private static final String    	productname = "productname";    //	       产品名称    是	String	产品名称，注意：不要含中文。
    private static final String    	money = "money";    //	       支付金额    是	String	支付金额,单位（元）
    private static final String    	scantype = "scantype";    //	       扫码方式    是	String	参考 附录 4.1.2
    private static final String    	sendtime = "sendtime";    //	       发送时间    是	String	格式：yyyyMMddHHmmss
    private static final String    	notifyurl = "notifyurl";    //	       通知地址    是	String	通知回调地址
    private static final String    	buyerip = "buyerip";    //	       用户IP    是	String
    private static final String    	sign = "sign";    //	       签名串    是	String	签名结果




    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(usercode,channelWrapper.getAPI_MEMBERID());
            payParam.put(customno,channelWrapper.getAPI_ORDER_ID());
            payParam.put(productname,channelWrapper.getAPI_ORDER_ID());
            payParam.put(money,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(scantype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(sendtime,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
            payParam.put(notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(buyerip,channelWrapper.getAPI_Client_IP());
        }
        log.debug("[HadesPay支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        //签名原始串是：origin＝usercode+"|"+customno+"|"+ scantype+"|"+notifyurl+"|"+money+"|"+sendtime+"|"+buyerip+"|"+md5key；
        String paramsStr = String.format("%s|%s|%s|%s|%s|%s|%s|%s",
                params.get(usercode),
                params.get(customno),
                params.get(scantype),
                params.get(notifyurl),
                params.get(money),
                params.get(sendtime),
                params.get(buyerip),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[HadesPay支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
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
                            String imgSrc =  Jsoup.parse(resultSecond.getBody()).select("input#url").first().attr("value");
                            if(StringUtils.isBlank(imgSrc)){
                                log.error("[HadesPay支付]-[请求支付]-3.发送支付请求，及获取支付请求结果失败：二维码出错:{}",HandlerUtil.UrlDecode(imgSrc));
                                throw new PayException("[[HadesPay支付]-[请求支付]-3.发送支付请求，及获取支付请求结果失败：二维码出错:"+HandlerUtil.UrlDecode(imgSrc));
                            }
                            String rqContent = HandlerUtil.UrlDecode( imgSrc);
                            result.put(QRCONTEXT, rqContent); //HadesPay支付只有1个webwapapp通道，只支持二维码
                            payResultList.add(result);
                        }
                    }else {
                        throw new PayException(resultStr);
                    }

                } else if(1==2){//H5

                }else {
                    log.error("[HadesPay支付]3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(JSON.toJSONString(resultStr));
                }
            }
        } catch (Exception e) {
            log.error("[HadesPay支付]3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(e.getMessage(), e);
        }
        log.debug("[HadesPay支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[HadesPay支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}