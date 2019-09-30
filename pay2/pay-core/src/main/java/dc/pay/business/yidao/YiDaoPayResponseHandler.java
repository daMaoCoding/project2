package dc.pay.business.yidao;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.tomcat.util.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.kspay.AESUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * May 1, 2018
 */
@ResponsePayHandler("YIDAO")
public final class YiDaoPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //signature	数据签名	32	是	　
    private static final String signature  ="sign";
    
	//来用回传订单：第三方不能处理本字段 
	private static final String extra_para	  ="extra_para";
    private static final String reqJson  ="reqJson";
    private static final String merchantCode  ="merchantCode";
    private static final String transData  = "transData";
    private static final String Status  ="Status";
    private static final String totalAmount  ="totalAmount";

    private static final String RESPONSE_PAY_MSG = "SUCCESS";
    
    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String string = API_RESPONSE_PARAMS.get(reqJson);
        JSONObject parseObject = null;
        try {
      	  parseObject = JSON.parseObject(string);
        } catch (Exception e) {
      	  throw new PayException(SERVER_MSG.REQUEST_PAY_CHANNEL_RESULT_ERROR);
        }
        String partnerR = parseObject.getString(merchantCode);
        String ordernumberR = parseObject.getString(extra_para);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[易到]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        JSONObject parseObject = JSON.parseObject(api_response_params.get(reqJson));
        //解码报文
        String encrypt = AESUtil.decrypt(parseObject.getString(transData), channelWrapper.getAPI_KEY().split("-")[0]);
        byte[] decodeBase64 = Base64.decodeBase64(encrypt.getBytes());
        Map<String, String> urlToMap = HandlerUtil.urlToMap(new String(decodeBase64));
        urlToMap.remove(signature);
        String signStrByMap = null;
		try {
			signStrByMap = YiDao2Util.sort(urlToMap);
		} catch (Exception e) {
			e.printStackTrace();
		}
        String aesPrivage = AESUtil.encrypt(Base64.encodeBase64String(signStrByMap.getBytes()), channelWrapper.getAPI_KEY().split("-")[0]);
        String signMd5 = HandlerUtil.getMD5UpperCase(aesPrivage+channelWrapper.getAPI_KEY().split("-")[1]);
        log.debug("[易到]-[响应支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        JSONObject parseObject = JSON.parseObject(api_response_params.get(reqJson));
        String encrypt = AESUtil.decrypt(parseObject.getString(transData), channelWrapper.getAPI_KEY().split("-")[0]);
        byte[] decodeBase64 = Base64.decodeBase64(encrypt.getBytes());
        Map<String, String> urlToMap = HandlerUtil.urlToMap(new String(decodeBase64));
        boolean result = false;
        //1已支付  0未支付
        String payStatusCode = urlToMap.get(Status);
        String responseAmount = HandlerUtil.getFen(urlToMap.get(totalAmount));
        //db_amount数据库存入的是分 	第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("0")) {
            result = true;
        } else {
            log.error("[易到]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[易到]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：0");
        return result;
    }


    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        JSONObject parseObject = JSON.parseObject(api_response_params.get(reqJson));
        String encrypt = AESUtil.decrypt(parseObject.getString(transData), channelWrapper.getAPI_KEY().split("-")[0]);
        byte[] decodeBase64 = Base64.decodeBase64(encrypt.getBytes());
        Map<String, String> urlToMap = HandlerUtil.urlToMap(new String(decodeBase64));
        boolean result = urlToMap.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[易到]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[易到]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}