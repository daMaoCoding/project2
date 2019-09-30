package dc.pay.business.wanlitong;

/**
 * ************************
 * @author tony 3556239829
 */

import com.alibaba.fastjson.JSON;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@ResponsePayHandler("WANLITONG")
public final class WanLiTongPayResponseHandler extends PayResponseHandler {
    private final Logger log =  LoggerFactory.getLogger(getClass());
    public static final String split= "&";
    public static final String SUCCESS="SUCCESS";
    private static  final String rt1_customerNumber = "rt1_customerNumber";
    private static  final String rt2_orderId        = "rt2_orderId";
    private static  final String rt3_systemSerial   = "rt3_systemSerial";
    private static  final String rt4_status         = "rt4_status";
    private static  final String rt5_orderAmount    = "rt5_orderAmount";
    private static  final String rt6_currency       = "rt6_currency";
    private static  final String rt7_timestamp      = "rt7_timestamp";
    private static  final String rt8_desc           = "rt8_desc";
    private static  final String sign               = "sign";
    private static final String rt1_bizType         = "rt1_bizType";
    private static final String rt2_retCode         = "rt2_retCode";
    private static final String rt3_retMsg          = "rt3_retMsg";
    private static final String rt4_customerNumber  = "rt4_customerNumber";
    private static final String rt5_orderId         = "rt5_orderId";
    private static final String rt6_orderAmount     = "rt6_orderAmount";
    private static final String rt7_bankId          = "rt7_bankId";
    private static final String rt8_business        = "rt8_business";
    private static final String rt9_timestamp       = "rt9_timestamp";
    private static final String rt10_completeDate   = "rt10_completeDate";
    private static final String rt11_orderStatus    = "rt11_orderStatus";
    private static final String rt12_serialNumber   = "rt12_serialNumber";
    private static final String rt13_desc           = "rt13_desc";
    private static final String  RESPONSE_PAY_MSG   = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        String memberId = null;
        String orderId = null;
        if(null==API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty()) {
            log.error("[万里通支付]1.获取支付通道响应信息中的订单号错误，"+ JSON.toJSONString(API_RESPONSE_PARAMS));
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        }
        if(API_RESPONSE_PARAMS.containsKey(rt1_customerNumber)){
             memberId = API_RESPONSE_PARAMS.get(rt1_customerNumber);
             orderId = API_RESPONSE_PARAMS.get(rt2_orderId);
        }else if(API_RESPONSE_PARAMS.containsKey(rt4_customerNumber)) {
             memberId = API_RESPONSE_PARAMS.get(rt4_customerNumber);
             orderId = API_RESPONSE_PARAMS.get(rt5_orderId);
        }
        if(StringUtils.isBlank(memberId) || StringUtils.isBlank(orderId)){
            log.error("[万里通支付]1.获取支付通道响应信息中的订单号错误，"+ JSON.toJSONString(API_RESPONSE_PARAMS));
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        }
        log.debug("[万里通支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成："+ orderId);
        return orderId;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        StringBuffer sb = new StringBuffer();
        String signLocal = null;
        if(API_RESPONSE_PARAMS.containsKey(rt1_customerNumber)){
            String rt1_customerNumber = api_response_params.get(this.rt1_customerNumber).trim();
            String rt2_orderId        = api_response_params.get(this.rt2_orderId).trim();
            String rt3_systemSerial   = api_response_params.get(this.rt3_systemSerial).trim();
            String rt4_status         = api_response_params.get(this.rt4_status).trim();
            String rt5_orderAmount    = api_response_params.get(this.rt5_orderAmount).trim();
            String rt6_currency       = api_response_params.get(this.rt6_currency).trim();
            String rt7_timestamp      = api_response_params.get(this.rt7_timestamp).trim();
            String rt8_desc           = api_response_params.get(this.rt8_desc).trim();
            String sign               = api_response_params.get(this.sign).trim();
            String source =sb.
                    append(split).append(rt1_customerNumber).
                    append(split).append(rt2_orderId).
                    append(split).append(rt3_systemSerial).
                    append(split).append(rt4_status).
                    append(split).append(rt5_orderAmount).
                    append(split).append(rt6_currency).
                    append(split).append(rt7_timestamp).
                    append(split).append(rt8_desc).
                    append(split).append(api_key).toString();
            signLocal = WanLiTongPayUtil.disguiseMD5(source);
        }else if(API_RESPONSE_PARAMS.containsKey(rt4_customerNumber)) {
            String rt1_bizType = api_response_params.get(this.rt1_bizType).trim();
            String rt2_retCode = api_response_params.get(this.rt2_retCode).trim();
            String rt3_retMsg = api_response_params.get(this.rt3_retMsg).trim();
            String rt4_customerNumber = api_response_params.get(this.rt4_customerNumber).trim();
            String rt5_orderId = api_response_params.get(this.rt5_orderId).trim();
            String rt6_orderAmount = api_response_params.get(this.rt6_orderAmount).trim();
            String rt7_bankId = api_response_params.get(this.rt7_bankId).trim();
            String rt8_business = api_response_params.get(this.rt8_business).trim();
            String rt9_timestamp = api_response_params.get(this.rt9_timestamp).trim();
            String rt10_completeDate = api_response_params.get(this.rt10_completeDate).trim();
            String rt11_orderStatus = api_response_params.get(this.rt11_orderStatus).trim();
            String rt12_serialNumber = api_response_params.get(this.rt12_serialNumber).trim();
            String rt13_desc = api_response_params.get(this.rt13_desc).trim();
            String sign = api_response_params.get(this.sign).trim();
            String source =sb.
                    append(split).append(rt1_bizType).
                    append(split).append(rt2_retCode).
                    append(split).append(rt3_retMsg).
                    append(split).append(rt4_customerNumber).
                    append(split).append(rt5_orderId).
                    append(split).append(rt6_orderAmount).
                    append(split).append(rt7_bankId).
                    append(split).append(rt8_business).
                    append(split).append(rt9_timestamp).
                    append(split).append(rt10_completeDate).
                    append(split).append(rt11_orderStatus).
                    append(split).append(rt12_serialNumber).
                    append(split).append(rt13_desc).
                    append(split).append(api_key).toString();
            signLocal = WanLiTongPayUtil.disguiseMD5(source);
        }
        log.debug("[万里通支付]-[响应支付]-2.响应内容生成md5完成："+ signLocal);
        return signLocal;
    }


    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params,String amount) throws PayException {
        boolean result = false;
        String payStatusCode   =null;
        String responseAmount  =null;
        if(API_RESPONSE_PARAMS.containsKey(rt1_customerNumber)){
             payStatusCode   = api_response_params.get(rt4_status);
             responseAmount  = api_response_params.get(rt5_orderAmount);
        }else if(API_RESPONSE_PARAMS.containsKey(rt4_customerNumber)) {
             payStatusCode   = api_response_params.get(rt11_orderStatus);
            responseAmount   = api_response_params.get(rt6_orderAmount);
        }
        responseAmount = HandlerUtil.getFen(responseAmount);
        boolean checkAmount = amount.equalsIgnoreCase(responseAmount);
        if(checkAmount && StringUtils.isNotBlank(payStatusCode) && payStatusCode.equalsIgnoreCase(SUCCESS)  ){
            result = true;
        }else{
            log.error("[万里通支付]-[响应支付]金额及状态验证错误,订单号："+channelWrapper.getAPI_ORDER_ID()+",第三方支付状态："+payStatusCode +" ,支付金额："+responseAmount+" ，应支付金额："+amount);
        }
        log.debug("[万里通支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果："+ result+" ,金额验证："+checkAmount+" ,responseAmount="+responseAmount +" ,数据库金额："+amount+",第三方响应支付成功标志:"+payStatusCode+" ,计划成功：1");
        return result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params,String signMd5) {
        String signMsg     = api_response_params.get(sign);
        boolean result     = signMsg.equalsIgnoreCase(signMd5);
        log.debug("[万里通支付]-[响应支付]-4.验证MD5签名："+ result);
        return result;
    }
    @Override
    protected String responseSuccess() {
        log.debug("[万里通支付]-[响应支付]-5.第三方支付确认收到消息返回内容："+ RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}