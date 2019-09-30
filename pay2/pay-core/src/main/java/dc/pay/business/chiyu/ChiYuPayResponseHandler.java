package dc.pay.business.chiyu;

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
 * Dec 21, 2017
 */
@ResponsePayHandler("CHIYU")
public final class ChiYuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    //memberid	商户编号	100**
    private static final String memberid  ="memberid";
    //orderid	订单号	
    private static final String orderid  ="orderid";
    //amount	订单金额	1元以上
    private static final String amount  ="amount";
    //datetime	交易时间	
    private static final String datetime  ="datetime";
    //returncode	交易状态	“00” 为成功
    private static final String returncode  ="returncode";
    //reserved1	扩展返回1	
    private static final String reserved1  ="reserved1";
    //reserved2	扩展返回1	
    private static final String reserved2  ="reserved2";
    //reserved3	 扩展返回1	
    private static final String reserved3  ="reserved3";
    
    private static final String key  ="key";

    private static final String sign = "sign";
    
    private static final String RESPONSE_PAY_MSG = "OK";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(memberid);
        String ordernumberR = API_RESPONSE_PARAMS.get(orderid);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[驰誉]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
    	//1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
    	//2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
		StringBuffer signSrc= new StringBuffer();
		signSrc.append(amount+"=>").append(api_response_params.get(amount)).append("&");
		signSrc.append(datetime+"=>").append(api_response_params.get(datetime)).append("&");
		signSrc.append(memberid+"=>").append(api_response_params.get(memberid)).append("&");
		signSrc.append(orderid+"=>").append(api_response_params.get(orderid)).append("&");
		signSrc.append(returncode+"=>").append(api_response_params.get(returncode)).append("&");
		if (null != api_response_params.get(reserved1) && StringUtils.isNotBlank(api_response_params.get(reserved1))) {
			signSrc.append(reserved1+"=>").append(api_response_params.get(reserved1)).append("&");
		}
		if (null != api_response_params.get(reserved2) && StringUtils.isNotBlank(api_response_params.get(reserved2))) {
			signSrc.append(reserved2+"=>").append(api_response_params.get(reserved2)).append("&");
		}
		if (null != api_response_params.get(reserved3) && StringUtils.isNotBlank(api_response_params.get(reserved3))) {
			signSrc.append(reserved3+"=>").append(api_response_params.get(reserved3)).append("&");
		}
    	signSrc.append(key+"=").append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[驰誉]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String pay_amount) throws PayException {
        boolean result = false;
        //“00” 为成功
        String payStatusCode = api_response_params.get(returncode);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(amount));
        //amount数据库存入的是分 	第三方返回的responseAmount是元
        boolean checkAmount = pay_amount.equalsIgnoreCase(responseAmount);
        //2代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("00")) {
            result = true;
        } else {
            log.error("[驰誉]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + pay_amount);
        }
        log.debug("[驰誉]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + pay_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：00");
        return result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("[驰誉]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[驰誉]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}