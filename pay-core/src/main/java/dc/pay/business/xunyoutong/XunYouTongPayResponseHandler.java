package dc.pay.business.xunyoutong;

import java.util.List;
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
import dc.pay.utils.MapUtils;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Apr 28, 2018
 */
@ResponsePayHandler("XUNYOUTONG")
public final class XunYouTongPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

	//字段名称					类型				可否为空			最大长度			说明
	//payKey				String				否				32				商户支付Key
	//productName			String				否				200				支付产品名称
	//productType			String				否				8				产品类型,如：QQ钱包T0扫码:70000103,其他详见编码表
	//orderPrice			Float				否				12				订单金额，单位：元,保留小数点后两位
	//orderTime				String				否				14				下单时间，格式(yyyyMMddHHmmss)
	//outTradeNo			String				否				30				商户支付订单号（长度30以内）
	//tradeStatus			String				否				20				订单状态,交易成功:SUCCESS;交易完成:FINISH;交易失败:FAILED;等待支付:WAITING_PAYMENT
	//trxNo					String				否				50				交易流水号
	//successTime			String				否				14				成功时间，格式(yyyyMMddHHmmss)
	//remark				String				是				200				备注
	//sign					String				否				50				MD5大写签名
	private static final String payKey	  	="payKey";
//	private static final String productName	  ="productName";
//	private static final String productType	  ="productType";
	private static final String orderPrice	  ="orderPrice";
//	private static final String orderTime	  ="orderTime";
	private static final String outTradeNo	  ="outTradeNo";
	private static final String tradeStatus	  ="tradeStatus";
//	private static final String trxNo	  	="trxNo";
//	private static final String successTime	  ="successTime";
//	private static final String remark	  ="remark";
    private static final String signature  ="sign";

    private static final String paySecret  ="paySecret";
    
    private static final String RESPONSE_PAY_MSG = "SUCCESS";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(payKey);
        String ordernumberR = API_RESPONSE_PARAMS.get(outTradeNo);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[迅游通]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
//		 平台默认使用MD5签名方式进行数据验签，保证数据完整性。请求方在请求数据是将请求数据按照键值对的,方式通过'&'符号进行拼接，获取到签名源文。将源文进行MD5(大写)签名后，作为sign字段放在请求报文中。源文拼接方式为：按照参数名称进行ASCII编码排序，如果参数值为空，则不参与签名
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!signature.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
        		signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
			}
        }
        signSrc.append(paySecret+"=" + channelWrapper.getAPI_KEY());
		String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[迅游通]-[响应支付]-2.生成加密URL签名完成："+ JSON.toJSONString(signMd5));
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean result = false;
        //tradeStatus	String	否		20		订单状态,交易成功:SUCCESS;交易完成:FINISH;交易失败:FAILED;等待支付:WAITING_PAYMENT
        String payStatusCode = api_response_params.get(tradeStatus);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(orderPrice));
        //orderPrice	Float	否		12		订单金额，单位：元,保留小数点后两位
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("SUCCESS")) {
            result = true;
        } else {
            log.error("[迅游通]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[迅游通]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：SUCCESS");
        return result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[迅游通]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[迅游通]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}