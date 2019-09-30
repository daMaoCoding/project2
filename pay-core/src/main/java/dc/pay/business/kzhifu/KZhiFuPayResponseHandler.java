package dc.pay.business.kzhifu;

/**
 * ************************
 * @author tony 3556239829
 */

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.BouncyCastleAES;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.HashUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.TreeMap;

@ResponsePayHandler("KZHIFU")
public final class KZhiFuPayResponseHandler extends PayResponseHandler {
     private final Logger log = LoggerFactory.getLogger(getClass());
     private static final String RESPONSE_PAY_MSG = "SUCCESS";

     private static final String   merAccount = "merAccount";  // "091fde3be727466ab36bb4eb22697bc4",
     private static final String   data = "data";  // "Wd5lCaaER2bAM/cJCQh1UXYpBVlmcOI4ibXB85hMWTWUUZEavwWEXCqxYlj8KmEdMCnvSC2tEvMSyIe1Ur4lk2IprLJxxeMw7UTD0LCygNVrgeIGXNUMu2dy6ypiFZhFOPNyuanXQuk98OY/qcmErrl3XPmn4yVyiUk2L5heho4YpQI59gZ96ONO1n62t1I+mdADL1CuddGAg+LY2KnSO/mQ8Qyce608N3EyQ64CSwgu3k5Cj9o3wKy380nO84amotrE2Y/exPEZ98wk3d4/t7yregwD4437S9R5iaBxAlk=",
     private static final String   orderId = "orderId";  // "20180911150124"

      private static final String  amount = "amount";  // "1.000000",
      //private static final String  merAccount = "merAccount";  // "091fde3be727466ab36bb4eb22697bc4",
      //private static final String  orderId = "orderId";  // "20180911150124",
      private static final String  sign = "sign";  // "5a82e3bdcc369e21a423835a677a078317521759",
      private static final String  orderStatus = "orderStatus";  // "SUCCESS",
      private static final String  mbOrderId = "mbOrderId";  // "1000137801536649285736qt9i7u"


    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(orderId);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[K支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        JSONObject json = proc(params);
        json.remove(sign);
        StringBuffer buffer		= new StringBuffer();
        TreeMap<String, Object> treeMap = new TreeMap<String, Object>(json);
        for (Map.Entry<String, Object> entry : treeMap.entrySet()) {
            if (entry.getValue() != null) {
                buffer.append(entry.getValue());
            }
        }
        buffer.append(channelWrapper.getAPI_KEY());
        String sign=  HashUtil.sha1(buffer.toString());
        log.debug("[K支付]-[请求支付]-2.生成加密URL签名完成：" + sign);
        return sign;
    }



    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
        JSONObject json = proc(api_response_params);
        boolean checkResult = false;
        String payStatus = json.getString(orderStatus);
        String responseAmount =  HandlerUtil.getFen(json.getString(amount));
        boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        if (checkAmount && payStatus.equalsIgnoreCase("SUCCESS")) {
            checkResult = true;
        } else {
            log.error("[K支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatus + " ,支付金额：" + responseAmount + " ，应支付金额：" + amountDb);
        }
        log.debug("[K支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + checkResult + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amountDb + ",第三方响应支付成功标志:" + payStatus + " ,计划成功：1");
        return checkResult;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        JSONObject json = null;
        try {
            json = proc(api_response_params);
        } catch (PayException e) {
           return false;
        }
        boolean result = json.getString(sign).equalsIgnoreCase(signMd5);
        log.debug("[K支付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[K支付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }



    public JSONObject  proc(Map<String, String> params) throws PayException {
        try {
           // JSONObject json =JSON.parseObject(BouncyCastleAES.decrypt(params.get(data),channelWrapper.getAPI_KEY()));
            JSONObject json =JSON.parseObject(BouncyCastleAES.decode3(params.get(data),channelWrapper.getAPI_KEY()));
            return json;
        } catch (Exception e) {
            throw  new PayException("通道返回数据异常");
        }
    }

}