package dc.pay.business.haoyongbao;

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
 * 
 * @author sunny
 * 03 11, 2019
 */
@ResponsePayHandler("HAOYONGBAO")
public final class HaoYongBaoPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

//    参数名					类型					说明
//    noise					string(16-32)		随机串,噪音元素
//    notify_id				string				每次异步通知的唯一标识
//    notify_time			string(15)			异步通知发起请求时的Unix毫秒时间戳
//    sequence				string				商户创建支付订单时携带的内部订单编号
//    order_id				string				支付系统的唯一订单编号
//    origin_amount			string(decimal(10,2))	订单原始金额
//    payed_amount			string(decimal(10,2))	实际支付金额
//    platform				int						选择的支付平台,以下值取其一: 1: alipay, 2: weixin
//    up_order_id			string					上游订单号,支付宝或微信的订单号
//    up_create_time		string				上游订单创建时间,支付宝或微信的订单号
//    pay_status			int					支付状态 0:未支付 1:支付 2:部分支付 3:超付 5:关闭
//    signature				string(32)			参数签名值,详见签名算法

    private static final String noise                   	 ="noise";
    private static final String notify_id                    ="notify_id";
    private static final String notify_time                  ="notify_time";
    private static final String sequence                	 ="sequence";
    private static final String origin_amount             	 ="origin_amount";
    private static final String payed_amount                 ="payed_amount";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="signature";

    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("success");

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        JSONObject resJson=getResponseJson();
        String ordernumberR = resJson.getString(sequence);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[好用宝支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
    	JSONObject resJson=getResponseJson();
    	String signSrc=String.format("%s%s%s%s%s%s%s%s%s%s%s%s", 
    			noise+"="+resJson.getString(noise)+"&",
    			notify_id+"="+resJson.getString(notify_id)+"&",
    			notify_time+"="+resJson.getString(notify_time)+"&",
    			"order_id"+"="+resJson.getString("order_id")+"&",
    			"origin_amount"+"="+resJson.getString("origin_amount")+"&",
    			"pay_status"+"="+resJson.getString("pay_status")+"&",
    			"payed_amount"+"="+resJson.getString("payed_amount")+"&",
    			"platform"+"="+resJson.getString("platform")+"&",
    			"sequence"+"="+resJson.getString("sequence")+"&",
    			"up_create_time"+"="+resJson.getString("up_create_time")+"&",
    			"up_order_id"+"="+resJson.getString("up_order_id")+"&",
    			channelWrapper.getAPI_KEY()
    	);
        String paramsStr = signSrc.toString();
        String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[好用宝支付]-[响应支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return signMD5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
    	JSONObject resJson=getResponseJson();
        boolean my_result = false;
        String payStatusCode = resJson.getString("pay_status");
        String responseAmount = HandlerUtil.getFen(resJson.getString("origin_amount"));
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount&&payStatusCode.equalsIgnoreCase("1")) {
            my_result = true;
        } else {
            log.error("[好用宝支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[好用宝支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
    	boolean my_result=false;
    	try {
    		 JSONObject resJson=getResponseJson();
			 my_result = resJson.getString(signature).equalsIgnoreCase(signMd5);
		} catch (PayException e) {
			e.printStackTrace();
		}
        log.debug("[好用宝支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[好用宝支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return  RESPONSE_PAY_MSG;
    }
    
    private JSONObject getResponseJson() throws PayException{
    	String responseJson = "";
        for (String keyValue : API_RESPONSE_PARAMS.keySet()) {
        	 responseJson = keyValue;
        }
        JSONObject resJson;
        try {
            resJson = JSONObject.parseObject(responseJson);
        } catch (Exception e) {
            e.printStackTrace();
            throw new PayException(responseJson);
        }
        if(null==resJson){
        	throw new PayException(responseJson);
        }
        return resJson;
    }
}