package dc.pay.business.kuaifubaozhifu;

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
 * July 19, 2019
 */
@ResponsePayHandler("KUAIFUBAOZHIFU")
public final class KuaiFuBaoZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String yftNo     = "yftNo";       // 生成的订单唯一订单号    string(24)    一定存在。一个24位字符串，是此订单在服务器上的唯一编号(订单未匹配成功时"")
    private static final String orderid   = "orderid";     // 您的自定义订单号    string(50)    一定存在。是您在发起付款接口传入的您的自定义订单号(订单未匹配成功时为"")
    private static final String price     = "price";       // 订单定价    string(20)    一定存在。是您在发起付款接口传入的订单价格(订单未匹配成功时为"")
    private static final String realprice = "realprice";   // 实际支付金额    float    一定存在。表示用户实际支付的金额。一般会和price值一致，如果同时存在多个用户支付同一金额，就会和price存在一定差额，差额一般在1-2分钱上下，越多人同时付款，差额越大。
    private static final String orderuid  = "orderuid";    // 您的自定义用户ID    string(100)    如果您在发起付款接口带入此参数，我们会原封不动传回。
    private static final String uid       = "uid";         // 您的商户号    string(100)    在平台的唯一用户标识
    private static final String istype    = "istype";      // 支付类型    int(2)    1：微信；2：支付宝 3:QQ 4:银联 5.快捷
    private static final String notifyurl = "notifyurl";   // 成功回调通知地址    long(20)    商户提供的成功回调通知地址
    private static final String createAt  = "createAt";    // 订单创建时间    long(20)    订单创建时间

    // 秘钥    string(32)    一定存在。我们把使用到的所有参数，连您的Token一起，按参数名字母升序排序。把参数值拼接在一起。做md5-32位加密。
    private static final String signature = "key";

    private static final String RESPONSE_PAY_MSG = "200";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(orderid);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[快付宝支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}", ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
//        orderid + orderuid + yftNo + price + istype + realprice + createAt + uid + token + notifyUrl
        String paramsStr = String.format("%s%s%s%s%s%s%s%s%s%s",
                api_response_params.get(orderid),
                api_response_params.get(orderuid),
                api_response_params.get(yftNo),
                api_response_params.get(price),
                api_response_params.get(istype),
                api_response_params.get(realprice),
                api_response_params.get(createAt),
                api_response_params.get(uid),
                channelWrapper.getAPI_KEY(),
                api_response_params.get(notifyurl));
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[快付宝支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5));
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result      = false;
        String  responseAmount = HandlerUtil.getFen(api_response_params.get(realprice));

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount = HandlerUtil.isAllowAmountt(db_amount, responseAmount, "100");//我平台默认允许一元偏差

        if (checkAmount) {
            my_result = true;
        } else {
            log.error("[快付宝支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[快付宝支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount);
        return my_result;
    }


    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[快付宝支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[快付宝支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}