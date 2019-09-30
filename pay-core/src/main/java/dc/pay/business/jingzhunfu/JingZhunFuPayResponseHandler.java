package dc.pay.business.jingzhunfu;

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
 * May 11, 2018
 */
@ResponsePayHandler("JINGZHUNFU")
public final class JingZhunFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

	//变量名称				名称				输入类型			说明
	//接口名字				apiName				ans(.30)			必输	取值：“PAY_RESULT_NOTIFY”
	//通知时间				notifyTime			n(14)				通知时间
	//支付金额				tradeAmt			n(12，2)			实际的支付金额
	//商户号				merchNo				ans(..32)			商户号
	//商户参数				merchParam			ans(..256)			支付时上送的商户参数
	//商户订单号			orderNo				n(..32)				商户订单号
	//交易日期				tradeDate			n(8)				商户交易日期
	//支付平台订单号			accNo				n(..8)				支付平台订单号
	//支付平台订单支付日期		accDate				n(8)				支付平台订单支付日期
	//订单状态				orderStatus			n(1)				0 未支付，1 成功，2失败
	//签名				signMsg				Ans(…600)			支付系统对发送数据的签名
	//通知类型				notifyType			n(1)				0页面跳转，商户给用户展示商品购买成功的页面。	1服务器，商户需回写应答。该字段不参与签名、验签
	private static final String apiName		="apiName";
	private static final String notifyTime	="notifyTime";
	private static final String tradeAmt		="tradeAmt";
	private static final String merchNo		="merchNo";
	private static final String merchParam	="merchParam";
	private static final String orderNo		="orderNo";
	private static final String tradeDate		="tradeDate";
	private static final String accNo			="accNo";
	private static final String accDate		="accDate";
	private static final String orderStatus	="orderStatus";
//	private static final String notifyType	="notifyType";
	
	private static final String signature  ="signMsg";
    
    private static final String RESPONSE_PAY_MSG = "SUCCESS";
    
    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(merchNo);
        String ordernumberR = API_RESPONSE_PARAMS.get(orderNo);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[精准付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
		StringBuffer signSrc= new StringBuffer();
		signSrc.append(apiName+"=").append(api_response_params.get(apiName)).append("&");
		signSrc.append(notifyTime+"=").append(api_response_params.get(notifyTime)).append("&");
		signSrc.append(tradeAmt+"=").append(api_response_params.get(tradeAmt)).append("&");
		signSrc.append(merchNo+"=").append(api_response_params.get(merchNo)).append("&");
		signSrc.append(merchParam+"=").append(api_response_params.get(merchParam)).append("&");
		signSrc.append(orderNo+"=").append(api_response_params.get(orderNo)).append("&");
		signSrc.append(tradeDate+"=").append(api_response_params.get(tradeDate)).append("&");
		signSrc.append(accNo+"=").append(api_response_params.get(accNo)).append("&");
		signSrc.append(accDate+"=").append(api_response_params.get(accDate)).append("&");
		signSrc.append(orderStatus+"=").append(api_response_params.get(orderStatus));
		signSrc.append(api_key);
		String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[精准付]-[响应支付]-2.生成加密URL签名完成，参数：" + JSON.toJSONString(paramsStr) +" ,值："+ JSON.toJSONString(signMd5));
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
    	boolean result = false;
    	//0 未支付，1 成功，2失败
    	String payStatusCode = api_response_params.get(orderStatus);
    	String responseAmount = HandlerUtil.getFen(api_response_params.get(tradeAmt));
    	//amount数据库存入的是分 	第三方返回的responseAmount是元
    	boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
    	//1代表第三方支付成功
    	if (checkAmount && payStatusCode.equalsIgnoreCase("1")) {
    		result = true;
    	} else {
    		log.error("[精准付]-[响应支付]-3.金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
    	}
    	log.debug("[精准付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
    	return result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[精准付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[精准付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}