package dc.pay.business.aoke;

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
import org.apache.shiro.crypto.hash.Sha256Hash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@ResponsePayHandler("AOKE")
public final class AoKePayResponseHandler extends PayResponseHandler {
     private final Logger log = LoggerFactory.getLogger(getClass());
     private static final String RESPONSE_PAY_MSG = "<result>yes</result>";



    private static final String  order_amount  = "order_amount";         //  "order_amount": "1.00",
    private static final String  successcode  = "successcode";           //  "successcode": "ok", 支付成功

    private static final String  add_string  = "add_string";            //  "add_string": "52ccd56fe81f56adc665fee1dfd481fc",
    private static final String  add_string2  = "add_string2";          //  "add_string2": "24610f514cb8ffcdb7bc30931a4fbd0499411ec38575f3c0141f7b62b4bea763",
    private static final String  merchantid  = "merchantid";             //  "merchantid": "MIDTES1",
    private static final String  oid  = "oid";                          //  "oid": "AOKE_WAP_QQ-aodSw",
    private static final String  siteid  = "siteid";                    //  "siteid": "2000334",
    private static final String  system_id  = "system_id";               //  "system_id": "20180502-8000860639-MIDTES1-231533_894_21560208",
    private static final String  version  = "version";                  //  "version": "2.0"


    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(oid);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[澳科支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        //$checkAddString = md5(system_id . $oid . 'ok' . $orderAmount . $merchantId . $siteId . $password);
        //$checkAddString2 = hash('sha256', system_id . $oid . 'ok' . $orderAmount . $merchantId . $siteId . $password);
        String pay_sign_str1=String.format("%s%s%s%s%s%s%s",params.get(system_id),params.get(oid),"ok",params.get(order_amount),params.get(merchantid),params.get(siteid),channelWrapper.getAPI_KEY());
        String md51 = HandlerUtil.getMD5UpperCase(pay_sign_str1).toLowerCase();
        String sha2561 = new Sha256Hash(pay_sign_str1).toString();

        String signMd5 ="false";
        if(md51.equalsIgnoreCase(params.get(add_string)) && sha2561.equalsIgnoreCase(params.get(add_string2))){
            signMd5= "true";
        }
        log.debug("[澳科支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }



    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean result = false;
        String payStatus = api_response_params.get(successcode);
        String responseAmount =  HandlerUtil.getFen(api_response_params.get(order_amount));
        boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        if (checkAmount && payStatus.equalsIgnoreCase("ok")) {
            result = true;
        } else {
            log.error("[澳科支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatus + " ,支付金额：" + responseAmount + " ，应支付金额：" + amountDb);
        }
        log.debug("[澳科支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amountDb + ",第三方响应支付成功标志:" + payStatus + " ,计划成功：ok");
        return result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = Boolean.valueOf(signMd5);
        log.debug("[澳科支付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[澳科支付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}