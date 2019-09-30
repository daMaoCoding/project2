package dc.pay.business.aba;

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

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Dec 22, 2018
 */
@ResponsePayHandler("ABA")
public final class ABaPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //status 状态 1 是 3⽀付成功，其他情况不会通知
    private static final String status                 ="status";
    //request_no 商⼾订单号 32 是 原样返回
    private static final String request_no                ="request_no";
    //amount ⾦额 16 是 实际⽀付⾦额
    private static final String amount                ="amount";
    //merchant_no 商⼾号 20 是 原样返回
    private static final String merchant_no              ="merchant_no";
//    private static final String call_nums              ="call_nums";
    
    private static final String data              ="data";
    
    //sign 签名 32 是
//    private static final String sign                 ="sign";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "SUCCESS";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
       //String api_KEY = channelWrapper.getAPI_KEY();
       //if (null == api_KEY || !api_KEY.contains("-") || api_KEY.split("-").length != 2) {
       //    log.error("[a8]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //    throw new PayException("[a8]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //}
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String myData = API_RESPONSE_PARAMS.get(data);
        JSONObject parseObject = null;
        try {
            parseObject = JSON.parseObject(myData);
        } catch (Exception e) {
            throw new PayException(SERVER_MSG.REQUEST_PAY_CHANNEL_RESULT_ERROR);
        }
        String partnerR = parseObject.getString(merchant_no);
        String ordernumberR = parseObject.getString(request_no);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[a8]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        Map<String,String> jsonToMap = JSONObject.parseObject(api_response_params.get(data), Map.class);
        List paramKeys = MapUtils.sortMapByKeyAsc(jsonToMap);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if(!signature.equals(paramKeys.get(i)) && (jsonToMap.get(paramKeys.get(i)) instanceof java.lang.String)){
                signSrc.append(paramKeys.get(i)).append("=").append(jsonToMap.get(paramKeys.get(i))).append("&");
            }else if(!signature.equals(paramKeys.get(i))){
                Object string2 = jsonToMap.get(paramKeys.get(i));
                signSrc.append(paramKeys.get(i)).append("=").append(string2+"").append("&");
            }
        }
        signSrc.append(key +"="+ channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[a8]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        JSONObject parseObject = JSONObject.parseObject(api_response_params.get(data));
        boolean my_result = false;
        //status 状态 1 是 3⽀付成功其他情况不会
        String payStatusCode = parseObject.getString(status);
//        String responseAmount = HandlerUtil.getFen(parseObject.getString(amount));
        String responseAmount = HandlerUtil.getFen(parseObject.getString(amount));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        //行者YLP299        也就是整十整百的支付金额， 会自动浮减1元 或 2元了~~~
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"200");//第三方回调金额差额1元内
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("3")) {
            my_result = true;
        } else {
            log.error("[a8]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[a8]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：3");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        Map<String, String> jsonToMap = handlerUtil.jsonToMap(api_response_params.get(data));
        boolean my_result = jsonToMap.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[a8]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[a8]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}