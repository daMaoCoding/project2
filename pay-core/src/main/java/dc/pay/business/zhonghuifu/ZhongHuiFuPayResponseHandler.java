package dc.pay.business.zhonghuifu;

import java.util.Map;

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
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Dec 21, 2018
 */
@ResponsePayHandler("ZHONGHUIFU")
public final class ZhongHuiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //status    支付状态    String（10）  00表示支付成功，非00表示失败    非空  00成功01失败
    private static final String status                ="status";
    //out_trade_no  订单号 String（64）  对应签名中的orderNo字段 非空  8e123abff8e54fe8aaa2b5e4c7fbffeb
    private static final String out_trade_no                 ="out_trade_no";
    //total_fee 订单金额    String（10）  以分为单位   非空  100
    private static final String total_fee                ="total_fee";
    //cas_time_stamp    时间戳（精确到毫秒秒） String（64）  1495610242425对应签名中的timeStamp字段非空  1495610242425
    private static final String cas_time_stamp                ="cas_time_stamp";
    private static final String orderTime                ="orderTime";
    //sign  签名  String（64）  验签规则：MD5（out_trade_no + total_fee + orderTime + cas_time_stamp +key） 转小写    非空  
//    private static final String sign                ="sign";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "SUCCESS";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
//        String partnerR = API_RESPONSE_PARAMS.get(merchno);
        String ordernumberR = API_RESPONSE_PARAMS.get(out_trade_no);
        if ( StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[众惠付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        StringBuilder signStr = new StringBuilder();
        signStr.append(api_response_params.get(out_trade_no));
        signStr.append(api_response_params.get(total_fee));
        signStr.append(api_response_params.get(orderTime));
        signStr.append(api_response_params.get(cas_time_stamp));
        signStr.append(api_key);
        String paramsStr =signStr.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[众惠付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }
    
    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //status    支付状态    String（10）  00表示支付成功，非00表示失败    非空  00成功01失败
        String payStatusCode = api_response_params.get(status);
        String responseAmount = api_response_params.get(total_fee);
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("00")) {
            my_result = true;
        } else {
            log.error("[众惠付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[众惠付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：00");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[众惠付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[众惠付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}