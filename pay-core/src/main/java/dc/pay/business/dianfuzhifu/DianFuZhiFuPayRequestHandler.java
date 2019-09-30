package dc.pay.business.dianfuzhifu;

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
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.RsaUtil;
import dc.pay.utils.ValidateUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author Cobby
 * July 16, 2019
 */
@RequestPayHandler("DIANFUZHIFU")
public final class DianFuZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(DianFuZhiFuPayRequestHandler.class);

    private static final String merchNo    = "merchNo";    //商户号 merchNo
    private static final String orderNo    = "orderNo";    //订单号
    private static final String outChannel = "outChannel"; //支付渠道 ali支付宝扫码 wx微信扫码
    private static final String userId     = "userId";     //用户标志 userId
    private static final String title      = "title";      //订单标题 title
    private static final String product    = "product";    //产品名称 product
    private static final String amount     = "amount";     //支付金额 单位 元 amount
    private static final String currency   = "currency";   //币种 currency
    private static final String returnUrl  = "returnUrl";  //前端返回地址 returnUrl
    private static final String notifyUrl  = "notifyUrl";  //后台通知地址 notifyUrl
    private static final String reqTime    = "reqTime";    //请求时间 reqTime
    private static final String acctType   = "acctType";   //对公 acctType


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merchNo, channelWrapper.getAPI_MEMBERID());
                put(orderNo, channelWrapper.getAPI_ORDER_ID());
                put(outChannel, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(userId, "user" + HandlerUtil.randomStr(6));
                put(title, "chongzhi");
                put(product, "chongzhi");
                put(amount, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(currency, "CNY");
                put(returnUrl, channelWrapper.getAPI_WEB_URL());
                put(notifyUrl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(reqTime, new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
                put(acctType, "1");
            }
        };
        String context = null;
        try {
            context = RsaUtil.encryptToBase64(JSON.toJSONString(payParam), channelWrapper.getAPI_PUBLIC_KEY());
        } catch (Exception e) {
            e.printStackTrace();
        }
        payParam.put("context", context);
        log.debug("[电付支付]-[请求支付]-1.组装请求参数完成：{}", JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
        String signMd5 = Md5Util.sign(api_response_params.get("context"), channelWrapper.getAPI_KEY(), "UTF-8");
        log.debug("[电付支付]-[请求支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        JSONObject jo = new JSONObject();
        jo.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        jo.put("context", payParam.get("context"));
        String payParamJson = jo.toJSONString();

        HashMap<String, String> result = Maps.newHashMap();
        try {
            String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParamJson);

            JSONObject resJson;
            try {
                resJson = JSONObject.parseObject(resultStr);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("[电付支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            //只取正确的值，其他情况抛出异常
            if (null != resJson && resJson.containsKey("msg") && "success".equalsIgnoreCase(resJson.getString("msg"))
                    && resJson.containsKey("context") && StringUtils.isNotBlank(resJson.getString("context"))) {
                byte[]     base64decodedBytes = Base64.getDecoder().decode(resJson.getString("context"));
                String     resJsonStr         = new String(base64decodedBytes, "utf-8");
                JSONObject jsonObject         = JSONObject.parseObject(resJsonStr);
                String     code_url           = jsonObject.getString("payurl");
                String     decode             = decode(code_url);
                result.put(handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, decode);
            } else {
                log.error("[电付支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }

        } catch (Exception e) {
            log.error("[电付支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null", e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[电付支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));
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
        log.debug("[电付支付]-[请求支付]-4.处理请求响应成功：{}", JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }


    public static String decode(String url) {
        try {
            String prevURL   = "";
            String decodeURL = url;
            while (!prevURL.equals(decodeURL)) {
                prevURL = decodeURL;
                decodeURL = URLDecoder.decode(decodeURL, "UTF-8");
            }
            return decodeURL;
        } catch (UnsupportedEncodingException e) {
            return "Issue while decoding" + e.getMessage();
        }
    }
}