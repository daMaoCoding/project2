package dc.pay.business.xinkuangbaozhifu;

import java.util.List;
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
import dc.pay.utils.MapUtils;
import dc.pay.utils.Sha1Util;

/**
 * @author sunny
 * @date 13 Sep 2019
 */
@ResponsePayHandler("XINKUANGBAOZHIFU")
public final class XinKuangBaoZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String code                   ="code";
    private static final String merchant_order_sn      ="merchant_order_sn";
    private static final String order_money            ="order_money";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";
    
    private static final String apikey        ="apikey";

    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("{\"code\":200,\"msg\":\"提交成功\"}");

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String data=API_RESPONSE_PARAMS.get("data");
        JSONObject resJson=null;
		try {
			resJson = JSONObject.parseObject(data);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("[新狂暴支付]-[响应支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(data) + "订单号："+ channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
			throw new PayException(data);
		}
        String ordernumberR = resJson.getString(merchant_order_sn);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[新狂暴支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {String data=API_RESPONSE_PARAMS.get("data");
    JSONObject resJson=null;
	try {
		resJson = JSONObject.parseObject(data);
	} catch (Exception e) {
		e.printStackTrace();
		log.error("[新狂暴支付]-[响应支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(data) + "订单号："+ channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
		throw new PayException(data);
	}
	List paramKeys = MapUtils.sortMapByKeyAsc(JSON.parseObject(data,Map.class));
	paramKeys.remove(signature);
    StringBuilder signSrc = new StringBuilder();
    for (int i = 0; i < paramKeys.size(); i++) {
//        if (!pay_productname.equals(paramKeys.get(i)) && !pay_attach.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
        if (StringUtils.isNotBlank(resJson.getString(paramKeys.get(i).toString()))) {
        	signSrc.append(paramKeys.get(i)).append("=").append(HandlerUtil.UrlEncode(resJson.getString(paramKeys.get(i).toString()))).append("&");
        }
    }
    //最后一个&转换成#
    //signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "#" );
    signSrc.append(apikey+"="+channelWrapper.getAPI_KEY());
    String paramsStr = signSrc.toString().toUpperCase();
    String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr);
    log.debug("[新狂暴支付]-[响应支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
    return signMD5;}

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
    	String data=api_response_params.get("data");
        JSONObject resJson=null;
		try {
			resJson = JSONObject.parseObject(data);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("[新狂暴支付]-[响应支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(data) + "订单号："+ channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
			throw new PayException(data);
		}
        boolean my_result = false;
        String payStatusCode = api_response_params.get("code");
        String responseAmount = HandlerUtil.getFen(resJson.getString("order_money"));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        boolean checkAmount = HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差
        //1代表第三方支付成功
        if (checkAmount&&payStatusCode.equalsIgnoreCase("200")) {
            my_result = true;
        } else {
            log.error("[新狂暴支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[新狂暴支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：200");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
    	String data=api_response_params.get("data");
        JSONObject resJson=null;
		try {
			resJson = JSONObject.parseObject(data);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("[新狂暴支付]-[响应支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(data) + "订单号："+ channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
		}
        boolean my_result = resJson.getString(signature).equalsIgnoreCase(signMd5);
        log.debug("[新狂暴支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[新狂暴支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return  RESPONSE_PAY_MSG;
    }
}