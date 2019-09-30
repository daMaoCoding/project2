package dc.pay.business.fengshang;

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
 * May 30, 2018
 */
@ResponsePayHandler("FENGSHANG")
public final class FengShangPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

	//中文域名				对应DTD元素				类型						请求			应答			说明
	//商户编号				merchantno				VARCHAR(11)				M						扫描支付平台为商户分配的唯一编号
	//应用编号				appno					VARCHAR(11)				M						商户应用编号
	//商户订单号			merchantorder			VARCHAR(64)				M						商户订单号
	//扫码支付平台交易流水号	transid					VARCHAR(32)				M						扫码支付平台交易流水号
	//交易金额				amount					VARCHAR(20)				M						单位为分
	//币种				currency				VARCHAR(3)				M						
	//商品描述				remark					VARCHAR(30)				M						商品描述信息，商户传来的订单备注参数
	//支付方式				paytype					VARCHAR(11)				M						
	//支付结果				result					VARCHAR（1）				M						判断支付结果及交易状态。1-正在支付2-支付成功3-支付失败
	//用户编号				customerno				VARCHAR(32)				O						用户在合作方的编号
//	private static final String merchantno				="merchantno";
//	private static final String appno					="appno";
	private static final String merchantorder			="merchantorder";
//	private static final String transid					="transid";
	private static final String amount					="amount";
//	private static final String currency				="currency";
//	private static final String remark					="remark";
//	private static final String paytype					="paytype";
	private static final String result					="result";
//	private static final String customerno				="customerno";
	
	//户编号merchantid、商户订单号merchantorder、订单状态result、金额amount判断是否
	private static final String merchantid				="merchantid";
	private static final String transdata				="transdata";

    //signature	数据签名	32	是	　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "000000";

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
        String partnerR = parseObject.getString(merchantid);
        String ordernumberR = parseObject.getString(merchantorder);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[风上]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        String data = HandlerUtil.UrlDecode(API_RESPONSE_PARAMS.get(transdata));
        boolean result = RsaUtil.validateSignByPublicKey(data, channelWrapper.getAPI_PUBLIC_KEY(), API_RESPONSE_PARAMS.get(signature));
        log.debug("[风上]-[响应支付]-2.生成加密URL签名完成：" + JSON.toJSONString(result) );
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
        //判断支付结果及交易状态。	        1-正在支付	        2-支付成功	        3-支付失败
        String payStatusCode = parseObject.getString(result);
        String responseAmount = parseObject.getString(amount);
        //db_amount数据库存入的是分 	第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("2")) {
        	myresult = true;
        } else {
            log.error("[风上]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[风上]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + myresult + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：2");
        return myresult;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
    	Boolean signMd5Boolean =   Boolean.valueOf(signMd5);
//        boolean result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[风上]-[响应支付]-4.验证MD5签名：" + signMd5Boolean.booleanValue());
        return signMd5Boolean.booleanValue();
    }

    @Override
    protected String responseSuccess() {
        log.debug("[风上]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}