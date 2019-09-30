package dc.pay.business.mingjiefu;

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
import dc.pay.utils.JsonUtil;
import dc.pay.utils.MapUtils;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * May 5, 2018
 */
@ResponsePayHandler("MINGJIEFU")
public final class MingJieFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    
	//参数名					参数含义					参数长度			是否必填
	//merchNo				商户号					16				是
	//netwayCode			支付网关					16				是
	//orderNum				订单号					20				是
	//amount				金额（单位：分）				14				是
	//goodsName				商品名称					20				是
	//payStateCode			支付状态，00表示成功			16				是
	//payDate				支付时间，格式：yyyyMMddHHmmss			19				是
	//sign					签名（字母大写）				32				是
	private static final String merchNo	  ="merchNo";
//	private static final String netwayCode	  ="netwayCode";
	private static final String orderNum	  ="orderNum";
	private static final String amount	  ="amount";
//	private static final String goodsName	  ="goodsName";
	private static final String payStateCode  ="payStateCode";
//	private static final String payDate	  ="payDate";
	
	private static final String data	  ="data";
	
	//signature	数据签名	32	是	　
	private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "SUCCESS";
    
    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String string = API_RESPONSE_PARAMS.get(data);
        JSONObject parseObject = null;
        try {
      	  parseObject = JSON.parseObject(string);
        } catch (Exception e) {
      	  throw new PayException(SERVER_MSG.REQUEST_PAY_CHANNEL_RESULT_ERROR);
        }
        String partnerR = parseObject.getString(merchNo);
        String ordernumberR = parseObject.getString(orderNum);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[明捷付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
    	Map sort = MapUtils.sortMapByKeyAsc2(JsonUtil.parse(api_response_params.get(data).toString(), Map.class));
    	sort.remove(signature);
        String signMd5 = HandlerUtil.getMD5UpperCase(YiDao2Util.mapToJson(sort)+api_key);
        log.debug("[明捷付]-[响应支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        JSONObject parseObject = JSON.parseObject(api_response_params.get(data));
        boolean result = false;
        //支付状态，00表示成功
        String payStatusCode = parseObject.getString(payStateCode);
        String responseAmount = parseObject.getString(amount);
        //db_amount数据库存入的是分 	第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("00")) {
            result = true;
        } else {
            log.error("[明捷付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[明捷付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：00");
        return result;
    }


    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
    	JSONObject parseObject = JSON.parseObject(api_response_params.get(data));
        boolean result = parseObject.getString(signature).equalsIgnoreCase(signMd5);
        log.debug("[明捷付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[明捷付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}