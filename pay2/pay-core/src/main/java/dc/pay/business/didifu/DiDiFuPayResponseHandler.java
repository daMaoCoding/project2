package dc.pay.business.didifu;

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

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Nov 26, 2018
 */
@ResponsePayHandler("DIDIFU")
public final class DiDiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //字段名              变量名              类型              说明                        可空          
    //交易结果码          resultcode          string            0000          交易成功【已支付，结算到可用帐户】          1001          未支付          1002          交易处理中【已支付，结算到丌可用帐户】1003          交易失败          1004          协查调单          Y          
    //交易结果信息        resultmsg           string                                        Y          
    //交易流水号          queryid             string                                        N          
    //支付金额            txnamt              int                                           N          
    //商户号              merid               string                                        N          
    //商户订单号          orderid             string                                        N          
    //支付时间            paytime             datetime          未支付订单返回空            Y          
    private static final String resultcode              ="resultcode";
//    private static final String resultmsg               ="resultmsg";
//    private static final String queryid                 ="queryid";
    private static final String txnamt                  ="txnamt";
    private static final String merid                   ="merid";
    private static final String orderid                 ="orderid";
//    private static final String paytime                 ="paytime";
    
    private static final String resp                    ="resp";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "0000";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String respData = new String(Base64.decodeBase64(API_RESPONSE_PARAMS.get(resp)));
        if (!respData.contains("{") || !respData.contains("}")) {
            log.error("[樀樀付]-[响应支付]-1.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(API_RESPONSE_PARAMS) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(JSON.toJSONString(API_RESPONSE_PARAMS));
        }
        JSONObject respObject = JSONObject.parseObject(respData);
        String partnerR = respObject.getString(merid);
        String ordernumberR = respObject.getString(orderid);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[樀樀付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        String respData = api_response_params.get(resp);
        String signMd5 = HandlerUtil.getMD5UpperCase(respData+api_key);
        log.debug("[樀樀付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        String respData = new String(Base64.decodeBase64(api_response_params.get(resp)));
        JSONObject respObject = JSONObject.parseObject(respData);
        boolean my_result = false;
      //交易结果码          resultcode          string            0000          交易成功【已支付，结算到可用帐户】          1001          未支付          1002          交易处理中【已支付，结算到丌可用帐户】1003          交易失败          1004          协查调单          Y          
        String payStatusCode = respObject.getString(resultcode);
        //支付金额            txnamt              int                                           N          
        String responseAmount = respObject.getString(txnamt);
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("0000")) {
            my_result = true;
        } else {
            log.error("[樀樀付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[樀樀付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：0000");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[樀樀付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[樀樀付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}