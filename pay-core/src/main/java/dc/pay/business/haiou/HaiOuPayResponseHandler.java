package dc.pay.business.haiou;

/**
 * ************************
 * @author tony 3556239829
 */

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
@ResponsePayHandler("HAIOU")
public final class HaiOuPayResponseHandler extends PayResponseHandler {
   private final Logger log =  LoggerFactory.getLogger(getClass());
    private static final String USERID     = "userid";
    private static final String ORDERID = "orderid";
    private static final String BILLNO = "billno";
    private static final String PRICE    = "price";
    private static final String PAYVIA = "payvia";
    private static final String STATE   = "state";
    private static final String TIMESPAN   = "timespan";
    private static final String SIGN      = "sign";
    private static final String RESPONSE_PAY_MSG= "success";
    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if(null==API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
            String memberId = API_RESPONSE_PARAMS.get(USERID);
            String orderId = API_RESPONSE_PARAMS.get(ORDERID);
        if(StringUtils.isBlank(memberId) || StringUtils.isBlank(orderId))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[海鸥]-[响应支付]-1.获取支付通道响应信息中的订单号完成："+ orderId);
         return orderId;
    }
    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        String paramsStr = String.format("userid=%s&orderid=%s&billno=%s&price=%s&payvia=%s&state=%s&timespan=%s&key=%s",
                api_response_params.get(USERID),
                api_response_params.get(ORDERID),
                api_response_params.get(BILLNO),
                api_response_params.get(PRICE),
                api_response_params.get(PAYVIA),
                api_response_params.get(STATE),
                api_response_params.get(TIMESPAN),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        signMd5+= channelWrapper.getAPI_KEY();
        signMd5=HandlerUtil.getMD5UpperCase(signMd5).toLowerCase();
        log.debug("[海鸥]-[响应支付]-2.响应内容生成md5完成："+ signMd5);
        return signMd5;
    }
    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params,String amount) throws PayException {
        boolean result = false;
        String payStatusCode = api_response_params.get(STATE);
        String responseAmount = api_response_params.get(PRICE);
        responseAmount = HandlerUtil.getFen(responseAmount);
        boolean checkAmount = amount.equalsIgnoreCase(responseAmount);
        if(checkAmount && payStatusCode.equalsIgnoreCase("1")  ){
            result = true;
        }else{
            log.error("[海鸥]-[响应支付]金额及状态验证错误,订单号："+channelWrapper.getAPI_ORDER_ID()+",第三方支付状态："+payStatusCode +" ,支付金额："+responseAmount+" ，应支付金额："+amount);
        }
        log.debug("[海鸥]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果："+ result+" ,金额验证："+checkAmount+" ,responseAmount="+responseAmount +" ,数据库金额："+amount+",第三方响应支付成功标志:"+payStatusCode+" ,计划成功：1");
        return result;
    }
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params,String signMd5) {
        boolean result = api_response_params.get(SIGN).equalsIgnoreCase(signMd5);
        log.debug("[海鸥]-[响应支付]-4.验证MD5签名："+ result);
        return result;
    }
    @Override
    protected String responseSuccess() {
        log.debug("[海鸥]-[响应支付]-5.第三方支付确认收到消息返回内容："+ RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}
