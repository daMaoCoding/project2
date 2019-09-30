package dc.pay.business.hengfutong;

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
 * @author sunny
 * Dec 14, 2018
 */
@ResponsePayHandler("HENGFUTONG")
public final class HengFuTongResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

//    参数名					说明				类型(长度）	备注
//    merchantId			商户id			String(20）	成功才返回
//    result				下单结果			String(20）	true成功，false失败
//    code					返回状态			String(20）	返回状态：00 交易成功；false交易失败
//    corp_flow_no			订单号			String(255）	下游商户请求的订单号
//    totalAmount			交易金额			Decimal (11,2)	以元为单位的整数，下游商户需判断金额是否相等
//    sign					签名				String(255）	MD5(merchantId+"pay" +corp_flow_no+商户秘钥)
//    desc					订单描述			String(255）	



    private static final String merchantId                 ="merchantId";
    private static final String result                     ="result";
    private static final String code               	       ="code";
    private static final String corp_flow_no               ="corp_flow_no";
    private static final String totalAmount                ="totalAmount";
    private static final String desc                       ="desc";

    //signature    数据签名    32    是    　
    private static final String signature  ="sign";
    
    private static final String key        ="key";

    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("ok");

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(merchantId);
        String ordernumberR = API_RESPONSE_PARAMS.get(corp_flow_no);
        if (StringUtils.isBlank(ordernumberR)||StringUtils.isBlank(partnerR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[恒付通支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
    	//签名规则
    	StringBuilder signSrc = new StringBuilder();
    	signSrc.append(api_response_params.get(merchantId));
    	signSrc.append("pay");
    	signSrc.append(api_response_params.get(corp_flow_no));
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr =signSrc.toString();
        String signMD5 = HandlerUtil.getPhpMD5(paramsStr);
        log.debug("[恒付通支付]-[响应支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return signMD5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //transStatus          交易状态         是            “2” 为成功
        String payStatusCode = api_response_params.get(result);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(totalAmount));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        //实际支付金额
        boolean checkAmount=db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount&&payStatusCode.equalsIgnoreCase("true")) {
            my_result = true;
        } else {
            log.error("[恒付通支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[恒付通支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：true");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[恒付通支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[恒付通支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}