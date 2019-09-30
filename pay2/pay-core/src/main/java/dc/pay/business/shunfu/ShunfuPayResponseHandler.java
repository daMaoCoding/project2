package dc.pay.business.shunfu;

import com.alibaba.fastjson.JSONObject;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.TreeMap;

/**
 * ************************
 *
 * @author tony 3556239829
 */
@ResponsePayHandler("SHUNFU")
public class ShunfuPayResponseHandler extends PayResponseHandler {
    private static final Logger log = LoggerFactory.getLogger(ShunfuPayResponseHandler.class);
    private static final String RESPONS_MAP_KEY = "data";
    private static final String AMOUNT = "amount";
    private static final String GOODSINFO = "goodsName";
    private static final String MERNO = "merNo";
    private static final String ORDERNUM = "orderNo";
    private static final String PAYDATE = "payDate";
    private static final String PAYNETWAY = "payNetway";
    private static final String RESULTCODE = "resultCode";
    private static final String SIGN = "sign";
    private static final String RETURNCODE_SUCCESS = "00";
    private static final String RESPONSE_PAY_MSG = "000000";
    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        String orderNum;
        if (null != API_RESPONSE_PARAMS && !API_RESPONSE_PARAMS.isEmpty()) {
            String responseJsonStr = API_RESPONSE_PARAMS.get(RESPONS_MAP_KEY);
            JSONObject responseJsonObject = JSONObject.parseObject(responseJsonStr);
            orderNum = responseJsonObject.getString(ORDERNUM);
            if (StringUtils.isBlank(orderNum))
                throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        } else {
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_PARAM_ERROR);
        }
        log.debug("[瞬付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + orderNum);
        return orderNum;
    }
    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        JSONObject responseJsonObject = JSONObject.parseObject(API_RESPONSE_PARAMS.get(RESPONS_MAP_KEY));
        Map<String, String> metaSignMap = new TreeMap<String, String>();
        metaSignMap.put(MERNO, responseJsonObject.getString(MERNO));
        metaSignMap.put(PAYNETWAY, responseJsonObject.getString(PAYNETWAY));
        metaSignMap.put(ORDERNUM, responseJsonObject.getString(ORDERNUM));
        metaSignMap.put(AMOUNT, responseJsonObject.getString(AMOUNT));
        metaSignMap.put(GOODSINFO, responseJsonObject.getString(GOODSINFO));
        metaSignMap.put(RESULTCODE, responseJsonObject.getString(RESULTCODE));
        metaSignMap.put(PAYDATE, responseJsonObject.getString(PAYDATE));
        String jsonStr = HandlerUtil.mapToJson(metaSignMap);
        String signMd5 = HandlerUtil.getMD5UpperCase(jsonStr.toString() + api_key, "UTF-8");
        log.debug("[瞬付]-[响应支付]-2.响应内容生成md5完成：" + signMd5);
        return signMd5;
    }
    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amount) throws PayException {
        boolean result = false;
        JSONObject responseJsonObject = JSONObject.parseObject(api_response_params.get(RESPONS_MAP_KEY));
        String payStatusCode = responseJsonObject.getString(RESULTCODE);
        String responseAmount = responseJsonObject.getString(AMOUNT);
        boolean checkAmount = amount.equalsIgnoreCase(responseAmount);
        if (checkAmount && payStatusCode.equalsIgnoreCase(RETURNCODE_SUCCESS)) {
            result = true;
        } else {
            log.error("[瞬付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + amount);
        }
        log.debug("[瞬付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return result;
    }
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        JSONObject responseJsonObject = JSONObject.parseObject(api_response_params.get(RESPONS_MAP_KEY));
        boolean result = responseJsonObject.getString(SIGN).equalsIgnoreCase(signMd5);
        log.debug("[瞬付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }
    @Override
    protected String responseSuccess() {
        log.debug("[瞬付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}