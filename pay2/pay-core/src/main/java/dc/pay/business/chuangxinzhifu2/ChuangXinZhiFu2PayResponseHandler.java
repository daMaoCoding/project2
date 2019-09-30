package dc.pay.business.chuangxinzhifu2;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.JsonUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.Sha1Util;

/**
 * @author sunny
 * @date 26 Sep 2019
 */
@ResponsePayHandler("CHUANGXINZHIFU2")
public final class ChuangXinZhiFu2PayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String mch_code                   ="mch_code";
    private static final String order_no                   ="order_no";
    private static final String real_amount                ="real_amount";
    private static final String status                	   ="status";
    private static final String resultData                 ="resultData";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("success");

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        JSONObject resJson=getResultJson();
        String partnerR = resJson.getString(mch_code);
        String ordernumberR = resJson.getString(order_no);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[创新支付2]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
    	JSONObject resJson=getResultJson();
    	List paramKeys = MapUtils.sortMapByKeyAsc(HandlerUtil.jsonToMap(resJson.toJSONString()));
    	paramKeys.remove(signature);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (StringUtils.isNotBlank(resJson.getString(paramKeys.get(i).toString()))) {
            	signSrc.append(paramKeys.get(i)).append("=").append(resJson.getString(paramKeys.get(i).toString())).append("&");
            }
        }
        //最后一个&转换成#
        signSrc.append(key+"="+channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[创新支付2]-[响应支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return signMD5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
    	JSONObject resJson=getResultJson();
        boolean my_result = false;
        String payStatusCode = resJson.getString(status);
        String responseAmount = HandlerUtil.getFen(resJson.getString(real_amount));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        boolean checkAmount = HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差
        //1代表第三方支付成功
        if (checkAmount&&payStatusCode.equalsIgnoreCase("1")) {
            my_result = true;
        } else {
            log.error("[创新支付2]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[创新支付2]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
    	JSONObject resJson=null;
		try {
			resJson = getResultJson();
		} catch (PayException e) {
			e.printStackTrace();
		}
        boolean my_result = resJson.getString(signature).equalsIgnoreCase(signMd5);
        log.debug("[创新支付2]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[创新支付2]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return  RESPONSE_PAY_MSG;
    }
    
    private JSONObject getResultJson() throws PayException{
    	String data=API_RESPONSE_PARAMS.get(resultData);
        JSONObject resJson=null;
		try {
			resJson = JSONObject.parseObject(data);
			if(ObjectUtils.isEmpty(resJson)){
				throw new PayException("[创新支付2]-[响应支付]-获取支付请求结果：" + JSON.toJSONString(API_RESPONSE_PARAMS));
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error("[创新支付2]-[响应支付]-获取支付请求结果：" + JSON.toJSONString(API_RESPONSE_PARAMS) + "订单号："+ channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
			throw new PayException(JSON.toJSONString(API_RESPONSE_PARAMS));
		}
		return resJson;
    }
}