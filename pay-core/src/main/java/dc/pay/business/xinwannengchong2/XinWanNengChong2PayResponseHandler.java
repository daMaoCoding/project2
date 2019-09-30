package dc.pay.business.xinwannengchong2;

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
 * Apr 17, 2019
 */
@ResponsePayHandler("XINWANNENGCHONG2")
public final class XinWanNengChong2PayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //通知报文
    //字段名 变量名 必填  类型  示例值 描述
    //商户号 merchNo 是   String(32)      商户在平台开通的商户号
    private static final String merchNo                ="merchNo";
    //商户交易单号  orderNo 是   String(23)  商户交易单号  请求支付中的商户单号
    private static final String orderNo                ="orderNo";
    //交易单号    businessNo  是   String(14)      yyyyMMddHHmmss
//    private static final String businessNo                ="businessNo";
    //交易状态    orderState  是   String(1)   1   详见“附录4.2 交易状态编码”  支付成功    1   交易下单成功与支付成功，且收到第三方成功回执
    private static final String orderState                ="orderState";
    //交易金额    amount  是   String(12)  10.00   以元为单位
    private static final String amount                ="amount";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="signature";

    private static final String RESPONSE_PAY_MSG = "ok";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
       //String api_KEY = channelWrapper.getAPI_KEY();
       //if (null == api_KEY || !api_KEY.contains("-") || api_KEY.split("-").length != 2) {
       //    log.error("[新万能充2]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //    throw new PayException("[新万能充2]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //}
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(merchNo);
        String ordernumberR = API_RESPONSE_PARAMS.get(orderNo);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[新万能充2]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

//    @Override
//    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
//        StringBuilder signStr = new StringBuilder();
//        signStr.append(amount+"=").append(api_response_params.get(amount)).append("&");
//        signStr.append(merchname+"=").append(api_response_params.get(merchname)).append("&");
//        signStr.append(merchno+"=").append(api_response_params.get(merchno)).append("&");
//        if (null != api_response_params.get(openid)) {
//            signStr.append(openid+"=").append(api_response_params.get(openid)).append("&");
//        }
//        signStr.append(orderno+"=").append(api_response_params.get(orderno)).append("&");
//        signStr.append(paytype+"=").append(api_response_params.get(paytype)).append("&");
//        signStr.append(status+"=").append(api_response_params.get(status)).append("&");
//        signStr.append(traceno+"=").append(api_response_params.get(traceno)).append("&");
//        signStr.append(transdate+"=").append(api_response_params.get(transdate)).append("&");
//        signStr.append(transtime+"=").append(api_response_params.get(transtime)).append("&");
//        signStr.append(channelWrapper.getAPI_KEY());
//        String paramsStr =signStr.toString();
//        
////        //使用对方返回的数据进行签名
////        String paramsStr = String.format(amount+"=%s&"+merchname+"=%s&"+merchno+"=%s&"+openid+"=%s&"+orderno+"=%s&"+paytype+"=%s&"+status+"=%s&"+traceno+"=%s&"+transdate+"=%s&"+transtime+"=%s&%s",
////                api_response_params.get(amount),
////                api_response_params.get(merchname),
////                api_response_params.get(merchno),
////                api_response_params.get(openid),
////                api_response_params.get(orderno),
////                api_response_params.get(paytype),
////                api_response_params.get(status),
////                api_response_params.get(traceno),
////                api_response_params.get(transdate),
////                api_response_params.get(transtime),
////                channelWrapper.getAPI_KEY());
//        System.out.println("签名源串=========>"+paramsStr);
//        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
//        log.debug("[新万能充2]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
//        return signMd5;
//    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        //Map<String, String> jsonToMap = handlerUtil.jsonToMap(api_response_params.get(data));
        //或者直接取出数值类型
        //String responseAmount = JSON.parseObject(api_response_params.get(data)).getString(payMoney);
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!signature.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        signSrc.append(key +"="+ channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        System.out.println("签名源串=========>"+paramsStr);
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[新万能充2]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //交易状态    orderState  是   String(1)   1   详见“附录4.2 交易状态编码”  支付成功    1   交易下单成功与支付成功，且收到第三方成功回执
        String payStatusCode = api_response_params.get(orderState);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(amount));

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        //boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("1")) {
            my_result = true;
        } else {
            log.error("[新万能充2]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[新万能充2]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[新万能充2]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[新万能充2]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}