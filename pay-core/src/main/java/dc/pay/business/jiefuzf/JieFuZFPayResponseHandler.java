package dc.pay.business.jiefuzf;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
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
import dc.pay.utils.RsaUtil;
import dc.pay.utils.SecurityRSAPay;


/**
 * ************************
 * @author beck 2229556569
 */

@ResponsePayHandler("JIEFUZF")
public final class JieFuZFPayResponseHandler extends PayResponseHandler {
     private final Logger log = LoggerFactory.getLogger(getClass());
     private static final String RESPONSE_PAY_MSG = "SUCCESS";

     private static final String merchantId = "merchant_code";         //商户号
     private static final String orderNumber = "merchant_order_no";      //订单号
     private static final String data = "data";
     private static final String  signature = "sign";
     
     private JSONObject dencryData= null;   //解密数据
     
    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        
        log.debug(API_RESPONSE_PARAMS.get(signature));
        
        dencryData = this.dencryData(API_RESPONSE_PARAMS.get(data));    //解密数据
        
        String merchant_order_no = dencryData.getString(orderNumber);   //获取订单号
        String ordernumberR = merchant_order_no;
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[捷付支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        
        String signStr = params.get(signature);
        String dataStr = params.get(data);

        boolean result = RsaUtil.validateSignByPublicKey3(dataStr, channelWrapper.getAPI_PUBLIC_KEY(), signStr);
         
        //boolean result =validateByPublicKey(dataStr, signStr,channelWrapper.getAPI_PUBLIC_KEY());
        
        return String.valueOf(result);
    }
    
    /**
     * 验签
     * */
    public  boolean validateByPublicKey(String content, String sign, String publicKey) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA","SunRsaSign");
            byte[] encodedKey = Base64.decodeBase64(publicKey);
            PublicKey pubKey =  keyFactory.generatePublic(new X509EncodedKeySpec(encodedKey));

            java.security.Signature signature = java.security.Signature.getInstance("SHA1WithRSA");

            signature.initVerify(pubKey);
            signature.update(content.getBytes());

            boolean bverify = signature.verify(Base64.decodeBase64(sign));
            return bverify;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }
    

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean checkResult = false;
        String resultStr = "无";//api_response_params.get("");
        String responseAmount =  HandlerUtil.getFen(dencryData.getString("amount"));
        boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        if (checkAmount && resultStr.equalsIgnoreCase("无")) {
            checkResult = true;
        } else {
            log.error("[捷付支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + resultStr + " ,支付金额：" + responseAmount + " ，应支付金额：" + amountDb);
        }
        log.debug("[捷付支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + checkResult + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amountDb + ",第三方响应支付成功标志:" + resultStr + " ,计划成功：无");
        return checkResult;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        Boolean rsaBoolean = Boolean.valueOf(signMd5);
        log.debug("[捷付支付]-[响应支付]-4.验证MD5签名：" + rsaBoolean.booleanValue());
        return rsaBoolean.booleanValue();
    }

    @Override
    protected String responseSuccess() {
        log.debug("[捷付支付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
    
    
    /**
     * 获取订单号
     * */
    private String getOrderNumber(){
        
        return "";
    }
    
    
    /**
     * 解密数据
     * */
    private JSONObject dencryData(String encryData){
        String privateKey = handlerUtil.getApiKeyFromReqPayMemberId(API_RESPONSE_PARAMS.get(merchantId));
        byte[] data  = Base64.decodeBase64(encryData);
        byte[] privateKeyBytes = Base64.decodeBase64(privateKey);
        String dencryData = SecurityRSAPay.decryptByPrivateKey2(data, privateKeyBytes);
        JSONObject jsonObject = JSON.parseObject(dencryData);
        return jsonObject;
    }
    
    
}