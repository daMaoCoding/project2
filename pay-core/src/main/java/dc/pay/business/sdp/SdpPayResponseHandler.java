package dc.pay.business.sdp;

import java.util.Map;

import dc.pay.utils.XmlUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;


/**
 * ************************
 * @author beck 2229556569
 */

@ResponsePayHandler("SDP")
public final class SdpPayResponseHandler extends PayResponseHandler {
     private final Logger log = LoggerFactory.getLogger(getClass());
     private static final String RESPONSE_PAY_MSG = "SUCCESS";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String orderNumber = this.getParamValue("order");          //获取订单号
        String ordernumberR = orderNumber;
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[SdpPay]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        return null;
    }
    

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean checkResult = false;
        //String resultStr = api_response_params.get("status");
        String resultStr = this.getParamValue("result");          //获取支付结果
        //log.debug("支付结果："+resultStr);
        String amount = this.getParamValue("money");              //获取支付金额
        //log.debug("支付金额："+amount);
        String responseAmount =  HandlerUtil.getFen(amount);
        boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        if (checkAmount && resultStr.equalsIgnoreCase("1")) {
            checkResult = true;
        } else {
            log.error("[SdpPay]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + resultStr + " ,支付金额：" + responseAmount + " ，应支付金额：" + amountDb);
        }
        log.debug("[SdpPay]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + checkResult + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amountDb + ",第三方响应支付成功标志:" + resultStr + " ,计划成功：1");
        return checkResult;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        return true;
    }

    @Override
    protected String responseSuccess() {
        String merchantid = API_RESPONSE_PARAMS.get("pid");
        String orderNumber = "";
        try{
            orderNumber = this.getParamValue("order");    
        }catch(PayException e){
           
        }
        
        
        String resMsg = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>";
        resMsg += "<message>";
        resMsg += "<cmd>60071</cmd>";
        resMsg += "<merchantid>{merchantid}</merchantid>";
        resMsg += "<order>{order}</order>";
        resMsg += "<username> testuser</username>";
        resMsg += "<result>100</result>";
        resMsg += "</message>";
        
        resMsg = resMsg.replace("{merchantid}", merchantid);
        resMsg = resMsg.replace("{order}", orderNumber);
        
        log.debug("[SdpPay]-[响应支付]-5.第三方支付确认收到消息返回内容：" + resMsg);
        //return RESPONSE_PAY_MSG;
        return resMsg;
    }
    
    /**
     * 获取参数的值
     * */
    private String  getParamValue(String paramKey) throws PayException {
        String decryptXML = this.getResponseParam();            
        Map<String, String> decryptData = XmlUtil.xml2Map(decryptXML);
        String value = decryptData.get(paramKey);         
        
        return value;
    }
    
    private String getResponseParam() throws PayException {
        String fullKey = handlerUtil.getApiKeyFromReqPayMemberId(API_RESPONSE_PARAMS.get("pid"));
        
        String res = API_RESPONSE_PARAMS.get("res");
        String key1 = this.getDesKey(fullKey);
        String key2 = this.getDesVector(fullKey);
        
        String decryptMsg = "";
        try {
            decryptMsg = MyDecrypt.DecryptData(res, key1, key2);
            String[] items = decryptMsg.split("</message>");
            decryptMsg = items[0]+"</message>";
            
            log.debug("[sdp支付]-[支付响应]，解密数据："+decryptMsg);
            
        } catch (Exception e) {
            String errorMsg = "[sdp支付]-[支付响应]第三方返回数据处理错误。返回原数据：" + JSON.toJSONString(API_RESPONSE_PARAMS);
            log.error(errorMsg);
            throw new PayException(errorMsg);
        }
        
        return decryptMsg;
    }
    

    /**
     * 获取md5Key
     * */
    private String getMD5Key(String md5Key) throws PayException{
        String items[] = this.splitAPIKey(md5Key);
        return items[0];
    }
    
    /**
     * 获取des加密key
     * */
    private String getDesKey(String md5Key) throws PayException{
        String items[] = this.splitAPIKey(md5Key);
        return items[1];
    }
    
    /**
     * 获取des加密向量
     * */
    private String getDesVector(String md5Key) throws PayException{
        String items[] = this.splitAPIKey(md5Key);
        return items[2];
    }
    
    
    /**
     * 分割秘钥
     * */
    private String[] splitAPIKey(String md5Key) throws PayException {
        String[] keys = md5Key.split("&");
        if(keys.length < 3){
            String errorMsg = "[sdp]-[请求支付]-5. 商户信息填写错误，填写格式：商户号&key1(des加密秘钥)&key2(des加密向量)。";
            log.error(errorMsg);
            throw new PayException(errorMsg);
        }
        return keys;
    }
}