package dc.pay.business.caimengzhifu;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.business.ruijietong.RuiJieTongUtil;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;

/**
 * 
 * @author andrew
 * Aug 20, 2019
 */
@ResponsePayHandler("CAIMENGZHIFU")
public final class CaiMengZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //3.2.3 请求 3.2.3.1 请求格式
    //参数代码 参数名称 类型 必须 备注 
    //merOrderNo 商户订单号 String 是 商户订单号 
    private static final String merOrderNo                ="merOrderNo";
    //merId 商户编号 String 是 
    private static final String merId                ="merId";
    //data 参数列表 String 是 使用 RSA 加密 data 格式 
    private static final String data                ="data";

    //参数代码 参数名称 类型 必须 签名 备注 
    //merOrderNo 商户订单号 String 是 是 商户订单号 
//    private static final String merOrderNo                ="merOrderNo";
    //orderState 订单状态 Integer 是 是 订单状态(0=处理 中，1=成功，2=失 败) 
    private static final String orderState                ="orderState";
    //orderNo 平台订单号 String 是 是 
    private static final String orderNo                ="orderNo";
    //amount 订单金额 Decimal 是 是
    private static final String amount                ="amount";
    //sign 签名信息 String 是 否 参照 参数签名章 节 
    private static final String sign                ="sign";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
       //String api_KEY = channelWrapper.getAPI_KEY();
       //if (null == api_KEY || !api_KEY.contains("-") || api_KEY.split("-").length != 2) {
       //    log.error("[彩盟支付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //    throw new PayException("[彩盟支付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //}
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(merId);
        String ordernumberR = API_RESPONSE_PARAMS.get(merOrderNo);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[彩盟支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        
        boolean my_result = false;
       // 根据商户私钥解密协议体
        try {
            byte[] result = RuiJieTongUtil.decryptByPrivateKey(org.apache.shiro.codec.Base64.decode(api_response_params.get(data)),channelWrapper.getAPI_KEY().split("-")[1]);
            String my_data = new String(result, RuiJieTongUtil.CHARSET);

            JSONObject dataJSON = JSONObject.parseObject(my_data);
//            System.out.println("my_data=========>"+my_data);

            StringBuilder signStr = new StringBuilder();
            signStr.append(amount+"=").append(dataJSON.getString(amount)).append("&");
            signStr.append(merOrderNo+"=").append(dataJSON.getString(merOrderNo)).append("&");
            signStr.append(orderNo+"=").append(dataJSON.getString(orderNo)).append("&");
            signStr.append(orderState+"=").append(dataJSON.getString(orderState)).append("&");
            signStr.append(key +"=").append(channelWrapper.getAPI_KEY().split("-")[0]);
            String paramsStr =signStr.toString();
            
//            System.out.println("签名源串=========>"+paramsStr);
            String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr,"UTF-8").toLowerCase();
            my_result = signMd5.equalsIgnoreCase(dataJSON.getString(sign)) ? true : false;
//            System.out.println("签名我方=========>"+signMd5);
//            System.out.println("签名三方=========>"+dataJSON.getString(sign));
            
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new PayException(SERVER_MSG.REQUEST_PAY_CHANNEL_RESULT_ERROR);
        }
        log.debug("[彩盟支付]-[响应支付]-2.生成加密URL签名完成：{}", String.valueOf(my_result) );
        return String.valueOf(my_result);
    }
    
    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        
        String my_data = null;
        try {
            byte[] result = RuiJieTongUtil.decryptByPrivateKey(org.apache.shiro.codec.Base64.decode(api_response_params.get(data)),channelWrapper.getAPI_KEY().split("-")[1]);
            my_data = new String(result, RuiJieTongUtil.CHARSET);
        } catch (Exception e) {
            e.printStackTrace();
            throw new PayException(SERVER_MSG.REQUEST_PAY_CHANNEL_RESULT_ERROR);
        }
        JSONObject dataJSON = JSONObject.parseObject(my_data);
        
        boolean my_result = false;
        
        //orderState 订单状态 Integer 是 是 订单状态(0=处理 中，1=成功，2=失 败)
        String payStatusCode = dataJSON.getString(orderState);
        String responseAmount = HandlerUtil.getFen(dataJSON.getString(amount));

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        // 1 代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("1")) {
            my_result = true;
        } else {
            log.error("[彩盟支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[彩盟支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return my_result;
    }

    
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
//        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        
        Boolean my_result = new Boolean(signMd5);
        log.debug("[彩盟支付]-[响应支付]-4.验证MD5签名：{}", my_result.booleanValue());
        return my_result.booleanValue();
    }

    @Override
    protected String responseSuccess() {
        log.debug("[彩盟支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}