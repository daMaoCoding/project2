package dc.pay.business.shangyifu;

/**
 * ************************
 * @author tony 3556239829
 */

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.business.luobo.LuoBoPayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@ResponsePayHandler("SHANGYIFU")
public final class ShangYiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(LuoBoPayResponseHandler.class);

    private static final String RESPONSE_PAY_MSG = "opstate=0";
    private static final String  orderid = "orderid";    //: "SHANGYIFU_WAP_ZFB-ekYaX",
    private static final String  opstate = "opstate";    //: "0",
    private static final String  ovalue = "ovalue";    //: "30.00",
    private static final String  sysorderid = "sysorderid";    //: "B5094724029920852396",
    private static final String  systime = "systime";    //: "2018/06/02 16:35:47",
    private static final String  attach = "attach";    //: "",
    private static final String  msg = "msg";    //: "",
    private static final String  sign = "sign";    //: "57b9b141b0f5e4541b1d8511ea52bf4e"


    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String orderId = API_RESPONSE_PARAMS.get(orderid);
        if (StringUtils.isBlank(orderId))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[商易付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + orderId);
        return orderId;
    }
    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {

        String paramsStr = String.format("orderid=%s&opstate=%s&ovalue=%s&time=%s&sysorderid=%s%s",
                api_response_params.get(orderid),
                api_response_params.get(opstate),
                api_response_params.get(ovalue),
                api_response_params.get(systime),
                api_response_params.get(sysorderid),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[商易付]-[响应支付]-2.响应内容生成md5完成：" + signMd5);
        return signMd5;
    }
    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amount) throws PayException {
        boolean result = false;
        String payStatusCode = api_response_params.get(opstate);
        String responseAmount = api_response_params.get(ovalue);
        responseAmount = HandlerUtil.getFen(responseAmount);
        boolean checkAmount = amount.equalsIgnoreCase(responseAmount);
        if (checkAmount && payStatusCode.equalsIgnoreCase("0")) {
            result = true;
        } else {
            log.error("[商易付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + amount);
        }
        log.debug("[商易付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return result;
    }
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("[商易付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }
    @Override
    protected String responseSuccess() {
        log.debug("[商易付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}