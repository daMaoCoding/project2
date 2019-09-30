package dc.pay.business.kuaizhifu;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;

/**
 * 
 * @author andrew
 * Feb 5, 2018
 */
@ResponsePayHandler("KUAIZHIFU")
public final class KuaiZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private static final String MemberID = "MemberID";
    private static final String TerminalID = "TerminalID";
    private static final String TransID = "TransID";
    private static final String Result = "Result";
    private static final String ResultDesc = "ResultDesc";
    private static final String FactMoney = "FactMoney";
    private static final String AdditionalInfo = "AdditionalInfo";
    private static final String SuccTime = "SuccTime";
    private static final String Md5Sign = "Md5Sign";
    private static final String RESPONSE_PAY_MSG = "SUCCESS";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String memberId = API_RESPONSE_PARAMS.get(MemberID);
        String orderId = API_RESPONSE_PARAMS.get(TransID);
        if (StringUtils.isBlank(memberId) || StringUtils.isBlank(orderId))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[快支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + orderId);
        return orderId;
    }

    @Override
    protected String buildPaySign(Map<String, String> payParam, String api_key) throws PayException {
        String MARK = "~|~";
        String md5 = "MemberID=" + payParam.get(MemberID) + MARK + "TerminalID=" + payParam.get(TerminalID) + MARK + "TransID=" + payParam.get(TransID) + MARK +
                    "Result=" + payParam.get(Result) + MARK + "ResultDesc=" + payParam.get(ResultDesc) + MARK +
                    "FactMoney=" + payParam.get(FactMoney) + MARK + "AdditionalInfo=" + payParam.get(AdditionalInfo) + MARK + "SuccTime=" + payParam.get(SuccTime)+ MARK + "Md5Sign=" + channelWrapper.getAPI_KEY();
        String signMd5  = HandlerUtil.getMD5UpperCase(md5).toLowerCase();
        log.debug("[快支付]-[响应支付]-2.响应内容生成md5完成：" + signMd5);
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amount) throws PayException {
        boolean result = false;
        String payStatusCode = api_response_params.get(Result);
        String responseAmount = api_response_params.get(FactMoney);
        responseAmount = HandlerUtil.getFen(responseAmount);
        boolean checkAmount = amount.equalsIgnoreCase(responseAmount);
        if (checkAmount && payStatusCode.equalsIgnoreCase("1")) {
            result = true;
        } else {
            log.error("[快支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + amount);
        }
        log.debug("[快支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(Md5Sign).equalsIgnoreCase(signMd5);
        log.debug("[快支付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[快支付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}