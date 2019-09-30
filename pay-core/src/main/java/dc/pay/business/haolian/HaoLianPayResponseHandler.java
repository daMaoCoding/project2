package dc.pay.business.haolian;

/**
 * ************************
 * @author tony 3556239829
 */

import com.alibaba.fastjson.JSON;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@ResponsePayHandler("HAOLIAN")
public final class HaoLianPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

             private static final String RESPONSE_PAY_MSG = "success";
             private static final String  customerid     ="customerid";  //"10891",
             private static final String  paytype        ="paytype";     //"alipay",
             private static final String  remark         ="remark";      //"",
             private static final String  sdorderno      ="sdorderno";   //"HAOLIAN_ZFB_SM-dCGHZ",
             private static final String  sdpayno        ="sdpayno";     //"2017111909424252147",
             private static final String  sign           ="sign";        //"4195f18a4b3fbdee56be1043265e9103",
             private static final String  status         ="status";      //"1",
             private static final String  total_fee      ="total_fee";   //"0.01"

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String memberId = API_RESPONSE_PARAMS.get(customerid);
        String orderId = API_RESPONSE_PARAMS.get(sdorderno);
        if (StringUtils.isBlank(memberId) || StringUtils.isBlank(orderId))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[豪联]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + orderId);
        return orderId;
    }

    @Override
    protected String buildPaySign(Map<String, String> payParam, String api_key) throws PayException {
        String paramsStr = String.format("customerid=%s&status=%s&sdpayno=%s&sdorderno=%s&total_fee=%s&paytype=%s&%s",
                payParam.get(customerid),
                payParam.get(status),
                payParam.get(sdpayno),
                payParam.get(sdorderno),
                payParam.get(total_fee),
                payParam.get(paytype),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();

        log.debug("[豪联]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amount) throws PayException {
        boolean result = false;
        String payStatusCode = api_response_params.get(status);
        String responseAmount = api_response_params.get(total_fee);
        boolean checkAmount = amount.equalsIgnoreCase(HandlerUtil.getFen(responseAmount));
        if (checkAmount && payStatusCode.equalsIgnoreCase("1")) {
            result = true;
        } else {
            log.error("[豪联]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + amount);
        }
        log.debug("[豪联]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("[豪联]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[豪联]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}