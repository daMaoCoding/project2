package dc.pay.business.rongxinfu;

/**
 * ************************
 * @author tony 3556239829
 */

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayRequestHandler;
import dc.pay.business.RequestPayResult;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@RequestPayHandler("RONGXINFU")
public final class RongXinFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(RongXinFuPayRequestHandler.class);
    private static final String PARTNER = "partner";
    private static final String BANKTYPE = "banktype";
    private static final String PAYMONEY = "paymoney";
    private static final String ORDERNUMBER = "ordernumber";
    private static final String CALLBACKURL = "callbackurl";
    private static final String SIGN = "sign";
    private static final String QRCONTEXT = "QrContext";
    private static final String HTMLCONTEXT = "HtmlContext";
    private static final String PARSEHTML = "parseHtml";
    private static final String ISSHOW = "isshow";
    private static final String JUMPURL = "JUMPURL";



    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(PARTNER, channelWrapper.getAPI_MEMBERID());
                put(BANKTYPE, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(PAYMONEY, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(ORDERNUMBER, channelWrapper.getAPI_ORDER_ID());  //.concat(channelWrapper.getAPI_MEMBERID()
                put(CALLBACKURL, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
               // put(ISSHOW,"0");
            }
        };
        log.debug("[融信付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }
    protected String buildPaySign(Map payParam) throws PayException {
        String paramsStr = String.format("partner=%s&banktype=%s&paymoney=%s&ordernumber=%s&callbackurl=%s%s",
                payParam.get(PARTNER),
                payParam.get(BANKTYPE),
                payParam.get(PAYMONEY),
                payParam.get(ORDERNUMBER),
                payParam.get(CALLBACKURL),
                channelWrapper.getAPI_KEY());
        String pay_md5sign = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[融信付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
         HashMap<String, String> result = Maps.newHashMap();
        try {

             result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));//            String channel_flag = channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG();
////            String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.GET).trim();
//            if(HandlerUtil.isWapOrApp(channelWrapper) ||HandlerUtil.isWY(channelWrapper)){
//                String getURLForwapAPP = HttpUtil.getURLWithParam(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
//                HashMap<String, String> result = Maps.newHashMap();
//                result.put(JUMPURL, getURLForwapAPP);
//                payResultList.add(result);
//            }else {
//                Result firstPayresult = HttpUtil.get(channelWrapper.getAPI_CHANNEL_BANK_URL(), null, payParam);
//                if (null==firstPayresult || firstPayresult.getBody().length() < 10 ||  firstPayresult.getBody().contains("408")) {
//                    log.error("[融信付]3.发送支付请求，及获取支付请求结果：" + firstPayresult.getBody() + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                    throw new PayException(firstPayresult.getBody());
//                }
//                if (200 == firstPayresult.getStatusCode() && 0 < firstPayresult.getBody().length()) {
//                    if (1==2 && channel_flag.equalsIgnoreCase("TENPAY") && channel_flag.equalsIgnoreCase("WEIXINWAP") ) {
//                        if (firstPayresult.getBody().contains("data=") && firstPayresult.getBody().contains("<script>")) {
//                            String body = firstPayresult.getBody().trim();
//                            String subString = body.substring(body.indexOf("data=") + 5);
//                            subString = subString.substring(0, subString.indexOf("<script>"));
//                            String tenPayQrContext = HandlerUtil.UrlDecode(subString);
//                            HashMap<String, String> result = Maps.newHashMap();
//                            result.put(QRCONTEXT, tenPayQrContext);
//                            result.put(PARSEHTML, body);
//                            payResultList.add(result);
//
//                        }
//                    } else if (1==1 || channel_flag.equalsIgnoreCase("WEIXIN") || channel_flag.equalsIgnoreCase("ALIPAY") || channel_flag.equalsIgnoreCase("QQ") || channel_flag.equalsIgnoreCase("JDPAY")) {
//                        Document document = Jsoup.parse(firstPayresult.getBody());  //Jsoup.parseBodyFragment(html)
//                        String imgSrc="";
//                        if( 1==2 && channel_flag.equalsIgnoreCase("JDPAY")){
//                            imgSrc =  HandlerUtil.UrlDecode(document.select("div.qr-image img ").first().attr("src"));  //二维码地址
//                        }if( 1==1 || channel_flag.equalsIgnoreCase("ALIPAY")){ //只验证这1个
//                            // <img id="show_qrcode" width="300" height="300" src="/generator/qrcode.do?pay_url=http%3A%2F%2Fapi.feichipay.com%2Fsend.do%3Ftoken%3DTWpBeE9EQTJNakkzTVRJMk9URTJOekkz" >
//                            //第三方讲，全部都是这样。。
//                            imgSrc = document.select("img#show_qrcode").first().attr("src");  //二维码地址
//                        }else{
//                            imgSrc = document.select("div#divQRCode img ").first().attr("src");  //二维码地址
//                        }
//                        //String wxQRContext = HandlerUtil.UrlDecode(imgSrc.substring(imgSrc.indexOf("data=") + 5));
//                        String wxQRContext = HandlerUtil.UrlDecode(imgSrc.substring(imgSrc.indexOf("pay_url=") + 8));
//                        if(StringUtils.isBlank(wxQRContext)){
//                            throw new PayException("错误的二维码地址："+imgSrc);
//                        }
//
//                        HashMap<String, String> result = Maps.newHashMap();
//                        result.put(QRCONTEXT, wxQRContext);
//                        result.put(PARSEHTML, document.toString());
//                        payResultList.add(result);
//                        if (StringUtils.isBlank(wxQRContext) || !wxQRContext.contains("//")) {
//                            throw new PayException(SERVER_MSG.REQUEST_PAY_PARSE_RESULT_ERROR);
//                        }
//                    }  else {
//                        String body = HandlerUtil.replaceBlank(firstPayresult.getBody().trim());
//                        HashMap<String, String> result = Maps.newHashMap();
//                        result.put(HTMLCONTEXT, body);
//                        payResultList.add(result);
//                    }
//                } else {
//                    throw new PayException(SERVER_MSG.NOT200);
//                }
//            }
//

        } catch (Exception e) {
            log.error("[融信付]3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(e.getMessage(), e);
        }
         payResultList.add(result);
        log.debug("[融信付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
        return payResultList;
    }


    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (null != resultListMap && !resultListMap.isEmpty()) {
            if (resultListMap.size() == 1) {
                Map<String, String> resultMap = resultListMap.get(0);
                requestPayResult=  buildResult(resultMap, channelWrapper,requestPayResult);
            }
            if (ValidateUtil.requestesultValdata(requestPayResult)) {
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            } else {
                throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[融信付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
    
    
/*
    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (null != resultListMap && !resultListMap.isEmpty()) {
            if (resultListMap.size() == 1) {
                Map<String, String> resultMap = resultListMap.get(0);
                if (null != resultMap && resultMap.containsKey(QRCONTEXT)) {
                    requestPayResult.setRequestPayamount(channelWrapper.getAPI_AMOUNT());
                    requestPayResult.setRequestPayOrderId(channelWrapper.getAPI_ORDER_ID());
                    requestPayResult.setRequestPayOrderCreateTime(HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "yyyy-MM-dd HH:mm:ss"));
                    requestPayResult.setRequestPayQRcodeURL(null);
                    requestPayResult.setRequestPayChannelBankName(channelWrapper.getAPI_CHANNEL_BANK_NAME());
                    requestPayResult.setRequestPayQRcodeContent(resultMap.get(QRCONTEXT));
                }
                if (null != resultMap && resultMap.containsKey(HTMLCONTEXT)) {
                    requestPayResult.setRequestPayamount(channelWrapper.getAPI_AMOUNT());
                    requestPayResult.setRequestPayOrderId(channelWrapper.getAPI_ORDER_ID());
                    requestPayResult.setRequestPayOrderCreateTime(HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "yyyy-MM-dd HH:mm:ss"));
                    requestPayResult.setRequestPayQRcodeURL(null);
                    requestPayResult.setRequestPayChannelBankName(channelWrapper.getAPI_CHANNEL_BANK_NAME());
                    requestPayResult.setRequestPayHtmlContent(resultMap.get(HTMLCONTEXT));
                }
                if (null != resultMap && resultMap.containsKey(JUMPURL)) {
                    requestPayResult.setRequestPayamount(channelWrapper.getAPI_AMOUNT());
                    requestPayResult.setRequestPayOrderId(channelWrapper.getAPI_ORDER_ID());
                    requestPayResult.setRequestPayOrderCreateTime(HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "yyyy-MM-dd HH:mm:ss"));
                    requestPayResult.setRequestPayQRcodeURL(null);
                    requestPayResult.setRequestPayChannelBankName(channelWrapper.getAPI_CHANNEL_BANK_NAME());
                    requestPayResult.setRequestPayJumpToUrl(resultMap.get(JUMPURL));
                }

            }
            if (ValidateUtil.requestesultValdata(requestPayResult)) {
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            } else {
                throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[融信付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }*/
}