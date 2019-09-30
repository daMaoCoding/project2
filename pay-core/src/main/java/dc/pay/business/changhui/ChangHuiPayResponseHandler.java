package dc.pay.business.changhui;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;

/**
 * 
 * 
 * @author kevin
 * Jul 20, 2018
 */
@ResponsePayHandler("CHANGHUI")
public final class ChangHuiPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());


    private static final String p1_MerId  		="p1_MerId";
    private static final String r0_Cmd    		="r0_Cmd";
    private static final String r1_Code   		="r1_Code";
    private static final String r2_TrxId  		="r2_TrxId";
    private static final String r3_Amt    		="r3_Amt";
    private static final String r4_Cur    		="r4_Cur";
    private static final String r5_Pid    		="r5_Pid";
    private static final String r6_Order  		="r6_Order";	
    private static final String r8_MP     		="r8_MP";
    private static final String r9_BType  		="r9_BType";
    private static final String ro_BankOrderId  ="ro_BankOrderId";
    private static final String rp_PayDate      ="rp_PayDate";
    private static final String hmac            ="hmac";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(p1_MerId);
        String ordernumberR = API_RESPONSE_PARAMS.get(r6_Order);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[畅汇]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" , ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        
    	StringBuffer sValue = new StringBuffer();
		sValue.append(api_response_params.get(p1_MerId));
		sValue.append(api_response_params.get(r0_Cmd));
		sValue.append(api_response_params.get(r1_Code));
		sValue.append(api_response_params.get(r2_TrxId));
		sValue.append(api_response_params.get(r3_Amt));
		sValue.append(api_response_params.get(r4_Cur));
		sValue.append(api_response_params.get(r5_Pid));
		sValue.append(api_response_params.get(r6_Order));
		sValue.append(api_response_params.get(r8_MP));
		sValue.append(api_response_params.get(r9_BType));
		sValue.append(api_response_params.get(ro_BankOrderId));
		sValue.append(api_response_params.get(rp_PayDate));
		String signMd5 = HmacMd5Util.hmacSign(sValue.toString(), channelWrapper.getAPI_KEY()).toLowerCase();
        log.debug("[畅汇]-[响应支付]-2.生成加密URL签名完成：{}" , JSON.toJSONString(signMd5));
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //r1_Code		交易状态	1-支付成功	9999:失败
        String payStatusCode = api_response_params.get(r1_Code);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(r3_Amt));
        //r3_Amt数据库存入的是分 	第三方返回的r3_Amt是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("1")) {
            my_result = true;
        } else {
            log.error("[畅汇]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[畅汇]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
    	//System.out.println("hmac=========>"+api_response_params.get(hmac));
    	//System.out.println("signMd5=========>"+signMd5);
        boolean my_result = api_response_params.get(hmac).equalsIgnoreCase(signMd5);
        log.debug("[畅汇]-[响应支付]-4.验证MD5签名：{}" , my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[畅汇]-[响应支付]-5.第三方支付确认收到消息返回内容：{}" , RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}