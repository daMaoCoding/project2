package dc.pay.business.huilianfuzhifu;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;

/**
 * @author sunny
 * 04 17, 2019
 */
@ResponsePayHandler("HUILIANFU")
public final class HuiLianFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

//    字段名			变量名			必填			类型				示例值		描述
//    汇联中国商户号	merchNo			是			String(32)		商户在汇联中国平台开通的商户号
//    商户交易单号	orderNo			是			String(23)	商户交易单号	请求支付中的商户单号
//    汇联中国交易单号 	businessNo 		是			String(14)		yyyyMMddHHmmss
//    交易状态		orderState		是			String(1)	1	详见“附录4.2 交易状态编码”
//    交易金额		amount			是			String(12)	10.00	以元为单位

    private static final String merchNo                    ="merchNo";
    private static final String orderNo                    ="orderNo";
    private static final String businessNo                 ="businessNo";
    private static final String orderState                 ="orderState";
    private static final String amount             			="amount";
    private static final String code                 		="code";
    private static final String context              		="context";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("ok");

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(merchNo);
        String ordernumberR = API_RESPONSE_PARAMS.get(orderNo);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[汇联付支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
    	String sendSign="";
		JSONObject resJson1=responseStr(api_response_params);
		String content = resJson1.toJSONString()+channelWrapper.getAPI_KEY();
		MessageDigest messageDigest=null;
		try {
			messageDigest = MessageDigest.getInstance("MD5");
			messageDigest.update(content.getBytes());
			sendSign = HexUtil.byte2hex(messageDigest.digest());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
        log.debug("[汇联付支付]-[响应支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(sendSign));
        return sendSign;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
    	JSONObject resJson1=responseStr(api_response_params);
        boolean my_result = false;
        String payStatusCode = resJson1.getString("orderState");
        String responseAmount = HandlerUtil.getFen(resJson1.getString("amount"));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        boolean checkAmount = HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差
        //1代表第三方支付成功
        if (checkAmount&&payStatusCode.equalsIgnoreCase("1")) {
            my_result = true;
        } else {
            log.error("[汇联付支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[汇联付支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：10000");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[汇联付支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[汇联付支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return  RESPONSE_PAY_MSG;
    }
    
    private JSONObject responseStr(Map<String, String> api_response_params){
    	String jsonStr = HandlerUtil.mapToJson(api_response_params);
    	JSONObject resJson = JSONObject.parseObject(jsonStr);
    	byte[] recvContext = resJson.getBytes("context");
    	String recvSource="";
		try {
			recvSource = new String(recvContext,"UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		JSONObject resJson1=JSONObject.parseObject(recvSource);
		return resJson1;
    }
}