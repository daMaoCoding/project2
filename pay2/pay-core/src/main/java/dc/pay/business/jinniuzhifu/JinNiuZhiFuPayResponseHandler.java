package dc.pay.business.jinniuzhifu;

import com.alibaba.fastjson.JSON;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.Map;
import java.util.TreeMap;

@ResponsePayHandler("JINNIUZHIFU")
@Slf4j
public class JinNiuZhiFuPayResponseHandler extends PayResponseHandler {
    //参数名称	参数含义	参与签名	参数说明
    //	商户编号	是
    private static final String MEMBERID = "memberid";
    //	订单号	是
    private static final String ORDERID = "orderid";
    //	订单金额	是
    private static final String AMOUNT = "amount";
    //	交易流水号	是
    private static final String TRANSACTIONID = "transaction_id";
    //	交易时间	是
    private static final String DATETIME = "datetime";
    //	交易状态	是	“00” 为成功
    private static final String RETURNCODE = "returncode";


    //	扩展返回	否	商户附加数据返回
    private static final String ATTACH = "attach";
    //	签名	否	请看验证签名字段格式
    private static final String SIGN = "sign";


    private static final String RESPONSE_PAY_MSG = "OK";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (CollectionUtils.isEmpty(API_RESPONSE_PARAMS))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(MEMBERID);
        String ordernumberR = API_RESPONSE_PARAMS.get(ORDERID);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[金牛支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }

    /**
     * @param api_response_params 三方返回的支付结果参数
     * @param amount              db保存的金额
     * @return
     * @throws PayException
     */
    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amount) throws PayException {
        boolean result = false;
        String payStatusCode = api_response_params.get(RETURNCODE);
        //金牛技术说 返回的也是 元 2019-08-17
        String responseAmount = HandlerUtil.getFen(api_response_params.get(AMOUNT));
        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount = HandlerUtil.isAllowAmountt(amount, responseAmount, "100");//我平台默认允许一元偏差
        if (checkAmount && "00".equals(payStatusCode)) {
            result = true;
        } else {
            log.error("[金牛支付]-[响应支付]-3.1 金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + amount);
        }
        log.debug("[金牛支付]-[响应支付]-3.2 验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：00");
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[金牛支付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        if (CollectionUtils.isEmpty(api_response_params)) return Boolean.FALSE;
        boolean result = signMd5.equalsIgnoreCase(api_response_params.get(SIGN));
        log.debug("[金牛支付]-[响应支付]-4.验证MD5签名：{}", result);
        return result;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        log.debug("[金牛支付]-[响应支付]-2.1 生成加密URL签名完成，传入参数：" + JSON.toJSONString(api_response_params));
        if (CollectionUtils.isEmpty(api_response_params) || StringUtils.isBlank(api_key)) return StringUtils.EMPTY;
        StringBuilder paraStr = new StringBuilder();
        Map<String, String> map = new TreeMap<>(api_response_params);
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (StringUtils.isNotBlank(entry.getValue()) && !ATTACH.equals(entry.getKey()) && !SIGN.equals(entry.getKey())) {
                paraStr.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
            }
        }
        paraStr.append("key=").append(channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paraStr.toString()).toLowerCase();
        log.debug("[金牛支付]-[响应支付]-2.2 生成加密URL签名完成，参数：" + JSON.toJSONString(paraStr) + " ,值：" + JSON.toJSONString(signMd5));
        return signMd5;

    }
}
