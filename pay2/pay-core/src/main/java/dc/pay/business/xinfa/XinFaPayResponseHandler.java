package dc.pay.business.xinfa;

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
 * 
 * @author kevin
 * Aug 17, 2018
 */
@ResponsePayHandler("XINFA")
public final class XinFaPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String data    		="data";
    private static final String merchNo  		="merchNo";
    private static final String payType    		="payType";
    private static final String orderNo         ="orderNo";
    private static final String amount  		="amount";
    private static final String goodsName    	="goodsName";
    private static final String payStateCode    ="payStateCode";
    private static final String payDate    		="payDate";
    private static final String sign    		="sign";

    private static final String RESPONSE_PAY_MSG = "SUCCESS";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(merchNo);
        String ordernumberR = API_RESPONSE_PARAMS.get(orderNo);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[鑫发]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" , ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
    	 Map<String, String> metaSignMap = XinFaUtil.parseData(api_response_params.get(data), channelWrapper.getAPI_KEY());
         metaSignMap.remove(sign);
         String jsonStr = HandlerUtil.mapToJson(metaSignMap);
         String sign = XinFaUtil.MD5(jsonStr.toString() + channelWrapper.getAPI_MEMBERID().split("&")[1], XinFaUtil.CHARSET);
         log.debug("[鑫发]-[响应支付]-2.生成加密URL签名完成：{}" , JSON.toJSONString(sign));
         return sign;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
    	boolean my_result = false;
        //payStateCode		交易状态	00-支付成功
    	Map<String, String> metaSignMap = XinFaUtil.parseData(api_response_params.get(data), channelWrapper.getAPI_KEY());
        String responseAmount =metaSignMap.get(amount);
        String payStatus = metaSignMap.get(payStateCode);
        //amount数据库存入的是分 	第三方返回的amount是分
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //00代表第三方支付成功
        if (checkAmount && payStatus.equalsIgnoreCase("00")) {
            my_result = true;
        } else {
            log.error("[鑫发]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatus + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[鑫发]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatus + " ,计划成功：00");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
    	Map<String, String> metaSignMap;
        try {
             metaSignMap = XinFaUtil.parseData(api_response_params.get(data), channelWrapper.getAPI_KEY());
        } catch (PayException e) {
            return false;
        }
        boolean result = metaSignMap.get(sign).equalsIgnoreCase(signMd5);
        log.debug("[鑫发]-[响应支付]-4.验证MD5签名：{}" , result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[鑫发]-[响应支付]-5.第三方支付确认收到消息返回内容：{}" , RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}