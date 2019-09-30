package dc.pay.business.sssfu;

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
@ResponsePayHandler("SSSFU")
public final class SssFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

//    参数名称			类型				开发含义				参数说明				必填				签名顺序
//    version			String			系统版本号,直接填”1.0”即可	1.0				是					1
//    agentId			String			商户ID				商户ID				是					2
//    agentOrderId		String			商户订单号			商户订单号			是					3
//    jnetOrderId		String			我方平台订单号	我方订单号					是					4
//    payAmt			String			正常订单金额	实际金额，但订单失败和处理中 交易金额为0.00	是			5
//    payResult			String			订单状态	订单结果: SUCCESS表示成功, FAIL表示失败, TREATMENT表示处理中	是	6
//    payMessage		String			订单结果	支付结果信息	否	
//    sign				String			用竖线相隔,参数拼接后用md5加密	签名串拼接规则：1|2|3|4|5|6|key
//    数字表示实际内容，参数间用竖线分隔,按照顺序拼接字符串
//    Key: 联系商务
//    加密方式:MD5,比如:
//    md5(version+”|”+agentId+”|”+agentOrderId+” |”+jnetOrderId+” |”+payAmt+” |”+payResult+”|”+key)	是	

    private static final String version                    ="version";
    private static final String agentId                    ="agentId";
    private static final String agentOrderId               ="agentOrderId";
    private static final String jnetOrderId                ="jnetOrderId";
    private static final String payAmt                     ="payAmt";
    private static final String payResult                  ="payResult";
    private static final String payMessage                 ="payMessage";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("OK");

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(agentId);
        String ordernumberR = API_RESPONSE_PARAMS.get(agentOrderId);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[sss支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
    	//签名规则
        StringBuilder signSrc = new StringBuilder();
        signSrc.append(api_response_params.get("version")).append("|");
        signSrc.append(api_response_params.get("agentId")).append("|");
        signSrc.append(api_response_params.get("agentOrderId")).append("|");
        signSrc.append(api_response_params.get("jnetOrderId")).append("|");
        signSrc.append(api_response_params.get("payAmt")).append("|");
        signSrc.append(api_response_params.get("payResult")).append("|");
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr =signSrc.toString();
        String signMD5 = HandlerUtil.getPhpMD5(paramsStr);
        log.debug("[sss支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return signMD5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //payResult          交易状态         是            “SUCCESS” 为成功
        String payStatusCode = api_response_params.get(payResult);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(payAmt));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //实际支付金额  
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount&&payStatusCode.equalsIgnoreCase("SUCCESS")) {
            my_result = true;
        } else {
            log.error("[sss支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[sss支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：SUCCESS");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[sss支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[sss支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}