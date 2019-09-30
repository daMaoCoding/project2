package dc.pay.business.maowangzhifu;

import java.util.List;
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
import dc.pay.utils.MapUtils;
import dc.pay.utils.Sha1Util;

/**
 * 
 * @author sunny
 * Dec 14, 2018
 */
@ResponsePayHandler("MAOWANGZHIFU")
public final class MaoWangZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

//    NO	参数名称				参数含义			长度				是否必填			参数说明
//    1		inputCharset		字符集			String(1)		是				固定填1；1代表UTF-8
//    2		partnerId			商户号			String(32)		是	
//    3		signType			签名类型			String(1)		是				1代表RSA
//    4		tradeSeq			平台交易流水号		String(50)		是				字符串，平台交易流水号
//    5		orderNo				商户订单号		String(50)		是				商户的唯一订单号
//    6		orderAmount			商户金额			String(10)		是				整型数字，单位是分，
//    7		orderDatetime		商户订单提交时间	String(14)		是				日期格式：yyyyMMDDhhmmss，例如：20180116020101
//    8		payDatetime			支付完成时间		String(14)		是				日期格式：yyyyMMDDhhmmss，例如：20180116020101
//    9		payResult			处理结果			String(1)		是				1：支付成功
//    10	signMsg				签名信息			String(1024)	是				请参见本文档“3.3节 签名与验证”
//    11	returnDatetime		结果返回时间		String(14)		是				系统返回支付结果的时间，日期格式：yyyyMMDDhhmmss
//    12	extraCommonParam	公用回传参数		String(100)		否				如果用户请求时传递了该参数，则返回给商户时会回传该参数。

    private static final String inputCharset                   	="inputCharset";
    private static final String partnerId                      	="partnerId";
    private static final String signType                  	   	="signType";
    private static final String tradeSeq                		="tradeSeq";
    private static final String orderNo             			="orderNo";
    private static final String orderAmount                 	="orderAmount";
    private static final String orderDatetime              		="orderDatetime";
    private static final String payDatetime              		="payDatetime";
    private static final String payResult              			="payResult";
    private static final String signMsg              			="signMsg";
    private static final String returnDatetime              	="returnDatetime";
    private static final String extraCommonParam              	="extraCommonParam";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("success");

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(partnerId);
        String ordernumberR = API_RESPONSE_PARAMS.get(orderNo);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[猫王付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
    	List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
    	paramKeys.remove(signMsg);
    	paramKeys.remove(signType);
    	StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
//            if (!pay_productname.equals(paramKeys.get(i)) && !pay_attach.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
            if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
            	signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        //最后一个&转换成#
        signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "");
        String paramsStr = signSrc.toString();
        Boolean signMD5 = RSASignature.doCheck(paramsStr, api_response_params.get(signMsg), channelWrapper.getAPI_PUBLIC_KEY());
        log.debug("[猫王付]-[响应支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return String.valueOf(signMD5);
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        String payStatusCode = api_response_params.get(payResult);
        String responseAmount = api_response_params.get(orderAmount);
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount&&payStatusCode.equalsIgnoreCase("1")) {
            my_result = true;
        } else {
            log.error("[猫王付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[猫王付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
    	Boolean signMd5Boolean = Boolean.valueOf(signMd5);
        log.debug("[猫王付]-[响应支付]-4.验证MD5签名：" + signMd5Boolean.booleanValue());
        return signMd5Boolean.booleanValue();
    }

    @Override
    protected String responseSuccess() {
        log.debug("[猫王付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return  RESPONSE_PAY_MSG;
    }
}