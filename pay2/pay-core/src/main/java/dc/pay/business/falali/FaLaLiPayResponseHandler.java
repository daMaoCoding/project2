package dc.pay.business.falali;

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

/**
 * 
 * @author andrew
 * Aug 12, 2019
 */
@ResponsePayHandler("FALALI")
public final class FaLaLiPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //异步通知
    //传参方式：POST+json
    //用户付款成功后，我们会向您在发起付款接口传入的notify_url网址发送通知(POST)。您的服务器只要返回小写字符串“success”（不包括引号），就表示回调成功。通知内容(json)如下:
    //#   参数名 含义  类型  说明  参与加密
    //1.  user_order_no   您的自定义订单号    string(50)  一定存在。是您在发起付款接口传入的您的自定义订单号   
    private static final String user_order_no                ="user_order_no";
    //2.  orderno 平台生成的订单号    string(50)  一定存在。是此订单在本服务器上的唯一编号    
    private static final String orderno                ="orderno";
    //3.  tradeno 支付流水号   string(50)  一定存在。支付宝支付或微信支付的流水订单号   
    private static final String tradeno                ="tradeno";
    //4.  price   订单定价    float   一定存在。是您在发起付款接口传入的订单价格   
    private static final String price                ="price";
    //5.  realprice   实际支付金额  float   一定存在。表示用户实际支付的金额。一般会和price值一致，如果同时存在多个用户支付同一金额，就会和price存在一定差额，差额一般在1-2分钱上下，越多人同时付款，差额越大 
    private static final String realprice                ="realprice";
    //6.  cuid    您的自定义用户唯一标识 string(50)  如果您在发起付款接口带入此参数，我们会原封不动传回   
//    private static final String cuid                ="cuid";
    //7.  note    附加内容    string(1000)    如果您在发起付款接口带入此参数，我们会原封不动传回   
    private static final String note                ="note";
    //8.  sign    签名  string(32)  将参数1至5按顺序连Token一起，做md5-32位加密，取字符串小写。您需要在您的服务端按照同样的算法，自己验证此sign是否正确。只在正确时，执行您自己逻辑中支付成功代码。（拼接顺序：user_order_no + orderno + tradeno + price + realprice + token）    
//    private static final String sign                ="sign";
    
//    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
       //String api_KEY = channelWrapper.getAPI_KEY();
       //if (null == api_KEY || !api_KEY.contains("-") || api_KEY.split("-").length != 2) {
       //    log.error("[法拉利]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //    throw new PayException("[法拉利]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //}
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(note);
        String ordernumberR = API_RESPONSE_PARAMS.get(user_order_no);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[法拉利]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        //Map<String, String> jsonToMap = handlerUtil.jsonToMap(api_response_params.get(data));
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(api_response_params.get(user_order_no));
        signSrc.append(api_response_params.get(orderno));
        signSrc.append(api_response_params.get(tradeno));
        signSrc.append(api_response_params.get(price));
        signSrc.append(api_response_params.get(realprice));
        signSrc.append(api_key);
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[法拉利]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //status        交易状态    1    是    0-未支付    1-支付成功        2-支付失败
//        String payStatusCode = api_response_params.get(status);
        String payStatusCode = "无";
        String responseAmount = HandlerUtil.getFen(api_response_params.get(realprice));

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        // 1 代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("无")) {
            my_result = true;
        } else {
            log.error("[法拉利]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[法拉利]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：无");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[法拉利]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[法拉利]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}