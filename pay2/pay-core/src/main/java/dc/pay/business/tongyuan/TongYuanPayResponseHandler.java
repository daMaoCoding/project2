package dc.pay.business.tongyuan;

import java.math.BigDecimal;
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
 * Mar 12, 2018
 */
@ResponsePayHandler("TONGYUAN")
public final class TongYuanPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //订单状态		status		int(1)		1:成功，其他失败
    private static final String status  ="status";
    //商户编号		customerid	int(8)	
    private static final String customerid  ="customerid";
    //平台订单号	sdpayno		varchar(20)	
    private static final String sdpayno  ="sdpayno";
    //商户订单号	sdorderno	varchar(20)	
    private static final String sdorderno  ="sdorderno";
    //交易金额		total_fee	decimal(10,2)	最多两位小数
    private static final String total_fee  ="total_fee";
    //支付类型		paytype		varchar(20)	
    private static final String paytype  ="paytype";
    //订单备注说明	remark		varchar(50)	原样返回
    private static final String remark  ="remark";
    //md5验证签名串	sign		varchar(32)	参照签名方法
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(customerid);
        String ordernumberR = API_RESPONSE_PARAMS.get(sdorderno);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[通源]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
		//1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
		//2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
    	//{value}要替换成接收到的值，{apikey}要替换成平台分配的接入密钥，可在商户后台获取
    	//customerid={value}&status={value}&sdpayno={value}&sdorderno={value}&total_fee={value}&paytype={value}&{apikey}
		StringBuffer signSrc= new StringBuffer();
		signSrc.append(customerid+"=").append(api_response_params.get(customerid)).append("&");
		signSrc.append(status+"=").append(api_response_params.get(status)).append("&");
		signSrc.append(sdpayno+"=").append(api_response_params.get(sdpayno)).append("&");
		signSrc.append(sdorderno+"=").append(api_response_params.get(sdorderno)).append("&");
		signSrc.append(total_fee+"=").append(api_response_params.get(total_fee)).append("&");
		signSrc.append(paytype+"=").append(api_response_params.get(paytype)).append("&");
		signSrc.append(channelWrapper.getAPI_KEY());
		String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[通源]-[请求支付]-2.生成加密URL签名完成："+ JSON.toJSONString(signMd5));
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean result = false;
        //1:成功，其他失败
        String payStatusCode = api_response_params.get(status);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(total_fee));
        //db_amount数据库存入的是分 	第三方返回的responseAmount是元
        boolean checkAmount;
        if (channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("WEBWAPAPP_WX_SM")) {
        	double responseAmountDouble = Double.parseDouble(responseAmount);
        	//第三方回复：付款金额等于提交金额+0.00 ~ 0.1		业主同意：@132 主 主管 所以，在回调的时候，针对微信扫码转账，我会做这个多余支付0.1元的判断：偏差在0.1元这个范围内的，我都会认为是正确的付款金额
        	double db_amountDouble1 = new BigDecimal(Double.parseDouble(db_amount)).add(new BigDecimal("0")).doubleValue();
        	double db_amountDouble2 = new BigDecimal(Double.parseDouble(db_amount)).add(new BigDecimal("10")).doubleValue();
        	//compare(double d1, double d2)如果 d1 在数字上等于 d2，则返回 0；如果 d1 在数字上小于 d2，则返回小于 0 的值；如果 d1 在数字上大于 d2，则返回大于 0 的值。
        	checkAmount = (!(Double.compare(responseAmountDouble, db_amountDouble1) < 0) && !(Double.compare(responseAmountDouble, db_amountDouble2) > 0)) ? true : false;;
		}else {
			checkAmount = db_amount.equalsIgnoreCase(responseAmount);
		}
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("1")) {
            result = true;
        } else {
            log.error("[通源]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[通源]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[通源]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[通源]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}