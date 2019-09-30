package dc.pay.business.hengfuyun;

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
import dc.pay.utils.HandlerUtil;
import org.apache.commons.lang.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
@ResponsePayHandler("HENGFUYUN")
public final class HengFuYunPayResponseHandler extends PayResponseHandler {
      //private final Logger log = LoggerFactory.getLogger(getClass());
      private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("success");


    private static String msg = "msg";     // -> "20181229161202244125"
    private static String data = "data";     // -> "{"mer_no":10147,"amount":1000,"orderno":"20181229161202244125","sign":"b715438f5bc73643b4459f32fe2b2d21"}"
    private static String status = "status";     // -> "10000"

     private static String  mer_no = "mer_no";   // 10147,
     private static String  amount = "amount";   // 1000,
     private static String  orderno = "orderno";   // "20181229161202244125",
     private static String  sign = "sign";   // "b715438f5bc73643b4459f32fe2b2d21"



    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty()  ||!API_RESPONSE_PARAMS.containsKey(status) || !API_RESPONSE_PARAMS.get(status).equalsIgnoreCase("10000")   || !API_RESPONSE_PARAMS.containsKey(data) || StringUtils.isBlank(API_RESPONSE_PARAMS.get(data)))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        JSONObject resData = JSON.parseObject(API_RESPONSE_PARAMS.get(data));
        String ordernumberR = resData.getString(orderno);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[恒富云]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        // mer_no={0}&amount={1}&orderno={2}&key={3}
        JSONObject resData = JSON.parseObject(params.get(data));
        String paramsStr = String.format("mer_no=%s&amount=%s&orderno=%s&key=%s",
                resData.getString(mer_no),
                resData.getString(amount),
                resData.getString(orderno),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[恒富云]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }



    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean checkResult = false;
        JSONObject resData = JSON.parseObject(api_response_params.get(data));

        String payStatus = api_response_params.get(status);
        String responseAmount =  resData.getString(amount);
        //boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        boolean checkAmount =  HandlerUtil.isRightAmount(amountDb,responseAmount,"100");//第三方回调金额差额1元内
        if (checkAmount && payStatus.equalsIgnoreCase("10000")) {
            checkResult = true;
        } else {
            log.error("[恒富云]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatus + " ,支付金额：" + responseAmount + " ，应支付金额：" + amountDb);
        }
        log.debug("[恒富云]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + checkResult + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amountDb + ",第三方响应支付成功标志:" + payStatus + " ,计划成功：1");
        return checkResult;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        JSONObject resData = JSON.parseObject(api_response_params.get(data));
        boolean result = resData.getString(sign).equalsIgnoreCase(signMd5);
        log.debug("[恒富云]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[恒富云]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}