package dc.pay.business.ugbizhifu;

import java.util.Map;
import java.util.TreeMap;

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
 * Jul 9, 2019
 */
@ResponsePayHandler("UGBIZHIFU")
public final class UGBiZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //支付请求参数定义如下：参数 参数名称 长度 使用
    //data 代码加密串 1024 AES 公钥加密串
    private static final String data                ="data";
    //merchNo 商户号 40 需要时可以获取商户号接口返回参数定义参数 参数名称 长度（String） 使用
    private static final String merchNo                ="merchNo";
    //extra 附加属性 256 非必填
//    private static final String extra                ="extra";
    //merchCardNo 商户钱包地址 42 非必填
//    private static final String merchCardNo                ="merchCardNo";
    //amount 支付金额(单位:UG) 20 必填
    private static final String amount                ="amount";
//    //merchNo 商户号 32 必填
//    private static final String merchNo                ="merchNo";
    //orderSn 平台订单号 32 必填
//    private static final String orderSn                ="orderSn";
    //merchOrderSn 商户订单号 32 必填
    private static final String merchOrderSn                ="merchOrderSn";
    //userName 收款方登陆帐号 32 必填
//    private static final String userName                ="userName";
    //userPayCardNo 收款方钱包地址 42 非必填
//    private static final String userPayCardNo                ="userPayCardNo";
    //payState 支付状态 42 非必填
    private static final String payState                ="payState";
    //payViewUrl 回显地址 512 必填
//    private static final String payViewUrl                ="payViewUrl";
    //payDate 支付时间 20 必填
//    private static final String payDate                ="payDate";
    //sign 签名 32 必填
    private static final String sign                ="sign";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = jsonResponsePayMsg("{\"code\": 0,\"message\": \"SUCCESS\",\"data\": null}");

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
       //String api_KEY = channelWrapper.getAPI_KEY();
       //if (null == api_KEY || !api_KEY.contains("-") || api_KEY.split("-").length != 2) {
       //    log.error("[UG币支付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //    throw new PayException("[UG币支付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //}
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String apiKeyFromReqPayMemberId = handlerUtil.getApiKeyFromReqPayMemberId(API_RESPONSE_PARAMS.get(merchNo));
        String api_KEY = apiKeyFromReqPayMemberId;
        if (null == api_KEY || !api_KEY.contains("-") || api_KEY.split("-").length != 4) {
            log.error("[UG币支付]-[响应支付]-“密钥（私钥）”输入数据格式为【中间使用-分隔】：钱包地址-MD5密钥-DES密钥-RSA支付私钥" );
            throw new PayException("[UG币支付]-[响应支付]-“密钥（私钥）”输入数据格式为【中间使用-分隔】：钱包地址-MD5密钥-DES密钥-RSA支付私钥" );
        }
        //调用解密方法用出币私钥进行解密
        String result = CertificateUtil.decryptByPrivateKey(API_RESPONSE_PARAMS.get(data), api_KEY.split("-")[3]);

        Map<String,String> contentMap = JSONObject.parseObject(result, Map.class);
        
        String partnerR = contentMap.get(merchNo);
        String ordernumberR = contentMap.get(merchOrderSn);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[UG币支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        String result = CertificateUtil.decryptByPrivateKey(api_response_params.get(data), api_key.split("-")[3]);

        //未返回标准串解决
        Map<String,String> contentMap = JSONObject.parseObject(result, Map.class);
        String my_sign = contentMap.get(sign);
        contentMap.remove(sign);
        
        Map<String,String> map = new TreeMap<>(contentMap);
//        String signMd5 = ToolsUtils.MD5(JSON.toJSONString(map)+api_key.split("-")[2], "UTF-8");
        String signMd5 = HandlerUtil.getMD5UpperCase(JSON.toJSONString(map)+api_key.split("-")[2]);

        log.debug("[UG币支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        String result = CertificateUtil.decryptByPrivateKey(api_response_params.get(data), channelWrapper.getAPI_KEY().split("-")[3]);

        //未返回标准串解决
        Map<String,String> contentMap = JSONObject.parseObject(result, Map.class);
         
        boolean my_result = false;
        //payState 支付状态 42 非必填
        String payStatusCode = contentMap.get(payState);
        String responseAmount = HandlerUtil.getFen(contentMap.get(amount));

        //tony(Tony) 01-17 15:09:28
        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        //boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("0")) {
            my_result = true;
        } else {
            log.error("[UG币支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[UG币支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：0");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        String result = CertificateUtil.decryptByPrivateKey(api_response_params.get(data), channelWrapper.getAPI_KEY().split("-")[3]);

        //未返回标准串解决
        Map<String,String> contentMap = JSONObject.parseObject(result, Map.class);
        
        boolean my_result = contentMap.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[UG币支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[UG币支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
    

}