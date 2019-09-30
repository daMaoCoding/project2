package dc.pay.business.shiguangzhifu;

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
 * Apr 13, 2019
 */
@ResponsePayHandler("SHIGUANGZHIFU")
public final class ShiGuangZhiFuResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String platform_trade_no      ="platform_trade_no";//    平台生成的订单ID号    string(24)    一定存在。一个24位字符串，是此订单在本服务器上的唯一编号
//    private static final String transaction_id         ="transaction_id";   //    支付流水号    string(60)    支付宝支付或微信支付的流水订单号
    private static final String orderid                ="orderid";          //    您的自定义订单号    string(50)    一定存在。是您在发起付款接口传入的您的自定义订单号
    private static final String price                  ="price";            //    订单定价    float    一定存在。是您在发起付款接口传入的订单价格
    private static final String realprice              ="realprice";        //    实际支付金额    float    一定存在。表示用户实际支付的金额。一般会和price值一致，如果同时存在多个用户支付同一金额，就会和price存在一定差额，差额一般在1-2分钱上下，越多人同时付款，差额越大。
    private static final String orderuid               ="orderuid";         //    您的自定义用户ID    string(100)    如果您在发起付款接口带入此参数，我们会原封不动传回。
//    private static final String attach                 ="attach";           //    附加内容    string(2048)    将会根据您传入的attch字段原样返回

    //signature    数据签名    32    是    　
    private static final String signature  ="key";

    private static final String RESPONSE_PAY_MSG = "OK";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(orderid);
        if ( StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[时光支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        //orderid +“+”+ orderuid +“+”+ platform_trade_no +“+”+ price +“+”+ realprice +“+”+ token
        StringBuilder signStr = new StringBuilder();
        signStr.append(api_response_params.get(orderid)).append("+");
        signStr.append(api_response_params.get(orderuid)).append("+");
        signStr.append(api_response_params.get(platform_trade_no)).append("+");
        signStr.append(api_response_params.get(price)).append("+");
        signStr.append(api_response_params.get(realprice)).append("+");
        signStr.append(channelWrapper.getAPI_KEY());
        String paramsStr =signStr.toString();
        
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[时光支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //无成功 标识
//        String payStatusCode = api_response_params.get(attach);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(price));

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        //无成功 标识
        if (checkAmount ) {
            my_result = true;
        } else {
            log.error("[时光支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID()+ " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[时光支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount );
        return my_result;
    }

    
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[时光支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[时光支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}