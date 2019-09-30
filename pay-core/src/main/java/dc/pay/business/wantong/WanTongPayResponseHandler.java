package dc.pay.business.wantong;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RsaUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Jun 16, 2018
 */
@ResponsePayHandler("WANTONG")
public final class WanTongPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

	//中文域名			对应DTD元素			类型				请求			应答			说明
	//商户订单号		order_no			String			M						商户订单号
	//商品名称			product_name		String			M						支付时上传的商品名称
	//交易金额			order_amount		Number			M						单位为元
	//商品ID			product_code		String			M						支付时上传的商品ID
	//支付方式			pay_type			String			M						微信: ‘weixin’
	//支付结果			payment				String			M						返回‘支付成功’
	//用户编号			user_no				String			M						支付时上传用户在合作方的编号
	//订单时间			order_time			String			M						支付时上传的订单时间
	private static final String order_no					="order_no";
//	private static final String product_name				="product_name";
	private static final String order_amount				="order_amount";
//	private static final String product_code				="product_code";
//	private static final String pay_type					="pay_type";
	private static final String payment						="payment";
//	private static final String user_no						="user_no";
//	private static final String order_time					="order_time";

	private static final String transdata					="transdata";
	
    //signature	数据签名	32	是	　
    private static final String signature					="sign";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String data = HandlerUtil.UrlDecode(API_RESPONSE_PARAMS.get(transdata));
        JSONObject parseObject = null;
        try {
        	parseObject = JSON.parseObject(data);
		} catch (Exception e) {
			throw new PayException(SERVER_MSG.REQUEST_PAY_CHANNEL_RESULT_ERROR);
		}
//        String partnerR = parseObject.getString(merchantid);
        String ordernumberR = parseObject.getString(order_no);
//        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[万通]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        String data = HandlerUtil.UrlDecode(API_RESPONSE_PARAMS.get(transdata));
        boolean result = RsaUtil.validateSignByPublicKey(data, channelWrapper.getAPI_PUBLIC_KEY(), HandlerUtil.UrlDecode(API_RESPONSE_PARAMS.get(signature)), "SHA256withRSA");
        log.debug("[万通]-[响应支付]-2.生成加密URL签名完成：" + JSON.toJSONString(result) );
        return String.valueOf(result);
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        String data = HandlerUtil.UrlDecode(API_RESPONSE_PARAMS.get(transdata));
        JSONObject parseObject = null;
        try {
        	parseObject = JSON.parseObject(data);
		} catch (Exception e) {
			throw new PayException(SERVER_MSG.REQUEST_PAY_CHANNEL_RESULT_ERROR);
		}
        boolean myresult = false;
        //支付结果	payment	String	M		返回‘支付成功’
        String payStatusCode = parseObject.getString(payment);
        String responseAmount = HandlerUtil.getFen(parseObject.getString(order_amount));
        //db_amount数据库存入的是分 	第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("支付成功")) {
        	myresult = true;
        } else {
            log.error("[万通]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[万通]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + myresult + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：支付成功");
        return myresult;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
    	Boolean signMd5Boolean =   Boolean.valueOf(signMd5);
//        boolean result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[万通]-[响应支付]-4.验证MD5签名：" + signMd5Boolean.booleanValue());
        return signMd5Boolean.booleanValue();
    }

    @Override
    protected String responseSuccess() {
        log.debug("[万通]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}