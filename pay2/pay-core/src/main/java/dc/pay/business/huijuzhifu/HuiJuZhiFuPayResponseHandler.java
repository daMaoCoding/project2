package dc.pay.business.huijuzhifu;

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

@ResponsePayHandler("HUIJUZHIFU")
public final class HuiJuZhiFuPayResponseHandler extends PayResponseHandler {
     private final Logger log = LoggerFactory.getLogger(getClass());
     private static final String RESPONSE_PAY_MSG = "success";

     private static final String  r1_MerchantNo = "r1_MerchantNo";   // "888100000002340",
     private static final String  r2_OrderNo = "r2_OrderNo";   // "20180910164844",
     private static final String  r3_Amount = "r3_Amount";   // "10.00",
     private static final String  r4_Cur = "r4_Cur";   // "1",
     private static final String  r5_Mp = "r5_Mp";   // "",
     private static final String  r6_Status = "r6_Status";   // "100",
     private static final String  r7_TrxNo = "r7_TrxNo";   // "100218091002062867",
     private static final String  r8_BankOrderNo = "r8_BankOrderNo";   // "100218091002062867",
     private static final String  r9_BankTrxNo = "r9_BankTrxNo";   // "100218091002062867",
     private static final String  ra_PayTime = "ra_PayTime";   // "2018-09-10+16%3A49%3A59",
     private static final String  rb_DealTime = "rb_DealTime";   // "2018-09-10+16%3A49%3A59",
     private static final String  rc_BankCode = "rc_BankCode";   // "CCB",
     private static final String  hmac = "hmac";   // "6deeb6d365f89148847498f561867c19"

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(r2_OrderNo);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[汇聚支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        // opstate=opstate值&orderid=orderid值&ovalue=ovalue值&parter=商家接口调用ID&key=商家
        String paramsStr = String.format("%s%s%s%s%s%s%s%s%s%s%s%s%s",
                params.get(r1_MerchantNo),
                params.get(r2_OrderNo),
                params.get(r3_Amount),
                params.get(r4_Cur),
                params.get(r5_Mp),
                params.get(r6_Status),
                params.get(r7_TrxNo),
                params.get(r8_BankOrderNo),
                params.get(r9_BankTrxNo),
                HandlerUtil.UrlDecode(params.get(ra_PayTime)),
                HandlerUtil.UrlDecode(params.get(rb_DealTime)),
                params.get(rc_BankCode),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[汇聚支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }



    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean checkResult = false;
        String payStatus = api_response_params.get(r6_Status);
        String responseAmount =  HandlerUtil.getFen(api_response_params.get(r3_Amount));
        boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        if (checkAmount && payStatus.equalsIgnoreCase("100")) {
            checkResult = true;
        } else {
            log.error("[汇聚支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatus + " ,支付金额：" + responseAmount + " ，应支付金额：" + amountDb);
        }
        log.debug("[汇聚支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + checkResult + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amountDb + ",第三方响应支付成功标志:" + payStatus + " ,计划成功：1");
        return checkResult;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(hmac).equalsIgnoreCase(signMd5);
        log.debug("[汇聚支付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[汇聚支付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}