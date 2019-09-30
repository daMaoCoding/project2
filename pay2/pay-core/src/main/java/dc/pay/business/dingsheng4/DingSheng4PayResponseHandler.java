package dc.pay.business.dingsheng4;

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
import dc.pay.utils.RsaUtil;

/**
 * 
 * @author andrew
 * Aug 28, 2019
 */
@ResponsePayHandler("DINGSHENG4")
public final class DingSheng4PayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //•通知参数列表
    //参数含义    参数名称    必填  加入签名    说明
    //商户号 merchant    是   是   商户号，由支付平台提供
    private static final String merchant                ="merchant";
    //类型  qrtype  是   是   支付类型    固定值：wp微信；ap支付宝；aph5支付宝H5 
    private static final String qrtype                ="qrtype";
    //商户订单号   customno    是   是   请求时商户传入的customno
    private static final String customno                ="customno";
    //订单时间    sendtime    是   是   请求时商户传入的sendtime 
    private static final String sendtime                ="sendtime";
    //支付平台订单号 orderno 是   是   支付平台系统产生的订单号，格式：数据字母组成
    private static final String orderno                ="orderno";
    //订单金额    money   是   是   客户支付金额，该参数与请求时商户传入的money相同。以“元”为单位，两位小数 
    private static final String money                ="money";
    //支付时间    paytime 是   是   客户支付时间，10位数UNIX时间戳
    private static final String paytime                ="paytime";
    //订单状态    state   是   是   固定值：1(支付成功)    暂时只对支付成功的订单回调通知
    private static final String state                ="state";
    //签名  sign    是   -   32位小写md5签名值
//    private static final String sign                ="sign";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "OK";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
       //String api_KEY = channelWrapper.getAPI_KEY();
       //if (null == api_KEY || !api_KEY.contains("-") || api_KEY.split("-").length != 2) {
       //    log.error("[鼎盛4]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //    throw new PayException("[鼎盛4]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //}
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(merchant);
        String ordernumberR = API_RESPONSE_PARAMS.get(customno);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[鼎盛4]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        StringBuilder signStr = new StringBuilder();
        signStr.append(merchant+"=").append(api_response_params.get(merchant)).append("&");
        signStr.append(qrtype+"=").append(api_response_params.get(qrtype)).append("&");
        signStr.append(customno+"=").append(api_response_params.get(customno)).append("&");
        signStr.append(sendtime+"=").append(api_response_params.get(sendtime)).append("&");
        signStr.append(orderno+"=").append(api_response_params.get(orderno)).append("&");
        signStr.append(money+"=").append(api_response_params.get(money)).append("&");
        signStr.append(paytime+"=").append(api_response_params.get(paytime)).append("&");
        signStr.append(state+"=").append(api_response_params.get(state));
//        signStr.append(channelWrapper.getAPI_KEY());
        String paramsStr =signStr.toString();
        boolean my_result = false;
        String wpay_public_key = channelWrapper.getAPI_PUBLIC_KEY();
        my_result = RsaUtil.validateSignByPublicKey(paramsStr, wpay_public_key, api_response_params.get(signature),"SHA256WithRSA");    // 验签   signInfoUU付返回的签名参数排序， wpay_public_keyUU付公钥， wpaySignUU付返回的签名
        log.debug("[鼎盛4]-[响应支付]-2.生成加密URL签名完成：" + JSON.toJSONString(my_result));
        return String.valueOf(my_result);
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //订单状态  state   是   是   固定值：1(支付成功)        暂时只对支付成功的订单回调通知
        String payStatusCode = api_response_params.get(state);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(money));

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        // 1 代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("1")) {
            my_result = true;
        } else {
            log.error("[鼎盛4]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[鼎盛4]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
//        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        Boolean my_result = new Boolean(signMd5);
        log.debug("[鼎盛4]-[响应支付]-4.验证MD5签名：{}", my_result.booleanValue());
        return my_result.booleanValue();
    }

    @Override
    protected String responseSuccess() {
        log.debug("[鼎盛4]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}