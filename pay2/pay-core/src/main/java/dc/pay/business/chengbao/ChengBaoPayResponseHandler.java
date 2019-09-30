package dc.pay.business.chengbao;

/**
 * ************************
 * 诚宝-响应处理器
 *
 * @author tony 3556239829
 */

import com.alibaba.fastjson.JSON;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.util.Map;

@Slf4j
@ResponsePayHandler("CHENGBAO")
public final class ChengBaoPayResponseHandler extends PayResponseHandler {
    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("success");
     private static final String  edattch ="edattch";  // "",
     private static final String  edddh ="edddh";  // "20181006102022",
     private static final String  eddesc ="eddesc";  // "20181006102022",
     private static final String  edfee ="edfee";  // "1.00",
     private static final String  edid ="edid";  // "2018101",
     private static final String  edorder ="edorder";  // "f1bc35603b634d8fb5a08f2582c0f61e",
     private static final String  edsign ="edsign";  // "4fa0a61e7b22bee66dd454c2b43affe9",
     private static final String  edstatus ="edstatus";  // "1",
     private static final String  edtime ="edtime";  // "1538792658"

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty()) {
            log.error("[诚宝]-[响应支付]-1.获取支付通道响应信息中的订单号错误，" + JSON.toJSONString(API_RESPONSE_PARAMS));
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        }
        String memberId = API_RESPONSE_PARAMS.get(edid);
        String orderId = API_RESPONSE_PARAMS.get(edddh);
        if (StringUtils.isBlank(memberId) || StringUtils.isBlank(orderId)) {
            log.error("[诚宝]-[响应支付]-1.获取支付通道响应信息中的订单号错误，" + JSON.toJSONString(API_RESPONSE_PARAMS));
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        }
        log.debug("[诚宝]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + orderId);
        return orderId;
    }

    @Override
    protected String buildPaySign(Map<String, String> payParam, String api_key) throws PayException {
        //签名【md5(订单状态+商务号+商户订单号+支付金额+商户秘钥)】
        String paramsStr = String.format("%s%s%s%s%s",
                payParam.get(edstatus),
                payParam.get(edid),
                payParam.get(edddh),
                payParam.get(edfee),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[诚宝]-[请求支付]-2.生成加密URL签名完成：" + signMd5);
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amount) throws PayException {
        boolean result = false;
        String payStatusCode = api_response_params.get(edstatus);
        String responseAmount = api_response_params.get(edfee);
        responseAmount = HandlerUtil.getFen(responseAmount);
        boolean checkAmount = amount.equalsIgnoreCase(responseAmount);
        if (checkAmount && payStatusCode.equalsIgnoreCase("1")) {
            result = true;
        } else {
            log.error("[诚宝]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + amount);
        }
        log.debug("[诚宝]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(edsign).equalsIgnoreCase(signMd5);
        log.debug("[诚宝]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[诚宝]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}