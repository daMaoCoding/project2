package dc.pay.business.baifutong;

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

@RequestPayHandler("BAIFUTONG")
public final class BaiFuTongPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(BaiFuTongPayRequestHandler.class);
    private static final String pay_type = "pay_type";
    private static final String mch_id = "mch_id";
    private static final String order_id = "order_id";
    private static final String channel_id = "channel_id";
    private static final String pay_amount = "pay_amount";
    private static final String name = "name";
    private static final String client_ip = "client_ip";
    private static final String bank_code = "bank_code";
    private static final String is_qrimg = "is_qrimg";
    private static final String is_sdk = "is_sdk";
    private static final String ts = "ts";
    private static final String sign = "sign";
    private static final String notify_url = "notify_url";
    private static final String code = "code";
    private static final String msg = "msg";
    private static final String pay_url = "pay_url";
    private static final String real_amount = "real_amount";
    private static final String status = "status";
    private static final String order_no = "order_no";
    private static final String finish_time = "finish_time";
    private static final String ext = "ext";
    private static final String QRCONTEXT = "QrContext";
    private static final String HTMLCONTEXT = "HtmlContext";
    private static final String PARSEHTML = "parseHtml";
    public static final String WY_ = "_WY_";
    public static final String TWO = "2";
    public static final String PAY = "Pay";
    public static final String ZERO = "0";
    public static final String REGEX = ",";
    public static final String SM_ = "_SM_";
    public static final String BANK_WAP = "BANK_WAP";
    public static final String BANK_WEB = "BANK_WEB";
    public static final String ANDp = "&";
    public static final String KEY = "key=";
    public static final String EQU = "=";
    public static final String PAY_URL = "pay_url";
    public static final String CODE = "code";
    public static final String SM = "_SM";
    public static final String CODE1 = "code";
    public static final String CODE_URL = "code_url";
    public static final String FIRST_RESULT = "firstResult";
    public static final String SECONDE_RESULT = "secondeResult";
    public static final String STRING = "0";
    public static final String TENPAY = "TENPAY";
    public static final String WEIXINWAP = "WEIXINWAP";
    public static final String SCRIPT = "<script>";
    public static final String DATA = "data=";
    public static final String DATA1 = "data=";
    public static final String SCRIPT1 = "<script>";
    public static final String WEIXIN = "WEIXIN";
    public static final String ALIPAY = "ALIPAY";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newTreeMap();
        if (!channelWrapper.getAPI_CHANNEL_BANK_NAME().contains(WY_)) {
            payParam.put(pay_type, TWO);
            payParam.put(mch_id, channelWrapper.getAPI_MEMBERID());
            payParam.put(order_id, channelWrapper.getAPI_ORDER_ID());
            payParam.put(channel_id, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(pay_amount, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(notify_url, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(name, PAY);
            payParam.put(client_ip, HandlerUtil.getRandomIp(channelWrapper));
            payParam.put(bank_code, "");
            payParam.put(is_qrimg, ZERO);
            payParam.put(is_sdk, ZERO);
            payParam.put(ts, String.valueOf(System.currentTimeMillis()));
        } else if (channelWrapper.getAPI_CHANNEL_BANK_NAME().contains(WY_)) {
            payParam.put(pay_type, TWO);
            payParam.put(mch_id, channelWrapper.getAPI_MEMBERID());
            payParam.put(order_id, channelWrapper.getAPI_ORDER_ID());
            payParam.put(channel_id, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(REGEX)[0]);
            payParam.put(bank_code, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(REGEX)[1]);
            payParam.put(pay_amount, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(notify_url, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(name, PAY);
            payParam.put(client_ip, HandlerUtil.getRandomIp(channelWrapper));
            payParam.put(is_qrimg, ZERO);
            payParam.put(is_sdk, ZERO);
            payParam.put(ts, String.valueOf(System.currentTimeMillis()));
        }
        log.debug("[百付通]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map payParam) throws PayException {
        String pay_md5sign = null;
        if (channelWrapper.getAPI_CHANNEL_BANK_NAME().contains(SM_) || channelWrapper.getAPI_CHANNEL_BANK_NAME().contains(BANK_WAP) || channelWrapper.getAPI_CHANNEL_BANK_NAME().contains(BANK_WEB)) {
            List paramKeys = MapUtils.sortMapByKeyAsc(payParam);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < paramKeys.size(); i++) {
                if (StringUtils.isBlank(String.valueOf(payParam.get(paramKeys.get(i)))))
                    continue;
                sb.append(paramKeys.get(i)).append(EQU).append(payParam.get(paramKeys.get(i))).append(ANDp);
            }
            sb.append(KEY + channelWrapper.getAPI_KEY());
            pay_md5sign = HandlerUtil.getMD5UpperCase(sb.toString());
        }
        log.debug("[百付通]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        try {
            String channel_flag = channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG();
            String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim(); //第一次结果
            JSONObject responseJsonObject = JSONObject.parseObject(resultStr);
            String pay_url = responseJsonObject.getString(PAY_URL);
            String code = responseJsonObject.getString(CODE);
            if ("0".equalsIgnoreCase(code)) {
                if (channelWrapper.getAPI_CHANNEL_BANK_NAME().contains(SM)) {
                    if (StringUtils.isNotBlank(pay_url)) {
                        String secondResult = HttpUtil.get(pay_url, null, null).getBody();
                        if (StringUtils.isNotBlank(secondResult)) {
                            JSONObject secondResultJsonObject = JSONObject.parseObject(secondResult);
                            String code2 = secondResultJsonObject.getString(CODE1);
                            if (STRING.equalsIgnoreCase(code2)) {
                                String code_url = secondResultJsonObject.getString(CODE_URL);
                                HashMap<String, String> finalResult = Maps.newHashMap();
                                finalResult.put(QRCONTEXT, code_url);
                                finalResult.put(FIRST_RESULT, resultStr);
                                finalResult.put(SECONDE_RESULT, secondResult);
                                payResultList.add(finalResult);
                                return payResultList;
                            }
                        }
                    }
                }
            } else {
                throw new PayException(BaiFuTongPayUtil.ServerErrorMsg.getMsgByCode(code));
            }
            Result firstPayresult = HttpUtil.get(channelWrapper.getAPI_CHANNEL_BANK_URL(), null, payParam);
            if (200 == firstPayresult.getStatusCode() && 0 < firstPayresult.getBody().length()) {
                if (channel_flag.equalsIgnoreCase(TENPAY) || channel_flag.equalsIgnoreCase(WEIXINWAP)) {
                    if (firstPayresult.getBody().contains(DATA) && firstPayresult.getBody().contains(SCRIPT)) {
                        String body = firstPayresult.getBody().trim();
                        String subString = body.substring(body.indexOf(DATA1) + 5);
                        subString = subString.substring(0, subString.indexOf(SCRIPT1));
                        String tenPayQrContext = HandlerUtil.UrlDecode(subString);
                        HashMap<String, String> result = Maps.newHashMap();
                        result.put(QRCONTEXT, tenPayQrContext);
                        result.put(PARSEHTML, body);
                        payResultList.add(result);

                    }
                } else if (channel_flag.equalsIgnoreCase(WEIXIN) || channel_flag.equalsIgnoreCase(ALIPAY)) {
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
            log.error("[百付通]3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(e.getMessage(), e);
        }
        log.debug("[百付通]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
        return payResultList;
    }

    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (null != resultListMap && !resultListMap.isEmpty() && resultListMap.size() == 1) {
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
            if (ValidateUtil.requestesultValdata(requestPayResult)) {
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            } else {
                throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[百付通]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }

}
