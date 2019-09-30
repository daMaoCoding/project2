package dc.pay.business.xingwangzhifu;

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
 * Apr 17, 2019
 */
@ResponsePayHandler("XINGWANGZHIFU")
public final class XingWangZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String tradeno               ="tradeno";    // 商户订单号
    private static final String partner               ="partner";    // 商户ID
    private static final String amount                ="amount";     // 金额，单位为分
    private static final String outtradeno            ="outtradeno"; // 平台订单号
    private static final String inttime               ="inttime";    // 时间戳（时间戳：秒）

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="signinfo";
    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(partner);
        String ordernumberR = API_RESPONSE_PARAMS.get(tradeno);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[星网支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        //    signinfo 是 小写 MD5
        //    签名:signinfo=md5(tradeno=tradeno&outtradeno=outtradeno&aunt=amount&partner=partner&inttime=inttime&key=k
        StringBuilder signStr = new StringBuilder();
        signStr.append(tradeno+"=").append(api_response_params.get(tradeno)).append("&");
        signStr.append(outtradeno+"=").append(api_response_params.get(outtradeno)).append("&");
        signStr.append(amount+"=").append(api_response_params.get(amount)).append("&");
        signStr.append(partner+"=").append(api_response_params.get(partner)).append("&");
        signStr.append(inttime+"=").append(api_response_params.get(inttime)).append("&");
        signStr.append(key + "="+channelWrapper.getAPI_KEY());
        String paramsStr =signStr.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[星网支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        String responseAmount = api_response_params.get(amount);

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        if (checkAmount ) {
            my_result = true;
        } else {
            log.error("[星网支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID()  + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[星网支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount );
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[星网支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[星网支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}