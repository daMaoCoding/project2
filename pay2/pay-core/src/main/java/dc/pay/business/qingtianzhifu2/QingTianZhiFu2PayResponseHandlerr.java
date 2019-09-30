package dc.pay.business.qingtianzhifu2;

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
@ResponsePayHandler("QINGTIANZHIFU2")
public final class QingTianZhiFu2PayResponseHandlerr extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //参数名称    变量名 类型长度    是否必填    说明
    //支付状态    status  int(8)  必填  1：成功，0：失败
    private static final String status                ="status";
    //商户编号    customerid  varchar(20) 必填  商户编号
    private static final String customerid                ="customerid";
    //商户订单号   sdorderno   varchar(100)    必填  商户订单号
    private static final String sdorderno                ="sdorderno";
    //平台订单号   sdpayno Varchar(100)    必填  平台订单号
    private static final String sdpayno                ="sdpayno";
    //订单金额    total_fee   decimal(10,2)   必填  商户支付金额
    private static final String total_fee                ="total_fee";
    //原始订单价格  order_fee   decimal(10,2)   必填  商户订单金额
    private static final String order_fee                ="order_fee";
    //订单原始金额  money   decimal(10,2)   必填  商户订单原始金额
//    private static final String money                ="money";
    //支付类型    paytype varchar(100)    必填  通道支付类型
    private static final String paytype                ="paytype";
    //备注  remark  varchar(100)    必填  备注
    private static final String remark                ="remark";
    // 签名 sign    varchar(100)    必填  签名（已不使用，为了兼容旧商户，可以忽略）
//    private static final String sign                ="sign";
    // 签名2    signV2  varchar(100)    必填  签名2
//    private static final String signV2                ="signV2";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="signV2";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
       //String api_KEY = channelWrapper.getAPI_KEY();
       //if (null == api_KEY || !api_KEY.contains("-") || api_KEY.split("-").length != 2) {
       //    log.error("[晴天支付2]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //    throw new PayException("[晴天支付2]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //}
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(customerid);
        String ordernumberR = API_RESPONSE_PARAMS.get(sdorderno);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[晴天支付2]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        StringBuilder signStr = new StringBuilder();
        signStr.append(customerid+"=").append(api_response_params.get(customerid)).append("&");
        signStr.append(status+"=").append(api_response_params.get(status)).append("&");
        signStr.append(sdpayno+"=").append(api_response_params.get(sdpayno)).append("&");
        signStr.append(sdorderno+"=").append(api_response_params.get(sdorderno)).append("&");
        signStr.append(order_fee+"=").append(api_response_params.get(order_fee)).append("&");
        signStr.append(total_fee+"=").append(api_response_params.get(total_fee)).append("&");
        signStr.append(paytype+"=").append(api_response_params.get(paytype)).append("&");
        signStr.append(channelWrapper.getAPI_KEY());
        String paramsStr =signStr.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[晴天支付2]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //支付状态  status  int(8)  必填  1：成功，0：失败
        String payStatusCode = api_response_params.get(status);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(total_fee));

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        // 1 代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("1")) {
            my_result = true;
        } else {
            log.error("[晴天支付2]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[晴天支付2]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return my_result;
    }

    
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[晴天支付2]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[晴天支付2]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}