package dc.pay.business.hefu;

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
 * Mar 31, 2018
 */
@ResponsePayHandler("HEFU")
public final class HeFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

	//参数名			参数				可空			加入签名		说明
	//外部订单号		outOrderNo			N			Y			商户系统的订单编号
	//交易金额			tradeAmount			N			Y			商户 商品价格（元）两位小数
	//商户code		shopCode			N			Y			点击头像，查看code
	//随机字符串		nonStr				N			Y			随机字符串
	//支付状态			msg					N			Y			成功：SUCCESS   失败：FAIL
	private static final String outOrderNo	      ="outOrderNo";
	private static final String tradeAmount	      ="tradeAmount";
	private static final String shopCode	      ="shopCode";
	private static final String nonStr	      ="nonStr";
	private static final String msg		      ="msg";
	
	 //signature	数据签名	32	是	　
    private static final String signature  ="sign";
    
    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(shopCode );
        String ordernumberR = API_RESPONSE_PARAMS.get(outOrderNo);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[合付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
		StringBuffer signSrc= new StringBuffer();
		signSrc.append(msg+"=").append(api_response_params.get(msg)).append("&");
		signSrc.append(nonStr+"=").append(api_response_params.get(nonStr)).append("&");
		signSrc.append(outOrderNo+"=").append(api_response_params.get(outOrderNo)).append("&");
		signSrc.append(shopCode+"=").append(api_response_params.get(shopCode)).append("&");
		signSrc.append(tradeAmount+"=").append(api_response_params.get(tradeAmount)).append("&");
		signSrc.append("key=").append(channelWrapper.getAPI_KEY());
		String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[合付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean result = false;
        //支付状态			msg					N			Y			成功：SUCCESS   失败：FAIL
        String payStatusCode = api_response_params.get(msg);
        //交易金额			tradeAmount			N			Y			商户 商品价格（元）两位小数
        String responseAmount = HandlerUtil.getFen(api_response_params.get(tradeAmount));
        //db_amount数据库存入的是分 	第三方返回的responseAmount是元
        boolean checkAmount = true;
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("SUCCESS")) {
            result = true;
        } else {
            log.error("[合付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[合付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：SUCCESS");
        return result;
    }


    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[合付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[合付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}