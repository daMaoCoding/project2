package dc.pay.business.eboopay;

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
import dc.pay.utils.*;
import dc.pay.utils.qr.QRCodeUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequestPayHandler("EBOOPAY")
public final class EbooPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(EbooPayRequestHandler.class);
    static final String RESULT_QRCODEURL = "QRcodeURL";
    static final String RESULT_ORDERID = "orderId";
    static final String RESULT_AMOUNT = "amount";
    static final String RESULT_CREATETIME = "CreateTime";
    static final String RESULT_BANKCODE = "bankcode";
    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new HashMap<String, String>() {
            {
                put("pay_memberid", channelWrapper.getAPI_MEMBERID());
                put("pay_amount", HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put("pay_notifyurl", channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put("pay_applydate", HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "yyyy-MM-dd HH:mm:ss"));
                put("pay_bankcode", channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put("pay_orderid", channelWrapper.getAPI_ORDER_ID().concat(channelWrapper.getAPI_MEMBERID()));
            }
        };
        log.debug("[易宝支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }
    @Override
    protected String buildPaySign(Map payParam) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(payParam);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            sb.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
        }
        sb.append("key=" + channelWrapper.getAPI_KEY());
        String pay_md5sign = HandlerUtil.getMD5UpperCase(sb.toString());
        log.debug("[易宝支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        try {
            if(HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper)){
                StringBuffer htmlContent = HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
                Map secondPayResult = Maps.newHashMap();
                secondPayResult.put(HTMLCONTEXT,htmlContent.toString());
                payResultList.add(secondPayResult);
            }else{
                Result firstPayresult = HttpUtil.post(channelWrapper.getAPI_CHANNEL_BANK_URL(), null, payParam, "UTF-8");
                if (200 == firstPayresult.getStatusCode() && 0 < firstPayresult.getBody().length()) {
                    Document document = Jsoup.parse(firstPayresult.getBody());
                    Elements forms = document.body().getElementsByTag("form");
                    if (1 == forms.size()) {
                        Element form = forms.get(0);
                        String method = form.attr("method");
                        String action = form.attr("action");
                        HashMap<String, String> secondPayParam = Maps.newHashMap();
                        Elements inputs = form.getElementsByTag("input");
                        if (inputs.size() > 0) {
                            Map firstPayResult = Maps.newHashMap();
                            for (Element input : inputs) {
                                String name = input.attr("name");
                                String value = input.attr("value");
                                secondPayParam.put(name, value);
                            }
                            payResultList.add(secondPayParam);
                        }
                        if (!StringUtils.isBlank(action) && action.startsWith("http")) {
                            Result secondPayresult = HttpUtil.post(action, null, secondPayParam, "UTF-8");
                            if (200 == secondPayresult.getStatusCode() && 0 < secondPayresult.getBody().length()) {
                                Map secondPayResult = Maps.newHashMap();
                                Document secondDocument = Jsoup.parse(secondPayresult.getBody());
                                Element qrImg = secondDocument.select("div.QRcode img").first();
                                Element amount = secondDocument.select("p.txt-b span.txt-orange").first();
                                Element timeAfter = secondDocument.select("div.notice  span.txt-orange").first();
                                Element orderNum = secondDocument.select("div.order-num").first();
                                Element orderTime = secondDocument.select("div.order-time").first();
                                if (null != qrImg) {
                                    secondPayResult.put(RESULT_QRCODEURL, qrImg.attr("src"));
                                    secondPayResult.put(RESULT_QRCODEURL + "_Width", qrImg.attr("width"));
                                    secondPayResult.put(RESULT_QRCODEURL + "_Height", qrImg.attr("height"));
                                }
                                if (null != amount) {
                                    secondPayResult.put(RESULT_AMOUNT, amount.text());
                                }
                                if (null != timeAfter) {
                                    secondPayResult.put(RESULT_QRCODEURL + "_TimeAfter", timeAfter.text());
                                }
                                if (null != orderNum) {
                                    secondPayResult.put(RESULT_ORDERID, (orderNum.text().substring(orderNum.text().indexOf(":") + 1)).replaceFirst(channelWrapper.getAPI_MEMBERID(), ""));
                                }
                                if (null != orderTime) {
                                    secondPayResult.put(RESULT_CREATETIME, orderTime.text().substring(orderTime.text().indexOf(":") + 1));
                                }
                                payResultList.add(secondPayResult);
                            }
                        }
                    }
                } else {
                    HashMap<String, Header> headerAll = firstPayresult.getHeaderAll();
                    if (!headerAll.isEmpty()) {
                        String payErrorCode = "";
                        if (null != headerAll.get("Location")) {
                            String locationValue = headerAll.get("Location").getValue();
                            if (locationValue.indexOf("respCode=") != -1) {
                                payErrorCode = (locationValue.substring(locationValue.indexOf("respCode="), locationValue.length())).replaceAll("respCode=", "");
                                String msgByCode = EbooPayUtil.ServerErrorMsg.getMsgByCode(payErrorCode);
                                log.error("[易宝支付]解析结果出错，易宝提示：" + msgByCode);
                                throw new PayException(msgByCode);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("[易宝支付]3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(e.getMessage(), e);
        }
        log.debug("[易宝支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
        return payResultList;
    }
    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (!resultListMap.isEmpty() && resultListMap.size() == 2) {
            for (Map<String, String> resultMap : resultListMap) {
                for (Map.Entry<String, String> entry : resultMap.entrySet()) {
                    if (RESULT_AMOUNT.equalsIgnoreCase(entry.getKey()))
                        requestPayResult.setRequestPayamount(HandlerUtil.getFen(entry.getValue()));
                    if (RESULT_ORDERID.equalsIgnoreCase(entry.getKey()))
                        requestPayResult.setRequestPayOrderId(entry.getValue());
                    if (RESULT_CREATETIME.equalsIgnoreCase(entry.getKey()))
                        requestPayResult.setRequestPayOrderCreateTime(entry.getValue());
                    if (RESULT_QRCODEURL.equalsIgnoreCase(entry.getKey()))
                        requestPayResult.setRequestPayQRcodeURL(entry.getValue());
                }
            }
            requestPayResult.setRequestPayChannelBankName(channelWrapper.getAPI_CHANNEL_BANK_NAME());
            String requestPayQRcodeContent = QRCodeUtil.decodeByUrl(requestPayResult.getRequestPayQRcodeURL());
            requestPayResult.setRequestPayQRcodeContent(requestPayQRcodeContent);
        } else if(!resultListMap.isEmpty() && resultListMap.size() == 1){
            buildResult(resultListMap.get(0), channelWrapper,requestPayResult);
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }

        if (ValidateUtil.requestesultValdata(requestPayResult)) {
            requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
        }

        log.debug("[易宝支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}