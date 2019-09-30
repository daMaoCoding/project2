package dc.pay.business.wangpaizhifu;

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
 * Apr 20, 2019
 */
@ResponsePayHandler("WANGPAIZHIFU")
public final class WangPaiZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //参数名 参数  说明
    //商户订单号   orderid 上行过程中商户系统传入的orderid。
    private static final String orderid                ="orderid";
    //订单结果    opstate 0：支付成功    1: 系统错误
    private static final String opstate                ="opstate";
    //订单金额    ovalue  订单实际支付金额，单位元
    private static final String ovalue                ="ovalue";
    //支付订单号   sysorderid  此次订单过程中王牌支付接口系统内的订单Id
    private static final String sysorderid                ="sysorderid";
    //支付订单时间  systime 此次订单过程中王牌支付接口系统内的订单结束时间。格式为   年/月/日 时：分：秒，如2010/04/05 21:50:58
    private static final String systime                ="systime";
    //备注信息    attach  备注信息，上行中attach原样返回, 同步地址不返回此参数。
    private static final String attach                ="attach";
    //订单结果说明  msg 订单结果说明
    private static final String msg                ="msg";
    //MD5签名   sign2   MD5（orderid+ovalue+key）
    private static final String sign2                ="sign2";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign2";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
       //String api_KEY = channelWrapper.getAPI_KEY();
       //if (null == api_KEY || !api_KEY.contains("-") || api_KEY.split("-").length != 2) {
       //    log.error("[王牌支付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //    throw new PayException("[王牌支付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //}
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
//        String partnerR = API_RESPONSE_PARAMS.get(orderid);
        String ordernumberR = API_RESPONSE_PARAMS.get(orderid);
//        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
        if ( StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[王牌支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        StringBuilder signStr = new StringBuilder();
        signStr.append(api_response_params.get(orderid));
        signStr.append(api_response_params.get(ovalue));
        signStr.append(channelWrapper.getAPI_KEY());
        String paramsStr =signStr.toString();
        
//        //使用对方返回的数据进行签名
//        String paramsStr = String.format(amount+"=%s&"+merchname+"=%s&"+merchno+"=%s&"+openid+"=%s&"+orderno+"=%s&"+paytype+"=%s&"+status+"=%s&"+traceno+"=%s&"+transdate+"=%s&"+transtime+"=%s&%s",
//                api_response_params.get(amount),
//                api_response_params.get(merchname),
//                api_response_params.get(merchno),
//                api_response_params.get(openid),
//                api_response_params.get(orderno),
//                api_response_params.get(paytype),
//                api_response_params.get(status),
//                api_response_params.get(traceno),
//                api_response_params.get(transdate),
//                api_response_params.get(transtime),
//                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[王牌支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //订单结果  opstate 0：支付成功        1: 系统错误
        String payStatusCode = api_response_params.get(opstate);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(ovalue));

        //tony(Tony) 01-17 15:09:28
        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        //boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("0")) {
            my_result = true;
        } else {
            log.error("[王牌支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[王牌支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：0");
        return my_result;
    }

    
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[王牌支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[王牌支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}