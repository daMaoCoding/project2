package dc.pay.business.xinma;

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

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Dec 12, 2017
 */
@ResponsePayHandler("XINMA")
public final class XinMaPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());
    
	//resultCode			返回系统编码(00表示成功,其余表示失败). 此字段是通信标识，非交易标识，交易是否成功需要查看resCode来判断
    private static final String resultCode  ="resultCode";
	//resultDesc		STR	返回系统描述
    private static final String resultDesc  ="resultDesc";
	//resCode			STR	返回业务编码(00表示成功,其余表示失败)
    private static final String resCode  ="resCode";
	//resDesc			STR	返回业务描述
    private static final String resDesc  ="resDesc";
	//nonceStr		Str	随机字符串，不长于32位
    private static final String nonceStr  ="nonceStr";
	//sign			STR	签名,详见签名算法
    private static final String sign  ="sign";
	//
	//branchId		STR	服务方平台分配的商户号
    private static final String branchId  ="branchId";
	//createTime		STR	订单时间，接入方发起订单的时间戳
    private static final String createTime  ="createTime";
	//orderAmt		INT	订单总金额(单位:分)
    private static final String orderAmt  ="orderAmt";
	//orderNo			STR	服务方系统订单流水号。下单支付的接口成功提交后，服务方系统的订单号会返回给接入方，后续也可作为账务核对的标准。
    private static final String orderNo  ="orderNo";
	//outTradeNo		STR	商户接入方系统内部订单号，要求32个字符内，建议是数字、大小写字母组合，且在同一个商户号下唯一
    private static final String outTradeNo  ="outTradeNo";
	//productDesc		STR	订单描述
    private static final String productDesc  ="productDesc";
	//payType			STR	支付方式
    private static final String payType  ="payType";
//	//attachContent	STR	该字段在下单交易中由商户接入方提交，主要用于商户携带订单的自定义数据，此为原样返回。如请求时为空则此处不返回
//    private static final String attachContent  ="attachContent";
	//status			STR	支付状态00:未支付 01:支付中 02:已支付 03:支付失败 04:取消  05:退款中 06:已退款 07:退款失败	注意：通知不一定是订单支付成功，也可能是支付失败或者其他，接入方一定要判断这个字段值是02的时候才代表订单成功了。
    private static final String status  ="status";
	
	private static final String key  ="key";
	
	//商户接入方在收到此通知报文后返回以下参数(json格式),否则服务方系统会持续发送通知一段时间
    private static final String RESPONSE_PAY_MSG = "{\"resCode\":\"00\",\"resDesc\":\"成功\"}";


    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(branchId);
        String ordernumberR = API_RESPONSE_PARAMS.get(outTradeNo);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[新码]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }

    /**
     * 注：请求的JSON数据中有value为空的，则不参与SIGN的计算，即stringA中不出现这种空值的字段。
     * 
     * @param api_response_params
     * @param api_key
     * @return
     * @throws PayException
     * @author andrew
     * Dec 12, 2017
     */
    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
		StringBuffer signSrc= new StringBuffer();
		signSrc.append(branchId+"=").append(api_response_params.get(branchId)).append("&");
		signSrc.append(createTime+"=").append(api_response_params.get(createTime)).append("&");
		signSrc.append(nonceStr+"=").append(api_response_params.get(nonceStr)).append("&");
		signSrc.append(orderAmt+"=").append(api_response_params.get(orderAmt)).append("&");
		signSrc.append(orderNo+"=").append(api_response_params.get(orderNo)).append("&");
		signSrc.append(outTradeNo+"=").append(api_response_params.get(outTradeNo)).append("&");
		signSrc.append(payType+"=").append(api_response_params.get(payType)).append("&");
		signSrc.append(productDesc+"=").append(api_response_params.get(productDesc)).append("&");
		signSrc.append(resCode+"=").append(api_response_params.get(resCode)).append("&");
		signSrc.append(resDesc+"=").append(api_response_params.get(resDesc)).append("&");
		signSrc.append(resultCode+"=").append(api_response_params.get(resultCode)).append("&");
		signSrc.append(resultDesc+"=").append(api_response_params.get(resultDesc)).append("&");
		signSrc.append(status+"=").append(api_response_params.get(status)).append("&");
    	signSrc.append(key+"=").append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[新码]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5)+"，参数："+JSON.toJSONString(paramsStr));
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amount) throws PayException {
        boolean result = false;
        String payStatusCode = api_response_params.get(status);
        String responseAmount = api_response_params.get(orderAmt);
        //amount数据库存入的是分 	第三方返回的responseAmount是分
        boolean checkAmount = amount.equalsIgnoreCase(responseAmount);
        //支付状态00:未支付 01:支付中 02:已支付 03:支付失败 04:取消  05:退款中 06:已退款 07:退款失败
        if (checkAmount && payStatusCode.equalsIgnoreCase("02")) {
            result = true;
        } else {
            log.error("[新码]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + amount);
        }
        log.debug("[新码]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：02");
        return result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("[新码]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
		JSONObject reqData = new JSONObject();
		reqData.put(resCode, "00");
		reqData.put(resDesc, "成功");
        log.debug("[新码]-[响应支付]-5.第三方支付确认收到消息返回内容：" + reqData.toJSONString());
        return reqData.toJSONString();
    }
}