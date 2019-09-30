package dc.pay.business.yibaotongfirst;

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
 * 
 * @author andrew
 * May 23, 2018
 */
@ResponsePayHandler("YIBAOTONGFIRST")
public final class YiBaoTongFirstPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

	//字段名						填写类型			说明
	//merchantNum				必填				商户号
	//orderNum					必填				商户订单号
	//amount					必填				交易金额,单位（分）
	//nonce_str					必填				随机串
	//orderStatus				必填				订单状态，“SUCCESS”为成功，其他失败
	//remark					必填				备注信息（不参与签名）
	//sign						必填				签名
	private static final String merchantNum				="merchantNum";
	private static final String orderNum				="orderNum";
	private static final String amount					="amount";
	private static final String nonce_str				="nonce_str";
	private static final String orderStatus				="orderStatus";
//	private static final String remark					="remark";
	private static final String sign					="sign";
	
    private static final String RESPONSE_PAY_MSG = "SUCCESS";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(merchantNum);
        String ordernumberR = API_RESPONSE_PARAMS.get(orderNum);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[亿宝通1.0]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(merchantNum+"=").append(params.get(merchantNum)).append("&");
        signSrc.append(orderNum+"=").append(params.get(orderNum)).append("&");
        signSrc.append(amount	+"=").append(params.get(amount	)).append("&");
        signSrc.append(nonce_str+"=").append(params.get(nonce_str)).append("&");
        signSrc.append(orderStatus+"=").append(params.get(orderStatus)).append("&");
        signSrc.append("key=").append(channelWrapper.getAPI_KEY());
        String signInfo = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(signInfo);
        log.debug("[亿宝通1.0]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean result = false;
        //orderStatus					必填				订单状态，“SUCCESS”为成功，其他失败
        String payStatusCode = api_response_params.get(orderStatus);
        String responseAmount = api_response_params.get(amount);
        //db_amount数据库存入的是分 	第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        if (checkAmount && payStatusCode.equalsIgnoreCase("SUCCESS")) {
            result = true;
        } else {
            log.error("[亿宝通1.0]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + amount);
        }
        log.debug("[亿宝通1.0]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：SUCCESS");
        return result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
//        Boolean signMd5Boolean = Boolean.valueOf(signMd5);
        boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("[亿宝通1.0]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[亿宝通1.0]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}