package dc.pay.business.ruyifu;

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
import dc.pay.utils.MD5Util;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@ResponsePayHandler("RUYIFU")
public final class RuYiFuPayResponseHandler extends PayResponseHandler {
    private static final Logger log = LoggerFactory.getLogger(RuYiFuPayResponseHandler.class);
    private static final String P_UserId = "P_UserId";
    private static final String P_OrderId = "P_OrderId";
    private static final String P_FaceValue = "P_FaceValue";
    private static final String RESPONSE_PAY_MSG = "ErrCode=0";
    private static final String P_ErrCode = "P_ErrCode";
    private static final String SIGN = "P_PostKey";
    private static final String P_ChannelId = "P_ChannelId";
    private static final String P_PayMoney = "P_PayMoney";


    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String memberId = API_RESPONSE_PARAMS.get(P_UserId);
        String orderId = API_RESPONSE_PARAMS.get(P_OrderId);
        if (StringUtils.isBlank(memberId) || StringUtils.isBlank(orderId))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[如一付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + orderId);
        return orderId;
    }
    @Override
    protected String buildPaySign(Map<String, String> payParam, String api_key) throws PayException {

      //  P_PostKey=md5(P_UserId|P_OrderId|P_CardId|P_CardPass|P_FaceValue|P_ChannelId|P_PayMoney|P_ErrCode|Key)
        String signStr = payParam.get(P_UserId) + "|" + payParam.get(P_OrderId) + "|" + "|" + "|" + payParam.get(P_FaceValue) + "|" + payParam.get(P_ChannelId) + "|" + payParam.get(P_PayMoney)+ "|" + payParam.get(P_ErrCode);
        String pay_md5sign = MD5Util.MD5(signStr + "|" + channelWrapper.getAPI_KEY());
        // pay_md5sign = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[如一付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }
    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amount) throws PayException {
        boolean result = false;
        String payStatusCode = api_response_params.get(P_ErrCode);
        String responseAmount = api_response_params.get(P_FaceValue);
        responseAmount = HandlerUtil.getFen(responseAmount);
        boolean checkAmount = amount.equalsIgnoreCase(responseAmount);
        if (checkAmount && payStatusCode.equalsIgnoreCase("0")) {
            result = true;
        } else {
            log.error("[如一付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + amount);
        }
        log.debug("[如一付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return result;
    }
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(SIGN).equalsIgnoreCase(signMd5);
        log.debug("[如一付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }
    @Override
    protected String responseSuccess() {
        log.debug("[如一付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}