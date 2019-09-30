package dc.pay.business.mifengzhifu;

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
import org.apache.shiro.codec.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@ResponsePayHandler("MIFENGZHIFU")
public final class MiFengZhiFuPayResponseHandler extends PayResponseHandler {
     private final Logger log = LoggerFactory.getLogger(getClass());
     private static final String RESPONSE_PAY_MSG = "success";

      private static final String  merchant_code = "merchant_code" ;       //"100000015",
      private static final String  merchant_order_no = "merchant_order_no" ;       //"20180623105633",
      private static final String  merchant_amount = "merchant_amount" ;       //"0.87",
      private static final String  merchant_amount_orig = "merchant_amount_orig" ;       //"1.00",
      private static final String  merchant_sign = "merchant_sign" ;       //"NWQ5M2IxOTQ5ZmM3MzE3NjNkODAzNjNmN2Q2OGQ1ZmI="


    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(merchant_order_no);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[蜜蜂支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        // base64_encode(md5('merchant_code='.$merchant_code.'&merchant_order_no='.$merchant_order_no.'&merchant_amount='.$request_amount.'&merchant_amount_orig='.$request_amount_orig.'&merchant_md5='.$merchant_md5))

        String paramsStr = String.format("merchant_code=%s&merchant_order_no=%s&merchant_amount=%s&merchant_amount_orig=%s&merchant_md5=%s",
                params.get(merchant_code),
                params.get(merchant_order_no),
                params.get(merchant_amount),
                params.get(merchant_amount_orig),
                channelWrapper.getAPI_KEY());
        String signMd5 = Base64.encodeToString(HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase().getBytes());
        log.debug("[蜜蜂支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }



    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean checkResult = false;
        String payStatus = HandlerUtil.getFen(api_response_params.get(merchant_amount_orig));
        String responseAmount =  HandlerUtil.getFen(api_response_params.get(merchant_amount));
        boolean checkAmount =  HandlerUtil.isRightAmount(amountDb,responseAmount,"100");//第三方回调金额差额100分内

        if (checkAmount && payStatus.equalsIgnoreCase(amountDb)) {
            checkResult = true;
        } else {
            log.error("[蜜蜂支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatus + " ,支付金额：" + responseAmount + " ，应支付金额：" + amountDb);
        }
        log.debug("[蜜蜂支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + checkResult + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amountDb + ",第三方响应支付成功标志:" + payStatus + " ,计划成功：1");
        return checkResult;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(merchant_sign).equalsIgnoreCase(signMd5);
        log.debug("[蜜蜂支付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[蜜蜂支付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}