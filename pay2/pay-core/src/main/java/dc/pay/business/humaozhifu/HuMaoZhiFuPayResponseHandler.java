package dc.pay.business.humaozhifu;

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
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
@ResponsePayHandler("HUMAOZHIFU")
public final class HuMaoZhiFuPayResponseHandler extends PayResponseHandler {
      //private final Logger log = LoggerFactory.getLogger(getClass());
      private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("success");
      private static final String  customerid = "customerid";    // "110033",
     private static final String  paytype = "paytype";    // "alipaywap",
     private static final String  remark = "remark";    // "",
     private static final String  sdorderno = "sdorderno";    // "20181203143302195797",
     private static final String  sdpayno = "sdpayno";    // "20181203143428500496",
     private static final String  sign = "sign";    // "58135de84a90dcc66be44a2bbec9d934",
     private static final String  status = "status";    // "1",
     private static final String  total_fee = "total_fee";    // "10.00"


    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(sdorderno);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[虎猫支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        //customerid={value}&status={value}&sdpayno={value}&sdorderno={value}&total_fee={value}&paytype={value}&{apikey}
        //=%s&=%s&=%s&=%s&=%s&=%s&%s
        String paramsStr = String.format("customerid=%s&status=%s&sdpayno=%s&sdorderno=%s&total_fee=%s&paytype=%s&%s",
                params.get(customerid),
                params.get(status),
                params.get(sdpayno),
                params.get(sdorderno),
                params.get(total_fee),
                params.get(paytype),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[虎猫支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }



    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean checkResult = false;
        String payStatus = api_response_params.get(status);
        String responseAmount =  HandlerUtil.getFen(api_response_params.get(total_fee));
        //boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        boolean checkAmount =  HandlerUtil.isRightAmount(amountDb,responseAmount,"100");//第三方回调金额差额1元内
        if (checkAmount && payStatus.equalsIgnoreCase("1")) {
            checkResult = true;
        } else {
            log.error("[虎猫支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatus + " ,支付金额：" + responseAmount + " ，应支付金额：" + amountDb);
        }
        log.debug("[虎猫支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + checkResult + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amountDb + ",第三方响应支付成功标志:" + payStatus + " ,计划成功：1");
        return checkResult;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("[虎猫支付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[虎猫支付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}