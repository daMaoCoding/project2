package dc.pay.business.twoonezhifu;

import java.util.List;
import java.util.Map;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.utils.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;

import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;

/**
 * @author cobby
 * Jan 28, 2019
 */
@ResponsePayHandler("TWOONEZHIFU")
public final class TwoOneZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());
// amount	实际支付金额	string(128)	一定存在。用户实际付款金额
// order_number	订单号	string(128)	一定存在。是您在发起付款接口传入的您的自定义订单号
// key	秘钥	string(32)	一定存在。我们把使用到的所有参数，连您的merkey一起，按参数名字母升序排序。把参数值拼接在一起。做md5-32位加密，取字符串大写。得到key。您需要在您的服务端按照同样的算法，自己验证此key是否正确。只在正确时，执行您自己逻辑中支付成功代码。

	private static final String amount            ="amount";         //订单金额
	private static final String order_number      ="order_number"; //交易流水号
	private static final String key               ="key";

	private static final String RESPONSE_PAY_MSG = "200";

	@Override
	public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {

		if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
			throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
		String orderId = API_RESPONSE_PARAMS.get(order_number);
		if ( StringUtils.isBlank(orderId))
			throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
		log.debug("[二一支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,orderId);
		return orderId;
	}

	@Override
	protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
		String signMd5 = null;
		List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < paramKeys.size(); i++) {
			if(StringUtils.isBlank(api_response_params.get(paramKeys.get(i))) || key.equalsIgnoreCase(paramKeys.get(i).toString()   )   ||key.equalsIgnoreCase(paramKeys.get(i).toString())  )  //
				continue;
			sb.append(api_response_params.get(paramKeys.get(i)));
		}
		sb.append(channelWrapper.getAPI_KEY());
		String signStr = sb.toString(); //.replaceFirst("&key=","")
		signMd5 = HandlerUtil.getMD5UpperCase(signStr);
		log.debug("[二一支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
		return signMd5;

	}

	@Override
	protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
		boolean my_result = false;
		//status        交易状态    1    是    0-未支付    1-支付成功        2-支付失败
//		String payStatusCode = api_response_params.get(returncode);
		String responseAmount = HandlerUtil.getFen(api_response_params.get(amount));
		//db_amount数据库存入的是分     第三方返回的responseAmount是元
		boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
		//1代表第三方支付成功
		if (checkAmount ) {//&& payStatusCode.equalsIgnoreCase("00")
			my_result = true;
		} else {
			log.error("[二一支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + "失败" + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
		}
		log.debug("[二一支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + 200 + " ,计划成功：200");
		return my_result;
	}

	@Override
	protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
		boolean my_result = api_response_params.get(key).equalsIgnoreCase(signMd5);
		log.debug("[二一支付]-[响应支付]-4.验证MD5签名：{}", my_result);
		return my_result;
	}

	@Override
	protected String responseSuccess() {
		log.debug("[二一支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
		return RESPONSE_PAY_MSG;
	}
}