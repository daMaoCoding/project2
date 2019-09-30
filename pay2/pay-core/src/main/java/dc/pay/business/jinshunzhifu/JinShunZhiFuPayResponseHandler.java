package dc.pay.business.jinshunzhifu;

import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;

import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import sun.misc.BASE64Decoder;

/**
 * @author cobby
 * Jan 15, 2019
 */
@ResponsePayHandler("JINSHUNZHIFU")
public final class JinShunZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

// 应用名称	application	20	NotifyOrder
// 通讯协议版本号	version	10	1.0.1
// 商户代码	merchantId	24
// 支付列表	deductList
// 支付订单号	deductList.item.payOrderId	20
// 支付金额	deductList.item.payAmt	12	分
// 支付状态	deductList.item.payStatus	2	00等待付款，01付款成功，02付款失败
// 支付描述	deductList.item.payDesc	128
// 支付时间	deductList.item.payTime	14	yyyyMMddHHmmss
// 退款列表	refundList		参考查询接口3.4节
    private static final String merchantOrderId                ="merchantOrderId"; // 商户订单号
    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {

        Map<String, String> map = null;
        try {
            map = this.parsePaymentNotify(API_RESPONSE_PARAMS.get("pay-core-respData"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = map.get(merchantOrderId);
        if ( StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[金顺支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        String paramsStr = API_RESPONSE_PARAMS.get("pay-core-respData");
        log.debug("[金顺支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(paramsStr));
        return paramsStr;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        Map<String, String> responseMap = null;
        try {
	        responseMap = this.parsePaymentNotify(API_RESPONSE_PARAMS.get("pay-core-respData"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        boolean my_result = false;
        //status 00等待付款，01付款成功，02付款失败
        String payStatusCode = responseMap.get("payStatus");
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseMap.get("payAmt"));
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("01")) {
            my_result = true;
        } else {
            log.error("[金顺支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseMap.get("payAmt") + " ，应支付金额：" + db_amount);
        }
        log.debug("[金顺支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseMap.get("payAmt") + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：01");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
	    boolean my_result = false;
	    try {
		    my_result = verify(signMd5, channelWrapper.getAPI_PUBLIC_KEY());
	    } catch (Exception e) {
		    e.printStackTrace();
	    }
	    log.debug("[金顺支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[金顺支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }

	/**
	 * 支付通知结果解析
	 * @param notifyResultStr 通知字符串
	 * @return 通知结果
	 * @throws Exception
	 */
	public Map<String ,String > parsePaymentNotify(String notifyResultStr) throws Exception {
		String [] e = xmlUtil.split(notifyResultStr, "|");
		String responseSrc = e[0];
		//验签
		if(e.length == 2){
			responseSrc = new String(Base64.getDecoder().decode(e[0]),"utf-8");
		}
		Map<String ,String> response = null;
		response = xmlUtil.xmlToPaymentNotifyResponse1(responseSrc);
		return response;
	}

	private static final JinShunZhiFuXmlUtil xmlUtil  = new JinShunZhiFuXmlUtil();
	public boolean  verify(String  paramsStr, String pubKey) throws Exception {
		String[] split = xmlUtil.split(paramsStr, "|");
		String signData = split[1];
		String base64SourceData = split[0];

		BASE64Decoder decoder = new BASE64Decoder();
		byte[] keyBytes = decoder.decodeBuffer(pubKey);

		// generate public key
		X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		PublicKey publicKey = keyFactory.generatePublic(spec);



		byte[] md5 = getDigest(new BASE64Decoder().decodeBuffer(base64SourceData));

		Signature signature = Signature.getInstance("SHA1withRSA");
		signature.initVerify(publicKey);
		signature.update(md5);
		boolean verifySuccessed = signature.verify(new BASE64Decoder().decodeBuffer(signData));
		return verifySuccessed;
	}

	public static byte[] getDigest(byte[] buffer) {
		try {
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			md5.update(buffer);
			return md5.digest();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}