package dc.pay.business.hengruntong;

import com.alibaba.fastjson.JSON;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
 
@ResponsePayHandler("HENGRUNTONG")
public final class HengRunTongPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String merchantId       = "merchantId";//商户号
    private static final String orderId          = "orderId";//支付平台订单号
    private static final String transAmt         = "transAmt";//订单金额
    private static final String orderTime        = "orderTime";//订单时间
    private static final String status           = "status";//交易状态
    private static final String transId          = "transId";//商户订单
    private static final String sign             = "sign"; //签名
    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(merchantId);
        String ordernumberR = API_RESPONSE_PARAMS.get(transId);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[恒润通]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
		StringBuffer signSrc= new StringBuffer();
		signSrc.append(merchantId+"=").append(api_response_params.get(merchantId)).append("&");
		signSrc.append(orderId+"=").append(api_response_params.get(orderId)).append("&");
		signSrc.append(orderTime+"=").append(api_response_params.get(orderTime)).append("&");
		signSrc.append(status+"=").append(api_response_params.get(status)).append("&");
		signSrc.append(transAmt+"=").append(api_response_params.get(transAmt)).append("&");
		signSrc.append(transId+"=").append(api_response_params.get(transId)).append("&");
		signSrc.append("key=").append(api_key);
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[恒润通]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) ); 
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean result = false;
        //tradeStatus		交易状态	订单状态<br>SUCCESS 交易成功 <br>FINISH 交易完成 <br>FAILED 交易失败 <br>WAITING_PAYMENT 等待支付 
        String payStatusCode = api_response_params.get(status);
        //String responseAmount = api_response_params.get(transAmt);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(transAmt));
        //amount数据库存入的是分 	第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //SUCCESS代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("1")) {
            result = true;
        } else {
            log.error("[恒润通-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[恒润通]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("[恒润通]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[恒润通]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}