package dc.pay.business.huidazhifu;

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

@ResponsePayHandler("HUIDAZHIFU")
public final class HuiDaZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private static final String apiName = "apiName";
    private static final String notifyTime = "notifyTime";
    private static final String tradeAmt = "tradeAmt";
    private static final String merchNo = "merchNo";
    private static final String merchParam = "merchParam";
    private static final String orderNo = "orderNo";
    private static final String tradeDate = "tradeDate";
    private static final String accNo = "accNo";
    private static final String accDate = "accDate";
    private static final String orderStatus = "orderStatus";
    private static final String signMsg = "signMsg";
    private static final String notifyType = "notifyType";
    private static final String RESPONSE_PAY_MSG = "SUCCESS";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty()) {
            log.error("[汇达支付]1.获取支付通道响应信息中的订单号错误，" + JSON.toJSONString(API_RESPONSE_PARAMS));
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        }
        String memberId = API_RESPONSE_PARAMS.get(merchNo);
        String orderId = API_RESPONSE_PARAMS.get(orderNo);
        if (StringUtils.isBlank(memberId) || StringUtils.isBlank(orderId)) {
            log.error("[汇达支付]1.获取支付通道响应信息中的订单号错误，" + JSON.toJSONString(API_RESPONSE_PARAMS));
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        }
        log.debug("[汇达支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + orderId);
        return orderId;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        String apiName = api_response_params.get("apiName");
        String notifyTime = api_response_params.get("notifyTime");
        String tradeAmt = api_response_params.get("tradeAmt");
        String merchNo = api_response_params.get("merchNo");
        String merchParam = api_response_params.get("merchParam");
        String orderNo = api_response_params.get("orderNo");
        String tradeDate = api_response_params.get("tradeDate");
        String accNo = api_response_params.get("accNo");
        String accDate = api_response_params.get("accDate");
        String orderStatus = api_response_params.get("orderStatus");
        String srcMsg = String.format("apiName=%s&notifyTime=%s&tradeAmt=%s&merchNo=%s&merchParam=%s&orderNo=%s&tradeDate=%s&accNo=%s&accDate=%s&orderStatus=%s",
                apiName, notifyTime, tradeAmt, merchNo,
                merchParam, orderNo, tradeDate, accNo, accDate,
                orderStatus);
        String signMd5 = HuiDaZhiFuPayUtil.signByMD5(srcMsg, api_key);
        log.debug("[汇达支付]-[响应支付]-2.响应内容生成md5完成：" + signMd5);
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amount) throws PayException {
        boolean result = false;
        String payStatusCode = api_response_params.get(orderStatus);
        String responseAmount = api_response_params.get(tradeAmt);
        responseAmount = HandlerUtil.getFen(responseAmount);
        boolean checkAmount = amount.equalsIgnoreCase(responseAmount);
        if (checkAmount && payStatusCode.equalsIgnoreCase("1")) {
            result = true;
        } else {
            log.error("[汇达支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + amount);
        }
        log.debug("[汇达支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        String signMsg = api_response_params.get("signMsg");
        signMsg = signMsg.replaceAll(" ", "\\+");
        boolean result = signMsg.equalsIgnoreCase(signMd5);
        log.debug("[汇达支付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[汇达支付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}