package dc.pay.business.newpayzhifu;

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
 * @author Cobby
 * Apr 25, 2019
 */
@ResponsePayHandler("NEWPAYZHIFU")
public final class NewPayZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String cp_order_no              ="cp_order_no";
    private static final String order_amount             ="order_amount";
    private static final String goods_id                 ="goods_id";
    private static final String order_uid                ="order_uid";
    private static final String pay_amount               ="pay_amount";

    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(cp_order_no);
        if ( StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[NewPay支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        //md5("cp_order_no="+cp_order_no+"&goods_id="+goods_id+"&order_amount="+order_amount+"&order_uid="+order_uid+"&pay_amount="+pay_amount+mch_key)
        StringBuilder signStr = new StringBuilder();
        signStr.append(cp_order_no+"=").append(api_response_params.get(cp_order_no)).append("&");
        signStr.append(goods_id+"=").append(api_response_params.get(goods_id)).append("&");
        signStr.append(order_amount+"=").append(api_response_params.get(order_amount)).append("&");
        signStr.append(order_uid+"=").append(api_response_params.get(order_uid)).append("&");
        signStr.append(pay_amount+"=").append(api_response_params.get(pay_amount));
        signStr.append(channelWrapper.getAPI_KEY());
        String paramsStr =signStr.toString();
        
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[NewPay支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        // 交易状态    无
        String responseAmount = api_response_params.get(order_amount);

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        // 交易状态    无
        if (checkAmount ) {
            my_result = true;
        } else {
            log.error("[NewPay支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：无"  + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[NewPay支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:无" );
        return my_result;
    }

    
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[NewPay支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[NewPay支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}