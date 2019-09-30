package dc.pay.business.lezhifu;

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
 * Jan 22, 2018
 */
@ResponsePayHandler("LEZHIFU")
public final class LeZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());
    

    //支付状态	status	1	否	1为支付成功		0为支付失败
    private static final String status  ="status";
    //商户号	account	10	是	系统给定的商户号
    private static final String account  ="account";
    //交易订单号order	40	是	订单号
    private static final String order  ="order";
    //平台订单号orders	40	是	平台订单号
    private static final String orders  ="orders";
    //支付类型	paytype	无	是	[wxwap]微信WAP	[更多参看，附1]
    private static final String paytype  ="paytype";
    //网银类型	type	4	否	1001 = 中信银行	[更多参看，附2]
    private static final String type  ="type";
    //交易金额	money	12	是	0.01单位：元，测试最低金额为1元
    private static final String money  ="money";
    //商品描述	body	20	否	
    private static final String body  ="body";
    //透传参数	ext		50	否	
    private static final String ext  ="ext";
    
    private static final String key = "key";
    
    private static final String sign = "sign";
    
    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(account);
        String ordernumberR = API_RESPONSE_PARAMS.get(order);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[乐支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }

    @Override
	protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
		StringBuffer signSrc= new StringBuffer();
		signSrc.append(account).append("=").append(api_response_params.get(account)).append("&");
		signSrc.append(money).append("=").append(api_response_params.get(money)).append("&");
		signSrc.append(order).append("=").append(api_response_params.get(order)).append("&");
		signSrc.append(orders).append("=").append(api_response_params.get(orders)).append("&");
		signSrc.append(paytype).append("=").append(api_response_params.get(paytype)).append("&");
		signSrc.append(api_key);
		String paramsStr = signSrc.toString();
		String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[乐支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
	}

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String pay_amount) throws PayException {
        boolean result = false;
        //1为支付成功        0为支付失败
        String payStatusCode = api_response_params.get(status);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(money));
        //pay_amount数据库存入的是分 	第三方返回的responseAmount是元
        boolean checkAmount = pay_amount.equalsIgnoreCase(responseAmount);
        //2代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("1")) {
            result = true;
        } else {
            log.error("[乐支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + pay_amount);
        }
        log.debug("[乐支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + pay_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("[乐支付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[乐支付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}