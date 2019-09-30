package dc.pay.business.jiuyizhifu;

import java.util.Map;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;

import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Jan 14, 2019
 */
@ResponsePayHandler("JIUYIZHIFU")
public final class JiuYiZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //输出项 输出项名称   注释
    //mch_id  商户编号    商户编号
    private static final String mch_id                ="mch_id";
    //out_trade_no    商户订单号   商户订单号
    private static final String out_trade_no                ="out_trade_no";
    //ordernumber 系统订单号   
    private static final String ordernumber                ="ordernumber";
    //transaction_id  第三方订单号  
    private static final String transaction_id                ="transaction_id";
    //transtypeid 系统支付类型的id   
    private static final String transtypeid                ="transtypeid";
    //total_fee   金额  两位小数
    private static final String total_fee                ="total_fee";
    //service 接口类型    
    private static final String service                ="service";
    //way 支付方式    
    private static final String way                ="way";
    //time_end    交易结束时间  支付完成时间，格式为yyyyMMddHHmmss，如2009年12月27日9点10分10秒表示为20091227091010。时区为GMT+8 beijing。该时间取自平台服务器
    private static final String time_end                ="time_end";
    //device_info 设备号 用户传的原样返回给用户
//    private static final String device_info                ="device_info";
    //attach  附加信息    用户传的原样返回给用户
//    private static final String attach                ="attach";
    //result_code 业务结果    0表示成功，非0表示失败
    private static final String result_code                ="result_code";
    //sign    签名加密    Md5(mch_id + time_end + out_trade_no + ordernumber + transtypeid + transaction_id + total_fee + service + way + result_code + 密钥)    md5加密的编码为utf-8签名后的字符串不区分大小写
//    private static final String sign                ="sign";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "SUCCESS";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
       //String api_KEY = channelWrapper.getAPI_KEY();
       //if (null == api_KEY || !api_KEY.contains("-") || api_KEY.split("-").length != 2) {
       //    log.error("[久易支付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //    throw new PayException("[久易支付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //}
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(mch_id);
        String ordernumberR = API_RESPONSE_PARAMS.get(out_trade_no);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[久易支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        StringBuilder signStr = new StringBuilder();
        signStr.append(api_response_params.get(mch_id));
        signStr.append(api_response_params.get(time_end));
        signStr.append(api_response_params.get(out_trade_no));
        signStr.append(api_response_params.get(ordernumber));
        signStr.append(api_response_params.get(transtypeid));
        signStr.append(api_response_params.get(transaction_id));
        signStr.append(api_response_params.get(total_fee));
        signStr.append(api_response_params.get(service));
        signStr.append(api_response_params.get(way));
        signStr.append(api_response_params.get(result_code));
        signStr.append(channelWrapper.getAPI_KEY());
        String paramsStr =signStr.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[久易支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //result_code   业务结果    0表示成功，非0表示失败
        String payStatusCode = api_response_params.get(result_code);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(total_fee));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("0")) {
            my_result = true;
        } else {
            log.error("[久易支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[久易支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功： 0");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[久易支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[久易支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}