package dc.pay.business.onegopayzhifu;

import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * @author Cobby
 * May 18, 2019
 */
@ResponsePayHandler("ONEGOPAYZHIFU")
public final class OneGoPayZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String trade_no               ="trade_no";//    uuid    查詢支付狀態時需使用此序號
    private static final String amount                 ="amount";//    Integer    支付請求金額
    private static final String out_trade_no           ="out_trade_no";//    String    廠商自訂的訂單號
    private static final String status                 ="status";//    String    狀態碼 success

    //signature    数据签名    32    是    　
    private static final String signature  ="_signature";

    private static final String RESPONSE_PAY_MSG = "ok";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(out_trade_no);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[OneGoPay支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        LinkedHashMap<String,String> params = new LinkedHashMap<>();
        params.put(trade_no,api_response_params.get(trade_no));
        params.put(amount,api_response_params.get(amount));
        params.put(out_trade_no,api_response_params.get(out_trade_no));
        params.put(status,api_response_params.get(status));
        String encode = Hasher.encode(params);
        Base64.Encoder encoder = Base64.getEncoder();
        Mac sha256 = null;
        String signMd5 =null;
        try {
            sha256 = Mac.getInstance("HmacSHA256");
            sha256.init(new SecretKeySpec(channelWrapper.getAPI_KEY().getBytes("UTF8"), "HmacSHA256"));
            signMd5 = encoder.encodeToString(sha256.doFinal(encode.getBytes("UTF8")));
        } catch (Exception e) {
            e.printStackTrace();
        }
        log.debug("[OneGoPay支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //status        交易状态    success -支付成功        2-支付失败
        String payStatusCode = api_response_params.get(status);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(amount));

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        // success 代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("success")) {
            my_result = true;
        } else {
            log.error("[OneGoPay支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[OneGoPay支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：success");
        return my_result;
    }

    
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[OneGoPay支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[OneGoPay支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}