package dc.pay.business.yonghengzhifu;

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
 * Jun 06, 2019
 */
@ResponsePayHandler("YONGHENGZHIFU")
public final class YongHengZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String userid                 ="userid";   //   商户号
    private static final String created                ="created";  //   订单时间
    private static final String orderno                ="orderno";  //   订单号
    private static final String alipay_no              ="alipay_no";//   支付宝交易流水号
    private static final String time_paid              ="time_paid";//   支付时间
    private static final String channel                ="channel";  //   支付通道
    private static final String amount                 ="amount";   //   充值金额
    private static final String subject                ="subject";  //   订单标题
    private static final String body                   ="body";     //   订单内容

    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "200";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(userid);
        String ordernumberR = API_RESPONSE_PARAMS.get(orderno);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[永恒支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {

//        //使用对方返回的数据进行签名
        String paramsStr = String.format("%s|%s|%s|%s|%s|%s|%s|%s|%s|%s",
                api_response_params.get(userid),
                api_response_params.get(created),
                api_response_params.get(orderno),
                api_response_params.get(alipay_no),
                api_response_params.get(time_paid),
                api_response_params.get(channel),
                api_response_params.get(amount),
                api_response_params.get(subject),
                api_response_params.get(body),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[永恒支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //status        交易状态   无
//        String payStatusCode = api_response_params.get(status);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(amount));

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        if (checkAmount ) {
            my_result = true;
        } else {
            log.error("[永恒支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：无"  + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[永恒支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志: 无"  + " ,计划成功：1");
        return my_result;
    }

    
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[永恒支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[永恒支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}