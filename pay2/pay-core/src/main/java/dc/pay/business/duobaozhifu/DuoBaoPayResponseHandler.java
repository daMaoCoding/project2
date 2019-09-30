package dc.pay.business.duobaozhifu;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@ResponsePayHandler("DUOBAOZHIFU")
public final class DuoBaoPayResponseHandler extends PayResponseHandler {
     private final Logger log = LoggerFactory.getLogger(getClass());
     private static final String RESPONSE_PAY_MSG = "opstate=0";

    private static final String  RSA_sign = "RSA_sign";   //: "pLWcrScE7T5YOaBdQJmGHJUSvysXVZtFGsncaI7kUUO5LtRU0LFQSvPIHUMcvbNVECOLvvd8cRBj86//pPzWRYxJsfom0qMMYH4nT5Juh5HSqwLE/rfHjIiZJ6HN4n5gcJp6kCCNJM//oo863XyGkOkXO5u3NHFsjBbiwit_/Uk=",
    private static final String  attach = "attach";   //: "",
    private static final String  completiontime = "completiontime";   //: "2018/06/26 17:40:16",
    private static final String  msg = "msg";   //: "",
    private static final String  opstate = "opstate";   //: "0",
    private static final String  orderid = "orderid";   //: "20180626173904",
    private static final String  ovalue = "ovalue";   //: "100",
    private static final String  sign = "sign";   //: "f435b6ec19bf689a58a5885a6ce75c85",
    private static final String  sysorderid = "sysorderid";   //: "18062617400456017968",
    private static final String  systime = "systime";   //: "2018/06/26 17:40:16",
    private static final String  type = "type";   //: "1100"


    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(orderid);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[多宝支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        // orderid={}&opstate={}&ovalue={}key
        String paramsStr = String.format("orderid=%s&opstate=%s&ovalue=%s%s",
                params.get(orderid),
                params.get(opstate),
                params.get(ovalue),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[多宝支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }



    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean checkResult = false;
        String payStatus = api_response_params.get(opstate);
        String responseAmount =  HandlerUtil.getFen(api_response_params.get(ovalue));
        boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        if (checkAmount && payStatus.equalsIgnoreCase("0")) {
            checkResult = true;
        } else {
            log.error("[多宝支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatus + " ,支付金额：" + responseAmount + " ，应支付金额：" + amountDb);
        }
        log.debug("[多宝支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + checkResult + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amountDb + ",第三方响应支付成功标志:" + payStatus + " ,计划成功：0");
        return checkResult;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("[多宝支付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[多宝支付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}