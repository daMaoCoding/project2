package dc.pay.business.kuaimazhifu;

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
import dc.pay.utils.MapUtils;

/**
 * @author Cobby
 * Apr 22, 2019
 */
@ResponsePayHandler("KUAIMAZHIFU")
public final class KuaiMaZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());
//callback_url??data={"status":"success","order_on":"系统订单号","out_trade_no":"商户订单号"，"amount":"订单金额"，"sign":"订签名串 "，
// "param":{"m_id":"xxx","pay_aisle":"xxx","out_trade_no":"xxx","amount":"xxx","callback_url":"xxx","success_url":"xxx"}}
    private static final String m_id                       ="param[m_id]";   //商户编号
    private static final String pay_aisle                  ="param[pay_aisle]";    //订单号
    private static final String amount                     ="param[amount]";
    private static final String out_trade_no               ="param[out_trade_no]";
    private static final String callback_url               ="param[callback_url]";
    private static final String success_url                ="param[success_url]";
    private static final String status                     ="status";

    //signature    数据签名    32    是    　
    private static final String signature  ="sign";
    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
//        String param = API_RESPONSE_PARAMS.get("param");
//        Map<String, String> mapParams = HandlerUtil.jsonToMap(param);
        String partnerR = API_RESPONSE_PARAMS.get(m_id);
        String ordernumberR = API_RESPONSE_PARAMS.get(out_trade_no);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[快马支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
//        String param = API_RESPONSE_PARAMS.get("param");
//        Map<String, String> mapParams = HandlerUtil.jsonToMap(param);
        //m_id=xxxpay_aisle=xxx=out_trade_no=xxx=amount=xxx=callback_url=xxx=success_ur=xxx
        StringBuffer signSrc= new StringBuffer();
        signSrc.append("m_id"+"=").append(api_response_params.get(m_id));
        signSrc.append("pay_aisle"+"=").append(api_response_params.get(pay_aisle));
        signSrc.append("out_trade_no"+"=").append(api_response_params.get(out_trade_no));
        signSrc.append("amount"+"=").append(api_response_params.get(amount));
        signSrc.append("callback_url"+"=").append(api_response_params.get(callback_url));
        signSrc.append("success_url"+"=").append(api_response_params.get(success_url));
        signSrc.append(channelWrapper.getAPI_KEY());
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[快马支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //returncode          交易状态         是            “00” 为成功
        String payStatusCode = api_response_params.get(status);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(amount));

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        if (checkAmount && payStatusCode.equalsIgnoreCase("success")) {
            my_result = true;
        } else {
            log.error("[快马支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[快马支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：success");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[快马支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[快马支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}