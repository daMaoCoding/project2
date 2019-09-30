package dc.pay.business.renzhizhifu;

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
 * Dec 14, 2018
 */
@ResponsePayHandler("RENZHIZHIFU")
public final class RenZhiZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

//    字段 			字段说明 				字段类型 			 备注
//    money			到账金额
//    old_money		订单发起时的金额
//    record		本平台订单号
//    remark		支付类型  固定为 -> 商品
//    order_id		开发者平台订单号
//    sign			签名 md5(md5(old_money.order_id.record.key)) //key	商户key		//old_money	强制两位小数后计算		//签名无连接符

    private static final String money                   	 ="money";
    private static final String old_money                    ="old_money";
    private static final String record                  	 ="record";
    private static final String remark                		 ="remark";
    private static final String order_id             		 ="order_id";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("SUCCESS");

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        JSONObject resJson=getResponseJson();
        String ordernumberR = resJson.getString(order_id);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[仁智支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        JSONObject resJson=getResponseJson();
    	String signSrc=String.format("%s%s%s%s", 
    			resJson.getString(old_money),
    			resJson.getString(order_id),
    			resJson.getString(record),
    			channelWrapper.getAPI_KEY()
    			);
        String paramsStr = signSrc.toString();
        String signMD5 = HandlerUtil.getMD5UpperCase(HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase()).toLowerCase();
        log.debug("[仁智支付]-[响应支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return signMD5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
    	JSONObject resJson=getResponseJson();
        boolean my_result = false;
        String payStatusCode = "无";
        String responseAmount = HandlerUtil.getFen(resJson.getString(money));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount&&payStatusCode.equalsIgnoreCase("无")) {
            my_result = true;
        } else {
            log.error("[仁智支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[仁智支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：无");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
    	JSONObject resJson=null;
		try {
			resJson = getResponseJson();
		} catch (PayException e) {
			e.printStackTrace();
		}
        boolean my_result = resJson.getString(signature).equalsIgnoreCase(signMd5);
        log.debug("[仁智支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[仁智支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
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