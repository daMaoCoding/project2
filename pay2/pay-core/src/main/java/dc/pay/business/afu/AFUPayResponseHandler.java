package dc.pay.business.afu;

import com.alibaba.fastjson.JSON;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

 
@ResponsePayHandler("AFU")
public final class AFUPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    // payKey      | 商户支付Key                                  | String | 否    | 32   |
    private static final String payKey  ="payKey";
    // productName | 支付产品名称                                 | String | 否    | 200  |
    private static final String productName  ="productName";
    // outTradeNo  | 商户订单号                                   | String | 否    | 30   |
    private static final String outTradeNo  ="outTradeNo";
    // orderPrice  | 订单金额，单位：元<br>保留小数点后两位       | String | 否    | 12   |
    private static final String orderPrice  ="orderPrice";
    // productType | 产品类型<br>10000101 微信T1扫码支付<br>20000301 支付宝T1扫码支付 | String | 否    | 8    |
    private static final String productType  ="productType";
    // tradeStatus | 订单状态<br>SUCCESS 交易成功 <br>FINISH 交易完成 <br>FAILED 交易失败 <br>WAITING_PAYMENT 等待支付 | String | 否    | 20   |
    private static final String tradeStatus  ="tradeStatus";
    // successTime | 成功时间，格式<br>yyyyMMddHHmmss             | String | 否    | 15   |
    private static final String successTime  ="successTime";
    // orderTime   | 下单时间，格式<br>yyyyMMddHHmmss             | String | 否    | 15   |
    private static final String orderTime  ="orderTime";
    // trxNo       | 交易流水号                                   | String | 否    | 50   |
    private static final String trxNo  ="trxNo";
    // remark      | 备注                                         | String | 是    | 200  |
    private static final String remark  ="remark";
    // sign        | 签名                                         | String | 否    | 50   |
    private static final String sign  ="sign";
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
        log.debug("[A付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
    	//1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
    	//2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
		StringBuffer signSrc= new StringBuffer();
		signSrc.append(orderPrice+"=").append(api_response_params.get(orderPrice)).append("&");
		signSrc.append(orderTime+"=").append(api_response_params.get(orderTime)).append("&");
		signSrc.append(outTradeNo+"=").append(api_response_params.get(outTradeNo)).append("&");
		signSrc.append(payKey+"=").append(api_response_params.get(payKey)).append("&");
		signSrc.append(productName+"=").append(api_response_params.get(productName)).append("&");
		signSrc.append(productType+"=").append(api_response_params.get(productType)).append("&");
		if (null != api_response_params.get(remark) && StringUtils.isNotBlank(api_response_params.get(remark))) {
			signSrc.append(remark+"=").append(api_response_params.get(remark)).append("&");
		}
		signSrc.append(successTime+"=").append(api_response_params.get(successTime)).append("&");
		signSrc.append(tradeStatus+"=").append(api_response_params.get(tradeStatus)).append("&");
		signSrc.append(trxNo+"=").append(api_response_params.get(trxNo)).append("&");
        signSrc.append(paySecret+"=").append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[A付]-[请求支付]-2.生成加密URL签名完成，参数：" + JSON.toJSONString(paramsStr) +" ,值："+ JSON.toJSONString(signMd5));
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean result = false;
        //tradeStatus		交易状态	订单状态<br>SUCCESS 交易成功 <br>FINISH 交易完成 <br>FAILED 交易失败 <br>WAITING_PAYMENT 等待支付 
        String payStatusCode = api_response_params.get(tradeStatus);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(orderPrice));
        //amount数据库存入的是分 	第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //SUCCESS代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("SUCCESS")) {
            result = true;
        } else {
            log.error("[A付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[A付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：SUCCESS");
        return result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("[A付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[A付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}