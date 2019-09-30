package dc.pay.business.longxiangzhifu;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
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
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.UnicodeUtil;
import dc.pay.utils.ValidateUtil;
import dc.pay.utils.qr.QRCodeUtil;

/**
 * copy andrew
 * tony
 */

@RequestPayHandler("LONGXIANGZHIFU")
public final class LongXiangZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(LongXiangZhiFuPayRequestHandler.class);

    private static final String auth_apply             ="http://api.henpay.net/api/v1.0/auth";
//    private static final String auth_apply             ="https://api3.henpay.net/api/v1.0/auth";
    private static final String company_service_id             ="company_service_id";
    private static final String trade_service_id               ="trade_service_id";
    private static final String trade_type                     ="trade_type";
    private static final String customer_id                    ="customer_id";
    private static final String item_code                      ="item_code";
    private static final String item_name                      ="item_name";
    private static final String amount                         ="amount";
    private static final String currency                       ="currency";
    private static final String finish_url                     ="finish_url";
    private static final String notify_url                     ="notify_url";
    private static final String timestamp                      ="timestamp";
    private static final String token                          ="token";

    private static final String auth_code                          ="auth_code";
    private static final String payment_type                       ="payment_type";
    

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(company_service_id, channelWrapper.getAPI_MEMBERID());
                put(trade_service_id,channelWrapper.getAPI_ORDER_ID());
//                put(trade_type,handlerUtil.getOrderForm(channelWrapper.getAPI_ORDER_FROM()));
                if ("3".equals(channelWrapper.getAPI_ORDER_FROM())) {
                    put(trade_type,"2");
                }else if ("4".equals(channelWrapper.getAPI_ORDER_FROM())) {
                    put(trade_type,"3");
                }else {
                    put(trade_type,"1");
                }
                put(customer_id,handlerUtil.getRandomStr(8));
                put(item_code,"item_code");
                put(item_name,"item_name");
                put(amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(currency,"1");
                put(finish_url,channelWrapper.getAPI_WEB_URL());
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(timestamp,DateUtil.formatDateTimeStrByParam("yyyy-MM-dd HH:mm:ss"));
                put(payment_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            }
        };
        log.debug("[龙亨支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }
    
    private String auth_code(Map<String, String> payParam,String pay_md5sign) throws PayException {
        Map<String, String> param = new TreeMap<String, String>() {
            {
                put(company_service_id, payParam.get(company_service_id));
                put(trade_service_id,payParam.get(trade_service_id));
                put(trade_type,payParam.get(trade_type));
                put(customer_id,payParam.get(customer_id));
                put(item_code,payParam.get(item_code));
                put(item_name,payParam.get(item_name));
                put(amount, payParam.get(amount));
                put(currency,payParam.get(currency));
                put(finish_url,payParam.get(finish_url));
                put(notify_url,payParam.get(notify_url));
                put(timestamp,payParam.get(timestamp));
                put(token,pay_md5sign);
            }
        };
       //  String resultStr = RestTemplateUtil.sendByRestTemplate(auth_apply, param, String.class, HttpMethod.GET).trim();
        String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(auth_apply, param, String.class, HttpMethod.GET,defaultHeaders).trim();



        if (StringUtils.isBlank(resultStr)) {
            log.error("[龙亨支付]-[请求支付]-auth_code().发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(param));
            throw new PayException("返回空,参数："+JSON.toJSONString(param));
        }
        JSONObject resJson = JSONObject.parseObject(UnicodeUtil.unicodeToString(resultStr));
        //只取正确的值，其他情况抛出异常
        if (null != resJson && resJson.containsKey("return_code") && "1".equalsIgnoreCase(resJson.getString("return_code"))  && resJson.containsKey("auth_code") && StringUtils.isNotBlank(resJson.getString("auth_code"))) {
            log.debug("[龙亨支付]-[请求支付]-auth_code().发送支付请求，及获取支付请求结果：{}" ,JSON.toJSONString(resJson));
            return resJson.getString("auth_code");
        }else {
            log.error("[龙亨支付]-[请求支付]-auth_code().发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
    }
    
    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
        //2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(api_response_params.get(company_service_id));
        signSrc.append(api_response_params.get(trade_service_id));
        signSrc.append(api_response_params.get(trade_type));
        signSrc.append(api_response_params.get(item_code));
        signSrc.append(api_response_params.get(item_name));
        signSrc.append(api_response_params.get(amount));
        signSrc.append(api_response_params.get(currency));
        signSrc.append(api_response_params.get(timestamp));
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[龙亨支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        Map<String,String> map = new LinkedHashMap<>();
        map.put(auth_code, auth_code(payParam,pay_md5sign));
        map.put(payment_type, payParam.get(payment_type));
        Map<String,String> result = Maps.newHashMap();
        JSONObject jsonObject = null;
        String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), map, String.class, HttpMethod.GET,defaultHeaders).trim();
        if (StringUtils.isBlank(resultStr)) {
            log.error("[龙亨支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
//            log.error("[龙亨支付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
//            throw new PayException("返回空,参数："+JSON.toJSONString(map));
        }
        resultStr = UnicodeUtil.unicodeToString(resultStr);
        jsonObject = JSONObject.parseObject(resultStr);
        //只取正确的值，其他情况抛出异常
        if (null != jsonObject && jsonObject.containsKey("return_code") && "1".equalsIgnoreCase(jsonObject.getString("return_code"))  && jsonObject.containsKey("qrcode_url") && StringUtils.isNotBlank(jsonObject.getString("qrcode_url"))) {
            if (channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("LONGXIANGZHIFU_BANK_WEBWAPAPP_WX_SM")) {
                Document document = Jsoup.parse(jsonObject.getString("qrcode_url"));  //Jsoup.parseBodyFragment(html)
                Element formEl = document.getElementsByTag("form").first();
                if (null == formEl) {
                    log.error("[龙亨支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
                Map<String, String> secondPayParam = HandlerUtil.parseFormElement(formEl);
                HtmlPage endHtml = handlerUtil.getEndHtml(secondPayParam.get("action"), channelWrapper.getAPI_ORDER_ID(), secondPayParam);
                if (null == endHtml || !endHtml.isHtmlPage()) {
                    log.error("[龙亨支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
                String asXml = endHtml.asXml();
                if (StringUtils.isBlank(asXml)) {
                    log.error("[龙亨支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + asXml + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(asXml);
                }
                Elements select = Jsoup.parse(asXml).select("[id=qrImg] [id=imgD]");
                if (null == select || select.size() < 1) {
                    log.error("[龙亨支付]-[请求支付]-3.5.发送支付请求，及获取支付请求结果：" + asXml + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(asXml);
                }
                String src = select.first().attr("src");
                if (StringUtils.isBlank(src)) {
                    log.error("[龙亨支付]-[请求支付]-3.6.发送支付请求，及获取支付请求结果：" + asXml + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(asXml);
                }
                String qr = QRCodeUtil.decodeByBase64(src);
                if (StringUtils.isBlank(qr)) {
                    log.error("[龙亨支付]-[请求支付]-3.6.发送支付请求，及获取支付请求结果：" + asXml + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(asXml);
                }
                result.put(QRCONTEXT, qr);
            }else {
                result.put(handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, jsonObject.getString("qrcode_url"));
            }
//            if (channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("THREERDPAY_BANK_WEBWAPAPP_ZFB_SM")) {
//                result.put(JUMPURL, jsonObject.getString("qrcode_url"));
//            }else {
//                result.put(QRCONTEXT, jsonObject.getString("qrcode_url"));
//            }
            result.put("第三方返回",jsonObject.toString()); //保存全部第三方信息，上面的拆开没必要
        }else {
            log.error("[龙亨支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        List<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[龙亨支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[龙亨支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}