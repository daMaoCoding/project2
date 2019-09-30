package dc.pay.business.dudu;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
*
* 
* @author kevin
* Aug 10, 2018
*/

import com.alibaba.fastjson.JSON;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;

/**
 * 
 * @author andrew
 * Sep 3, 2018
 */
@ResponsePayHandler("DUDU")
public final class DuDuPayResponseHandler extends PayResponseHandler {
     private final Logger log = LoggerFactory.getLogger(getClass());
     private static final String RESPONSE_PAY_MSG = "success";


     private static final String  order_id =  "order_id";           ///: "20180606153012",
     private static final String  amount =  "amount";           ///: "0.01",
//     private static final String  pay_method =  "pay_method";           ///: "5",
     private static final String  merchant_id =  "merchant_id";           ///: "10049",
     
     private static final String  isPaid  =  "isPaid";           ///: "10049",
     private static final String  sign =  "sign";           ///: "bd3ae57de12d484e38ac70abf9a888ca"


    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(order_id);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[嘟嘟]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        String paramsStr = String.format("merchant_id=%s&order_id=%s&amount=%s&sign=%s",
                params.get(merchant_id),
                params.get(order_id),
                params.get(amount),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[嘟嘟]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //isPaid        交易状态    1   1-支付成功 
        String payStatusCode = api_response_params.get(isPaid);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(amount));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("1")) {
            my_result = true;
        } else {
            log.error("[嘟嘟]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[嘟嘟]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return my_result;
    }

//    @Override
//    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
//        boolean checkResult = false;
//        String responseAmount =  HandlerUtil.getFen(api_response_params.get(amount));
//        boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
//        if (checkAmount) {
//            checkResult = true;
//        } else {
//            log.error("[嘟嘟]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID()  + " ,支付金额：" + responseAmount + " ，应支付金额：" + amountDb);
//        }
//        log.debug("[嘟嘟]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + checkResult + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amountDb);
//        return checkResult;
//    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("[嘟嘟]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[嘟嘟]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}