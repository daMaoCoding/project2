package dc.pay.business.xingpayzhifu;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
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
import sun.misc.BASE64Decoder;

/**
 * 
 * @author andrew
 * Aug 16, 2019
 */
@ResponsePayHandler("XINGPAYZHIFU")
public final class XingPayZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //公共请求参数
    //字段名 是否必传    类型  描述
    //sign    是   String  签名
//    private static final String sign                ="sign";
    //data    是   String  业务参数
    private static final String data                ="data";
    //业务参数组成
    //orderId test20160725000005  商户订单号   orderId
    private static final String orderId                ="orderId";
    //tranId  5c186836-e3b7-4ce9-b6fe-33699d0b2b79    交易编号（用于调用支付）    tranId
//    private static final String tranId                ="tranId";
    //amount      订单金额    amount
    private static final String amount                ="amount";
    //status  0，处理中、1，成功、2，失败 支付状态    status
    private static final String status                ="status";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = jsonResponsePayMsg("{'success':true}");

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
       //String api_KEY = channelWrapper.getAPI_KEY();
       //if (null == api_KEY || !api_KEY.contains("-") || api_KEY.split("-").length != 2) {
       //    log.error("[新Gpay支付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //    throw new PayException("[新Gpay支付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //}
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        JSONObject jsonObject = null;
        try {
            String my_data = parseBase64(URLDecoder.decode(API_RESPONSE_PARAMS.get(data), "utf-8"));
            jsonObject = JSONObject.parseObject(my_data);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            log.error("[新Gpay支付]-[响应支付]-1.验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",通道名称：" + channelWrapper.getAPI_CHANNEL_BANK_NAME() + ",postUrl:" + channelWrapper.getAPI_CHANNEL_BANK_URL() + ",payForManagerCGIResultJsonObj" + e.getMessage(), e);
            throw new PayException(e.getMessage(),e);
        }
        String ordernumberR = jsonObject.getString(orderId);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[新Gpay支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        String my_data = null;
        try {
            my_data = parseBase64(URLDecoder.decode(api_response_params.get(data), "utf-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            log.error("[新Gpay支付]-[响应支付]验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",通道名称：" + channelWrapper.getAPI_CHANNEL_BANK_NAME() + ",postUrl:" + channelWrapper.getAPI_CHANNEL_BANK_URL() + ",payForManagerCGIResultJsonObj" + e.getMessage(), e);
            throw new PayException(e.getMessage(),e);
        }
        String paramsStr =my_data+api_key;
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[新Gpay支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        JSONObject jsonObject = null;
        try {
            String my_data = parseBase64(URLDecoder.decode(API_RESPONSE_PARAMS.get(data), "utf-8"));
            jsonObject = JSONObject.parseObject(my_data);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            log.error("[新Gpay支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",通道名称：" + channelWrapper.getAPI_CHANNEL_BANK_NAME() + ",postUrl:" + channelWrapper.getAPI_CHANNEL_BANK_URL() + ",payForManagerCGIResultJsonObj" + e.getMessage(), e);
            throw new PayException(e.getMessage(),e);
        }
        
        boolean my_result = false;
        //status    0，处理中、1，成功、2，失败 支付状态    status
        String payStatusCode = jsonObject.getString(status);
        String responseAmount = jsonObject.getString(amount);

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        // 1 代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("1")) {
            my_result = true;
        } else {
            log.error("[新Gpay支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[新Gpay支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[新Gpay支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[新Gpay支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
    
    // 解密
    public static String parseBase64(String s) {
        byte[] b = null;
        String result = null;
        if (s != null) {
            BASE64Decoder decoder = new BASE64Decoder();
            try {
                b = decoder.decodeBuffer(s);
                result = new String(b, "utf-8");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }
}