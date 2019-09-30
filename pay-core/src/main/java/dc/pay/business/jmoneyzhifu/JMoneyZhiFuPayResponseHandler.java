package dc.pay.business.jmoneyzhifu;

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
@ResponsePayHandler("JMONEYZHIFU")
public final class JMoneyZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());


    private static final String orderid = "orderid";       //商户订单号  下单过程中商户系统传入的orderid
    private static final String result = "result";        //订单结果    0：未支付 1：支付成功 2：失败
    private static final String amount = "amount";        //支付金额    订单实际支付金额，单位元，保留两位小数
    private static final String systemorderid = "systemorderid"; //订单号      此次订单过程中Jmoney接口系统内的订单Id
    private static final String completetime = "completetime";  //订单时间    此次订单过程中Jmoney接口系统内的订单
//  private static final String notifytime    ="notifytime";     //通知时间    通知时间，长度14位，格式为：yyyymmddhhmmss例如：20170820172323 注：北京时间
//  private static final String attach        ="attach";         //备注信息    备注信息，下单中attach原样返回,编码方式为：UTF8
//  private static final String sourceamount  ="sourceamount";   //提交金额    原始提交金额，单位元，保留两位小数

    private static final String key = "key";
    //signature    数据签名    32    是    　
    private static final String signature = "sign";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(orderid);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[Jmoney支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}", ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        // orderid={0}&result={1}&amount={2}&systemorderid={3}&completetime={4}&key={5}
        StringBuilder signStr = new StringBuilder();
        signStr.append(orderid + "=").append(api_response_params.get(orderid)).append("&");
        signStr.append(result + "=").append(api_response_params.get(result)).append("&");
        signStr.append(amount + "=").append(api_response_params.get(amount)).append("&");
        signStr.append(systemorderid + "=").append(api_response_params.get(systemorderid)).append("&");
        signStr.append(completetime + "=").append(api_response_params.get(completetime)).append("&");
        signStr.append(key + "=").append(channelWrapper.getAPI_KEY());
        String paramsStr = signStr.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[Jmoney支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5));
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //订单结果        0：未支付 1：支付成功 2：失败
        String payStatusCode = api_response_params.get(result);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(amount));

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount = HandlerUtil.isAllowAmountt(db_amount, responseAmount, "100");//我平台默认允许一元偏差

        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("1")) {
            my_result = true;
        } else {
            log.error("[通源]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[通源]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return my_result;
    }


    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[Jmoney支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[Jmoney支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}