package dc.pay.business.zhilianzhifu0;

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
 * Aug 6, 2019
 */
@ResponsePayHandler("ZHILIANZHIFU0")
public final class ZhiLianZhiFu0PayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //字段名 字段解释
    //cpOrderId   cp订单号
    private static final String cpOrderId                ="cpOrderId";
    //cpId    Cpid
    private static final String cpId                ="cpId";
    //price   订单价格(单位:分)
    private static final String price                ="price";
    //orderId 我方订单号
    private static final String orderId                ="orderId";
    //payTime 支付时间（不参与签名）
//    private static final String payTime                ="payTime";
    //status  订单状态(200：表示成功，其他均为失败)
    private static final String status                ="status";
    //statusDes   订单状态描述
    private static final String statusDes                ="statusDes";
    //sign    签名数据,所有字段和值用|符号拼接后，并与key进行拼接后，进行md5加密的结果字符串    =md5(cpOrderId=xx|cpId=xx|price=xx|orderId=xx|status=xx|statusDes=xx|key=xxx)
//    private static final String sign                ="sign";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
       //String api_KEY = channelWrapper.getAPI_KEY();
       //if (null == api_KEY || !api_KEY.contains("-") || api_KEY.split("-").length != 2) {
       //    log.error("[智联支付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //    throw new PayException("[智联支付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //}
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(cpId);
        String ordernumberR = API_RESPONSE_PARAMS.get(cpOrderId);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[智联支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }
        
    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        StringBuilder signStr = new StringBuilder();
        signStr.append(cpOrderId+"=").append(api_response_params.get(cpOrderId)).append("|");
        signStr.append(cpId+"=").append(api_response_params.get(cpId)).append("|");
        signStr.append(price+"=").append(api_response_params.get(price)).append("|");
        signStr.append(orderId+"=").append(api_response_params.get(orderId)).append("|");
        signStr.append(status+"=").append(api_response_params.get(status)).append("|");
        signStr.append(statusDes+"=").append(api_response_params.get(statusDes)).append("|");
        signStr.append(key+"=").append(channelWrapper.getAPI_KEY());
        String paramsStr =signStr.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[智联支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }
    
    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //status    订单状态(200：表示成功，其他均为失败)
        String payStatusCode = api_response_params.get(status);
        String responseAmount = api_response_params.get(price);

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        // 1 代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("200")) {
            my_result = true;
        } else {
            log.error("[智联支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[智联支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：200");
        return my_result;
    }

    
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[智联支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[智联支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}