package dc.pay.business.pingfuzhifu;

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
@ResponsePayHandler("PINGFUZHIFU")
public final class PingFuZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log 	= LoggerFactory.getLogger(getClass());

//    参数					说明					示例				类型			是否为空
//    post_id				请求单号				pp20190221352d0ecd2e49e4fa		N
//    code					响应代码				1 申请成功 0等待处理				N
//    guo_ordes				流水单号				190220-449365279730116		文字	Y
//    order_statues			状态					未付款 已付款					文字	N
//    qrurl					支付地址				https://mapi.alipay.com/gateway.do?_input_charset=utf-8&service=alipay.wap.create.direct.pay.by.user..	文字	Y
//    money					金额					文字	N
//    remark				备注					文字	Y
//    sign					签名					MD5(金额+流水单号+key)	文字	

    private static final String post_id                   ="post_id";
    private static final String code                      ="code";
    private static final String guo_ordes                 ="guo_ordes";
    private static final String order_statues             ="order_statues";
    private static final String qrurl             		  ="qrurl";
    private static final String money                 	  ="money";
    private static final String remark              	  ="remark";
    
    

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("success");

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        JSONObject resJson=getResponseJson();
        String ordernumberR="";
        if(resJson!=null){
        	JSONObject data=resJson.getJSONObject("data");
        	ordernumberR = data.getString("remark");
        	if (StringUtils.isBlank(ordernumberR))
                throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
            log.debug("[拼付支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        }
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
    	JSONObject resJson=getResponseJson();
    	JSONObject data=resJson.getJSONObject("data");
    	String signSrc=String.format("%s%s%s", 
    		  data.getString("money"),
    		  data.getString("guo_ordes"),
      		  channelWrapper.getAPI_KEY()
        );
        String paramsStr = signSrc.toString();
        String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[拼付支付]-[响应支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return signMD5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        JSONObject resJson=getResponseJson();
    	JSONObject data=resJson.getJSONObject("data");
        String payStatusCode = data.getString("code");
        String responseAmount = HandlerUtil.getFen(data.getString("money"));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount&&payStatusCode.equalsIgnoreCase("1")) {
            my_result = true;
        } else {
            log.error("[拼付支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[拼付支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
    	JSONObject resJson=null;
    	boolean my_result=false;
		try {
			resJson = getResponseJson();
			JSONObject data=resJson.getJSONObject("data");
			my_result = data.getString(signature).equalsIgnoreCase(signMd5);
		} catch (PayException e) {
			e.printStackTrace();
		}
        log.debug("[拼付支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[拼付支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
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