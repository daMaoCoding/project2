package dc.pay.business.okFu;

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
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Nov 29, 2017
 */
@ResponsePayHandler("OKFU")
public final class OkFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());
    
	private static final String version  ="version";
	private static final String partner  ="partner";
	private static final String orderid  ="orderid";
	private static final String payamount  ="payamount";
	private static final String paytype  ="paytype";
	private static final String remark  ="remark";
	
	private static final String sign = "sign";
	private static final String opstate = "opstate";
	private static final String orderno = "orderno";
	private static final String okfpaytime = "okfpaytime";
    private static final String message = "message";

    private static final String RESPONSE_PAY_MSG = "success";



    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(partner);
        String ordernumberR = API_RESPONSE_PARAMS.get(orderid);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[OK付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
    	//使用对方返回的数据进行签名
        String paramsStr = String.format("version=%s&partner=%s&orderid=%s&payamount=%s&opstate=%s&orderno=%s&okfpaytime=%s&message=%s&paytype=%s&remark=%s&key=%s",
        		api_response_params.get(version),
                api_response_params.get(partner),
                api_response_params.get(orderid),
                api_response_params.get(payamount),
                api_response_params.get(opstate),
                api_response_params.get(orderno),
                api_response_params.get(okfpaytime),
                api_response_params.get(message),
                api_response_params.get(paytype),
                api_response_params.get(remark),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[OK付]-[请求支付]-2.生成加密URL签名完成，参数：" + JSON.toJSONString(paramsStr) +" ,值："+ JSON.toJSONString(signMd5));
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amount) throws PayException {
        boolean result = false;
        String payStatusCode = api_response_params.get(opstate);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(payamount));
        //amount数据库存入的是分 	第三方返回的responseAmount是元
        boolean checkAmount = amount.equalsIgnoreCase(responseAmount);
        //2代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("2")) {
            result = true;
        } else {
            log.error("[OK付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + amount);
        }
        log.debug("[OK付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：2");
        return result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("[OK付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[OK付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}