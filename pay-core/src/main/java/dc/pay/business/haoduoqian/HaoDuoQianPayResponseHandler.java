package dc.pay.business.haoduoqian;


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
import dc.pay.utils.RsaUtil;

/**
 * 
 * @author andrew
 * Dec 12, 2018
 */
@ResponsePayHandler("HAODUOQIAN")
public final class HaoDuoQianPayResponseHandler extends PayResponseHandler {
     private final Logger log = LoggerFactory.getLogger(getClass());

     //merchant_order_no    String (64)   字串 (64) Yes   必填  reference trans no of merchant for the transaction  订单号：详情请见商户转账订单号
     private static final String  merchant_order_no ="merchant_order_no";
     //amount   String (12)     字串 (12) Yes     必填  amount deposited by merchant member     金额：商户存款金额
     private static final String  amount ="amount";
     private static final String  merchant_code ="merchant_code";// "20180827113852",
     private static final String  data ="data";

     private static final String  sign ="sign";
     
     private static final String RESPONSE_PAY_MSG = jsonResponsePayMsg("{\"error_msg\":\"\",\"status\":\"1\"}");

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(merchant_code);
        if (StringUtils.isBlank(partnerR)){
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        }
        String apiKey = handlerUtil.getApiKeyFromReqPayMemberId(partnerR);
        if (StringUtils.isBlank(apiKey)){
            log.error("[好多钱]-[响应支付]-1.1.通过商号获取密钥异常");
            throw new PayException("[好多钱]-[响应支付]-1.1.通过商号获取密钥异常");
        }
        JSONObject parseObject = null;
        try {
            String decrypted = RSAutilJava8.decryptByPrivateKey(API_RESPONSE_PARAMS.get(data), apiKey);
            parseObject = JSON.parseObject(decrypted);
        } catch (Exception e) {
            throw new PayException(SERVER_MSG.REQUEST_PAY_CHANNEL_RESULT_ERROR);
        }
        String ordernumberR = parseObject.getString(merchant_order_no);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[好多钱]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        boolean my_result = false;
        my_result = RsaUtil.validateSignByPublicKey(params.get(data), channelWrapper.getAPI_PUBLIC_KEY(), params.get(sign),"SHA1withRSA");    // 验签   signInfoUU付返回的签名参数排序， wpay_public_keyUU付公钥， wpaySignUU付返回的签名
        log.debug("[好多钱]-[响应支付]-2.生成加密URL签名完成：" + JSON.toJSONString(my_result));
        return String.valueOf(my_result);
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
        JSONObject parseObject = null;
        try {
            String decrypted = RSAutilJava8.decryptByPrivateKey(API_RESPONSE_PARAMS.get(data), channelWrapper.getAPI_KEY());
            parseObject = JSON.parseObject(decrypted);
        } catch (Exception e) {
            e.printStackTrace();
            throw new PayException(SERVER_MSG.REQUEST_PAY_CHANNEL_RESULT_ERROR);
        }
        boolean my_result = false;
        String payStatus = "无";
        String responseAmount =  handlerUtil.getFen(parseObject.getString(amount));
        
        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  handlerUtil.isAllowAmountt(amountDb,responseAmount,"300");//我平台默认允许一元偏差
        
//        boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        if (checkAmount && payStatus.equalsIgnoreCase("无")) {
            my_result = true;
        } else {
            log.error("[好多钱]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatus + " ,支付金额：" + responseAmount + " ，应支付金额：" + amountDb);
        }
        log.debug("[好多钱]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amountDb + ",第三方响应支付成功标志:" + payStatus + " ,计划成功：无");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        Boolean my_result = new Boolean(signMd5);
        log.debug("[好多钱]-[响应支付]-4.验证MD5签名：" +  my_result.booleanValue());
        return  my_result.booleanValue();
    }

    @Override
    protected String responseSuccess() {
        log.debug("[好多钱]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}