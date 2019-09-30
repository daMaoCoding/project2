package dc.pay.business.yibaoyeepay;

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

@ResponsePayHandler("YIBAOYEEPAY")
public final class EbaoyeePayResponseHandler extends PayResponseHandler {
     private final Logger log = LoggerFactory.getLogger(getClass());
     private static final String RESPONSE_PAY_MSG = "SUCCESS";

     private static final String   p1_MerId ="p1_MerId";  // "10021930964",
     private static final String   r0_Cmd ="r0_Cmd";  // "Buy",
     private static final String   r1_Code ="r1_Code";  // "1",
     private static final String   r2_TrxId ="r2_TrxId";  // "218690540472112J",
     private static final String   r3_Amt ="r3_Amt";  // "1.00",
     private static final String   r4_Cur ="r4_Cur";  // "RMB",
     private static final String   r5_Pid ="r5_Pid";  // "",
     private static final String   r6_Order ="r6_Order";  // "20180717142154",
     private static final String   r7_Uid ="r7_Uid";  // "",
     private static final String   r8_MP ="r8_MP";  // "",
     private static final String   r9_BType ="r9_BType";  // "1",
     private static final String   rb_BankId ="rb_BankId";  // "ICBC-NET",
     private static final String   ro_BankOrderId ="ro_BankOrderId";  // "52839413721807",
     private static final String   rp_PayDate ="rp_PayDate";  // "20180717142353",
     private static final String   rq_CardNo ="rq_CardNo";  // "",
     private static final String   ru_Trxtime ="ru_Trxtime";  // "20180717142355",
     private static final String   rq_SourceFee ="rq_SourceFee";  // "0.00",
     private static final String   rq_TargetFee ="rq_TargetFee";  // "0.00",
     private static final String   hmac_safe ="hmac_safe";  // "076498d78e78da27a7c9f9eb38a0822f",
     private static final String   hmac ="hmac";  // "2c8fa33041da8d3a33d858d4ac34b948"

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(r6_Order);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[易宝YEEPAY]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        String [] paramsS = new String[]{params.get(p1_MerId),params.get(r0_Cmd),params.get(r1_Code),params.get(r2_TrxId),params.get(r3_Amt),params.get(r4_Cur),params.get(r5_Pid),params.get(r6_Order),params.get(r7_Uid),params.get(r8_MP),params.get(r9_BType),};
        String pay_md5sign=DigestUtil.getHmac(paramsS,channelWrapper.getAPI_KEY());//keyValu
        log.debug("[易宝YEEPAY]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }



    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean checkResult = false;
        String payStatus = api_response_params.get(r1_Code);
        String responseAmount =  HandlerUtil.getFen(api_response_params.get(r3_Amt));
        boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        if (checkAmount && payStatus.equalsIgnoreCase("1")) {
            checkResult = true;
        } else {
            log.error("[易宝YEEPAY]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatus + " ,支付金额：" + responseAmount + " ，应支付金额：" + amountDb);
        }
        log.debug("[易宝YEEPAY]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + checkResult + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amountDb + ",第三方响应支付成功标志:" + payStatus + " ,计划成功：1");
        return checkResult;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(hmac).equalsIgnoreCase(signMd5);
        log.debug("[易宝YEEPAY]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[易宝YEEPAY]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}