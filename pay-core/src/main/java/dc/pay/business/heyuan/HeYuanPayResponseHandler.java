package dc.pay.business.heyuan;

import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
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
import dc.pay.utils.UnicodeUtil;

/**
 * 
 * @author sunny
 * Dec 14, 2018
 */
@ResponsePayHandler("HEYUAN")
public final class HeYuanPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

//    参数名称			变量名			类型长度			是否可空	说明
//    版本号				version			varchar(5)		默认1.0.0
//    机构订单号			orgOrderNo		varchar(32)		
//    平台订单号			systemId		varchar(32)		
//    订单支付状态		payStatus		varchar(6)		附录2
//    下单时间			downTime		varchar(20)		
//    支付时间			payTime			varchar(20)		
//    支付金额（单位分）		payAmount		decimal(10,2)		
//    结果代码			respCode		Varchar(6)		附录2
//    返回信息			respMsg			Varchar(100)		

    private static final String version                     ="version";
    private static final String orgOrderNo                  ="orgOrderNo";
    private static final String systemId                    ="systemId";
    private static final String payStatus              		="payStatus";
    private static final String downTime                    ="downTime";
    private static final String payTime                     ="payTime";
    private static final String payAmount                   ="payAmount";
    private static final String respCode                    ="respCode";
    private static final String respMsg                     ="respMsg";

    
    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = jsonResponsePayMsg("SUCCESS");

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
    	if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
    	String decodeBody="";
		try {
			String apiKey=handlerUtil.getStrFromRedis(API_RESPONSE_PARAMS.get("partnerNo"));
			decodeBody = AesSignUtil.decrypt(apiKey,API_RESPONSE_PARAMS.get("encryptData"),API_RESPONSE_PARAMS.get("sign"));
		} catch (Exception e) {
			 throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
		}
    	JSONObject resJson=JSONObject.parseObject(UnicodeUtil.unicodeToString(decodeBody));
        String partnerR = resJson.get(orgOrderNo).toString();
        String ordernumberR = resJson.get(orgOrderNo).toString();
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[合源]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
    	String apiKey=handlerUtil.getStrFromRedis(API_RESPONSE_PARAMS.get("partnerNo"));
    	String dataPlain = AES.decode(Base64.decode(api_response_params.get("encryptData")), apiKey.substring(0, 16));
    	String sha1Sign = DigestUtils.sha1Hex(dataPlain + apiKey.substring(16));
    	log.debug("[合源]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(sha1Sign));
		return sha1Sign;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        String decodeBody="";
        try {
			decodeBody = AesSignUtil.decrypt(channelWrapper.getAPI_KEY(),API_RESPONSE_PARAMS.get("encryptData"),API_RESPONSE_PARAMS.get("sign"));
		} catch (Exception e) {
			 throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
		}
    	JSONObject resJson=JSONObject.parseObject(UnicodeUtil.unicodeToString(decodeBody));
        //returncode          交易状态         是            “00” 为成功
        String payStatusCode = resJson.get(payStatus).toString();
        String responseAmount = HandlerUtil.getFen(resJson.get(payAmount).toString());
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        //boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //实际支付金额  有可能比提交金额少0.01-0.05
        boolean checkAmount =  HandlerUtil.isRightAmount(db_amount,responseAmount,"5");//第三方回调金额差额0.05元内
        //1代表第三方支付成功
        if (checkAmount&&payStatusCode.equalsIgnoreCase("300001")) {
            my_result = true;
        } else {
            log.error("[合源]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[合源]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：00");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[合源]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[合源]]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}