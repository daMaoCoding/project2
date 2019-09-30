package dc.pay.business.yidongzhifu;

/**
 * ************************
 * @author tony 3556239829
 */

import com.alibaba.fastjson.JSON;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@ResponsePayHandler("YIDONGZHIFU")
public final class YiDongZhiFuPayResponseHandler extends PayResponseHandler {
     private final Logger log = LoggerFactory.getLogger(getClass());
     private static final String RESPONSE_PAY_MSG = "<xml><return_code>SUCCESS</return_code></xml>";

     private static final String   transaction_id = "transaction_id";   // -> "20180625200040011100870010332467"
     private static final String   charset = "charset";   // -> "UTF-8"
     private static final String   nonce_str = "nonce_str";   // -> "9chpbu7nm0imt7mevhkzt"
     private static final String   method = "method";   // -> "mbupay.alipay.sqm"
     private static final String   sign = "sign";   // -> "C37D09CF98768D8B91702A1FF40FE893"
     private static final String   fee_type = "fee_type";   // -> "CNY"
     private static final String   mch_id = "mch_id";   // -> "cm2018062310000134"
     private static final String   version = "version";   // -> "2.0.0"
     private static final String   out_trade_no = "out_trade_no";   // -> "20180625165223"
     private static final String   total_fee = "total_fee";   // -> "10000"
     private static final String   appid = "appid";   // -> "ca2018062310000134"
     private static final String   result_code = "result_code";   // -> "SUCCESS"
     private static final String   time_end = "time_end";   // -> "20180625165313"
     private static final String   return_code = "return_code";   // -> "SUCCESS"
     private static final String   sign_type = "sign_type";   // -> "MD5"


    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(out_trade_no);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[移动支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        String pay_md5sign = SignUtil.sign(params, channelWrapper.getAPI_KEY());
        log.debug("[移动支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }



    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean checkResult = false;
        if(!api_response_params.containsKey(return_code) ||!api_response_params.containsKey(result_code)){
            return checkResult;
        }
        String return_code_R = api_response_params.get(return_code);
        String result_code_R = api_response_params.get(result_code);
        String responseAmount =   api_response_params.get(total_fee);
        boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        if (checkAmount && return_code_R.equalsIgnoreCase("SUCCESS")  &&  result_code_R.equalsIgnoreCase("SUCCESS") ) {
            checkResult = true;
        } else {
            log.error("[移动支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + result_code_R + " ,支付金额：" + responseAmount + " ，应支付金额：" + amountDb);
        }
        log.debug("[移动支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + checkResult + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amountDb + ",第三方响应支付成功标志:" + result_code_R + " ,计划成功：SUCCESS");
        return checkResult;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("[移动支付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[移动支付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}