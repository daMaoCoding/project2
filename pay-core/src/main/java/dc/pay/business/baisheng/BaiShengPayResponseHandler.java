package dc.pay.business.baisheng;

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

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Feb 27, 2018
 */
@ResponsePayHandler("BAISHENG")
public final class BaiShengPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //signature	数据签名	32	是	　
    private static final String signature  ="Sign";
    
    //MerchantId	String	M	商户号
    private static final String MerchantId  ="MerchantId";
//    //PaymentNo	String	M	平台入款流水号
//    private static final String PaymentNo  ="PaymentNo";
    //OutPaymentNo	String	M	商户的入款流水号
    private static final String OutPaymentNo  ="OutPaymentNo";
    //PaymentAmount	String	C	入款金额，单位为分，1元 = 100
    private static final String PaymentAmount  ="PaymentAmount";
//    //PaymentFee	String	M	入款手续费，单位为分，1元 = 100
//    private static final String PaymentFee  ="PaymentFee";
    //PaymentState	String	C	入款状态（S-入款成功）
    private static final String PaymentState  ="PaymentState";
//    //PassbackParams	String	R	通知应答时，会按照商户在入款请求时上送的值进行原样返回
//    private static final String PassbackParams  ="PassbackParams";
//    //错误代码（Code=200时代表成功，其他为失败）
//    private static final String Code  ="Code";
//    //错误消息（当Code!=200时，Message返回错误描述原因，Code=200时，不返回任何数据。）
//    private static final String Message  ="Message";
    
    private static final String RESPONSE_PAY_MSG = "SUCCESS";
    
    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(MerchantId);
        String ordernumberR = API_RESPONSE_PARAMS.get(OutPaymentNo);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[百盛]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!signature.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
        		signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
			}
        }
        String paramsStr = signSrc.toString();
        //去除最后一个&符
        paramsStr = paramsStr.substring(0,paramsStr.length()-1);
        paramsStr = paramsStr+channelWrapper.getAPI_KEY();
        String pay_md5sign = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[百盛]-[响应支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }
    
    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean result = false;
        //入款状态（S-入款成功）
        String payStatusCode = api_response_params.get(PaymentState);
        String responseAmount = api_response_params.get(PaymentAmount);
        //amount数据库存入的是分 	第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("S")) {
            result = true;
        } else {
            log.error("[百盛]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[百盛]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：S");
        return result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[百盛]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[百盛]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}