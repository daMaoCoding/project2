package dc.pay.business.yingtongbao;

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

@ResponsePayHandler("YINGTONGBAO")
public final class YingTongBaoPayResponseHandler extends PayResponseHandler {
   private final Logger log =  LoggerFactory.getLogger(YingTongBaoPayResponseHandler.class);

    private static final String PARTNER     = "partner";
    private static final String ORDERNUMBER = "ordernumber";
    private static final String ORDERSTATUS = "orderstatus";
    private static final String PAYMONEY    = "paymoney";
    private static final String SYSNUMBER   = "sysnumber";
    private static final String ATTACH      = "attach";
    private static final String SIGN        = "sign";
    private static final String  RESPONSE_PAY_MSG= "ok";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if(null==API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
            String memberId = API_RESPONSE_PARAMS.get(PARTNER);
            String orderId = API_RESPONSE_PARAMS.get(ORDERNUMBER);
        if(StringUtils.isBlank(memberId) || StringUtils.isBlank(orderId))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        if(!orderId.contains(memberId))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_PARAM_ERROR);
        log.debug("[赢通宝]-[响应支付]-1.获取支付通道响应信息中的订单号完成："+ orderId);
         return orderId;
    }


    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        String paramsStr = String.format("partner=%s&ordernumber=%s&orderstatus=%s&paymoney=%s%s",
                api_response_params.get(PARTNER),
                api_response_params.get(ORDERNUMBER),
                api_response_params.get(ORDERSTATUS),
                api_response_params.get(PAYMONEY),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[赢通宝]-[响应支付]-2.响应内容生成md5完成："+ signMd5);
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params,String amount) throws PayException {
        boolean result = false;
        String payStatusCode = api_response_params.get(ORDERSTATUS);
        String responseAmount = api_response_params.get(PAYMONEY);
        responseAmount = HandlerUtil.getFen(responseAmount);
        boolean checkAmount = amount.equalsIgnoreCase(responseAmount);
        if(checkAmount && payStatusCode.equalsIgnoreCase("1")  ){
            result = true;
        }else{
            log.error("[赢通宝]-[响应支付]金额及状态验证错误,订单号："+channelWrapper.getAPI_ORDER_ID()+",第三方支付状态："+payStatusCode +" ,支付金额："+responseAmount+" ，应支付金额："+amount);
        }
        log.debug("[赢通宝]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果："+ result+" ,金额验证："+checkAmount+" ,responseAmount="+responseAmount +" ,数据库金额："+amount+",第三方响应支付成功标志:"+payStatusCode+" ,计划成功：1");
        return result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params,String signMd5) {
        boolean result = api_response_params.get(SIGN).equalsIgnoreCase(signMd5);
        log.debug("[赢通宝]-[响应支付]-4.验证MD5签名："+ result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[赢通宝]-[响应支付]-5.第三方支付确认收到消息返回内容："+ RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}