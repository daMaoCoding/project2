package dc.pay.business.kuyou;

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
 * Jun 7, 2018
 */
@ResponsePayHandler("KUYOU")
public final class KuYouPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

	//参数名称					参数含义			参数说明					签名顺序
	//reCode				返回码			1：表示成功；0：表示失败。			1
	//trxMerchantNo			商户编号			商户在系统的唯一身份标识。			2
	//trxMerchantOrderno	商户订单号			系统返回商户订单号。				3
	//result				订单结果			DEAL：处理中；  FAIL：失败； SUCCESS：成功	4
	//productNo				产品编码			该字段请参考附录4.1：支付方式编码列表		5
	//memberGoods			商品名称			系统将商户所传的商品名称原样返回		6
	//amount				成功金额			单位:元，保留两位小数。例如10元=10.00		7
	//retMes				返回信息			订单支付结果返回信息，该字段回传中文描述，有可能为空。	不参与签名
	//hmac					签名数据			按照文档签名顺序，最后附上&key=密钥做32位md5小写加密，示例：reCode=1&trxMerchantNo=800666000037&trxMerchantOrderno=1504233022730&result=SUCCESS&productNo=WX-YF&memberGoods=1504233022730&amount=10.00&key=C2q7J35a9IBX851m41ilLJ4bZY148k205riLq85J77e348yzGf3BG96U838a对此签名串进行MD5加密。
	private static final String reCode						="reCode";
	private static final String trxMerchantNo				="trxMerchantNo";
	private static final String trxMerchantOrderno			="trxMerchantOrderno";
	private static final String result						="result";
	private static final String productNo					="productNo";
	private static final String memberGoods					="memberGoods";
	private static final String amount						="amount";
//	private static final String retMes						="retMes";

    //signature	数据签名	32	是	　
    private static final String signature  ="hmac";

    private static final String RESPONSE_PAY_MSG = "SUCCESS";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(trxMerchantNo);
        String ordernumberR = API_RESPONSE_PARAMS.get(trxMerchantOrderno);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[酷游]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
		//1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
		//2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
		StringBuffer signSrc= new StringBuffer();
		signSrc.append(reCode+"=").append(api_response_params.get(reCode)).append("&");
		signSrc.append(trxMerchantNo+"=").append(api_response_params.get(trxMerchantNo)).append("&");
		signSrc.append(trxMerchantOrderno+"=").append(api_response_params.get(trxMerchantOrderno)).append("&");
		signSrc.append(result+"=").append(api_response_params.get(result)).append("&");
		signSrc.append(productNo+"=").append(api_response_params.get(productNo)).append("&");
		signSrc.append(memberGoods+"=").append(api_response_params.get(memberGoods)).append("&");
		signSrc.append(amount+"=").append(api_response_params.get(amount)).append("&");
		signSrc.append("key=").append(channelWrapper.getAPI_KEY());
		String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[酷游]-[响应支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //result				订单结果			DEAL：处理中；  FAIL：失败； SUCCESS：成功	4
        String payStatusCode = api_response_params.get(result);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(amount));
        //db_amount数据库存入的是分 	第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("SUCCESS")) {
        	my_result = true;
        } else {
            log.error("[酷游]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[酷游]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：SUCCESS");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[酷游]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[酷游]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}