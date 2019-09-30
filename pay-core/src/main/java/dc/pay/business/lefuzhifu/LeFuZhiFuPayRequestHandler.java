package dc.pay.business.lefuzhifu;

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
import dc.pay.utils.ValidateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Cobby
 * June 22, 2019
 */
@RequestPayHandler("LEFUZHIFU")
public final class LeFuZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(LeFuZhiFuPayRequestHandler.class);

    private static final String merchantId = "merchantId";//    商户号
    private static final String payment    = "payment";   //    支付通道
    private static final String payMoney   = "payMoney";  //    金额，分为单位
    private static final String orderNo    = "orderNo";   //    商户订单号
    private static final String orderName  = "orderName"; //    商品名称
    private static final String notifyUrl  = "notifyUrl"; //    回调通知地址
    private static final String returnUrl  = "returnUrl"; //    跳转页面地址
    private static final String version    = "version";   //    系统版本号, 3.0
    private static final String mode       = "mode";      //    值：form 不参与签名

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merchantId, channelWrapper.getAPI_MEMBERID());
                put(payment, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(payMoney, channelWrapper.getAPI_AMOUNT());
                put(orderNo, channelWrapper.getAPI_ORDER_ID());
                put(orderName, "name");
                put(notifyUrl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(returnUrl, channelWrapper.getAPI_WEB_URL());
                put(version, "3.0");
                put(mode, "form");
            }
        };
        log.debug("[乐富支付]-[请求支付]-1.组装请求参数完成：{}", JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
        // sign=md5(merchantId&payment&payMoney&orderNo&orderName& notifyUrl&returnUrl&version&key) —-32位大写密文
        String paramsStr = String.format("%s&%s&%s&%s&%s&%s&%s&%s&%s",
                api_response_params.get(merchantId),
                api_response_params.get(payment),
                api_response_params.get(payMoney),
                api_response_params.get(orderNo),
                api_response_params.get(orderName),
                api_response_params.get(notifyUrl),
                api_response_params.get(returnUrl),
                api_response_params.get(version),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[乐富支付]-[请求支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);

        HashMap<String, String> result = Maps.newHashMap();
        try {

            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam).toString());

        } catch (Exception e) {
            log.error("[乐富支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null", e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[乐富支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));
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
        log.debug("[乐富支付]-[请求支付]-4.处理请求响应成功：{}", JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}