package dc.pay.business.kuaikazhifu2;

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
 * @author mikey
 * Jun 4, 2019
 */
@ResponsePayHandler("KUAIKAZHIFU2")
public final class KuaiKaZhiFu2PayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());
/*
    	参数名称		参数含义		参与签名	参数说明
    	memberid		商户编号		是	
    	orderid			订单号			是	
    	amount			订单金额		是	
    	transaction_id	交易流水号		是	
    	datetime		交易时间		是	
    	returncode		交易状态		是			“00” 为成功
    	attach			扩展返回		否			商户附加数据返回
    	sign			签名			否			请看验证签名字段格式
*/
    private static final String memberid		= "memberid";			//商户编号	
    private static final String orderid			= "orderid";			//订单号		
    private static final String amount			= "amount";				//订单金额	
    private static final String transaction_id	= "transaction_id";		//交易流水号	
    private static final String datetime		= "datetime";			//交易时间	
    private static final String returncode		= "returncode";			//交易状态	
    private static final String sign			= "sign";				//签名		
    private static final String RESPONSE_PAY_MSG = "OK";
    private static final String key        		=	"key";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(memberid);
        String ordernumberR = API_RESPONSE_PARAMS.get(orderid);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[快卡支付2]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        StringBuilder signSrc = new StringBuilder();
        signSrc.append(amount+"=").append(api_response_params.get(amount)).append("&");
        signSrc.append(datetime+"=").append(api_response_params.get(datetime)).append("&");
        signSrc.append(memberid+"=").append(api_response_params.get(memberid)).append("&");
        signSrc.append(orderid+"=").append(api_response_params.get(orderid)).append("&");
        signSrc.append(returncode+"=").append(api_response_params.get(returncode)).append("&");
        signSrc.append(transaction_id+"=").append(api_response_params.get(transaction_id)).append("&");
        signSrc.append(key +"="+ channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[快卡支付2]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //returncode		交易状态		是			“00” 为成功
        String payStatusCode = api_response_params.get(returncode);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(amount));
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差
        if (checkAmount && payStatusCode.equalsIgnoreCase("00")) {
            my_result = true;
        } else {
            log.error("[快卡支付2]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[快卡支付2]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：00");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("[快卡支付2]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[快卡支付2]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}