package dc.pay.business.yalefu;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;

/**
 * 
 * @author sunny
 * Dec 14, 2018
 */
@ResponsePayHandler("YALEFU")
public final class YaLeFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

//    参数					参数名称 				类型（长度）			是否有值			签名顺序			说明
//    merchantCode			商户号				String(15)			必有				1				支付平台分配的唯一商户号
//    orderNo				商户订单号			String(100)			必有				2				商户上送的订单号。
//    amount				订单金额				Number(9)			必有				3				上送的订单金额，以分为单位，整数。
//    successAmt			成功支付金额			Number(9)			必有				4				表示用户实际支付的金额，以分为单位，整数。一般会和amount值一致
//    payOrderNo			支付订单号			String(40)			必有				5				亚乐付生成的订单号
//    orderStatus			订单状态				String(10)			必有				6				Success 支付成功 Fail 支付失败
//    extraReturnParam		公用回传参数			String(100)			必有				7				商户如果支付请求时传递了该参数，则通知商户支付成功时会原样回传该参数(允许值为空字符串，如： extraReturnParam="")。
//    signType				签名方式				String(10)			必有								固定值：RSA，不参与签名
//    sign					签名					String				必有								使用RSA方式加密，请参考Demo

    private static final String merchantCode                   	="merchantCode";
    private static final String orderNo                    		="orderNo";
    private static final String amount                  		="amount";
    private static final String successAmt                		="successAmt";
    private static final String payOrderNo             			="payOrderNo";
    private static final String orderStatus                 	="orderStatus";
    private static final String extraReturnParam              	="extraReturnParam";
    private static final String signType              			="signType";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("SUCCESS");

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(merchantCode);
        String ordernumberR = API_RESPONSE_PARAMS.get(orderNo);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[亚乐付支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
    	String signSrc=String.format("%s%s%s%s%s%s%s", 
    			merchantCode+"="+api_response_params.get(merchantCode)+"&",
    			orderNo+"="+api_response_params.get(orderNo)+"&",
    			amount+"="+api_response_params.get(amount)+"&",
    			successAmt+"="+api_response_params.get(successAmt)+"&",
    			payOrderNo+"="+api_response_params.get(payOrderNo)+"&",
    			orderStatus+"="+api_response_params.get(orderStatus)+"&",
    			extraReturnParam+"="+api_response_params.get(extraReturnParam)
        		);
    	//String publicKeyStr = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC0sJMGLD0UQUYObjsMHBGUYQEVEOCkBCNzzkYWSM0RYToK49hLpmxpNLbNcSMSUwOs6AfzDW9Tbpcotjg4JiphZqrBjG4Vj2acQPxBp06oJBYdvoCM42AFFLthHNDTmP+O7OYPrwiTTSYPlIUO8HyojhfQ6Dc9guiit7L98FWhmQIDAQAB";
        Boolean result = validataSign(signSrc, api_response_params.get(signature),channelWrapper.getAPI_PUBLIC_KEY());
        log.debug("[亚乐付支付]-[响应支付]-2.生成加密URL签名完成：{}" ,result);
        return String.valueOf(result);	
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        String payStatusCode = api_response_params.get(orderStatus);
        String responseAmount = api_response_params.get(successAmt);
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount&&payStatusCode.equalsIgnoreCase("Success")) {
            my_result = true;
        } else {
            log.error("[亚乐付支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[亚乐付支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：Success");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
    	Boolean signMd5Boolean = Boolean.valueOf(signMd5);
        log.debug("[亚乐付支付]-[响应支付]-4.验证MD5签名：" + signMd5Boolean.booleanValue());
        return signMd5Boolean.booleanValue();
    }

    @Override
    protected String responseSuccess() {
        log.debug("[亚乐付支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return  RESPONSE_PAY_MSG;
    }
    
    /** 
     * RSA validate signature
     * @param content: Signature data to be signed 
     * @param sign: Signature value
     * @param publicKey: merchant's public key
     * @param encode: Character set coding
     * @return boolean
     */  
     public static final String SIGN_ALGORITHMS = "SHA1WithRSA";  
     public static boolean validataSign(String content, String sign, String publicKey)  
     {  
         try   
         {  
             KeyFactory keyFactory = KeyFactory.getInstance("RSA");  
             byte[] encodedKey = Base64Utils.decode(publicKey);  
             PublicKey pubKey = keyFactory.generatePublic(new X509EncodedKeySpec(encodedKey));  
             java.security.Signature signature = java.security.Signature  
             .getInstance(SIGN_ALGORITHMS);  
             signature.initVerify(pubKey);  
             signature.update( content.getBytes() );  
             boolean bverify = signature.verify( Base64Utils.decode(sign) );  
             return bverify;  
         }   
         catch (Exception e)   
         {  
             e.printStackTrace();  
         }  
           
         return false;  
     }  
}