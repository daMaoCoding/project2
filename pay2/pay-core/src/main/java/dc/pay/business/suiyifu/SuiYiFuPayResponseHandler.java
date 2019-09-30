package dc.pay.business.suiyifu;

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
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Jul 3, 2018
 */
@ResponsePayHandler("SUIYIFU")
public final class SuiYiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //参数名                     参数                       说明
    //商户订单号                 orderid                    下单过程中商户系统传入的orderid
    //订单结果                   opstate                    0：支付成功
    //订单金额                   ovalue                     订单实际支付金额，单位元
    //通知时间戳                 systime                    第一次通知时的时间戳，年/月/日 时：分：秒，如2010/04/05 21:50:58
    //MD5 签名                   sign                       32位小写MD5 签名值，GB2312 编码
    //订单号                     sysorderid                 此次订单过程中随意付接口系统内的订单Id
    //订单时间                   completiontime             此次订单过程中随意付接口系统内的订单结束时间。格式为年/月/日 时：分：秒，如2010/04/05 21:50:58
    //银行类型                   type                       银行类型，具体请参考附录1
    //备注信息                   attach                     备注信息，下单中attach 原样返回
    //订单结果说明               msg                        订单结果说明
    //RSA 签名                   RSA_sign                    Base64+GB2312 编码，“+”被换成“_”
    private static final String orderid                        ="orderid";
    private static final String opstate                        ="opstate";
    private static final String ovalue                         ="ovalue";
//    private static final String systime                        ="systime";
//    private static final String sysorderid                     ="sysorderid";
//    private static final String completiontime                 ="completiontime";
//    private static final String type                           ="type";
    private static final String attach                         ="attach";
//    private static final String msg                            ="msg";
//    private static final String RSA_sign                       ="RSA_sign";

    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "opstate=0";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(attach);
        String ordernumberR = API_RESPONSE_PARAMS.get(orderid);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[随意付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        StringBuilder signStr = new StringBuilder();
        signStr.append(orderid+"=").append(api_response_params.get(orderid)).append("&");
        signStr.append(opstate+"=").append(api_response_params.get(opstate)).append("&");
        signStr.append(ovalue+"=").append(api_response_params.get(ovalue));
        signStr.append(channelWrapper.getAPI_KEY());
        String paramsStr =signStr.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[随意付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //0：支付成功
        String payStatusCode = api_response_params.get(opstate);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(ovalue));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("0")) {
            my_result = true;
        } else {
            log.error("[随意付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[随意付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：0");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[随意付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[随意付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}