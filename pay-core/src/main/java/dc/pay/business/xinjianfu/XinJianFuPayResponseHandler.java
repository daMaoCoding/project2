package dc.pay.business.xinjianfu;

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
 * Oct 22, 2018
 */
@ResponsePayHandler("XINJIANFU")
public final class XinJianFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //Name                 Description                           Type               Additional information
    //pid                   商户id                               integer               Required
    //state                 支付状态 1 为成功  其他失败          integer               
    //Name                  商品名称                             String               
    //orderid               订单id                               string                Required
    //trade_no              万付宝订单id                         String               
    //pt_type               1微信， 2支付宝， 3QQ                integer               Required
    //money                 金额                                 decimal number        Default value is 0
    //notify_url            支付结果通知地址                     string                Default value is
    //endtime               订单最后状态时间                     string                Default value is
    //sign                  大写MD5加密                          String        
    private static final String pid                        ="pid";
    private static final String state                      ="state";
    private static final String name                       ="name";
    private static final String orderid                    ="orderid";
//    private static final String trade_no                   ="trade_no";
    private static final String pt_type                    ="pt_type";
    private static final String money                      ="money";
    private static final String notify_url                 ="notify_url";
//    private static final String endtime                    ="endtime";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(pid);
        String ordernumberR = API_RESPONSE_PARAMS.get(orderid);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[新吉安付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        StringBuilder signStr = new StringBuilder();
        signStr.append(key+"=").append(channelWrapper.getAPI_KEY()).append("&");
        signStr.append(orderid+"=").append(api_response_params.get(orderid)).append("&");
        signStr.append(pt_type+"=").append(api_response_params.get(pt_type)).append("&");
        signStr.append(name+"=").append(api_response_params.get(name)).append("&");
        signStr.append(money+"=").append(api_response_params.get(money)).append("&");
        signStr.append(notify_url+"=").append(api_response_params.get(notify_url));
        String paramsStr =signStr.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[新吉安付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //state                 支付状态 1 为成功  其他失败          integer               
        String payStatusCode = api_response_params.get(state);
        //money                 金额                                 decimal number        Default value is 0
        String responseAmount = HandlerUtil.getFen(api_response_params.get(money));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("1")) {
            my_result = true;
        } else {
            log.error("[新吉安付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[新吉安付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[新吉安付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[新吉安付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}