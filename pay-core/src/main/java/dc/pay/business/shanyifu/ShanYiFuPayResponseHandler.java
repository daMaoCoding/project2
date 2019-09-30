package dc.pay.business.shanyifu;

import java.util.Map;
import java.util.TreeMap;

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
import dc.pay.utils.JsonUtil;

/**
 * 
 * @author andrew
 * Apr 19, 2018
 */
@ResponsePayHandler("SHANYIFU")
public final class ShanYiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

	//参数名				参数含义						参数长度		是否必填
	//merNo				商户号						16			是
	//netway			支付网关						16			是
	//orderNum			订单号						20			是
	//amount			金额（单位：分）					14			是
	//goodsName			商品名称						20			是
	//payResult			支付状态，00表示成功				16			是
	//payDate			支付时间，格式：yyyyMMddHHmmss	19			是
	//sign				签名（字母大写）					32			是
	private static final String merNo			 = "merNo";
	private static final String netway			 = "netway";
	private static final String orderNum		 = "orderNum";
	private static final String amount			 = "amount";
	private static final String goodsName		 = "goodsName";
	private static final String payResult		 = "payResult";
	private static final String payDate			 = "payDate";
	private static final String sign			 = "sign";
	
	private static final String data		 = "data";
	
    private static final String RESPONSE_PAY_MSG = "0";

	@Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        JSONObject parseObject = null;
        try {
        	parseObject = JSON.parseObject(API_RESPONSE_PARAMS.get(data));
		} catch (Exception e) {
			throw new PayException(SERVER_MSG.REQUEST_PAY_CHANNEL_RESULT_ERROR);
		}
		String partnerR = parseObject.get(merNo)+"";
		String ordernumberR = parseObject.get(orderNum)+"";
		if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
		    throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
		log.debug("[闪亿付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }
	
    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        JSONObject parseObject = JSON.parseObject(params.get(data));
        Map<String, String> signMap = new TreeMap<String, String>();
		signMap.put(merNo, parseObject.getString(merNo));
		signMap.put(netway, parseObject.getString(netway));
		signMap.put(orderNum, parseObject.getString(orderNum));
		signMap.put(amount, parseObject.getString(amount));
		signMap.put(goodsName, parseObject.getString(goodsName));
		signMap.put(payResult, parseObject.getString(payResult));// 支付状态
		signMap.put(payDate, parseObject.getString(payDate));// yyyyMMddHHmmss
        String str = JsonUtil.stringify(signMap);
		String pay_md5sign = HandlerUtil.getMD5UpperCase(str+channelWrapper.getAPI_KEY(),"UTF-8");
        log.debug("[闪亿付]-[响应支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        JSONObject parseObject = JSON.parseObject(API_RESPONSE_PARAMS.get(data));
        boolean result = false;
        //支付状态，00表示成功
        String payStatusCode = parseObject.get(payResult)+"";
        String responseAmount = parseObject.get(amount)+"";
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        if (checkAmount && payStatusCode.equalsIgnoreCase("00")) {
            result = true;
        } else {
            log.error("[闪亿付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[闪亿付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：00");
        return result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        JSONObject parseObject = JSON.parseObject(api_response_params.get(data));
        boolean result = signMd5.equalsIgnoreCase(parseObject.get(sign).toString());
        log.debug("[闪亿付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[闪亿付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}