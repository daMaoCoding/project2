package dc.pay.business.siyzhifu;

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
 * Sep 3, 2019
 */
@ResponsePayHandler("SIYZHIFU")
public final class SiYZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //序号  参数名称    参数名 类型  可否为空    说明
    //1   APP编号   app_id  Number  必填  
    private static final String app_id                ="app_id";
    //2   商户订单号   order_id    String  必填  
    private static final String order_id                ="order_id";
    //3   支付流水号   pay_seq String  必填  由支付平台生成，唯一，不超过30个字符
    private static final String pay_seq                ="pay_seq";
    //4   实付金额    pay_amt Number  必填  建议商户系统校验实付的金额以确定充值的正确性
    private static final String pay_amt                ="pay_amt";
    //5   支付结果    pay_result  Number  必填  20—支付成功，    其它为失败，目前值通知成功的订单
    private static final String pay_result                ="pay_result";
    //6   支付结果描述  result_desc string  选填  支付结果描述
//    private static final String result_desc                ="result_desc";
    //7   扩展参数    extends String  选填  商户自定义参数或扩展参数
//    private static final String my_extends                ="extends";
    //8   签名  sign    String  必填  参数机制（参见2.4  HTTP参数签名机制）    参数组成（参见下面的签名参数说明）
//    private static final String sign                ="sign";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "ok";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
       //String api_KEY = channelWrapper.getAPI_KEY();
       //if (null == api_KEY || !api_KEY.contains("-") || api_KEY.split("-").length != 2) {
       //    log.error("[4y支付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //    throw new PayException("[4y支付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //}
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(app_id);
        String ordernumberR = API_RESPONSE_PARAMS.get(order_id);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[4y支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        StringBuilder signStr = new StringBuilder();
        signStr.append(app_id+"=").append(api_response_params.get(app_id)).append("&");
        signStr.append(order_id+"=").append(api_response_params.get(order_id)).append("&");
        signStr.append(pay_seq+"=").append(api_response_params.get(pay_seq)).append("&");
        signStr.append(pay_amt+"=").append(api_response_params.get(pay_amt)).append("&");
        signStr.append(pay_result+"=").append(api_response_params.get(pay_result)).append("&");
        signStr.append(key+"=").append(HandlerUtil.getMD5UpperCase(channelWrapper.getAPI_KEY()).toLowerCase());
        String paramsStr =signStr.toString();
        
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[4y支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //5 支付结果    pay_result  Number  必填  20—支付成功，        其它为失败，目前值通知成功的订单
        String payStatusCode = api_response_params.get(pay_result);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(pay_amt));

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        // 1 代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("20")) {
            my_result = true;
        } else {
            log.error("[4y支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[4y支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：20");
        return my_result;
    }

    
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[4y支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[4y支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}