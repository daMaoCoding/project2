package dc.pay.business.xinhuanqiuzhifu;

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
 * 12 04, 2019
 */
@ResponsePayHandler("XINHUANQIUZHIFU")
public final class XinHuanQiuZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

//    字段名				变量名			类型			说明			可空
//    版本号				version			String(3)	当前接口版本号1.0	N
//    商户ID				partnerid		String(4)	商户在平台的用户ID	N
//    商户订单号			partnerorderid	String(32)	商户订单号	N
//    订单总金额			payamount		Int			单位：分	N
//    订单状态			orderstatus		Date		订单状态： 0待支付，1 已支付，2 支付失败，3 提交失败,4 已结算，5 未结算，6 结算失败，7 冻结，9 异常	N
//    平台订单号			orderno			String(32)	平台订单号	N
//    订单完成时间		okordertime		Date		订单完成时间	N
//    支付类型			paytype			String(10)	支付类型见8表	N
//    MD5签名			sign			String(32)	MD5签名结果	N
//    订单交易结果说明		message			String(255)	订单交易结果说明（0 支付成功，其他失败）	N
//    商家自定义数据包		remark			String(50)	商户自定义数据包，原样返回，例如：可填写会员ID(验签时为编码状态)	Y

    private static final String version                   	="version";
    private static final String partnerid                   ="partnerid";
    private static final String partnerorderid              ="partnerorderid";
    private static final String payamount                	="payamount";
    private static final String orderstatus             	="orderstatus";
    private static final String amount                 ="amount";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("success");

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        JSONObject resJson=getResponseJson();
        String ordernumberR = resJson.getString(partnerorderid);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[新环球支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
    	String responseJson="";
    	for (String keyValue : API_RESPONSE_PARAMS.keySet()) {
       	 	responseJson = keyValue;
        }
    	Map<String, String> responseMap=JSON.parseObject(responseJson,Map.class);  	
    	List paramKeys = MapUtils.sortMapByKeyAsc(responseMap);
    	paramKeys.remove(signature);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
//            if (!pay_productname.equals(paramKeys.get(i)) && !pay_attach.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
            if (StringUtils.isNotBlank(responseMap.get(paramKeys.get(i)))) {
            	signSrc.append(paramKeys.get(i)).append("=").append(responseMap.get(paramKeys.get(i))).append("&");
            }
        }
        //最后一个&转换成#
        //signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "#" );
        signSrc.append(key+"="+channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[新环球支付]-[响应支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return signMD5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
    	JSONObject resJson=getResponseJson();
        boolean my_result = false;
        String payStatusCode =resJson.getString(orderstatus);
        String responseAmount = resJson.getString(payamount);
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        boolean checkAmount = HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差
        //1代表第三方支付成功
        if (checkAmount&&(payStatusCode.equalsIgnoreCase("1")||payStatusCode.equalsIgnoreCase("4"))) {
            my_result = true;
        } else {
            log.error("[新环球支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[新环球支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1或者4");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
    	boolean my_result=false;
    	try {
			JSONObject resJson=getResponseJson();
			my_result = resJson.getString(signature).equalsIgnoreCase(signMd5);
	        log.debug("[新环球支付]-[响应支付]-4.验证MD5签名：{}", my_result);
		} catch (PayException e) {
			e.printStackTrace();
		}
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[新环球支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
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