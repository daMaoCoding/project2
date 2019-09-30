package dc.pay.business.fulezhifu;

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
 * Jun 14, 2019
 */
@ResponsePayHandler("FULEZHIFU")
public final class FuLeZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //#   参数名 含义  类型  说明
    //1   order_id    发起请求支付的订单号  string(50)  一定存在。是用户在发起付款接口传入的您的自定义订单号
    private static final String order_id                     ="order_id";
    //2   price   用户实际成功支付金额  float   一定存在。是用户实际成功支付金额，我方实际收到的支付金额（你方以此金额入帐）
    private static final String price                     ="price";
    //3   txnTime 用户成功支付的时间   string(50)  一定存在。用户成功支付的时间
    private static final String txnTime                     ="txnTime";
    //4   sign    32位小写md5签名  string(32)  一定存在。传入的订单号 order_id，传入的支付金额 price，支付的时间 txnTime，用户的key，依次连接起来根据md5-32位加密，取字符串小写,得到 sign 。
//    private static final String sign                     ="sign";

    //md5验证签名串    sign    varchar(32) 参照签名方法
//    private static final String sign                     ="sign";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
//        String partnerR = API_RESPONSE_PARAMS.get(customerid);
        String ordernumberR = API_RESPONSE_PARAMS.get(order_id);
        
//        String ordernumberR = API_RESPONSE_PARAMS.get(fxddh);
//        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[富乐支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        StringBuffer signSrc= new StringBuffer();
//        一定存在。传入的订单号 ，传入的支付金额 ，支付的时间 ，用户的key，依次连接起来根据md5-32位加密，取字符串小写,得到 sign 。
        signSrc.append(api_response_params.get(order_id));
        signSrc.append(api_response_params.get(price));
        signSrc.append(api_response_params.get(txnTime));
        signSrc.append(api_key);
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[富乐支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {

        boolean my_result = false;
        //1:成功，其他失败
//        String payStatusCode = api_response_params.get(status);
        String payStatusCode = "无";
        String responseAmount = HandlerUtil.getFen(api_response_params.get(price));

        //tony(Tony) 01-17 15:09:28
        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("无")) {
            my_result = true;
        } else {
            log.error("[富乐支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[富乐支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：无");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {

        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[富乐支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[富乐支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}