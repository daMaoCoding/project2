package dc.pay.business.xincaifubao2;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;

/**
 * 
 * @author andrew
 * Jan 26, 2019
 */
@ResponsePayHandler("XINCAIFUBAO2")
public final class XinCaiFuBao2PayResponseHandler extends PayResponseHandler {
     private final Logger log = LoggerFactory.getLogger(getClass());
     private static final String RESPONSE_PAY_MSG = "SUCCESS";

      private static final String  nonce_str = "nonce_str";    // "e03eadf71e2a46f3840d180d20ce31e8",
//       private static final String  trade_state = "trade_state";    // "SUCCESS",
       private static final String  out_trade_no = "out_trade_no";    // "20181113110714823867",
       private static final String  total_fee = "total_fee";    // "2000",
//       private static final String  real_total_fee = "real_total_fee";    // "2000",
       private static final String  sign = "sign";    // "1A57922507E3FA6AE97FFCA753D403A5",
       private static final String  result_code = "result_code";    // "true",
//       private static final String  fee_type = "fee_type";    // "CNY",
//       private static final String  notify_Url = "notify_Url" ;    //"http://66p.huiek888.com:30000/respPayWeb/XINCAIFUBAO_BANK_WAP_WX_SM/",
       private static final String  return_code = "return_code";    // "true",
       private static final String  app_id = "app_id";    // "in018c7421d3e7434dada0d4d99566268d",
//       private static final String  transaction_no = "transaction_no";    // "9369093043121356837"

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(out_trade_no);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[新彩富宝2]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        String paramsStr = String.format("app_id=%s&nonce_str=%s&out_trade_no=%s&sign_type=MD5&total_fee=%s&version=4.1&key=%s",
                params.get(app_id),
                params.get(nonce_str),
                params.get(out_trade_no),
                params.get(total_fee),
                channelWrapper.getAPI_KEY());
        String pay_md5sign = HandlerUtil.getMD5UpperCase(paramsStr.trim());
        log.debug("[新彩富宝2]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean result = false;
        String payStatusReturn_code = api_response_params.get(return_code);
        String payStatusResult_code = api_response_params.containsKey(result_code)?api_response_params.get(result_code):"null";
        String responseAmount = api_response_params.get(total_fee);
        boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        if (checkAmount && payStatusReturn_code.equalsIgnoreCase("true")  && payStatusResult_code.equalsIgnoreCase("true")) {
            result = true;
        } else {
            log.error("[新彩富宝2]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusResult_code + " ,支付金额：" + responseAmount + " ，应支付金额：" + amountDb);
        }
        log.debug("[新彩富宝2]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amountDb + ",第三方响应支付成功标志:" + payStatusResult_code + " ,计划成功：true");
        return result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("[新彩富宝2]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[新彩富宝2]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}