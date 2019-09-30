package dc.pay.business.xinyu;

import java.util.List;
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
import dc.pay.utils.MapUtils;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Aug 3, 2018
 */
@ResponsePayHandler("XINYU")
public final class XinYuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //字段名          变量名            必填        类型                  说明
    //版本号          version            是        String(8)              版本号，version默认值是1.0
    //字符集          charset            是        String(8)              可选值 UTF-8 ，默认为 UTF-8
    //签名方式        sign_type          是        String(8)              签名类型，取值：MD5默认：MD5
    //返回状态码      status             是        String(16)             0表示成功，非0表示失败此字段是通信标识，非交易标识，交易是否成功需要查看result_code 来判断
    //返回信息        message            否        String(128)            返回信息，如非空，为错误原因签名失败参数格式校验错误
    //以下字段在 status 为 0的时候有返回     
    //字段名          变量名            必填        类型                 说明
    //业务结果        result_code        是        String(16)            0表示成功，非0表示失败
    //商户号          mch_id             是        String(32)            商户号，由平台分配
    //随机字符串      nonce_str          是        String(32)            随机字符串，不长于 32 位
    //错误代码        err_code           否        String(32)            参考错误码
    //错误代码描述    err_msg            否        String (128)          结果信息描述
    //签名            sign               是        String(32)            MD5签名结果，详见“安全规范”
    //以下字段在 status 和 result_code 都为 0的时候有返    
    //字段名        变量名            必填        类型                 说明
    //交易类型        trade_type        是        String(32)           WECHAT: 微信扫码、WECHAT_WAP: 微信WAP、WECHAT_OFFICE_ACCOUNT: 微信公众号、ALIPAY: 支付宝扫码、QQ: QQ扫码、QQ_WAP: QQ WAP、NET: 网关支付
    //支付结果        pay_result        是        Int                  支付结果：0—成功；其它—失败
    //平台订单号      transaction_id    是        String(32)           平台交易单号
    //商户订单号      out_trade_no      是        String(32)           商户系统内部的定单号，32个字符内、可包含字母
    //总金额          total_fee         是        Int                  总金额，以分为单位，不允许包含任何字、符号
    //货币种类        fee_type          是        String(8)            货币类型，符合 ISO 4217 标准的三位字母代码，默认人民币：CNY
    //支付完成时间    time_end          是        String(14)           支付完成时间，格式为yyyy-MM-dd HH:mm:ss，如2009年12月27日9点10分10秒表示为2009-12-27 09:10:10。时区为GMT+8 beijing。该时间取自平台服务器
//    private static final String version                        ="version";
//    private static final String charset                        ="charset";
//    private static final String sign_type                      ="sign_type";
//    private static final String status                         ="status";
//    private static final String message                        ="message";
//    private static final String result_code                    ="result_code";
    private static final String mch_id                         ="mch_id";
//    private static final String nonce_str                      ="nonce_str";
//    private static final String err_code                       ="err_code";
//    private static final String err_msg                        ="err_msg";
//    private static final String trade_type                     ="trade_type";
    private static final String pay_result                     ="pay_result";
//    private static final String transaction_id                 ="transaction_id";
    private static final String out_trade_no                   ="out_trade_no";
    private static final String total_fee                      ="total_fee";
//    private static final String fee_type                       ="fee_type";
//    private static final String time_end                       ="time_end";
    
    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(mch_id);
        String ordernumberR = API_RESPONSE_PARAMS.get(out_trade_no);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[信誉]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!signature.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        signSrc.append(key +"="+ channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[信誉]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //支付结果        pay_result        是        Int                  支付结果：0—成功；其它—失败
        String payStatusCode = api_response_params.get(pay_result);
        String responseAmount = api_response_params.get(total_fee);
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("0")) {
            my_result = true;
        } else {
            log.error("[信誉]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[信誉]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：0");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[信誉]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[信誉]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}