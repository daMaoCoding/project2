package dc.pay.business.xintiantianzhifu;

import java.io.UnsupportedEncodingException;
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
import dc.pay.utils.RsaUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Apr 16, 2019
 */
@ResponsePayHandler("XINTIANTIANZHIFU")
public final class XinTianTianZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //1.HEAD报文
    //说明：报文头(仅支持JSON)
    //参数  类型  是否必填    描述  示例
    //userReqNo   VARCHAR(32) 是   商户方请求流水号    
    private static final String userReqNo                ="userReqNo";
    //reqTime CHAR(14)    是   商户方请求时间 yyyyMMddHHmmss
//    private static final String reqTime                ="reqTime";
    //respNo  VARCHAR(18) 是   天天支付响应流水号   
//    private static final String respNo                ="respNo";
    //respTime    VARCHAR(14) 是   天天支付响应时间    
//    private static final String respTime                ="respTime";
    //respCode    CHAR(6) 是   响应码 参考应答表
//    private static final String respCode                ="respCode";
    //respDesc    VARCHAR(100)    是   响应信息    
//    private static final String respDesc                ="respDesc";
    //sign    VARCHAR 是   签名串 
    private static final String encryptKey                ="encryptKey";
    //version VARCHAR(6)  是   版本号 
//    private static final String version                ="version";
//    private static final String sign                ="sign";
    //encryptKey  VARCHAR 是   RSA加密后的AES key  
    
    //2.请求报文  body
    //参数  类型  是否必填    描述  示例
    //orderNo VARCHAR(32) 是   平台订单号   
//    private static final String orderNo                ="orderNo";
    //userOrderNo VARCHAR(32) 是   商户订单号   
//    private static final String userOrderNo                ="userOrderNo";
    //channelNo   VARCHAR(32) 是   商户登录账号编号    
    private static final String channelNo                ="channelNo";
    //merchantNo  VARCHAR(32) 是   商户支付编号  
//    private static final String merchantNo                ="merchantNo";
    //merchantName    VARCHAR(128)    是   商户名称    
//    private static final String merchantName                ="merchantName";
    //payType CHAR(2) 是   支付类型01:网关支付02:其它   
//    private static final String payType                ="payType";
    //orderAmt    DECIMAL(12,2)   是   支付总金额   
    private static final String orderAmt                ="orderAmt";
    //orderTitle  VARCHAR(256)    是   订单标题    
//    private static final String orderTitle                ="orderTitle";
    //orderDesc   VARCHAR(512)    是   订单描述    
//    private static final String orderDesc                ="orderDesc";
    //accountType VARCHAR(2)  是   借贷标识    1：借，2：贷，3：其它
//    private static final String accountType                ="accountType";
    //beginTime   CHAR(14)    是   订单开始时间  yyyyMMddHHmmss
//    private static final String beginTime                ="beginTime";
    //endTime CHAR(14)    是   订单结束时间  yyyyMMddHHmmss
//    private static final String endTime                ="endTime";
    //status  CHAR(2) 是   订单状态：00：订单创建01：支付中02：支付成功03：支付失败04：已退款05：已撤销  
    private static final String status                ="status";
    //errorMsg    VARCHAR(100)    是   支付失败时返回 
//    private static final String errorMsg                ="errorMsg";
    
//    "":"用户充值",
//    private static final String orderTitle                ="orderTitle";
//    "":"ZFBSMZF",
//    private static final String payCode                ="payCode";
//    "":"1026804123154124800",
//    private static final String merchantNo                ="merchantNo";
//    "":"http://45.32.1.3:80/notify/26",
//    private static final String notifyUrl                ="notifyUrl";
//    "":"20181120406645176654",
//    private static final String userOrderNo                ="userOrderNo";
//    "":"100.0"
//    private static final String orderAmt                ="orderAmt";

    private static final String body                ="body";
    private static final String head                ="head";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "000000";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
       //String api_KEY = channelWrapper.getAPI_KEY();
       //if (null == api_KEY || !api_KEY.contains("-") || api_KEY.split("-").length != 2) {
       //    log.error("[新天天支付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //    throw new PayException("[新天天支付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //}
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = JSON.parseObject(API_RESPONSE_PARAMS.get(head)).getString(channelNo);
        String ordernumberR = JSON.parseObject(API_RESPONSE_PARAMS.get(head)).getString(userReqNo);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[新天天支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        
        boolean my_result = false;
        
        String my_content = api_response_params.get(body);
        String my_sign = JSON.parseObject(api_response_params.get(head)).getString(signature);
        String my_encryptKey = JSON.parseObject(api_response_params.get(head)).getString(encryptKey);

        Encrypter encrypter = new Encrypter(channelWrapper.getAPI_PUBLIC_KEY(), api_key);
        try {
            byte[] decodeBase64KeyBytes = Base64.decodeBase64(my_encryptKey.getBytes("utf-8"));
            byte[] merchantAESKeyBytes = encrypter.RSADecrypt(decodeBase64KeyBytes);
            // 使用base64解码商户请求报文
            byte[] decodeBase64DataBytes = Base64.decodeBase64(my_content.getBytes("utf-8"));
            byte[] realText = encrypter.AESDecrypt(decodeBase64DataBytes, merchantAESKeyBytes);
            my_result = RsaUtil.validateSignByPublicKey(new String(realText), channelWrapper.getAPI_PUBLIC_KEY(), my_sign,"SHA1withRSA");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            throw new PayException(e.getMessage(),e);
        }
        log.debug("[新天天支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(my_result) );
        return String.valueOf(my_result);
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        String my_content = api_response_params.get(body);
        String my_encryptKey = JSON.parseObject(api_response_params.get(head)).getString(encryptKey);
      
        JSONObject parseObject = null;
        Encrypter encrypter = new Encrypter(channelWrapper.getAPI_PUBLIC_KEY(), channelWrapper.getAPI_KEY());
        try {
            byte[] decodeBase64KeyBytes = Base64.decodeBase64(my_encryptKey.getBytes("utf-8"));
            byte[] merchantAESKeyBytes = encrypter.RSADecrypt(decodeBase64KeyBytes);
            // 使用base64解码商户请求报文
            byte[] decodeBase64DataBytes = Base64.decodeBase64(my_content.getBytes("utf-8"));
            byte[] realText = encrypter.AESDecrypt(decodeBase64DataBytes, merchantAESKeyBytes);
            parseObject = JSON.parseObject(new String(realText, "utf-8"));
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
            throw new PayException(e1.getMessage(),e1);
        }
        
        boolean my_result = false;
        ////status  CHAR(2) 是   订单状态：00：订单创建01：支付中02：支付成功03：支付失败04：已退款05：已撤销  
        String payStatusCode = parseObject.getJSONObject(body).getString(status);
        String responseAmount = HandlerUtil.getFen(parseObject.getJSONObject(body).getString(orderAmt));
        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        //boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("02")) {
            my_result = true;
        } else {
            log.error("[新天天支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[新天天支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：02");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        Boolean my_result = new Boolean(signMd5);
        
//        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[新天天支付]-[响应支付]-4.验证MD5签名：{}", my_result.booleanValue());
        return my_result.booleanValue();
    }

    @Override
    protected String responseSuccess() {
        log.debug("[新天天支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}