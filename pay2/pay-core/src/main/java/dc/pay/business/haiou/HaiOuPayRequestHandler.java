package dc.pay.business.haiou;

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
import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.util.*;

@RequestPayHandler("HAIOU")
public final class HaiOuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(HaiOuPayRequestHandler.class);
    private static final String USERID = "userid";
    private static final String ORDERID = "orderid";
    private static final String PRICE = "price";
    private static final String PAYVIA = "payvia";
    private static final String NOTIFY = "notify";
    private static final String CALLBACK = "callback";
    private static final String TIMESPAN = "timespan";
    private static final String FORMAT = "format";
    private static final String SIGN = "sign";
    private static final String QRCONTEXT = "QrContext";
    private static final String HTMLCONTEXT = "HtmlContext";
    private static final String PARSEHTML = "parseHtml";
    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(USERID, channelWrapper.getAPI_MEMBERID());
                put(ORDERID, channelWrapper.getAPI_ORDER_ID());
                put(PRICE, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(PAYVIA, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(NOTIFY, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(CALLBACK, "http://www.baidu.com");
                put(TIMESPAN, HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "yyyyMMddHHmmss"));
                put(TIMESPAN, HandlerUtil.getDateTimeByMilliseconds(String.valueOf(System.currentTimeMillis()), "yyyyMMddHHmmss"));
                put(FORMAT, "json");
            }
        };
        log.debug("[海鸥]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }
    protected String buildPaySign(Map payParam) throws PayException {
        String paramsStr = String.format("userid=%s&orderid=%s&price=%s&payvia=%s&notify=%s&callback=%s&key=%s",
                payParam.get(USERID),
                payParam.get(ORDERID),
                payParam.get(PRICE),
                payParam.get(PAYVIA),
                payParam.get(NOTIFY),
                payParam.get(CALLBACK),
                channelWrapper.getAPI_KEY());
        String pay_md5sign = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        pay_md5sign += channelWrapper.getAPI_KEY();
        pay_md5sign = HandlerUtil.getMD5UpperCase(pay_md5sign).toLowerCase();
        log.debug("[海鸥]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();  //详细返回结果
        try {
            String channel_flag = channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG();
            String firstPayresult = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
            if (firstPayresult.length() < 10) {
                log.error("[海鸥]3.发送支付请求，及获取支付请求结果：" + firstPayresult + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(firstPayresult);
            }
            if (0 == Jsoup.parse(firstPayresult).select("a").size()) {
                throw new PayException(firstPayresult);
            }
            String jumpToUrl = Jsoup.parse(firstPayresult).select("a").first().attr("href");
            String secondPayresult = RestTemplateUtil.getRestTemplate().getForObject(jumpToUrl, String.class);
            String state = JSON.parseObject(secondPayresult).getString("state");
            String img = JSON.parseObject(secondPayresult).getString("img");
            if (StringUtils.isNotBlank(state) && "1".equalsIgnoreCase(state) && StringUtils.isNotBlank(img)) {
                HashMap<String, String> result = Maps.newHashMap();
                result.put(QRCONTEXT, img);
                result.put(PARSEHTML, secondPayresult);
                payResultList.add(result);
            }
        } catch (Exception e) {
            log.error("[海鸥]3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(e.getMessage(), e);
        }
        log.debug("[海鸥]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
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
        log.debug("[海鸥]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}