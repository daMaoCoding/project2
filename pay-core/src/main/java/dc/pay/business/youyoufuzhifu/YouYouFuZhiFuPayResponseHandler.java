package dc.pay.business.youyoufuzhifu;

import java.util.Base64;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Jan 3, 2019
 */
@ResponsePayHandler("YOUYOUFUZHIFU")
public final class YouYouFuZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //merchno        商户号        15    是    　
    //status         交易状态      1     是    0-未支付    1-支付成功        2-支付失败
    //traceno        商户流水号    30    是    商家的流水号
    //orderno        系统订单号    12    是    系统订单号,同上面接口的refno。
    //merchName      商户名称      30    是    　
    //amount         交易金额      12    是    单位/元
    //transDate      交易日期      10    是    　
    //transTime      交易时间      8     是    　
    //payType        支付方式      1     是    1-支付宝    2-微信    3-百度钱包    4-QQ钱包    5-京东钱包    
    //openId         用户OpenId    50    否    支付的时候返回
    
    //order_id: xxx,    接入商网站提交的订单号
    private static final String order_id                ="order_id";
    //amount: xxxx,   金额，单位 分，没有小数点
    private static final String amount                 ="amount";
    // cmd: “order_success”/ “order_revoked”/ “order_timeout”
//    private static final String cmd                ="cmd";
    //  verified_time:  xxx,   订单完成时间，UNIX时间戳秒值
    private static final String verified_time                ="verified_time";
    //"status": "四种状态 verified / timeout / revoked / created", 
    private static final String status                ="status";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="qsign";

    private static final String RESPONSE_PAY_MSG = "true";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
//        String partnerR = API_RESPONSE_PARAMS.get(merchno);
        String ordernumberR = API_RESPONSE_PARAMS.get(order_id);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[友付支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        StringBuilder signStr = new StringBuilder();
        signStr.append(order_id+"=").append(api_response_params.get(order_id)).append("&");
        signStr.append(amount+"=").append(api_response_params.get(amount)).append("&");
        signStr.append(verified_time+"=").append(api_response_params.get(verified_time));
//        signStr.append(channelWrapper.getAPI_KEY());
        String paramsStr =signStr.toString();
        String signMd5 = null;
        try {
            signMd5 = HmacSHA1Encrypt1(paramsStr, channelWrapper.getAPI_KEY());
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[友付支付]-[响应支付]-2.生成加密URL签名出错，签名出错：{}",e.getMessage(),e);
            throw new PayException(e.getMessage(),e);
        }
        log.debug("[友付支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //"status": "四种状态 verified / timeout / revoked / created", 
        String payStatusCode = api_response_params.get(status);
        String responseAmount = api_response_params.get(amount);
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("verified")) {
            my_result = true;
        } else {
            log.error("[友付支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[友付支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：verified");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[友付支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[友付支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
    
    
    private static final String MAC_NAME1 = "HmacSHA1";
    public static String HmacSHA1Encrypt1(String encryptText, String encryptKey) throws Exception
    {
//        byte[] data=encryptKey.getBytes(ENCODING);
//        //根据给定的字节数组构造一个密钥,第二参数指定一个密钥算法的名称
//        SecretKey secretKey = new SecretKeySpec(data, MAC_NAME);
//        //生成一个指定 Mac 算法 的 Mac 对象
//        Mac mac = Mac.getInstance(MAC_NAME);
//        //用给定密钥初始化 Mac 对象
//        mac.init(secretKey);
//
//        byte[] text = encryptText.getBytes(ENCODING);
//        //完成 Mac 操作
//        String result = Base64.getEncoder().encodeToString(mac.doFinal(text));
        SecretKeySpec signingKey = new SecretKeySpec(encryptKey.getBytes(), MAC_NAME1);
        Mac mac = Mac.getInstance(MAC_NAME1);
        mac.init(signingKey);
        byte[] rawHmac = mac.doFinal(encryptText.getBytes());
        String result = Base64.getEncoder().encodeToString(rawHmac);
        return result;
    }
}