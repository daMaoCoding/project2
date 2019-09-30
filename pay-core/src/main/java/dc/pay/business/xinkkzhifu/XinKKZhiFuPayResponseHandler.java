package dc.pay.business.xinkkzhifu;

import java.util.List;
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
import dc.pay.utils.MapUtils;
import dc.pay.utils.Sha1Util;

/**
 * 
 * @author sunny Dec 14, 2018
 */
@ResponsePayHandler("XINKKZHIFU")
public final class XinKKZhiFuPayResponseHandler extends PayResponseHandler {
	private final Logger log = LoggerFactory.getLogger(getClass());

//	字段名			变量名			必返回			类型				说明
//	订单号			orderNumber		是				String			商户订单号(不超过50位)
//	平台订单号		platNumber		是				String			系统平台返回的订单
//	金额				amount			是				Int				支付金额 单位为分
//	商务Id			merchId			是				Int				商户注册获得的商务Id
//	支付类型			payType			是				String			微信公众号为wxwap；微信扫码为wxqrcode；微信H5为wxhtml；支付宝扫码为aliqrcode；支付宝为aliwap；银联为ylpay；银联扫码为ylqrcode；QQ扫码为qqqrcode；QQ公众号为qqwap；
//	透传参数			other			N				String			商户下单时传的透传参数
//	支付状态			status			是				Int				1：成功；0：失败
//	时间戳			timestamp		是				Int				时间戳
//	签名				sign			是				String	

	private static final String orderNumber 				= "orderNumber";
	private static final String platNumber 					= "platNumber";
	private static final String amount 						= "amount";
	private static final String merchId 					= "merchId";
	private static final String payType 					= "payType";
	private static final String other 						= "other";
	private static final String status 						= "status";
	private static final String timestamp 					= "timestamp";

	private static final String key = "key";
	// signature 数据签名 32 是
	private static final String signature = "sign";

	private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("SUCCESS");

	@Override
	public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
		if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
			throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
		String partnerR = API_RESPONSE_PARAMS.get(merchId);
		String ordernumberR = API_RESPONSE_PARAMS.get(orderNumber);
		if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
			throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
		log.debug("[新KK支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}", ordernumberR);
		return ordernumberR;
	}

	@Override
	protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
		List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
		paramKeys.remove(signature);
		paramKeys.remove(other);
		StringBuilder signSrc = new StringBuilder();
		for (int i = 0; i < paramKeys.size(); i++) {
			if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
				signSrc.append(paramKeys.get(i)).append(api_response_params.get(paramKeys.get(i)));
			}
		}
		signSrc.append(channelWrapper.getAPI_KEY());
		String paramsStr = signSrc.toString();
		String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr);
		log.debug("[新KK支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMD5));
		return signMD5;
	}

	@Override
	protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount)throws PayException {
		boolean my_result = false;
		String payStatusCode = api_response_params.get(status);
		String responseAmount = api_response_params.get(amount);
		// db_amount数据库存入的是分 第三方返回的responseAmount是元
		// boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
		boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
		// 1代表第三方支付成功
		if (checkAmount && payStatusCode.equalsIgnoreCase("1")) {
			my_result = true;
		} else {
			log.error("[新KK支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode+ " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
		}
		log.debug("[新KK支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount+ " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode+ " ,计划成功：1");
		return my_result;
	}

	@Override
	protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
		boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
		log.debug("[新KK支付]-[响应支付]-4.验证MD5签名：{}", my_result);
		return my_result;
	}

	@Override
	protected String responseSuccess() {
		log.debug("[新KK支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
		return RESPONSE_PAY_MSG;
	}
}