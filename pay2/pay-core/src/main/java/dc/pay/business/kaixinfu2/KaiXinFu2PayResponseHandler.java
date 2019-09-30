package dc.pay.business.kaixinfu2;

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

/**
 * 
 * @author andrew
 * Aug 1, 2019
 */
@ResponsePayHandler("KAIXINFU2")
public final class KaiXinFu2PayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //支付请求返回值说明： 
    //变量  说明  备注
    //return_type 返回值一个json字符串    Json模型见下方
    private static final String return_type                ="return_type";
    //return_type模型
    //变量  说明  备注
    //order_id    订单编号    由商户传入
    private static final String order_id                ="order_id";
    //mark    备注  由商户传入
    private static final String mark                ="mark";
    //price   金额  由商户传入
    private static final String price                ="price";
    //api_id  平台订单号   由创建订单接口返回的订单号
    private static final String api_id                ="api_id";
    //msg 成功状态    可用于支付状态判断（一般msg=支付成功  不成功的不会回调）
    private static final String msg                ="msg";
    //pay_time    支付时间    
    private static final String pay_time                ="pay_time";
    //sign    数字签名     签名方式为api_id=api_id&mark=mark&msg=msg&order_id=order_id&pay_time=pay_time&price=price&key=key拼接后转大写进行MD5验签（等式后的是参数值）
//    private static final String sign                ="sign";
    
    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
       //String api_KEY = channelWrapper.getAPI_KEY();
       //if (null == api_KEY || !api_KEY.contains("-") || api_KEY.split("-").length != 2) {
       //    log.error("[开心付2]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //    throw new PayException("[开心付2]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //}
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String string = API_RESPONSE_PARAMS.get(return_type);
        JSONObject parseObject = null;
        try {
            parseObject = JSON.parseObject(string);
        } catch (Exception e) {
            throw new PayException(SERVER_MSG.REQUEST_PAY_CHANNEL_RESULT_ERROR);
        }
//        String partnerR = parseObject.getString(merchno);
        String ordernumberR = parseObject.getString(order_id);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[开心付2]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }
    
    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        String string = api_response_params.get(return_type);
        JSONObject parseObject = JSON.parseObject(string);
        StringBuilder signStr = new StringBuilder();
        signStr.append(api_id+"=").append(parseObject.getString(api_id)).append("&");
        signStr.append(mark+"=").append(parseObject.getString(mark)).append("&");
        signStr.append(msg+"=").append(parseObject.getString(msg)).append("&");
        signStr.append(order_id+"=").append(parseObject.getString(order_id)).append("&");
        signStr.append(pay_time+"=").append(parseObject.getString(pay_time)).append("&");
        signStr.append(price+"=").append(parseObject.getString(price)).append("&");
        signStr.append(key+"=").append(channelWrapper.getAPI_KEY());
        String paramsStr =signStr.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr.toUpperCase()).toLowerCase();
        log.debug("[开心付2]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        String string = api_response_params.get(return_type);
        JSONObject parseObject = JSON.parseObject(string);
        
        boolean my_result = false;
        //msg   成功状态    可用于支付状态判断（一般msg=支付成功  不成功的不会回调）
        String payStatusCode = parseObject.getString(msg);
        String responseAmount = HandlerUtil.getFen(parseObject.getString(price));

        //tony(Tony) 01-17 15:09:28
        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        //boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("支付成功")) {
            my_result = true;
        } else {
            log.error("[开心付2]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[开心付2]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：支付成功");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        String string = api_response_params.get(return_type);
        JSONObject parseObject = JSON.parseObject(string);

        boolean my_result = parseObject.getString(signature).equalsIgnoreCase(signMd5);
        log.debug("[开心付2]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[开心付2]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}