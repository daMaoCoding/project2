package dc.pay.business.yingjia;

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

@ResponsePayHandler("YINGJIAZHIFU")
public final class YingJiaPayResponseHandler extends PayResponseHandler {
     private final Logger log = LoggerFactory.getLogger(getClass());
     private static final String RESPONSE_PAY_MSG = "SUCCESS";


     private static final String   result = "result";       //: "SUCCESS",
     private static final String   trxMerchantOrderno = "trxMerchantOrderno";       //: "20180606120216",
     private static final String   amount = "amount";       //: "2.00",
     private static final String   trxMerchantNo = "trxMerchantNo";       //: "80076000390",
     private static final String   memberGoods = "memberGoods";       //: "20180606120216",
     private static final String   reCode = "reCode";       //: "1",
     private static final String   hmac = "hmac";       //: "c5e0fe5c9fffee6b31a0c5094e8094c0",
     private static final String   productNo = "productNo";       //: "QQWALLET-JS"
     private static final String   retMes = "retMes";       //: "QQWALLET-JS"



    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(trxMerchantOrderno);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[盈加]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        String pay_sign_str1=String.format("reCode=%s&trxMerchantNo=%s&trxMerchantOrderno=%s&result=%s&productNo=%s&memberGoods=%s&amount=%s&key=%s",
                params.get(reCode),params.get(trxMerchantNo),params.get(trxMerchantOrderno),params.get(result),params.get(productNo),
                params.get(memberGoods),params.get(amount),channelWrapper.getAPI_KEY());
        String pay_md5sign = null;
        pay_md5sign = HandlerUtil.getMD5UpperCase(pay_sign_str1).toLowerCase();
        log.debug("[盈加]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }



    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean checkResult = false;
        String payStatus = api_response_params.get(reCode);
        String payResult = api_response_params.get(result);
        String responseAmount =  HandlerUtil.getFen(api_response_params.get(amount));
        boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        if (checkAmount && payStatus.equalsIgnoreCase("1") && payResult.equalsIgnoreCase("SUCCESS")) {
            checkResult = true;
        } else {
            log.error("[盈加]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatus + " ,支付金额：" + responseAmount + " ，应支付金额：" + amountDb);
        }
        log.debug("[盈加]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + checkResult + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amountDb + ",第三方响应支付成功标志:" + payStatus + " ,计划成功：1");
        return checkResult;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(hmac).equalsIgnoreCase(signMd5);
        log.debug("[盈加]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[盈加]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}