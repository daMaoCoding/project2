package dc.pay.business.lude;

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

@ResponsePayHandler("LUDEZHIFU")
public final class LuDePayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String RESPONSE_PAY_MSG = "SUCCESS";
    private static final String  service   = "service";           //: "TRADE.NOTIFY",
    private static final String  merId   = "merId";                //: "2017052444010020",
    private static final String  tradeNo   = "tradeNo";            //: "LUDEZHIFU_WX_SM-rlxz7",
    private static final String  tradeDate   = "tradeDate";        //: "20171114",
    private static final String  opeNo   = "opeNo";                //: "12037140",
    private static final String  opeDate   = "opeDate";            //: "20171114",
    private static final String  amount   = "amount";              //: "0.01",
    private static final String  status   = "status";              //: "1",
    private static final String  extra   = "extra";                //: "PAY",
    private static final String  payTime   = "payTime";            //: "20171114150320",
    private static final String  sign   = "sign";                  //: "74FE3EB7EFA744A2FBFC36FF04260F5D",
    private static final String  notifyType   = "notifyType";      //: "1"

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {

        String merIdRes = API_RESPONSE_PARAMS.get(merId);
        String orderIdRes = API_RESPONSE_PARAMS.get(tradeNo);
        if (StringUtils.isBlank(merIdRes) || StringUtils.isBlank(orderIdRes))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[路德]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + orderIdRes);
        return orderIdRes;
    }

    @Override
    protected String buildPaySign(Map<String, String> payParam, String api_key) throws PayException {
        String paramsStr = String.format("service=%s&merId=%s&tradeNo=%s&tradeDate=%s&opeNo=%s&opeDate=%s&amount=%s&status=%s&extra=%s&payTime=%s",
                payParam.get(service),  payParam.get(merId), payParam.get(tradeNo), payParam.get(tradeDate),
                payParam.get(opeNo), payParam.get(opeDate), payParam.get(amount), payParam.get(status), payParam.get(extra),
                payParam.get(payTime)
                );
        paramsStr = paramsStr.concat(channelWrapper.getAPI_KEY());
        String pay_md5sign = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[路德]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amount) throws PayException {
        boolean result = false;
        String payStatusCode = api_response_params.get(status);
        String responseAmount = api_response_params.get("amount");
        boolean checkAmount = amount.equalsIgnoreCase(HandlerUtil.getFen(responseAmount));
        if (checkAmount && payStatusCode.equalsIgnoreCase("1")) {
            result = true;
        } else {
            log.error("[路德]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + amount);
        }
        log.debug("[路德]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("[路德]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[路德]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}