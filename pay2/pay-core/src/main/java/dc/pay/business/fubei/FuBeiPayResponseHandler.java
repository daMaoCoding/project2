package dc.pay.business.fubei;/**
 * Created by admin on 2017/5/25.
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

/**
 * ************************
 *
 * @author tony 3556239829
 */

@ResponsePayHandler("FUBEI")
public class FuBeiPayResponseHandler extends PayResponseHandler {
    private static final Logger log =  LoggerFactory.getLogger(FuBeiPayResponseHandler.class);
    private static final String ORDERID = "orderid";
    private static final String OPSTATE = "opstate";
    private static final String OVALUE = "ovalue";
    private static final String SIGN = "sign";
    private static final String SYSTIME = "systime";
    private static final String SYSORDERID = "sysorderid";
    private static final String RETURNCODE_SUCCESS = "0";
    private static final String  RESPONSE_PAY_MSG_OK= "opstate=0";
    private static final String  RESPONSE_PAY_MSG_NOOK= "opstate=-2";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if(null==API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String orderId = API_RESPONSE_PARAMS.get(ORDERID);
        if(StringUtils.isBlank(orderId))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_PARAM_ERROR);
        log.debug("[付呗支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + orderId);
        return orderId;
    }


    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        StringBuilder sb = new StringBuilder();
        sb.append(ORDERID+"=").append(api_response_params.get(ORDERID)).append("&")
                .append(OPSTATE+"=").append(api_response_params.get(OPSTATE)).append("&")
                .append(OVALUE+"=").append(api_response_params.get(OVALUE)).append("&")
                .append("time=").append(api_response_params.get(SYSTIME)).append("&")
                .append(SYSORDERID+"=").append(api_response_params.get(SYSORDERID))
                .append(channelWrapper.getAPI_KEY());

        String pay_md5sign = HandlerUtil.getMD5UpperCase(sb.toString()).toLowerCase();
        log.debug("[付呗支付]-[响应支付]-2.响应内容生成md5完成：" + pay_md5sign);
        return  pay_md5sign;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amount) throws PayException {
        boolean result = false;
        String payStatusCode =  api_response_params.get(OPSTATE);
        String responseAmount = api_response_params.get(OVALUE);
               responseAmount = HandlerUtil.getFen(responseAmount);
        boolean checkAmount = amount.equalsIgnoreCase(responseAmount);
        if(checkAmount && payStatusCode.equalsIgnoreCase(RETURNCODE_SUCCESS)){
            result = true;
        }else{
            log.error("[付呗支付]-[响应支付]金额及状态验证错误,订单号："+channelWrapper.getAPI_ORDER_ID()+",第三方支付状态："+payStatusCode +" ,支付金额："+responseAmount+" ，应支付金额："+amount);
        }
        log.debug("[付呗支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果："+ result+" ,金额验证："+checkAmount+" ,responseAmount="+responseAmount +" ,数据库金额："+amount+",第三方响应支付成功标志:"+payStatusCode+" ,计划成功：1");
        return result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(SIGN).equalsIgnoreCase(signMd5);
        log.debug("[付呗支付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[付呗支付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG_OK);
        return RESPONSE_PAY_MSG_OK;
    }
}