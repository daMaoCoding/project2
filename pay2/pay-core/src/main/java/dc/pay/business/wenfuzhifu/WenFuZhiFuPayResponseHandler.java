package dc.pay.business.wenfuzhifu;

/**
 * ************************
 * @author tony 3556239829
 */

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

@ResponsePayHandler("WENFUZHIFU")
public final class WenFuZhiFuPayResponseHandler extends PayResponseHandler {
     private final Logger log = LoggerFactory.getLogger(getClass());
     private static final String RESPONSE_PAY_MSG = "SUCCESS";

      private static final String  mechno = "mechno";   // "MPSH15331306591219",
      private static final String  charset = "charset";   // "utf-8",
      private static final String  amount = "amount";   // "1000",
      private static final String  orderno = "orderno";   // "20180915103208",
      private static final String  extraparam = "extraparam";   // "null",
      private static final String  sign = "sign";   // "F6EE5D9C5316B3B1AF1A9AD54D3068FB",
      private static final String  transactionid = "transactionid";   // "1867e96270d6b4ffef22d8cc854540",
      private static final String  status = "status";   // "100",
      private static final String  timestamp = "timestamp";   // "1536978778529"


    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(orderno);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[稳付支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        SortedMap<String, String> smap = new TreeMap<String, String>(params);
        StringBuffer stringBuffer = new StringBuffer();
        for (Map.Entry<String, String> m : smap.entrySet()) {
            Object value = m.getValue();
            if (!"null".equals(value)&&value != null && !StringUtils.isBlank(String.valueOf(value))&&!sign.equals(m.getKey())){
                stringBuffer.append(m.getKey()).append("=").append(value).append("&");
            }
        }
        String argPreSign = stringBuffer.append("key=").append(channelWrapper.getAPI_KEY()).toString();
        String  pay_md5sign = HandlerUtil.getMD5UpperCase(argPreSign);
        log.debug("[稳付支付]-[请求支付]-2.生成加密URL签名完成：" + pay_md5sign);
        return pay_md5sign;
    }



    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean checkResult = false;
        String payStatus = api_response_params.get(status);
        String responseAmount =  api_response_params.get(amount);
        boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        if (checkAmount && payStatus.equalsIgnoreCase("100")) {
            checkResult = true;
        } else {
            log.error("[稳付支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatus + " ,支付金额：" + responseAmount + " ，应支付金额：" + amountDb);
        }
        log.debug("[稳付支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + checkResult + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amountDb + ",第三方响应支付成功标志:" + payStatus + " ,计划成功：1");
        return checkResult;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("[稳付支付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[稳付支付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}