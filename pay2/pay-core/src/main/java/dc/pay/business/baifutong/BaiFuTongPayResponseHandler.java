package dc.pay.business.baifutong;

/**
 * ************************
 * @author tony 3556239829
 */

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@ResponsePayHandler("BAIFUTONG")
public final class BaiFuTongPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(BaiFuTongPayResponseHandler.class);
    private static final String pay_amount = "pay_amount";
    private static final String real_amount = "real_amount";
    private static final String pay_type = "pay_type";
    private static final String mch_id = "mch_id";
    private static final String order_id = "order_id";
    private static final String order_no = "order_no";
    private static final String status = "status";
    private static final String channel_id = "channel_id";
    private static final String finish_time = "finish_time";
    private static final String code = "code";
    private static final String msg = "msg";
    private static final String ts = "ts";
    private static final String sign = "sign";
    private static final String ext = "ext";
    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String memberId = API_RESPONSE_PARAMS.get(mch_id);
        String orderId = API_RESPONSE_PARAMS.get(order_id);
        if (StringUtils.isBlank(memberId) || StringUtils.isBlank(orderId))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[百付通]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + orderId);
        return orderId;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        String pay_md5sign = null;
        List<String> paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (StringUtils.isBlank(String.valueOf(api_response_params.get(paramKeys.get(i)))) || "sign".equalsIgnoreCase(paramKeys.get(i)))
                continue;
            sb.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
        }
        sb.append("key=" + channelWrapper.getAPI_KEY());//"key="+
        pay_md5sign = HandlerUtil.getMD5UpperCase(sb.toString());
        log.debug("[百付通]-[响应支付]-2.响应内容生成md5完成：" + pay_md5sign);
        return pay_md5sign;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amount) throws PayException {
        boolean result = false;
        String payStatusCode = api_response_params.get(status);
        String payStatusResult = api_response_params.get(code);
        String responseAmount = api_response_params.get(real_amount);
        responseAmount = HandlerUtil.getFen(responseAmount);
        boolean checkAmount = amount.equalsIgnoreCase(responseAmount);
        if (checkAmount && payStatusCode.equalsIgnoreCase("1") && payStatusResult.equalsIgnoreCase("0")) {
            result = true;
        } else {
            log.error("[百付通]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + amount);
        }
        log.debug("[百付通]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("[百付通]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }


    @Override
    protected String responseSuccess() {
        log.debug("[百付通]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }


}
