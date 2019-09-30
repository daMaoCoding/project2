package dc.pay.business.anyifu;

import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

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
 * Nov 7, 2018
 */
@ResponsePayHandler("ANYIFU")
public final class AnYiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //body
    //输入参数       　       　       　       　
    //字段名                 变量名             类型            说明       可空
    //安逸付订单号           transNo            String          平台支付订单号       N
    //外部订单号             outTradeNo         String          商户订单号       N
    //交易金额               transAmount        String          交易金额（单位：分）       N
    //支付金额               payAmount          String          成功金额（单位：分）       N
    //交易时间               transTime          String          格式：yyyyMMddHHmmss    例如：20180420110444       N
    //支付时间               payTime            String          格式：yyyyMMddHHmmss   例如：20180420110444       N
    //交易描述               transDesc          String                                N
    //交易状态               transStatus        String          订单交易状态编码（04：支付成功）       N
    //交易方式               payMode            String          网银：Bank          网银快捷：BankEX     微信扫码：Wechat    微信WAP：WechatWap      支付宝扫码：Alipay  支付宝WAP：AlipayWap      QQ扫码：QQ          QQ WAP：QQWAP  京东扫码：JD        京东WAP：JDWap       银联扫码：BankQRCode       N
    private static final String transNo                        ="transNo";
    private static final String outTradeNo                     ="outTradeNo";
    private static final String transAmount                    ="transAmount";
    private static final String payAmount                      ="payAmount";
    private static final String transTime                      ="transTime";
    private static final String payTime                        ="payTime";
    private static final String transDesc                      ="transDesc";
    private static final String transStatus                    ="transStatus";
    private static final String payMode                        ="payMode";
    
    //senderId  String  商户号
    private static final String senderId                        ="senderId";
    //traceNo   String  商户交易流水号，通常和商户请求订单号相同
    private static final String traceNo                        ="traceNo";

    //head
    private static final String charset                        ="charset";
//    private static final String senderId                     ="senderId"; 
//    private static final String traceNo                      ="traceNo";
    private static final String serviceName                    ="serviceName";
    private static final String version                        ="version";
    private static final String sendTime                       ="sendTime";
    
    private static final String head                 ="head";
    private static final String body                 ="body";
    
//    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="md5msg";

    private static final String RESPONSE_PAY_MSG = "true";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        Set<String> keys = API_RESPONSE_PARAMS.keySet();
        Iterator<String> iterator = keys.iterator();
        String next = iterator.next();
        if (!next.contains("{") || !next.contains("}")) {
            log.error("[安逸付]-[响应支付]-1.第三方响应结果：" + JSON.toJSONString(API_RESPONSE_PARAMS) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(JSON.toJSONString(API_RESPONSE_PARAMS));
         }
        JSONObject headObject = null;
        try {
            headObject = JSON.parseObject(next).getJSONObject("head");
        } catch (Exception e) {
            throw new PayException(SERVER_MSG.REQUEST_PAY_CHANNEL_RESULT_ERROR);
        }
        String partnerR = headObject.getString(senderId);
        String ordernumberR = headObject.getString(traceNo);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[安逸付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        Set<String> keys = api_response_params.keySet();
        Iterator<String> iterator = keys.iterator();
        String next = iterator.next();
        JSONObject headObject = JSON.parseObject(next).getJSONObject("head");
        JSONObject headData = new JSONObject(true);
        // 报文编码格式
        headData.put(charset, headObject.getString(charset));
        // 请求发送时间
        headData.put(sendTime, headObject.getString(sendTime));
        // 合作商户签约号
        headData.put(senderId, headObject.getString(senderId));
        // 服务名
        headData.put(serviceName, headObject.getString(serviceName));
        // 商户交易流水号
        headData.put(traceNo, headObject.getString(traceNo));
        // 接口版本号
        headData.put(version, headObject.getString(version));
        JSONObject bodyObject = JSON.parseObject(next).getJSONObject("body");
        JSONObject bodyData = new JSONObject(true);
        bodyData.put(outTradeNo , bodyObject.getString(outTradeNo ));
        bodyData.put(payAmount  , bodyObject.getString(payAmount  ));
        bodyData.put(payMode    , bodyObject.getString(payMode    ));
        bodyData.put(payTime    , bodyObject.getString(payTime    ));
        bodyData.put(transAmount, bodyObject.getString(transAmount));
        bodyData.put(transDesc  , bodyObject.getString(transDesc  ));
        bodyData.put(transNo    , bodyObject.getString(transNo    ));
        bodyData.put(transStatus, bodyObject.getString(transStatus));
        bodyData.put(transTime  , bodyObject.getString(transTime  ));
        JSONObject params = new JSONObject(true);
        params.put(head, headData);
        params.put(body, bodyData);
        String signMd5 = null;
        try {
            signMd5 = HandlerUtil.getMD5UpperCase(URLEncoder.encode(params.toString(), "utf-8")+api_key).toLowerCase();
        } catch (Exception e1) {
            e1.printStackTrace();
            throw new PayException(SERVER_MSG.REQUEST_PAY_CHANNEL_RESULT_ERROR);
        }
//        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[安逸付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        Set<String> keys = api_response_params.keySet();
        Iterator<String> iterator = keys.iterator();
        String next = iterator.next();
        JSONObject bodyObject = JSON.parseObject(next).getJSONObject("body");
        
        boolean my_result = false;
        //交易状态               transStatus        String          订单交易状态编码（04：支付成功）       N
        String payStatusCode = bodyObject.getString(transStatus);
        String responseAmount = bodyObject.getString(payAmount);
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("04")) {
            my_result = true;
        } else {
            log.error("[安逸付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[安逸付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：04");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        Set<String> keys = api_response_params.keySet();
        Iterator<String> iterator = keys.iterator();
        String next = iterator.next();
        
        boolean my_result = JSON.parseObject(next).getString(signature).equalsIgnoreCase(signMd5);
        log.debug("[安逸付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[安逸付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}