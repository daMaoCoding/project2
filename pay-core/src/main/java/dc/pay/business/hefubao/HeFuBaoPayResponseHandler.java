package dc.pay.business.hefubao;

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
 * Dec 12, 2017
 */
@ResponsePayHandler("HEFUBAO")
public final class HeFuBaoPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(HeFuBaoPayResponseHandler.class);

    private static final String r2_orderNumber = "r2_orderNumber";         
    private static final String r7_timestamp = "r7_timestamp";         
    private static final String r4_orderStatus = "r4_orderStatus";         
    private static final String r8_desc = "r8_desc";         
    private static final String r5_amount = "r5_amount";         
    private static final String r1_merchantNo = "r1_merchantNo";         
    private static final String r3_serialNumber = "r3_serialNumber";         
    private static final String r6_currency = "r6_currency";         
    private static final String sign = "sign";         
    
    private static final String RESPONSE_PAY_MSG = "SUCCESS";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(r1_merchantNo);
        String orderId = API_RESPONSE_PARAMS.get(r2_orderNumber);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(orderId))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[合付宝]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}", orderId);
        return orderId;
    }

    @Override
    protected String buildPaySign(Map<String, String> payParam, String api_key) throws PayException {
		// 注意参数需要trim，避免一个空格排查问题耽误时间
    	StringBuffer signSrc= new StringBuffer();
    	signSrc.append("#");
    	signSrc.append(payParam.get(r1_merchantNo)).append("#");
    	signSrc.append(payParam.get(r2_orderNumber)).append("#");
    	signSrc.append(payParam.get(r3_serialNumber)).append("#");
    	signSrc.append(payParam.get(r4_orderStatus)).append("#");
    	signSrc.append(payParam.get(r5_amount)).append("#");
    	signSrc.append(payParam.get(r6_currency)).append("#");
    	signSrc.append(payParam.get(r7_timestamp)).append("#");
    	if (null != payParam.get(r8_desc) && StringUtils.isNotBlank(payParam.get(r8_desc))) {
    		signSrc.append(payParam.get(r8_desc)).append("#");
		}
    	signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String pay_md5sign = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[合付宝]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign)+"，参数："+JSON.toJSONString(paramsStr));
        return pay_md5sign;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amount) throws PayException {
        boolean result = false;
        String payStatusCode = api_response_params.get(r4_orderStatus);
        String responseAmount =HandlerUtil.getFen(api_response_params.get(r5_amount));
        boolean checkAmount = amount.equalsIgnoreCase(responseAmount);
        if (checkAmount && payStatusCode.equalsIgnoreCase("SUCCESS")) {
            result = true;
        } else {
            log.error("[合付宝]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + amount);
        }
        log.debug("[合付宝]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：SUCCESS");
        return result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("[合付宝]-[响应支付]-4.验证MD5签名：{}", result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[合付宝]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}