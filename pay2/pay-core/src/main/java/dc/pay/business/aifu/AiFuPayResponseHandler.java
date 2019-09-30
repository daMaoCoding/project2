package dc.pay.business.aifu;

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

@ResponsePayHandler("AIFU")
public final class AiFuPayResponseHandler extends PayResponseHandler {
     private final Logger log = LoggerFactory.getLogger(getClass());
     private static final String RESPONSE_PAY_MSG = "success"; //通知请求返回的http状态为200即认为是通知成功，之后AI不再进行通知，商户无需返回任何值 ,无需success


    private static final String   sign = "sign";     ///: "e3b96b77458049e9c8d30b9dc498688c",
    private static final String   result = "result";     ///: "S",
    private static final String   order_amount = "order_amount";     ///: "10.00",
    private static final String   merchant_no = "merchant_no";     ///: "155805003915",
    private static final String   upstream_settle = "upstream_settle";     ///: "0",
    private static final String   trace_id = "trace_id";     ///: "2710638",
    private static final String   reserve = "reserve";     ///: "20180622181054",
    private static final String   original_amount = "original_amount";     ///: "10.00",
    private static final String   pay_time = "pay_time";     ///: "20180622181312",
    private static final String   order_no = "order_no";     ///: "20180622181054"



    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(order_no);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[爱付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        // =xxx&=xxx&=xxx&=xxx&=xxx&=xxx&=xxx&=xxx&=xxx&key=xxx
        String paramsStr = String.format("merchant_no=%s&order_no=%s&order_amount=%s&original_amount=%s&upstream_settle=%s&result=%s&pay_time=%s&trace_id=%s&reserve=%s&key=%s",
                params.get(merchant_no),
                params.get(order_no),
                params.get(order_amount),
                params.get(original_amount),
                params.get(upstream_settle),
                params.get(result),
                params.get(pay_time),
                params.get(trace_id),
                params.get(reserve),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[爱付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }



    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean checkResult = false;
        String payStatus = api_response_params.get(result);
        String responseAmount =  HandlerUtil.getFen(api_response_params.get(order_amount));
        boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        if (checkAmount && payStatus.equalsIgnoreCase("S")) {
            checkResult = true;
        } else {
            log.error("[爱付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatus + " ,支付金额：" + responseAmount + " ，应支付金额：" + amountDb);
        }
        log.debug("[爱付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + checkResult + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amountDb + ",第三方响应支付成功标志:" + payStatus + " ,计划成功：S");
        return checkResult;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("[爱付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[爱付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}