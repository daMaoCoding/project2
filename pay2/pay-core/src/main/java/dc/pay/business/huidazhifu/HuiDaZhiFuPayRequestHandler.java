package dc.pay.business.huidazhifu;

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

import java.util.*;

@RequestPayHandler("HUIDAZHIFU")
public final class HuiDaZhiFuPayRequestHandler extends PayRequestHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());
    static final String MOBAOPAY_API_VERSION = "1.0.0.0";
    static final String MOBAOPAY_MERCHPARAM = "3556239829";
    static final String MOBAOPAY_TRADESUMMARY = "3556239829";
    static final String HTML_CONTENT_KEY = "HTML_CONTENT_KEY";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> bankFlags = HuiDaZhiFuPayUtil.parseBankFlag(channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
        if (!bankFlags.isEmpty() && bankFlags.size() == 3) {
            String apiName = bankFlags.get("apiName");
            String choosePayType = bankFlags.get("choosePayType");
            String bankCode = bankFlags.get("bankCode");
            Map<String, String> paramsMap = new HashMap<String, String>();
            paramsMap.put("apiName", apiName);
            paramsMap.put("apiVersion", MOBAOPAY_API_VERSION);
            paramsMap.put("platformID", channelWrapper.getAPI_MEMBERID());
            paramsMap.put("merchNo", channelWrapper.getAPI_MEMBERID());
            paramsMap.put("orderNo", channelWrapper.getAPI_ORDER_ID());
           // paramsMap.put("orderNo", new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
            paramsMap.put("tradeDate", HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "yyyyMMdd"));
            paramsMap.put("amt", HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            paramsMap.put("merchUrl", channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            paramsMap.put("merchParam", MOBAOPAY_MERCHPARAM);
            paramsMap.put("tradeSummary", MOBAOPAY_TRADESUMMARY);
            paramsMap.put("bankCode", bankCode);
            paramsMap.put("choosePayType", choosePayType);
            log.debug("[汇达支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(paramsMap));
            return paramsMap;
        } else {
            log.debug("[汇达支付]-[请求支付]-1.组装请求参数出错：参数接口名，支付方式，银行代码解析为空或个数不对,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",Flag:" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG() + ",解析结果：" + JSON.toJSONString(bankFlags));
            throw new PayException(SERVER_MSG.REQUEST_PAY_INFO__ERROR);
        }
    }
    protected String buildPaySign(Map payParam) throws PayException {
        String paramsStr = HuiDaZhiFuPayUtil.generatePayRequest(payParam);
        String signMsg = HuiDaZhiFuPayUtil.signData(paramsStr, channelWrapper.getAPI_KEY());
        log.debug("[汇达支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMsg));
        return signMsg;
    }
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        if (null == payParam || !payParam.containsKey("choosePayType")) {
            log.error("[汇达支付]发送请求前检查参数错误，参数中不包含支付方式choosePayType，" + JSON.toJSONString(payParam));
            throw new PayException(SERVER_MSG.REQUEST_PAY_INFO__ERROR);
        }
        LinkedList<Map<String, String>> payResultList = Lists.newLinkedList();
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        String epayUrl = channelWrapper.getAPI_CHANNEL_BANK_URL();
        String choosePayType = payParam.get("choosePayType");
        Result firstPayresult=null;
        if (StringUtils.isNotBlank(choosePayType) && "4".equalsIgnoreCase(choosePayType) || "5".equalsIgnoreCase(choosePayType) || "6".equalsIgnoreCase(choosePayType) || "8".equalsIgnoreCase(choosePayType) || "17".equalsIgnoreCase(choosePayType)|| "11".equalsIgnoreCase(choosePayType)) {
            try {
                firstPayresult = HttpUtil.post(epayUrl, null, payParam, "UTF-8");
                Document document = Jsoup.parse(firstPayresult.getBody());

                 Map<String, String> secondPayParam=  HandlerUtil.parseFormElement(document.getElementsByTag("form").first());

                payResultList.add(secondPayParam);
                if (null != secondPayParam && "/standard/payment/cashier.cgi".equalsIgnoreCase(secondPayParam.get("action"))) {
                    Result result2 = HandlerUtil.sendToThreadPayServ(epayUrl,secondPayParam, channelWrapper.getAPI_CHANNEL_BANK_URL());
                    document = Jsoup.parse(result2.getBody());
                    Element bodyEl = document.getElementsByTag("body").first();
                    Element payForm = null;
                    Map<String, String> payFormMap = null;
                    String payUrlForManagerCGI = null;
                    String payForManagerCGIResultJsonStr = null;
                    JSONObject payForManagerCGIResultJsonObj = null;

                    //解析表单
                    Elements qqQrPayForm = bodyEl.getElementsByTag("form").select("form[name='qqPayForm']");
                    Elements wxQrPayForm = bodyEl.getElementsByTag("form").select("form[name='wxqrcPayForm']");
                    Elements aliQrPayForm = bodyEl.getElementsByTag("form").select("form[name='aliqrcPayForm']");
                    Elements jdQrPayForm = bodyEl.getElementsByTag("form").select("form[name='jdPayForm']");
                    Elements ylQrPayForm = bodyEl.getElementsByTag("form").select("form[name='ylPayForm']");

                    if (qqQrPayForm.size() == 1) {
                        payForm = qqQrPayForm.first();
                        payFormMap = HandlerUtil.parseFormElement(payForm);
                        payUrlForManagerCGI = HuiDaZhiFuPayUtil.buildQqUrlForManagerCGI(payFormMap);
                    } else if (aliQrPayForm.size() == 1) {
                        payForm = aliQrPayForm.first();
                        payFormMap = HandlerUtil.parseFormElement(payForm);
                        payUrlForManagerCGI = HuiDaZhiFuPayUtil.buildZfbUrlForManagerCGI(payFormMap);
                    } else if (wxQrPayForm.size() == 1) {
                        payForm = wxQrPayForm.first();
                        payFormMap = HandlerUtil.parseFormElement(payForm);
                        payUrlForManagerCGI = HuiDaZhiFuPayUtil.buildWxUrlForManagerCGI(payFormMap);
                    }else if (jdQrPayForm.size() == 1) {
                        payForm = jdQrPayForm.first();
                        payFormMap = HandlerUtil.parseFormElement(payForm);
                        payUrlForManagerCGI = HuiDaZhiFuPayUtil.buildJdUrlForManagerCGI(payFormMap);
                    }else if (ylQrPayForm.size() == 1) {
                        payForm = ylQrPayForm.first();
                        payFormMap = HandlerUtil.parseFormElement(payForm);
                        payUrlForManagerCGI = HuiDaZhiFuPayUtil.buildYlUrlForManagerCGI(payFormMap);
                    }




                    if (StringUtils.isNotBlank(payUrlForManagerCGI)) {
                        payForManagerCGIResultJsonStr = HttpUtil.get(payUrlForManagerCGI, null, null).getBody();
                        payForManagerCGIResultJsonObj = JSON.parseObject(payForManagerCGIResultJsonStr);
                        if ("00".equalsIgnoreCase(payForManagerCGIResultJsonObj.get("respCode").toString())) {
                            String codeUrl = payForManagerCGIResultJsonObj.getString("codeUrl");
                            String qr_Content = new String(Base64.getDecoder().decode(codeUrl));
                            HashMap<String, String> resultMap = Maps.newHashMap();
                            resultMap.put("qr_Content", qr_Content);
                            resultMap.put("qr_Detial", payForManagerCGIResultJsonObj.toJSONString());
                            payResultList.add(resultMap);
                        } else {
                            log.error("[汇达支付]-解析扫码结果出错，第三方支付结果页面不正常，订单号：" + channelWrapper.getAPI_ORDER_ID() + ",通道名称：" + channelWrapper.getAPI_CHANNEL_BANK_NAME() + ",postUrl:" + epayUrl + ",结果： " + payForManagerCGIResultJsonObj.toJSONString());
                            throw new PayException(payForManagerCGIResultJsonObj.toJSONString());
                        }
                    } else {
                        log.error("[汇达支付]-解析扫码结果出错，第三方支付结果页面不正常，提交地址不明确,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",通道名称：" + channelWrapper.getAPI_CHANNEL_BANK_NAME() + ",postUrl:" + epayUrl + ",payForManagerCGIResultJsonObj" + payUrlForManagerCGI+",第三方返回："+bodyEl.toString());
                        throw new PayException(bodyEl.toString());
                    }
                }
            } catch (Exception e) {

                log.error("[汇达支付]3.发送支付请求，及获取支付请求结果-获取微信||支付宝并解析出错，订单号：" + channelWrapper.getAPI_ORDER_ID() + ",通道名称：" + channelWrapper.getAPI_CHANNEL_BANK_NAME() + ",postUrl:" + epayUrl + ",payForManagerCGIResultJsonObj" + e.getMessage(), e);
                throw new PayException(e.getMessage());
            }
        } else {
            StringBuffer sbHtml = new StringBuffer();
            sbHtml.append("<form id='mobaopaysubmit' name='mobaopaysubmit' action='" + epayUrl + "' method='post'>");
            for (Map.Entry<String, String> entry : payParam.entrySet()) {
                sbHtml.append("<input type='hidden' name='" + entry.getKey() + "' value='" + entry.getValue() + "'/>");
            }
            sbHtml.append("</form>");
            sbHtml.append("<script>document.forms['mobaopaysubmit'].submit();</script>");
            Map result = Maps.newHashMap();
            result.put(HTML_CONTENT_KEY, sbHtml.toString());
            payResultList.add(result);
        }
        log.debug("[亿汇付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
        return payResultList;
    }
    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.ERROR.getCodeValue());
        requestPayResult.setRequestPayamount(channelWrapper.getAPI_AMOUNT());
        requestPayResult.setRequestPayOrderId(channelWrapper.getAPI_ORDER_ID());
        requestPayResult.setRequestPayChannelBankName(channelWrapper.getAPI_CHANNEL_BANK_NAME());
        requestPayResult.setRequestPayOrderCreateTime(channelWrapper.getAPI_OrDER_TIME());
        requestPayResult.setDetail(resultListMap);
        if (null != resultListMap && !resultListMap.isEmpty() && resultListMap.size() == 1) {
            Map<String, String> result = resultListMap.get(0);
            requestPayResult.setRequestPayHtmlContent(result.get(HTML_CONTENT_KEY));
        } else if (null != resultListMap && !resultListMap.isEmpty() && resultListMap.size() == 2) {
            Map<String, String> result = resultListMap.get(1);
            requestPayResult.setRequestPayQRcodeContent(result.get("qr_Content"));
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
        }
        if (ValidateUtil.requestesultValdata(requestPayResult)) {
            requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
        }
        log.debug("[汇达支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}