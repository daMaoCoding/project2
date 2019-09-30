package dc.pay.business.yuyue;

import java.util.Base64;
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
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Dec 14, 2018
 */
@ResponsePayHandler("YUYUE")
public final class YuYuePayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //字段名            变量名          签名          类型           描述
    //商户ID            memberid         是          String           商户号
    //订单号            orderid          是          String           商户订单号
    //提交金额          amount           是          float           提交金额
    //成功金额          moeny            是          float           成功交易金额
    //支付银行          bankcode         是          String           详见：相关编码->银行编码
    //支付场景          scene            是          String           详见：相关编码->支付场景
    //随机数            rand             是          String           32字节以内
    //支付状态          ontype           是          String           101未支付，102已支付
    //扩展字段          extend           否          String           提交什么在异步通知原样返回
    //待验签名          sign             否          String           例如:3b2a7a81db7659eefc023613ae7b5253
    private static final String memberid                     ="memberid";
    private static final String orderid                      ="orderid";
    private static final String amount                       ="amount";
    private static final String moeny                        ="moeny";
    private static final String bankcode                     ="bankcode";
    private static final String scene                        ="scene";
    private static final String rand                         ="rand";
    private static final String ontype                       ="ontype";
//    private static final String extend                       ="extend";
    
    private static final String body                         ="body";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty() || StringUtils.isBlank(API_RESPONSE_PARAMS.get(body)))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String data = new String(Base64.getDecoder().decode(API_RESPONSE_PARAMS.get(body)));
        JSONObject parseObject = null;
        try {
            parseObject = JSON.parseObject(data);
        } catch (Exception e) {
            throw new PayException(SERVER_MSG.REQUEST_PAY_CHANNEL_RESULT_ERROR);
        }
        String partnerR = parseObject.getString(memberid);
        String ordernumberR = parseObject.getString(orderid);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[鱼跃]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        String data = new String(Base64.getDecoder().decode(API_RESPONSE_PARAMS.get(body)));
        JSONObject parseObject = JSON.parseObject(data);
        StringBuilder signStr = new StringBuilder();
        signStr.append(amount+"=").append(parseObject.getString(amount)).append("&");
        signStr.append(moeny+"=").append(parseObject.getString(moeny)).append("&");
        signStr.append(bankcode+"=").append(parseObject.getString(bankcode)).append("&");
        signStr.append(scene+"=").append(parseObject.getString(scene)).append("&");
        signStr.append(memberid+"=").append(parseObject.getString(memberid)).append("&");
        signStr.append(orderid+"=").append(parseObject.getString(orderid)).append("&");
        signStr.append(rand+"=").append(parseObject.getString(rand)).append("&");
        signStr.append(ontype+"=").append(parseObject.getString(ontype)).append("&");
        signStr.append(key+"=").append(channelWrapper.getAPI_KEY());
        String paramsStr =signStr.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase()).toLowerCase();
        log.debug("[鱼跃]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

     @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        String data = new String(Base64.getDecoder().decode(API_RESPONSE_PARAMS.get(body)));
        JSONObject parseObject = JSON.parseObject(data);
        boolean my_result = false;
        //支付状态  ontype  是   String  101未支付，102已支付
        String payStatusCode = parseObject.getString(ontype);
        String responseAmount = HandlerUtil.getFen(parseObject.getString(amount));
        //amount数据库存入的是分     第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("102")) {
            my_result = true;
        } else {
            log.error("[鱼跃]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[鱼跃]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：102");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        String data = new String(Base64.getDecoder().decode(API_RESPONSE_PARAMS.get(body)));
        JSONObject parseObject = JSON.parseObject(data);
        boolean my_result = parseObject.getString(signature).equalsIgnoreCase(signMd5);
        log.debug("[鱼跃]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[鱼跃]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}