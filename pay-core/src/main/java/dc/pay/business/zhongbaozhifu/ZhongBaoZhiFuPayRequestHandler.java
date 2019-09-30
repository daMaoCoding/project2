package dc.pay.business.zhongbaozhifu;

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
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestPayHandler("ZHONGBAOZHIFU")
public final class ZhongBaoZhiFuPayRequestHandler extends PayRequestHandler {
     private static final Logger log = LoggerFactory.getLogger(ZhongBaoZhiFuPayRequestHandler.class);

     private static final String      merchantid = "merchantid";  //	  商户ID   否	商户id，由众宝商户系统分配
     private static final String      paytype = "paytype";  //	  支付类型   否	支付类型或银行类型，具体请参考附录1,支持的支付类型有：支付宝，微信，QQ钱包，网银，京东钱包，银联
     private static final String      amount = "amount";  //	  金额   否	单位元(人民币)2位小数，最小支付金额为2.00,微信支付宝至少2元，例如：2.00
     private static final String      orderid = "orderid";  //	  商户订单号   否	商户系统订单号，该订单号将作为众宝接口的返回数据。该值需在商户系统内唯一
     private static final String      notifyurl = "notifyurl";  //   异步通知地址   否	异步通知过程的返回地址，需要以http://开头且没有任何参数(如存在特殊字符请转码,注:不支持参数)
     private static final String      request_time = "request_time";  //	  请求时间   否	系统请求时间，精确到秒，长度14位，格式为：yyyymmddhhmmss例如：20170820172323 注：北京时间
     private static final String      isqrcode = "isqrcode";  //    是否单独返回二维码   如果值为Y，则单独返回二维码
     private static final String      bankcode = "bankcode";



    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(merchantid,channelWrapper.getAPI_MEMBERID());
            if(HandlerUtil.isOrderFromWapOrApp(channelWrapper.getAPI_ORDER_FROM()) && HandlerUtil.isWY(channelWrapper) ){
                payParam.put(bankcode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            }else{
                payParam.put(paytype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            }
            payParam.put(amount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(orderid,channelWrapper.getAPI_ORDER_ID());
            payParam.put(notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(request_time,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
            payParam.put(isqrcode,"Y");
        }
        log.debug("[众宝支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        // opstate=opstate值&orderid=orderid值&ovalue=ovalue值&parter=商家接口调用ID&key=商家
        String paramsStr="";
        if(HandlerUtil.isOrderFromWapOrApp(channelWrapper.getAPI_ORDER_FROM()) && HandlerUtil.isWY(channelWrapper) ){
            //merchantid={0}&amount={1}&orderid={2}&notifyurl={3}&request_time={4}&key={5}
              paramsStr = String.format("merchantid=%s&amount=%s&orderid=%s&notifyurl=%s&request_time=%s&key=%s",
                    params.get(merchantid),
                    params.get(amount),
                    params.get(orderid),
                    params.get(notifyurl),
                    params.get(request_time),
                    channelWrapper.getAPI_KEY());
        }else{
            //merchantid={0}&paytype={1}&amount={2}&orderid={3}&notifyurl={4}&request_time={5}&key={6}
            paramsStr = String.format("merchantid=%s&paytype=%s&amount=%s&orderid=%s&notifyurl=%s&request_time=%s&key=%s",
                    params.get(merchantid),
                    params.get(paytype),
                    params.get(amount),
                    params.get(orderid),
                    params.get(notifyurl),
                    params.get(request_time),
                    channelWrapper.getAPI_KEY());
        }
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[众宝支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)  || HandlerUtil.isWapOrApp(channelWrapper)    ) {
                if(HandlerUtil.isOrderFromWapOrApp(channelWrapper.getAPI_ORDER_FROM()) && HandlerUtil.isWY(channelWrapper) ){
                    result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent("https://gateway.zbpay365.com/FastPay/Index",payParam).toString().replace("method='post'","method='post'"));
                }else{
                    result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='post'"));
                }
                payResultList.add(result);
            }else{
                    resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
                    if(!resultStr.contains("<html>") && resultStr.contains("{")){
                        JSONObject jsonResultStr = JSON.parseObject(resultStr);
                        if(null!=jsonResultStr && jsonResultStr.containsKey("code") && "0".equalsIgnoreCase(jsonResultStr.getString("code")) && jsonResultStr.containsKey("qrtype") && jsonResultStr.containsKey("qrinfo")){
                            if(StringUtils.isNotBlank(jsonResultStr.getString("qrtype")) && StringUtils.isNotBlank(jsonResultStr.getString("qrinfo")) ){
                                if("url".equalsIgnoreCase(jsonResultStr.getString("qrtype"))){

                                    if(channelWrapper.getAPI_CHANNEL_BANK_NAME().endsWith("_WEBWAPAPP_JD_SM")){
                                        String trim = RestTemplateUtil.sendByRestTemplateRedirectWithSendSimpleForm(HandlerUtil.UrlDecode(jsonResultStr.getString("qrinfo")), null, HttpMethod.POST).trim();
                                        Document document = Jsoup.parse(trim);
                                        Element script = document.getElementsByTag("script").eq(2).first();  //获取第三个 <script>标签
                                        List<String> imgSrc = HandlerUtil.getImgSrc(script.html());
                                        if(null!=imgSrc && imgSrc.size()>0){
                                            result.put(QRCONTEXT,imgSrc.get(0));
                                        }else{
                                            throw  new PayException("无法获取二维码,"+jsonResultStr);
                                        }
                                    }else{
                                        result.put(JUMPURL, HandlerUtil.UrlDecode(jsonResultStr.getString("qrinfo")));
                                    }


                                }else{
                                    result.put(QRCONTEXT, HandlerUtil.UrlDecode(jsonResultStr.getString("qrinfo")));
                                }
                            }
                        }else {
                            throw new PayException(resultStr);
                        }
                    }else{//李晓 2018/8/24 14:59:36  isqrcode 这个参数现在逐步不支撑了 不好意思
                        //已知微信扫码不反json了
                        Elements selects = Jsoup.parse(resultStr).select("[id=qrimg]");
                        String val = selects.first().attr("src");
                        if(StringUtils.isNotBlank(val)){
                            result.put(QRCONTEXT, val);
                        }else{
                            throw new PayException(resultStr);
                        }
                    }

                }
                payResultList.add(result);
        } catch (Exception e) {
             log.error("[众宝支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[众宝支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
        return payResultList;
    }


    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (null != resultListMap && !resultListMap.isEmpty()) {
            if (resultListMap.size() >= 1) {
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
        log.debug("[众宝支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}