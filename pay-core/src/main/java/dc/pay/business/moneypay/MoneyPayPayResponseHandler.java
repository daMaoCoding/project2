package dc.pay.business.moneypay;

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
 * Aug 11, 2018
 */
@ResponsePayHandler("MONEYPAY")
public final class MoneyPayPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //参数名               类型               长度           是否为空             备注
    //company_id           Number            Max(32)           N             支付公司为商户分配的唯一账号
    //company_order_no     String            Max(32)           N             商户订单号
    //trade_no             String            Max(64)           N             支付公司的订单号
    //extra_param          String            Max(512)          Y             扩展字段
    //original_amount      number            Max(8)            N             原始申请金额
    //actual_amount        number            Max(8)            N             订单实际到账金额 以元为单位，精确到小数点后两位.例如：10.11
    //status               number            Max(1)            N             0: 充值成功  1：充值失败
    //apply_time           string            Max (14)          N             订单申请时间，以支付公司系统时间为准 格式：20171016135222
    //operating_time       string            Max(14)           N             客户实际支付完成时间 格式： 20171016135222
    //api_version          String            Max(4)            N             默认 1.5
    //type                 number            Max(1)            N             值为1：充值成功确认
    //sign                 String            Max(32)           N             加密字符串
    private static final String company_id                     ="company_id";
    private static final String company_order_no               ="company_order_no";
    private static final String trade_no                       ="trade_no";
//    private static final String extra_param                    ="extra_param";
//    private static final String original_amount                ="original_amount";
    private static final String actual_amount                  ="actual_amount";
    private static final String status                         ="status";
//    private static final String apply_time                     ="apply_time";
//    private static final String operating_time                 ="operating_time";
    private static final String api_version                    ="api_version";
    private static final String type                           ="type";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

//    private static final String RESPONSE_PAY_MSG = "{\"error_msg\":\"OK\",\"company_order_no\":\"DORAW212018062910085415245\",\"trade_no\":\"105007\",\"status\":\"0\"}";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(company_id);
        String ordernumberR = API_RESPONSE_PARAMS.get(company_order_no);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[moneypay]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        StringBuilder signStr = new StringBuilder();
        signStr.append(company_id+"=").append(api_response_params.get(company_id)).append("&");
        signStr.append(company_order_no+"=").append(api_response_params.get(company_order_no)).append("&");
        signStr.append(trade_no+"=").append(api_response_params.get(trade_no)).append("&");
        signStr.append(actual_amount+"=").append(api_response_params.get(actual_amount)).append("&");
        signStr.append(api_version+"=").append(api_response_params.get(api_version));
        signStr.append(channelWrapper.getAPI_KEY());
        String paramsStr =signStr.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[moneypay]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //type  number  Max(1)  N   值为1：充值成功确认
        String payStatusCode = api_response_params.get(type);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(actual_amount));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("1")) {
            my_result = true;
        } else {
            log.error("[moneypay]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[moneypay]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[moneypay]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        String MY_RESPONSE_PAY_MSG = "{\"error_msg\":\"OK\",\"company_order_no\":\""+API_RESPONSE_PARAMS.get(company_order_no)+"\",\"trade_no\":\""+API_RESPONSE_PARAMS.get(trade_no)+"\",\"status\":\""+API_RESPONSE_PARAMS.get(status)+"\"}";
        log.debug("[moneypay]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", MY_RESPONSE_PAY_MSG);
        return MY_RESPONSE_PAY_MSG;
    }
}