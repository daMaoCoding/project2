package dc.pay.business.liulian;

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
 * Nov 23, 2018
 */
@ResponsePayHandler("LIULIAN")
public final class LiuLianPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //参数名称            参数含义          参数说明                                              是否必填          长度          签名顺序
    //r1_MerchantNo       商户编号          商户在支付系统的唯一身份标。                            是               X(15)          1
    //r2_OrderNo          商户订单号        支付平台返回商户订单号。                                是               X(30)          2
    //r3_Amount           支付金额          单位:元，精确到分，保留两位小数。例如：10.23。          是               D(16,2)          3
    //r4_Cur              交易币种          默认设置为1（代表人民币）。                             是               X(10)          4
    //r5_Status           支付状态          100：支付成功；101：支付失败。                          是               X(10)          5
    //ra_PayTime          支付时间          格式：YY-MM-DD HH:mm:ss。                               是               X(20)          6
    //rb_DealTime         交易结果通知时间  格式：YYYY-MM-DD hh:mm:ss。                             是               X(20)          7
    //sign                签名数据          参见5签名机制。                                         是               X(4000)          
    private static final String r1_MerchantNo                      ="r1_MerchantNo";
    private static final String r2_OrderNo                         ="r2_OrderNo";
    private static final String r3_Amount                          ="r3_Amount";
    private static final String r4_Cur                             ="r4_Cur";
    private static final String r5_Status                          ="r5_Status";
    private static final String ra_PayTime                         ="ra_PayTime";
    private static final String rb_DealTime                        ="rb_DealTime";
//    private static final String sign                               ="sign";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(r1_MerchantNo);
        String ordernumberR = API_RESPONSE_PARAMS.get(r2_OrderNo);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[榴莲]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        StringBuilder signStr = new StringBuilder();
        signStr.append(api_response_params.get(r1_MerchantNo));
        signStr.append(api_response_params.get(r2_OrderNo));
        signStr.append(api_response_params.get(r3_Amount));
        signStr.append(api_response_params.get(r4_Cur));
        signStr.append(api_response_params.get(r5_Status));
        signStr.append(api_response_params.get(ra_PayTime));
        signStr.append(api_response_params.get(rb_DealTime));
        signStr.append(channelWrapper.getAPI_KEY());
        String paramsStr =signStr.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[榴莲]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //r5_Status           支付状态          100：支付成功；101：支付失败。                          是               X(10)          5
        String payStatusCode = api_response_params.get(r5_Status);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(r3_Amount));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("100")) {
            my_result = true;
        } else {
            log.error("[榴莲]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[榴莲]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：100");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[榴莲]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[榴莲]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}