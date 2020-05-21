package dc.pay.business.longteng;

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
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Nov 26, 2018
 */
@ResponsePayHandler("LONGTENG")
public final class LongTengPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //key   string(32)  Y   本平台生成的与用户网站的通讯KEY
    private static final String key                           ="key";
    //money float(2)    Y   金额。真正支付的金额。
    private static final String money                           ="money";
    //amount    float(2)    Y   用户发起支付请求时的金额。
//    private static final String amount                       ="amount";
    //record    string  Y   附加信息，如：用户网站的用户名，订单号，手机号，等唯一标识
    private static final String record                              ="record";
    //sign  string(32)  Y   数据签名 联系QQ 1329954529
    private static final String sdk                              ="sdk";
//    private static final String sign                          ="sign";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(key);
        String ordernumberR = API_RESPONSE_PARAMS.get(record);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[龙腾]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        String my_money = api_response_params.get(money);
        String responseAmount = null;
        if (my_money.endsWith(".00")) {
//        if (false) {
            double s = Double.valueOf(api_response_params.get(money));
            int num1 = (int) s;//整数部分
            responseAmount = Integer.toString(num1);
        }else {
            responseAmount = api_response_params.get(money);
        }
        StringBuilder signStr = new StringBuilder();
        signStr.append(responseAmount);
        signStr.append(api_response_params.get(record));
        signStr.append(api_response_params.get(sdk));
        String paramsStr =signStr.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[龙腾]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //龙腾  33  开户 2018/11/26 14:42:19
        //有paytime就说明支付成功啊
//        String payStatusCode = api_response_params.get(money);
        String payStatusCode = "无";
        
//        double s = Double.valueOf(api_response_params.get(money));
//        int num1 = (int) s;//整数部分
//        String responseAmount = HandlerUtil.getFen(Integer.toString(num1));
        
        String responseAmount = HandlerUtil.getFen(api_response_params.get(money));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && StringUtils.isNotBlank("无")) {
            my_result = true;
        } else {
            log.error("[龙腾]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[龙腾]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：无");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[龙腾]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[龙腾]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}