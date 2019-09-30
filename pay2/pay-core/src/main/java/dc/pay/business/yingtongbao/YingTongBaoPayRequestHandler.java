package dc.pay.business.yingtongbao;

/**
 * ************************
 *
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequestPayHandler("YINGTONGBAO")
public final class YingTongBaoPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(YingTongBaoPayRequestHandler.class);
    private static final String WEB_SM_MERCHNO = "merchno";
    private static final String WEB_SM_AMOUNT = "amount";
    private static final String WEB_SM_TRACENO = "traceno";
    private static final String WEB_SM_PAYTYPE = "payType";
    private static final String WEB_SM_SETTLETYPE = "settleType";
    private static final String WEB_SM_SIGNATURE = "signature";
    private static final String WEB_SM_notifyUrl = "notifyUrl";
    private static final String WEB_SM_RESPCODE = "respCode";
    private static final String WEB_SM_MESSAGE = "message";
    private static final String WEB_SM_barCode = "barCode";
    private static final String WAP_SM_MERCHNO = "merchno";
    private static final String WAP_SM_AMOUNT = "amount";
    private static final String WAP_SM_TRACENO = "traceno";
    private static final String WAP_SM_PAYTYPE = "payType";
    private static final String WAP_SM_SIGNATURE = "signature";
    private static final String WAP_SM_NOTIFYURL = "notifyUrl";
    private static final String WAP_SM_RESPCODE = "respCode";
    private static final String WAP_SM_MESSAGE = "message";
    private static final String WEB_BANK_MERCHNO = "merchno";
    private static final String WEB_BANK_AMOUNT = "amount";
    private static final String WEB_BANK_TRACENO = "traceno";
    private static final String WEB_BANK_CHANNEL = "channel";
    private static final String WEB_BANK_BANKCODE = "bankCode";
    private static final String WEB_BANK_SETTLETYPE = "settleType";
    private static final String WEB_BANK_NOTIFYURL = "notifyUrl";
    private static final String WEB_BANK_RETURNURL = "returnUrl";
    private static final String WEB_BANK_SIGNATURE = "signature";
    private static final String QRCONTEXT = "QrContext";
    private static final String HTMLCONTEXT = "HtmlContext";
    private static final String PARSEHTML = "parseHtml";
    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newTreeMap();
        if (channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("_WEB_SM_")) {
            payParam.put(WEB_SM_MERCHNO, channelWrapper.getAPI_MEMBERID());
            payParam.put(WEB_SM_AMOUNT, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(WEB_SM_TRACENO, channelWrapper.getAPI_ORDER_ID());
            payParam.put(WEB_SM_PAYTYPE, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(WEB_SM_notifyUrl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(WEB_SM_SETTLETYPE, "1");
        }
        log.debug("[赢通宝]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map payParam) throws PayException {
        String pay_md5sign = null;
        if (channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("_WEB_SM_")) {
            List paramKeys = MapUtils.sortMapByKeyAsc(payParam);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < paramKeys.size(); i++) {
                sb.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
            }
            sb.append(channelWrapper.getAPI_KEY());
            pay_md5sign = HandlerUtil.getMD5UpperCase(sb.toString()).toLowerCase();
        }
        log.debug("[赢通宝]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        try {
            String channel_flag = channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG();
            String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
            JSONObject responseJsonObject = JSONObject.parseObject(new String(resultStr.getBytes("ISO-8859-1"), "GBK"));
            String respCode = responseJsonObject.getString(WEB_SM_RESPCODE);
            if ("00".equalsIgnoreCase(respCode)) {
            } else {
                throw new PayException(YingTongBaoPayUtil.ServerErrorMsg.getMsgByCode(respCode));
            }
            Result firstPayresult = HttpUtil.get(channelWrapper.getAPI_CHANNEL_BANK_URL(), null, payParam);
            if (200 == firstPayresult.getStatusCode() && 0 < firstPayresult.getBody().length()) {
                if (channel_flag.equalsIgnoreCase("TENPAY") || channel_flag.equalsIgnoreCase("WEIXINWAP")) {
                    if (firstPayresult.getBody().contains("data=") && firstPayresult.getBody().contains("<script>")) {
                        String body = firstPayresult.getBody().trim();
                        String subString = body.substring(body.indexOf("data=") + 5);
                        subString = subString.substring(0, subString.indexOf("<script>"));
                        String tenPayQrContext = HandlerUtil.UrlDecode(subString);
                        HashMap<String, String> result = Maps.newHashMap();
                        result.put(QRCONTEXT, tenPayQrContext);
                        result.put(PARSEHTML, body);
                        payResultList.add(result);
                    }
                } else if (channel_flag.equalsIgnoreCase("WEIXIN") || channel_flag.equalsIgnoreCase("ALIPAY")) {
                    Document document = Jsoup.parse(firstPayresult.getBody());
                    String imgSrc = document.select("div#divQRCode img ").first().attr("src");
                    String wxQRContext = HandlerUtil.UrlDecode(imgSrc.substring(imgSrc.indexOf("data=") + 5));
                    HashMap<String, String> result = Maps.newHashMap();
                    result.put(QRCONTEXT, wxQRContext);
                    result.put(PARSEHTML, document.toString());
                    payResultList.add(result);
                    if (StringUtils.isBlank(wxQRContext)) {
                        throw new PayException(SERVER_MSG.REQUEST_PAY_PARSE_RESULT_ERROR);
                    }
                } else {
                    String body = firstPayresult.getBody().trim();
                    HashMap<String, String> result = Maps.newHashMap();
                    result.put(HTMLCONTEXT, body);
                    payResultList.add(result);
                }
            } else {
                throw new PayException(SERVER_MSG.NOT200);
            }
        } catch (Exception e) {
            log.error("[赢通宝]3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(e.getMessage(), e);
        }
        log.debug("[赢通宝]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
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
        log.debug("[赢通宝]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}