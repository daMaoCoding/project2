package dc.pay.business.wepayzhifu;

/**
 * ************************
 * @author tony 3556239829
 */

import com.alibaba.fastjson.JSON;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import org.apache.commons.lang.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
@ResponsePayHandler("WEPAYZHIFU")
public final class WePayZhiFuPayResponseHandler extends PayResponseHandler {
      //private final Logger log = LoggerFactory.getLogger(getClass());
        private static final String RESPONSE_PAY_MSG = jsonResponsePayMsg("{\"status\": true, \"err_msg\" : \"success\"}");

        private static final String   Amount ="Amount";  // "100",
        private static final String   BuCode ="BuCode";  // "zhangfa1001",
        private static final String   Sign ="Sign";  // "9e2a5091a8b1420aaee8b95864088652",
        private static final String   Status ="Status";  // "true",
        private static final String   TransId ="TransId";  // "zhangfa100120181029154941489100"


    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty()
                || !API_RESPONSE_PARAMS.containsKey(BuCode)  || StringUtils.isBlank(API_RESPONSE_PARAMS.get(BuCode))
                || !API_RESPONSE_PARAMS.containsKey(TransId) || StringUtils.isBlank(API_RESPONSE_PARAMS.get(TransId))
                )
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(TransId).replaceFirst(API_RESPONSE_PARAMS.get(BuCode),"");
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[WePay支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        // BuCode=wepay&TransId=wepay20180212141454532324&Amount=1000&Status=true&Key=21c3031066ec36ab7be4c6f80087694f
        String paramsStr = String.format("BuCode=%s&TransId=%s&Amount=%s&Status=%s&Key=%s",
                params.get(BuCode),
                params.get(TransId),
                params.get(Amount),
                params.get(Status),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[WePay支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }



    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean checkResult = false;
        String payStatus = api_response_params.get(Status);
        String responseAmount =  HandlerUtil.getFen(api_response_params.get(Amount));
        //boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        boolean checkAmount =  HandlerUtil.isRightAmount(amountDb,responseAmount,"100");//第三方回调金额差额1元内
        if (checkAmount && payStatus.equalsIgnoreCase("true")) {
            checkResult = true;
        } else {
            log.error("[WePay支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatus + " ,支付金额：" + responseAmount + " ，应支付金额：" + amountDb);
        }
        log.debug("[WePay支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + checkResult + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amountDb + ",第三方响应支付成功标志:" + payStatus + " ,计划成功：1");
        return checkResult;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(Sign).equalsIgnoreCase(signMd5);
        log.debug("[WePay支付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[WePay支付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}