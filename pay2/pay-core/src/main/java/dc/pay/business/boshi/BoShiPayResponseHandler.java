package dc.pay.business.boshi;

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
 * May 16, 2018
 */
@ResponsePayHandler("BOSHI")
public final class BoShiPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());


	//参数					说明
	//MerchantCode			商户号 由博士分配
	//OrderId				下单过程中商户系统传入的OrderId
	//OrderDate				请求时间，时间戳，精确到秒，长度10位
	//Status				1：成功 ，0：失败
	//Amount				订单实际支付金额，单位元
	//OutTradeNo			此次订单过程中博士接口系统内的订单号
	//BankCode				支付类型或银行类型，具体请参考附录
	//Time					发起通知时的时间戳
	//Remark				备注信息，下单中Remark原样返回
	//Sign					32位大写MD5签名值
	private static final String MerchantCode	="MerchantCode";
	private static final String OrderId			="OrderId";
	private static final String OrderDate		="OrderDate";
	private static final String Status			="Status";
	private static final String Amount			="Amount";
	private static final String OutTradeNo		="OutTradeNo";
	private static final String BankCode		="BankCode";
	private static final String Time			="Time";
	private static final String Remark			="Remark";
	
	private static final String TokenKey		="TokenKey";
  
    //signature	数据签名	32	是	　
    private static final String signature  ="Sign";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(MerchantCode);
        String ordernumberR = API_RESPONSE_PARAMS.get(OrderId);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[博士]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
		//1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
		//2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
		StringBuffer signSrc= new StringBuffer();
		signSrc.append(MerchantCode+"=").append("["+api_response_params.get(MerchantCode)+"]");
		signSrc.append(OrderId+"=").append("["+api_response_params.get(OrderId)+"]");
		signSrc.append(OutTradeNo+"=").append("["+api_response_params.get(OutTradeNo)+"]");
		signSrc.append(Amount+"=").append("["+api_response_params.get(Amount)+"]");
		signSrc.append(OrderDate+"=").append("["+api_response_params.get(OrderDate)+"]");
		signSrc.append(BankCode+"=").append("["+api_response_params.get(BankCode)+"]");
		signSrc.append(Remark+"=").append("["+api_response_params.get(Remark)+"]");
		signSrc.append(Status+"=").append("["+api_response_params.get(Status)+"]");
		signSrc.append(Time+"=").append("["+api_response_params.get(Time)+"]");
		signSrc.append(TokenKey+"=").append("["+api_key+"]");
		String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[博士]-[响应支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean result = false;
        //status		1：成功 ，0：失败
        String payStatusCode = api_response_params.get(Status);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(Amount));
        //db_amount数据库存入的是分 	第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("1")) {
            result = true;
        } else {
            log.error("[博士]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[博士]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[博士]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[博士]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}