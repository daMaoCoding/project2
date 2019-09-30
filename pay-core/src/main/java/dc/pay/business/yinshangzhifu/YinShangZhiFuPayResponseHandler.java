package dc.pay.business.yinshangzhifu;

/**
 * ************************
 * @author tony 3556239829
 */

import com.alibaba.fastjson.JSON;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.DigestUtil;
import dc.pay.utils.HandlerUtil;
import org.apache.commons.lang.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
@ResponsePayHandler("SHANGYINZHIFU")
public final class YinShangZhiFuPayResponseHandler extends PayResponseHandler {
      //private final Logger log = LoggerFactory.getLogger(getClass());
      private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("success");

      private static final String   p1_MerId = "p1_MerId";     // "90381",
      private static final String   r0_Cmd = "r0_Cmd";     // "Buy",
      private static final String   r1_Code = "r1_Code";     // "1",
      private static final String   r2_TrxId = "r2_TrxId";     // "20181231145122129811",
      private static final String   r3_Amt = "r3_Amt";     // "101.00",
      private static final String   r4_Cur = "r4_Cur";     // "RMB",
      private static final String   r5_Pid = "r5_Pid";     // "",
      private static final String   r6_Order = "r6_Order";     // "20181231145051639168",
      private static final String   r7_Uid = "r7_Uid";     // "",
      private static final String   r8_MP = "r8_MP";     // "",
      private static final String   r9_BType = "r9_BType";     // "2",
      private static final String   rb_BankId = "rb_BankId";     // "ICBC-NET",
      private static final String   ro_BankOrderId = "ro_BankOrderId";     // "",
      private static final String   rp_PayDate = "rp_PayDate";     // "2018-12-31 14:51:50",
      private static final String   rq_CardNo = "rq_CardNo";     // "",
      private static final String   ru_Trxtime = "ru_Trxtime";     // "2018-12-31 14:51:50",
      private static final String   hmac = "hmac";     // "2cdab67338a4ef0d815f0954cae4454b"


    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(r6_Order);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[商银支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        // ^|^^|^r1_Code^|^r2_TrxId^|^r3_Amt^|^r4_Cur^|^r5_Pid^|^r6_Order^|^r7_Uid^|^r8_MP^|^r9_BType^|^
        String paramsStr = String.format("%s^|^%s^|^%s^|^%s^|^%s^|^%s^|^%s^|^%s^|^%s^|^%s^|^%s^|^",
                params.get(p1_MerId),
                params.get(r0_Cmd),
                params.get(r1_Code),
                params.get(r2_TrxId),
                params.get(r3_Amt),
                params.get(r4_Cur),
                params.get(r5_Pid),
                params.get(r6_Order),
                params.get(r7_Uid),
                params.get(r8_MP),
                params.get(r9_BType));
        String signMd5 = DigestUtil.hmacSign(paramsStr, channelWrapper.getAPI_KEY());
        log.debug("[商银支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }



    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean checkResult = false;
        String payStatus = api_response_params.get(r1_Code);
        String responseAmount =  HandlerUtil.getFen(api_response_params.get(r3_Amt));
        //boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        boolean checkAmount =  HandlerUtil.isRightAmount(amountDb,responseAmount,"100");//第三方回调金额差额1元内
        if (checkAmount && payStatus.equalsIgnoreCase("1")) {
            checkResult = true;
        } else {
            log.error("[商银支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatus + " ,支付金额：" + responseAmount + " ，应支付金额：" + amountDb);
        }
        log.debug("[商银支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + checkResult + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amountDb + ",第三方响应支付成功标志:" + payStatus + " ,计划成功：1");
        return checkResult;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(hmac).equalsIgnoreCase(signMd5);
        log.debug("[商银支付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[商银支付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}