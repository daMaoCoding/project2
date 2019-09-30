package dc.pay.business.hewanzhifu;

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
 * June 17, 2019
 */
@ResponsePayHandler("HEWANZHIFU")
public final class HeWanZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String mchid       = "mchid";       //商户号*  是 string 商户号，由平台分配
    private static final String paytype     = "paytype";     //支付类型*
    private static final String amount      = "amount";      //总金额*
    private static final String ordertime   = "ordertime";   //订单支付时间*
    private static final String orderstatus = "orderstatus"; //订单支付状态*   支付状态 1-成功 0-失败
    private static final String orderno     = "orderno";     //平台订单编号*  是 此订单编号为本系统平台的订单编号
    private static final String mchorderno  = "mchorderno";  //商户订单编号*  是 此订单编号为商户自己系统平台的订单编号
    //signature    数据签名    32    是    　
    private static final String signature   = "sign";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR     = API_RESPONSE_PARAMS.get(mchid);
        String ordernumberR = API_RESPONSE_PARAMS.get(mchorderno);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[和万支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}", ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        //mchid={value}&paytype={value}&amount={value}&ordertime={value}&orderstatus={value}&orderno={value}&mchorderno={value}&apikey
        String paramsStr = String.format("mchid=%s&paytype=%s&amount=%s&ordertime=%s&orderstatus=%s&orderno=%s&mchorderno=%s&%s",
                params.get(mchid),
                params.get(paytype),
                params.get(amount),
                params.get(ordertime),
                params.get(orderstatus),
                params.get(orderno),
                params.get(mchorderno),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[和万支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5));
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        // 1 :成功，其他失败
        String payStatusCode  = api_response_params.get(orderstatus);
        String responseAmount = api_response_params.get(amount);

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount = HandlerUtil.isAllowAmountt(db_amount, responseAmount, "100");//我平台默认允许一元偏差

        //1 代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("1")) {
            my_result = true;
        } else {
            log.error("[和万支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[和万支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return my_result;
    }


    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[和万支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[和万支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}