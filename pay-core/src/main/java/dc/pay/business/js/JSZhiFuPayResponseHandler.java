package dc.pay.business.js;

import java.util.Map;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;


/**
 * @author cobby
 * Jan 17, 2019
 */
@ResponsePayHandler("JSZHIFU")
public final class JSZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

// OrderNo	String	平台订单号
// MerchantNo	String	商户号
// Amount	String	下单金额,分为单位（如1元=100）
// OutTradeNo	String	商户订单号，商户传入
// Status	String	交易状态，1：交易成功，其他失败
// Message	String	交易结果
// TradeTime	String	交易时间
// Attach	String	交易时传入的附加信息
// Sign	String	签名(MD5加密)
    private static final String OrderNo                ="OrderNo";  //平台订单号
    private static final String Amount                 ="Amount";//下单金额,分为单位（如1元=100）
    private static final String MerchantNo             ="MerchantNo";//商户号
    private static final String OutTradeNo             ="OutTradeNo";//接入类型，10：二维码转帐
    private static final String Status                 ="Status"; //支付方式，1：支付宝

    //signature    数据签名    32    是    　
    private static final String Sign  ="Sign";

    private static final String RESPONSE_PAY_MSG = "SUCCESS";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {

        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(OrderNo);
        String ordernumberR = API_RESPONSE_PARAMS.get(OutTradeNo);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[JS支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

	@Override
	protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
		//MD5 (OrderNo + MerchantNo + Amount + OutTradeNo + Status + (商户秘钥)).ToLower ()
		String paramsStr = String.format("%s%s%s%s%s%s",
				params.get(OrderNo),
				params.get(MerchantNo),
				params.get(Amount),
				params.get(OutTradeNo),
				params.get(Status),
				channelWrapper.getAPI_KEY());

		String pay_md5sign = HandlerUtil.getMD5UpperCase(paramsStr);
		log.debug("[JS支付]-[请求支付]-2.生成加密URL签名完成：" + pay_md5sign);
		return pay_md5sign;
	}

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //status       1-支付成功
        String payStatusCode = api_response_params.get(Status);
        String responseAmount = api_response_params.get(Amount);
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("1")) {
            my_result = true;
        } else {
            log.error("[JS支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[JS支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return my_result;
    }


    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(Sign).equalsIgnoreCase(signMd5);
        log.debug("[JS支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }


    @Override
    protected String responseSuccess() {
        log.debug("[JS支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}