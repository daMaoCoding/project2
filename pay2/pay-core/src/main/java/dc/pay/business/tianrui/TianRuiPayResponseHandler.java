package dc.pay.business.tianrui;

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
 * Aug 6, 2018
 */
@ResponsePayHandler("TIANRUI")
public final class TianRuiPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //字段名称            含义                   长度         是否必须        备注
    //status              状态                     1             是          3支付成功，其他情况不会通知 
    //request_no          商户订单号               32            是          原样返回 
    //request_time        商户请求时间戳           10            是          原样返回，商户下单时的时间戳 
    //pay_time            支付成功时间戳           10            是          
    //amount              金额                     16            是          实际支付金额 
    //merchant_no         商户号                   20            是          原样返回 
    //body                商户自定义               100           是          原样返回 
    //nonce_str           随机串                   32            是          
    //call_nums           通知次数                 1             是          平台在未收到商户接口返回 SUCCESS 字串的情况下，会持续通知5次， 每次间隔分别是0，1，2，3，4分钟
    //sign                签名                     32            是
    private static final String status                      ="status";
    private static final String request_no                  ="request_no";
//    private static final String request_time                ="request_time";
//    private static final String pay_time                    ="pay_time";
    private static final String amount                      ="amount";
    private static final String merchant_no                 ="merchant_no";
//    private static final String body                        ="body";
//    private static final String nonce_str                   ="nonce_str";
//    private static final String call_nums                   ="call_nums";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "SUCCESS";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String data = API_RESPONSE_PARAMS.get("data");
        JSONObject parseObject = null;
        try {
            parseObject = JSON.parseObject(data);
        } catch (Exception e) {
            throw new PayException(SERVER_MSG.REQUEST_PAY_CHANNEL_RESULT_ERROR);
        }
        
        String partnerR = parseObject.getString(merchant_no);
        String ordernumberR = parseObject.getString(request_no);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[天瑞]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        String data = API_RESPONSE_PARAMS.get("data");
        Map<String,String> parseObject = JSON.parseObject(data,Map.class);
        List paramKeys = MapUtils.sortMapByKeyAsc(parseObject);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!signature.equals(paramKeys.get(i)) && StringUtils.isNotBlank(parseObject.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(parseObject.get(paramKeys.get(i))).append("&");
            }
        }
        signSrc.append(key +"="+ channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[天瑞]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        String data = API_RESPONSE_PARAMS.get("data");
        JSONObject parseObject = JSON.parseObject(data);
        boolean my_result = false;
        //status              状态                     1             是          3支付成功，其他情况不会通知 
        String payStatusCode = parseObject.getString(status);
        //amount              金额                     16            是          实际支付金额 ？？？？   
        String responseAmount = HandlerUtil.getFen(parseObject.getString(amount));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        boolean checkAmount = HandlerUtil.isAllowAmountt(db_amount, responseAmount, "10");
        //3代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("3")) {
            my_result = true;
        } else {
            log.error("[天瑞]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[天瑞]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：3");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        String data = API_RESPONSE_PARAMS.get("data");
        JSONObject parseObject = JSON.parseObject(data);
        boolean my_result = parseObject.getString(signature).equalsIgnoreCase(signMd5);
        log.debug("[天瑞]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[天瑞]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}