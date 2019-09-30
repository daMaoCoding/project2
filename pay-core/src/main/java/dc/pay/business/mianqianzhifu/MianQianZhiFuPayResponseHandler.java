package dc.pay.business.mianqianzhifu;

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

/**
 * @author Cobby
 * June 12, 2019
 */
@ResponsePayHandler("MIANQIANZHIFU")
public final class MianQianZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String merchant_id           ="merchant_id"; //    必填    商户ID、在平台首页右边获取商户ID    10000
    private static final String pay_time              ="pay_time";    //    必填    支付成功的    格式为:20181201102435(2018年12月1日10时2分35秒)
    private static final String status                ="status";      //    必填    支付状态，success表示成功,fail表示失败    success
    private static final String order_amount          ="order_amount";//    必填    订单金额    1.00
    private static final String pay_amount            ="pay_amount";  //    必填    实际支付金额    1.00
    private static final String out_trade_no          ="out_trade_no";//    必填    商户订单号    2018062312410711888
    private static final String trade_no              ="trade_no";    //    必填    交易流水号，由系统生成的交易流水号    2018062312410729584
    private static final String fees                  ="fees";        //    必填    手续费，该笔订单的手续费(已在平台余额中扣除)    0.0400
    private static final String paytype               ="paytype";     //    必填    支付方式，支付宝:alipay，微信:wechat    wechat

    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(merchant_id);
        String ordernumberR = API_RESPONSE_PARAMS.get(out_trade_no);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[免签支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        //merchant_id={}&fees={}&order_amount={}&out_trade_no={}&pay_amount={}&pay_time={}&paytype={}&status={}&trade_no={}{key}
        StringBuilder signStr = new StringBuilder();
        signStr.append(merchant_id+"=").append(api_response_params.get(merchant_id)).append("&");
        signStr.append(fees+"=").append(api_response_params.get(fees)).append("&");
        signStr.append(order_amount+"=").append(api_response_params.get(order_amount)).append("&");
        signStr.append(out_trade_no+"=").append(api_response_params.get(out_trade_no)).append("&");
        signStr.append(pay_amount+"=").append(api_response_params.get(pay_amount)).append("&");
        signStr.append(pay_time+"=").append(api_response_params.get(pay_time)).append("&");
        signStr.append(paytype+"=").append(api_response_params.get(paytype)).append("&");
        signStr.append(status+"=").append(api_response_params.get(status)).append("&");
        signStr.append(trade_no+"=").append(api_response_params.get(trade_no));
        signStr.append(channelWrapper.getAPI_KEY());
        String paramsStr =signStr.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[免签支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

     @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //success:成功，其他失败
        String payStatusCode = api_response_params.get(status);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(pay_amount));
        boolean checkAmount =  HandlerUtil.isRightAmount(db_amount,responseAmount,"100");//第三方回调金额差额1元内
        // success 代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("success")) {
            my_result = true;
        } else {
            log.error("[免签支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[免签支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：success");
        return my_result;
    }


    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[免签支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[免签支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}