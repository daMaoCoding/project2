package dc.pay.business.yifusecond;

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
 * Mar 9, 2018
 */
@ResponsePayHandler("YIFUSECOND")
public final class YiFuSecondPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //商户ID		partner		Y		商户id,由易付2分配
    private static final String partner  ="partner";
    //商户订单号	ordernumber	y		上行过程中商户系统传入的ordernumber
    private static final String ordernumber  ="ordernumber";
    //订单结果		orderstatus	Y		1:支付成功，非1为支付失败
    private static final String orderstatus  ="orderstatus";
    //订单金额		paymoney	Y		单位元（人民币）
    private static final String paymoney  ="paymoney";
    //易付2订单号	sysnumber	N		此次交易中易付2接口系统内的订单ID
    private static final String sysnumber  ="sysnumber";
    //备注信息		attach		N		备注信息，上行中attach原样返回
    private static final String attach  ="attach";
    //MD5签名		sign		N		32位小写MD5签名值，GB2312编码
    private static final String sign  ="sign";
    
    private static final String RESPONSE_PAY_MSG = "ok";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(partner);
        String ordernumberR = API_RESPONSE_PARAMS.get(ordernumber);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[易付2]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }
    
    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
    	//1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
    	//2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
    	//partner={}&ordernumber={}&orderstatus={}&paymoney={}key
		StringBuffer signSrc= new StringBuffer();
		signSrc.append(partner+"=").append(api_response_params.get(partner)).append("&");
		signSrc.append(ordernumber+"=").append(api_response_params.get(ordernumber)).append("&");
		signSrc.append(orderstatus+"=").append(api_response_params.get(orderstatus)).append("&");
		signSrc.append(paymoney+"=").append(api_response_params.get(paymoney));
		signSrc.append(channelWrapper.getAPI_KEY());
		String paramsStr = signSrc.toString();
		String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[易付2]-[请求支付]-2.生成加密URL签名完成,值："+ JSON.toJSONString(signMd5));
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean result = false;
        //1:支付成功，非1为支付失败
        String payStatusCode = api_response_params.get(orderstatus);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(paymoney));
        //amount数据库存入的是分 	第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("1")) {
            result = true;
        } else {
            log.error("[易付2]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[易付2]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("[易付2]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[易付2]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}