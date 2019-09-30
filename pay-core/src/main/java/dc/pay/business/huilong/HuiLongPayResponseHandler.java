package dc.pay.business.huilong;

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
 * Jun 8, 2018
 */
@ResponsePayHandler("HUILONG")
public final class HuiLongPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

	//字段名				变量名				类型				说明				可空
	//响应码				status				string				200表示成功,其它表示失败 	N
	//应答信息				orderStatus			string				SUCCESS成功，			N
	//支付金额				orderAmount			string								N
	//支付方式				payType				string								N
	//支付时间				payoverTime			String								N
	//订单号				orderNo				string								N
	//验签				sign				string								N
	private static final String status					="status";
	private static final String orderStatus				="orderStatus";
	private static final String orderAmount				="orderAmount";
	private static final String payType					="payType";
	private static final String payoverTime				="payoverTime";
	private static final String orderNo					="orderNo";
	
	private static final String merSecret				="merSecret";

    //signature	数据签名	32	是	　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "SUCCESS";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
//        String partnerR = API_RESPONSE_PARAMS.get(merchno);
        String ordernumberR = API_RESPONSE_PARAMS.get(orderNo);
//        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[汇隆]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        StringBuilder signStr = new StringBuilder();
        signStr.append(status+"=").append(api_response_params.get(status)).append("&");
        signStr.append(payType+"=").append(api_response_params.get(payType)).append("&");
        signStr.append(orderNo+"=").append(api_response_params.get(orderNo)).append("&");
        signStr.append(orderStatus+"=").append(api_response_params.get(orderStatus)).append("&");
        signStr.append(orderAmount+"=").append(api_response_params.get(orderAmount)).append("&");
        signStr.append(payoverTime+"=").append(api_response_params.get(payoverTime)).append("&");
        signStr.append(merSecret+"=").append(channelWrapper.getAPI_KEY());
        String paramsStr =signStr.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[汇隆]-[响应支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //orderStatus	string	SUCCESS成功，FAILED失败，WAITTING_PAYMENT等待支付
        String payStatusCode = api_response_params.get(orderStatus);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(orderAmount));
        //db_amount数据库存入的是分 	第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("SUCCESS")) {
            my_result = true;
        } else {
            log.error("[汇隆]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[汇隆]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：SUCCESS");
        return my_result;
    }


    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[汇隆]-[响应支付]-4.验证MD5签名：" + my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[汇隆]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}