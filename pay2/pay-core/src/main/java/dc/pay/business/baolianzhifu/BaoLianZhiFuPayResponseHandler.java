package dc.pay.business.baolianzhifu;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
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
 * Sep 2, 2019
 */
@ResponsePayHandler("BAOLIANZHIFU")
public final class BaoLianZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //字段名 字段说明    数据类型    最大长度    备注  必填
    //mchtid  商户ID    int     商户ID（不参与reqdata加密）  是
//    private static final String mchtid                ="mchtid";
    //ordernumber 商户订单号   String      上行过程中商户系统传入的p4_orderno  是
    private static final String ordernumber                ="ordernumber";
    //reqdata RSA加密，    UrlEncode编码 String  4000    RSA公钥加密    UrlEncode(RSA("{\"partner\":\"22222\",\"ordernumber\":\"20180628134056\",\"orderstatus\":\"1\",\"paymoney\":\"10.1000\",\"sysnumber\":\"180628135408009042393\",\"attach\":\"attach\",\"sign\":\"1a35220c866f4013c28e118f6b032b90\"}")) 是
    private static final String reqdata                ="reqdata";
    
    //reqdata数据RSA加密前的数据参数列表:
    //参数名 参数  加入签名    说明
    //商户ID    partner Y   商户id,由宝联支付分配
    private static final String partner                ="partner";
    //商户订单号   ordernumber y   上行过程中商户系统传入的p4_orderno
//    private static final String ordernumber                ="ordernumber";
    //订单结果    orderstatus Y   1:支付成功，非1为支付失败
    private static final String orderstatus                ="orderstatus";
    //订单金额    paymoney    Y   单位元（人民币）
    private static final String paymoney                ="paymoney";
    //宝联支付订单号 sysnumber   N   此次交易中宝联支付接口系统内的订单ID
//    private static final String sysnumber                ="sysnumber";
    //备注信息    attach  N   备注信息，上行中p9_attach原样返回
//    private static final String attach                ="attach";
    //MD5签名   sign    N   32位小写MD5签名值 MD5(partner={}&ordernumber={}&orderstatus={}&paymoney={}key)
    private static final String sign                ="sign";
//    private static final String key        ="key";
    
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "ok";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
       //String api_KEY = channelWrapper.getAPI_KEY();
       //if (null == api_KEY || !api_KEY.contains("-") || api_KEY.split("-").length != 2) {
       //    log.error("[宝联支付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //    throw new PayException("[宝联支付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //}
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(partner);
        String ordernumberR = API_RESPONSE_PARAMS.get(ordernumber);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[宝联支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        
      Map<String,String> jsonToMap = null;
        String my_data = null;
        try {
            byte[] result = RuiJieTongUtil.decryptByPrivateKey(org.apache.shiro.codec.Base64.decode(api_response_params.get(reqdata)),channelWrapper.getAPI_KEY().split("-")[1]);
            my_data = new String(result, RuiJieTongUtil.CHARSET);

            jsonToMap = JSONObject.parseObject(my_data, Map.class);
        } catch (Exception e) {
            e.printStackTrace();
            throw new PayException(SERVER_MSG.REQUEST_PAY_CHANNEL_RESULT_ERROR);
        }
        
        StringBuilder signStr = new StringBuilder();
        signStr.append(partner+"=").append(jsonToMap.get(partner)).append("&");
        signStr.append(ordernumber+"=").append(jsonToMap.get(ordernumber)).append("&");
        signStr.append(orderstatus+"=").append(jsonToMap.get(orderstatus)).append("&");
        signStr.append(paymoney+"=").append(jsonToMap.get(paymoney));
        signStr.append(channelWrapper.getAPI_KEY().split("-")[0]);
        String paramsStr =signStr.toString();
        
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr,"UTF-8").toLowerCase();
        
        boolean my_result = false;
        my_result = signMd5.equals(jsonToMap.get(sign)) ? true : false;
        log.debug("[宝联支付]-[响应支付]-2.生成加密URL签名完成：" + JSON.toJSONString(my_result));
        return String.valueOf(my_result);
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        
        Map<String,String> jsonToMap = null;
        String my_data = null;
        try {
            byte[] result = RuiJieTongUtil.decryptByPrivateKey(org.apache.shiro.codec.Base64.decode(api_response_params.get(reqdata)),channelWrapper.getAPI_KEY().split("-")[1]);
            my_data = new String(result, RuiJieTongUtil.CHARSET);

            jsonToMap = JSONObject.parseObject(my_data, Map.class);
        } catch (Exception e) {
            e.printStackTrace();
            throw new PayException(SERVER_MSG.REQUEST_PAY_CHANNEL_RESULT_ERROR);
        }
        
        boolean my_result = false;
        //订单结果  orderstatus Y   1:支付成功，非1为支付失败
        String payStatusCode = jsonToMap.get(orderstatus);
        String responseAmount = HandlerUtil.getFen(jsonToMap.get(paymoney));

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        // 1 代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("1")) {
            my_result = true;
        } else {
            log.error("[宝联支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[宝联支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        Boolean my_result = new Boolean(signMd5);

        //boolean my_result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("[宝联支付]-[响应支付]-4.验证MD5签名：" + my_result.booleanValue());
        return my_result.booleanValue();
    }

    @Override
    protected String responseSuccess() {
        log.debug("[宝联支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}