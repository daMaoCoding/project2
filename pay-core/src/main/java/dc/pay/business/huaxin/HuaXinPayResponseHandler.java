package dc.pay.business.huaxin;

import java.io.UnsupportedEncodingException;
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
import dc.pay.business.ruijietong.RuiJieTongUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Jul 5, 2018
 */
@ResponsePayHandler("HUAXIN")
public final class HuaXinPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //数据类 
    //输入项              输入项名称            属性             注释                          型 
    //orderNum            订单号                M                商户生成的订单号              as..32
    //merNo               商户号                M                商户号                        as..30
    //data                回调数据              M                通过 RSA加密                  As..20 
    //data字段解密后数据包含以下参数 
    //输入项             输入项名称            属性              注释                          数据类型 
    //orderNum           订单号                M                                               as..32
    //merNo              商户号                M                商户号                         as..30
    //netway             网关类型              M                见 1.7.2附录                   As..20
    //amount             金额                  M                                               As..20
    //goodsName          商品名称              M                                               As..20 
    //payResult          支付结果              M                见 1.7.3支付状态代码字典        As..20
    //payDate            支付日期              M                                               As..20 
    //sign               签名                  M                                               As..20 
    private static final String orderNum                   ="orderNum";
//    private static final String merNo                      ="merNo";
    private static final String data                       ="data";
    
//    private static final String orderNum                   ="orderNum";
    private static final String merchNo                      ="merchNo";
//    private static final String netway                     ="netway";
    private static final String amount                     ="amount";
//    private static final String goodsName                  ="goodsName";
    private static final String payResult                  ="payResult";
//    private static final String payDate                    ="payDate";
    
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "SUCCESS";
    
    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(merchNo);
        String ordernumberR = API_RESPONSE_PARAMS.get(orderNum);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[华信]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
    	byte[] result = RuiJieTongUtil.decryptByPrivateKey(org.apache.shiro.codec.Base64.decode(api_response_params.get(data)),channelWrapper.getAPI_KEY().split("-")[1]);
    	String resultData = null;
		try {
			resultData = new String(result, RuiJieTongUtil.CHARSET);
		} catch (UnsupportedEncodingException e) {
			 throw new PayException("华信,解析响应支付返回加密数据出错");
		}
		Map<String, String> map = handlerUtil.jsonToMap(JSON.parseObject(resultData).toString());
		map.remove(signature);
        String signMd5 = HandlerUtil.getMD5UpperCase(JSON.toJSONString(new TreeMap<>(map))+channelWrapper.getAPI_KEY().split("-")[0]);
        log.debug("[华信]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
    	byte[] result = RuiJieTongUtil.decryptByPrivateKey(org.apache.shiro.codec.Base64.decode(api_response_params.get(data)),channelWrapper.getAPI_KEY().split("-")[1]);
    	String resultData = null;
		try {
			resultData = new String(result, RuiJieTongUtil.CHARSET);
		} catch (UnsupportedEncodingException e) {
			 throw new PayException("华信,解析响应支付返回加密数据出错");
		}
		JSONObject parseObject = JSON.parseObject(resultData);
        boolean my_result = false;
        //payResult        交易状态    00 -支付成功    
        String payStatusCode = parseObject.getString(payResult);
        String responseAmount = parseObject.getString(amount);
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("00")) {
            my_result = true;
        } else {
            log.error("[华信]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[华信]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：00");
        return my_result;
    }



    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
    	byte[] result = RuiJieTongUtil.decryptByPrivateKey(org.apache.shiro.codec.Base64.decode(api_response_params.get(data)),channelWrapper.getAPI_KEY().split("-")[1]);
    	String resultData = null;
		try {
			resultData = new String(result, RuiJieTongUtil.CHARSET);
		} catch (UnsupportedEncodingException e) {
			e.getStackTrace();
			log.error("[华信]-[响应支付],解析响应支付返回加密数据出错");
		}
		JSONObject parseObject = JSON.parseObject(resultData);
        boolean my_result = parseObject.getString(signature).equalsIgnoreCase(signMd5);
        log.debug("[华信]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }


    @Override
    protected String responseSuccess() {
        log.debug("[华信]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}