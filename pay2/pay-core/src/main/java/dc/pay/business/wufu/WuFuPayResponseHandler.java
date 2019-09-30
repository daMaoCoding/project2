package dc.pay.business.wufu;

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
 * Dec 18, 2017
 */
@ResponsePayHandler("WUFU")
public final class WuFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String tranTime = "tranTime";
    private static final String orderStatusMsg = "orderStatusMsg";
    private static final String merchOrderId = "merchOrderId";
    private static final String orderId = "orderId";
    private static final String amt = "amt";
    private static final String status = "status";
    private static final String merData = "merData";
    private static final String md5value = "md5value";
    //merId 商户编号 是 String(36) 商户编号
	private static final String merId = "merId";

	private static final String RESPONSE_PAY_MSG = "SUCCESS";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
//        String partnerR = API_RESPONSE_PARAMS.get(partner);
        String ordernumberR = API_RESPONSE_PARAMS.get(merchOrderId);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[五福]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
    	//使用对方返回的数据进行签名
    	StringBuffer signSrc= new StringBuffer();
    	signSrc.append(api_response_params.get(amt));
    	if (StringUtils.isNotBlank(api_response_params.get(merData))) {
    		signSrc.append(api_response_params.get(merData));
    	}
    	if (StringUtils.isNotBlank(api_response_params.get(merId))) {
    		signSrc.append(api_response_params.get(merId));
    	}
    	signSrc.append(api_response_params.get(merchOrderId));
    	signSrc.append(api_response_params.get(orderId));
    	signSrc.append(api_response_params.get(orderStatusMsg));
    	signSrc.append(api_response_params.get(status));
    	signSrc.append(api_response_params.get(tranTime));
    	signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr,"UTF-8");
        log.debug("[五福]-[响应支付]-2.生成加密URL签名完成："+ JSON.toJSONString(signMd5));
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amount) throws PayException {
        boolean result = false;
        //0表示成功
        String payStatusCode = api_response_params.get(status);
        String responseAmount = api_response_params.get(amt);
        //amount数据库存入的是分 	第三方返回的responseAmount是元
        boolean checkAmount = amount.equalsIgnoreCase(responseAmount);
        //2代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("0")) {
            result = true;
        } else {
            log.error("[五福]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + amount);
        }
        log.debug("[五福]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：0");
        return result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(md5value).equalsIgnoreCase(signMd5);
        log.debug("[五福]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[五福]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}