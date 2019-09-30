package dc.pay.business.gpay;

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
 * Nov 14, 2018
 */
@ResponsePayHandler("GPAY")
public final class GPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //NotifyUrl会用POST方式传送
    //参数                  说明                  批注
    //RtnCode               回传代码              1:成功,其他为失败
    //RtnMessage            回传讯息              
    //MerTradeID            店家交易编号              
    //MerProductID          店家商品代号              
    //MerUserID             店家消费者ID              
    //Amount                交易金额              
    //PaymentDate           付款时间              
    //Validate              检查码                编码方式请参阅备注二
    private static final String RtnCode                       ="RtnCode";
//    private static final String RtnMessage                    ="RtnMessage";
    private static final String MerTradeID                    ="MerTradeID";
//    private static final String MerProductID                  ="MerProductID";
    private static final String MerUserID                     ="MerUserID";
    private static final String Amount                        ="Amount";
//    private static final String PaymentDate                   ="PaymentDate";
//    private static final String Validate                      ="Validate";

//    private static final String key        ="ValidateKey";
    //signature    数据签名    32    是    　
    private static final String signature  ="Validate";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
//        String partnerR = API_RESPONSE_PARAMS.get(merchno);
        String ordernumberR = API_RESPONSE_PARAMS.get(MerTradeID);
//        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[GPAY]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        StringBuilder signStr = new StringBuilder();
        signStr.append("ValidateKey"+"=").append(channelWrapper.getAPI_KEY().split("-")[0]).append("&");
        signStr.append("HashKey"+"=").append(channelWrapper.getAPI_KEY().split("-")[2]).append("&");
        signStr.append(RtnCode+"=").append(api_response_params.get(RtnCode)).append("&");
        signStr.append("TradeID"+"=").append(api_response_params.get(MerTradeID)).append("&");
        signStr.append("UserID"+"=").append(api_response_params.get(MerUserID)).append("&");
        signStr.append("Money"+"=").append(api_response_params.get(Amount));
        String paramsStr =signStr.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[GPAY]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //RtnCode   回传代码    1:成功,其他为失败
        String payStatusCode = api_response_params.get(RtnCode);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(Amount));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("1")) {
            my_result = true;
        } else {
            log.error("[GPAY]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[GPAY]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[GPAY]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[GPAY]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}