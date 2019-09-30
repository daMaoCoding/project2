package dc.pay.business.xinbei;

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
 * Apr 17, 2018
 */
@ResponsePayHandler("XINBEI")
public final class XinBeiPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

	//参数				参数名称			长度			参数说明							是否为空
	//Version			网关版本号			4			使用网关的版本号						否
	//MerchantCode	商户编码			6			新贝平台商户编码						否
	//OrderId			商户订单号			30			商户自己业务逻辑的订单号					否
	//OrderDate		订单交易时间		14			商户产生订单时的交易时间，格式如：20130102030405		否
	//TradeIp			交易IP			19			向商户发起通知的IP地址						否
	//SerialNo		交易流水号			30			新贝平台交易流水号						否
	//Amount			交易金额			7			交易流程中真正发生的金额					否
	//PayCode			交易类型编码		6			接入平台时对应的接口编码					否
	//State			处理结果			4			处理后的结果编码						否
	//Message			处理结果信息		256			处理后的结果信息						可空
	//FinishTime		交易完成时间		14			平台交易处理完成时间，格式如：20130102030405			否
	//SignValue		加密字符串			32			根据接口文档组合参数加密后的字段				否
	private static final String Version	  ="Version";
	private static final String MerchantCode  ="MerchantCode";
	private static final String OrderId	  ="OrderId";
	private static final String OrderDate	  ="OrderDate";
	private static final String TradeIp	  ="TradeIp";
	private static final String SerialNo	  ="SerialNo";
	private static final String Amount	  ="Amount";
	private static final String PayCode	  ="PayCode";
	private static final String State	  		="State";
//	private static final String Message	  ="Message";
	private static final String FinishTime	  ="FinishTime";
	private static final String TokenKey	  ="TokenKey";
    //signature	数据签名	32	是	　
    private static final String signature  ="SignValue";

    private static final String RESPONSE_PAY_MSG = "OK";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(MerchantCode);
        String ordernumberR = API_RESPONSE_PARAMS.get(OrderId);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[新贝]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
		//1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
		//2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
		StringBuffer signSrc= new StringBuffer();
		signSrc.append(Version+"=").append("["+api_response_params.get(Version)+"]");
		signSrc.append(MerchantCode+"=").append("["+api_response_params.get(MerchantCode)+"]");
		signSrc.append(OrderId+"=").append("["+api_response_params.get(OrderId)+"]");
		signSrc.append(OrderDate+"=").append("["+api_response_params.get(OrderDate)+"]");
		signSrc.append(TradeIp+"=").append("["+api_response_params.get(TradeIp)+"]");
		signSrc.append(SerialNo+"=").append("["+api_response_params.get(SerialNo)+"]");
		signSrc.append(Amount+"=").append("["+api_response_params.get(Amount)+"]");
		signSrc.append(PayCode+"=").append("["+api_response_params.get(PayCode)+"]");
		signSrc.append(State+"=").append("["+api_response_params.get(State)+"]");
		signSrc.append(FinishTime+"=").append("["+api_response_params.get(FinishTime)+"]");
		signSrc.append(TokenKey+"=").append("["+channelWrapper.getAPI_KEY()+"]");
		String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[新贝]-[响应支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean result = false;
        //8888	提交成功/充值成功
        String payStatusCode = api_response_params.get(State);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(Amount));
        //db_amount数据库存入的是分 	第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("8888")) {
            result = true;
        } else {
            log.error("[新贝]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[新贝]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：8888");
        return result;
    }


    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[新贝]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[新贝]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}