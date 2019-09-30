package dc.pay.business.luobo;

/**
 * ************************
 *
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
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.HttpUtil;
import dc.pay.utils.Result;
import dc.pay.utils.ValidateUtil;
import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequestPayHandler("LUOBO")
public final class LuoBoPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(LuoBoPayRequestHandler.class);
    private static final String parter = "parter";
    private static final String type = "type";
    private static final String value = "value";
    private static final String orderid = "orderid";
    private static final String callbackurl = "callbackurl";
    private static final String orderstatus = "orderstatus";
    private static final String sysnumber = "sysnumber";
    private static final String attach = "attach";
    private static final String sign = "sign";
    private static final String RESPCODE = "respCode";
    private static final String MESSAGE = "message";
    private static final String BARCODE = "barCode";
    private static final String QRCONTEXT = "QrContext";
    private static final String HTMLCONTEXT = "HtmlContext";
    private static final String PARSEHTML = "parseHtml";
    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newTreeMap();
        payParam.put(parter, channelWrapper.getAPI_MEMBERID());
        payParam.put(type, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
        payParam.put(value, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
        payParam.put(orderid, channelWrapper.getAPI_ORDER_ID());
        payParam.put(callbackurl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
        log.debug("[萝卜]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }
    protected String buildPaySign(Map payParam) throws PayException {
        String pay_md5sign = null;
        String paramsStr = String.format("parter=%s&type=%s&value=%s&orderid=%s&callbackurl=%s%s",
                payParam.get(parter),
                payParam.get(type),
                payParam.get(value),
                payParam.get(orderid),
                payParam.get(callbackurl),
                channelWrapper.getAPI_KEY());
        pay_md5sign = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[萝卜]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        try {
            String channel_flag = channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG();
            Result firstPayresult = HttpUtil.get(channelWrapper.getAPI_CHANNEL_BANK_URL(), null, payParam);
            Document document = Jsoup.parse(firstPayresult.getBody().trim());
            if (document.getElementsByTag("form").size() == 0) {
                throw new PayException(firstPayresult.getBody());
            }
            HashMap<String, String> result = Maps.newHashMap();
            if (200 == firstPayresult.getStatusCode() && 0 < firstPayresult.getBody().length()) {
                Element form = document.getElementsByTag("form").first();
                String action = form.attr("action");
                if (action.contains("/WeixinQRCodePay") || action.contains("/AlipayQRCodePay") || action.contains("/JingdongQRCodePay")) {
                    String qrContext = form.select("input#url").first().attr("value");
                    result.put(QRCONTEXT, qrContext);
                    result.put(PARSEHTML, form.html());
                    payResultList.add(result);
                    if (StringUtils.isBlank(qrContext))
                        throw new PayException(SERVER_MSG.REQUEST_PAY_CHANNEL_TOTAL_MONEY_ERROR);
                } else if (action.contains("/QQSaomaQRCodePay")) {
                    String qrContext = form.select("input#url").first().attr("value");
                    result.put(QRCONTEXT, qrContext.replaceAll("\\|", "&"));
                    result.put(PARSEHTML, form.html());
                    payResultList.add(result);
                } else if (action.startsWith("http")) {
                    String body = HandlerUtil.replaceBlank(firstPayresult.getBody());
                    body = body.replaceFirst("/js/jquery_1.7.2_jquery.min.js", HandlerUtil.getDomain(channelWrapper.getAPI_CHANNEL_BANK_URL()) + "js/jquery_1.7.2_jquery.min.js");
                    result.put(HTMLCONTEXT, body);
                    payResultList.add(result);
                } else {
                    throw new PayException(firstPayresult.getBody());
                }
            } else {
                throw new PayException(SERVER_MSG.NOT200);
            }
            if (StringUtils.isBlank(result.get(QRCONTEXT)) && StringUtils.isBlank(result.get(HTMLCONTEXT))) {
                throw new PayException(SERVER_MSG.REQUEST_PAY_CHANNEL_RESULT_ERROR);
            }
        } catch (Exception e) {
            log.error("[萝卜]3.发送支付请求，及获取支付请求结果出错：" + e.getMessage(), e);
            throw new PayException(e.getMessage(), e);
        }
        log.debug("[萝卜]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
        return payResultList;
    }

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
            }
            if (ValidateUtil.requestesultValdata(requestPayResult)) {
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            } else {
                throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[萝卜]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}