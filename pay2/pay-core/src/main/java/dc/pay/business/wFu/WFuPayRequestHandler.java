package dc.pay.business.wFu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import dc.pay.utils.XmlUtil;
import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

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
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.HttpUtil;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.Result;
import dc.pay.utils.RsaUtil;
import dc.pay.utils.ValidateUtil;

@RequestPayHandler("WFU")
public final class WFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(WFuPayRequestHandler.class);

    private static final String merchant_code = "merchant_code";
    private static final String notify_url = "notify_url";
    private static final String interface_version = "interface_version";
    private static final String client_ip = "client_ip";
    private static final String sign_type = "sign_type";
    private static final String order_no = "order_no";
    private static final String order_time = "order_time";
    private static final String order_amount = "order_amount";
    private static final String product_name = "product_name";
    private static final String service_type = "service_type";
    private static final String pay_type = "pay_type";
    private static final String bank_code = "bank_code";
    private static final String input_charset = "input_charset";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put("merchant_code", channelWrapper.getAPI_MEMBERID());
                if(channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("WEB_WY") || channelWrapper.getAPI_CHANNEL_BANK_NAME().endsWith("WAP_YL_SM")){
                    put("service_type", "direct_pay");
                    put("input_charset", "UTF-8");
                    put("interface_version", "V3.0");
                    put("pay_type", HandlerUtil.isYLWAP(channelWrapper) ? "b2cwap" : "b2c");
                    put("bank_code",  channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                }else{
                    put("service_type", channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    put("interface_version", "V3.1");
                }
                put("notify_url", channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put("client_ip", channelWrapper.getAPI_Client_IP());
                put("sign_type", "RSA-S");
                put("order_no", channelWrapper.getAPI_ORDER_ID());
                 //put("order_no", new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));  //todo
                //put("order_no", "TI20170628151958591937");
                put("order_time", DateUtil.formatDateTimeStrByParam("yyyy-MM-dd HH:mm:ss")); //yyyy-MM-dd HH:mm:ss
                put("order_amount", HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put("product_name", "PAY");
            }
        };
        log.debug("[W付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map params) throws PayException {
        StringBuffer signSrc= new StringBuffer();
        if(channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("WEB_WY") ||  channelWrapper.getAPI_CHANNEL_BANK_NAME().endsWith("WAP_YL_SM")){
            signSrc.append("bank_code=").append(params.get(bank_code)).append("&");
            signSrc.append("client_ip=").append(params.get(client_ip)).append("&");
            signSrc.append("input_charset=").append(params.get(input_charset)).append("&");
            signSrc.append("interface_version=").append(params.get(interface_version)).append("&");
            signSrc.append("merchant_code=").append(params.get(merchant_code)).append("&");
            signSrc.append("notify_url=").append(params.get(notify_url)).append("&");
            signSrc.append("order_amount=").append(params.get(order_amount)).append("&");
            signSrc.append("order_no=").append(params.get(order_no)).append("&");
            signSrc.append("order_time=").append(params.get(order_time)).append("&");
            signSrc.append("pay_type=").append(params.get(pay_type)).append("&");
            signSrc.append("product_name=").append(params.get(product_name)).append("&");
            signSrc.append("service_type=").append(params.get(service_type));
        }else{
            signSrc.append("client_ip=").append(params.get(client_ip)).append("&");
            signSrc.append("interface_version=").append(params.get(interface_version)).append("&");
            signSrc.append("merchant_code=").append(params.get(merchant_code)).append("&");
            signSrc.append("notify_url=").append(params.get(notify_url)).append("&");
            signSrc.append("order_amount=").append(params.get(order_amount)).append("&");
            signSrc.append("order_no=").append(params.get(order_no)).append("&");
            signSrc.append("order_time=").append(params.get(order_time)).append("&");
            signSrc.append("product_name=").append(params.get(product_name)).append("&");
            signSrc.append("service_type=").append(params.get(service_type));
        }

        String signInfo = signSrc.toString();
        String signMd5="";
        try {
            signMd5 = RsaUtil.signByPrivateKey(signInfo,channelWrapper.getAPI_KEY());	// 签名
        } catch (Exception e) {
            log.error("[W付]-[请求支付]-2.生成加密URL签名出错，签名出错：{}",e.getMessage(),e);
        }
        //String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[W付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        try {
            String channel_bank = channelWrapper.getAPI_CHANNEL_BANK_NAME();
            if (channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("WEB_WY")  || channelWrapper.getAPI_CHANNEL_BANK_NAME().endsWith("WAP_YL_SM") ) {
                StringBuffer sbHtml = new StringBuffer();
                sbHtml.append("<form id='mobaopaysubmit' name='mobaopaysubmit' action='" + channelWrapper.getAPI_CHANNEL_BANK_URL() + "' method='post'>");
                for (Map.Entry<String, String> entry : payParam.entrySet()) {
                    sbHtml.append("<input type='hidden' name='" + entry.getKey() + "' value='" + entry.getValue() + "'/>");
                }
                sbHtml.append("</form>");
                sbHtml.append("<script>document.forms['mobaopaysubmit'].submit();</script>");
                Map result = Maps.newHashMap();
                result.put(HTMLCONTEXT, sbHtml.toString());
                payResultList.add(result);
            }else{
            String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
            resultStr = resultStr.replaceAll("<dinpay>", "").replaceAll("</dinpay>", "");
            Map<String, String> mapBodys = XmlUtil.toMap(resultStr.getBytes(), "utf-8");

            String resp_code = mapBodys.get("resp_code");//SUCCESS
            String order_amount = mapBodys.get("order_amount");//1.00
            String result_code = mapBodys.get("result_code");//0
            if ("SUCCESS".equalsIgnoreCase(resp_code) && "0".equalsIgnoreCase(result_code) && StringUtils.isNotBlank(order_amount)) {
                HashMap<String, String> result = Maps.newHashMap();
                if(HandlerUtil.isWapOrApp(channelWrapper)){
                    String payURL = mapBodys.get("payurl");
                    result.put(JUMPURL, HandlerUtil.UrlDecode(payURL));
                    payResultList.add(result);

                }else{
                    String qrcode = mapBodys.get("qrcode");
                    //支付宝官方3次跳转,WFU_BANK_WEBWAPAPP_ZFB_SM
                    if (channel_bank.equalsIgnoreCase("nothingToDoHere")) {
                        Map<String, String> headers = Maps.newHashMap();
                        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:56.0) Gecko/20100101 Firefox/56.0");
                        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
                        headers.put("Accept-Language", "zh-CN,zh;q=0.8,en-US;q=0.5,en;q=0.3");
                        headers.put("Accept-Encoding", "gzip, deflate, br");
                        headers.put("Connection", "keep-alive");
                        headers.put("Upgrade-Insecure-Requests", "1");
                        headers.put("Pragma", "no-cache");
                        headers.put("Cache-Control", "no-cache");
                        String url = HandlerUtil.UrlDecode(qrcode);
                        Result firstPayresult = HttpUtil.getNoRedirects(url, headers);
                        Map<String, String> headerAll = firstPayresult.getHeaderAllMap();
                        int statusCode = firstPayresult.getStatusCode();
                        String locationValue = headerAll.get("Location");
                        if (302 == statusCode && StringUtils.isNotBlank(locationValue)) {
                            Result firstPayresult1 = HttpUtil.getNoRedirects(locationValue, headerAll);
                            int statusCode1 = firstPayresult1.getStatusCode();
                            Map<String, String> headerAll1 = firstPayresult1.getHeaderAllMap();
                            String locationValue1 = headerAll1.get("Location");
                            if (302 == statusCode1 && StringUtils.isNotBlank(locationValue1)) {
                                Result firstPayresult2 = HttpUtil.getNoRedirects(locationValue1, headerAll1);
                                Map<String, String> headerAll2 = firstPayresult2.getHeaderAllMap();
                                int statusCode2 = firstPayresult2.getStatusCode();
                                String locationValue2 = headerAll2.get("Location");
                                if (302 == statusCode2 && StringUtils.isNotBlank(locationValue2)) {
                                    Result firstPayresult3 = HttpUtil.getNoRedirects(locationValue2, headerAll2);
                                    int statusCode3 = firstPayresult3.getStatusCode();
                                    Map<String, String> headerAll3 = firstPayresult3.getHeaderAllMap();
                                    String locationValue3 = headerAll3.get("Location");
                                    // System.out.println(firstPayresult3);
                                    //System.out.println(statusCode3);
                                    //System.out.println(locationValue3);
                                }
                            }
                        }
                        // System.out.println(locationValue);
                        Document document = Jsoup.parse(firstPayresult.getBody().trim());
                        // System.out.println(document);
                    } else {
                        result.put(QRCONTEXT, HandlerUtil.UrlDecode(qrcode));
                        payResultList.add(result);
                    }
                }
            } else {
                log.error("[W付]3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(mapBodys) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(JSON.toJSONString(mapBodys));
            }

        }

        } catch (Exception e) {
            log.error("[W付]3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(e.getMessage(), e);
        }
        log.debug("[W付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
        return payResultList;
    }


    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (null != resultListMap && !resultListMap.isEmpty()) {
            if (resultListMap.size() == 1) {
                Map<String, String> resultMap = resultListMap.get(0);
                requestPayResult = buildResult(resultMap, channelWrapper, requestPayResult);
            }
            if (ValidateUtil.requestesultValdata(requestPayResult)) {
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            } else {
                throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[W付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}