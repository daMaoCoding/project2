package dc.pay.business.anquanfu;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ************************
 * @author tony 3556239829
 */

import com.alibaba.fastjson.JSON;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RsaUtil;

@ResponsePayHandler("ANQUANFU")
public final class AnQuanFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

//    private static final String  subject     = "subject";
//    private static final String  body     = "body";   //否
    private static final String  trade_status     = "trade_status";
    private static final String  total_amount     = "total_amount";
//    private static final String  sysd_time     = "sysd_time";
//    private static final String  trade_time     = "trade_time";
//    private static final String  trade_no     = "trade_no";
    private static final String  out_trade_no     = "out_trade_no";
//    private static final String  notify_time     = "notify_time";
    private static final String  sign     = "sign";

    private static final String RESPONSE_PAY_MSG = "SUCCESS";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(out_trade_no);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[安全付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if(StringUtils.isBlank(params.get(paramKeys.get(i))) || sign.equalsIgnoreCase(paramKeys.get(i).toString()))
                continue;
            sb.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i))).append("&");
        }
        String signInfo = sb.toString().substring(0, sb.toString().length() - 1);
        boolean result = false;
        String wpay_public_key = channelWrapper.getAPI_PUBLIC_KEY();
        //String wpay_public_key = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCGosEaDEGG9VaZbJ0NOxevFLd9xGEI0/mXcy1EOfHaI0/NZgFbysS0SDf1M1vRCBLXL3dmoiUW8cLWNf0askCtQanxz5kWXXKrGmJpsL5a8dTu6PCl0wD4OB+9B0zCoe/SquACJLBGjsHNGeYS8FmitdYnDjfrTDClimkUUuRthQIDAQAB";
        result = RsaUtil.validateSignByPublicKey(signInfo, wpay_public_key, params.get(sign),"SHA256withRSA");	// 验签   signInfo安全付返回的签名参数排序， wpay_public_key安全付公钥， wpaySign安全付返回的签名
        log.debug("[安全付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(result));
        return String.valueOf(result);
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amount) throws PayException {
        boolean result = false;
        String payStatusCode = api_response_params.get(trade_status);
        String responseAmount = api_response_params.get(total_amount);
        boolean checkAmount = amount.equalsIgnoreCase(responseAmount);
        if (checkAmount && payStatusCode.equalsIgnoreCase("1")) {
            result = true;
        } else {
            log.error("[安全付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + amount);
        }
        log.debug("[安全付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        Boolean signMd5Boolean = Boolean.valueOf(signMd5);

        //boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("[安全付]-[响应支付]-4.验证MD5签名：" + signMd5Boolean.booleanValue());
        return signMd5Boolean.booleanValue();
    }

    @Override
    protected String responseSuccess() {
        log.debug("[安全付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}