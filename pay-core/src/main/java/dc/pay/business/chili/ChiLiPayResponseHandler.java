package dc.pay.business.chili;

import java.net.URLEncoder;
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
 * Dec 19, 2018
 */
@ResponsePayHandler("CHILI")
public final class ChiLiPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //"merchant_order_sn":"50N001456381505180625514131"，订单号
    private static final String merchant_order_sn                ="merchant_order_sn";
    //"total_fee":99，金额，单位：元
    private static final String total_fee                 ="total_fee";
    //"result_code":"200", 状态值
    private static final String result_code                ="result_code";
    //"result_message":"成功",  成功则表示支付成功，其他则失败
    private static final String result_message                ="result_message";
    //"trade_no":"2018092616294901086756739438"，三方流水号
    private static final String trade_no                ="trade_no";
    //"sign":"9F4E2E841C71B2D3F24DB6C5C3711890" 验签
//    private static final String sign                 ="sign";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
//        String partnerR = API_RESPONSE_PARAMS.get(merchno);
        String ordernumberR = API_RESPONSE_PARAMS.get(merchant_order_sn);
//        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
        if ( StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[驰力]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        StringBuilder signSrc = new StringBuilder();
        signSrc.append(merchant_order_sn+"=").append(api_response_params.get(merchant_order_sn)).append("&");
        signSrc.append(result_code+"=").append(api_response_params.get(result_code)).append("&");
        signSrc.append(result_message+"=").append(URLEncoder.encode(api_response_params.get(result_message))).append("&");
        signSrc.append(total_fee+"=").append(api_response_params.get(total_fee)).append("&");
        signSrc.append(trade_no+"=").append(api_response_params.get(trade_no));
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[驰力]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //"result_message":"成功",  成功则表示支付成功，其他则失败
        String payStatusCode = api_response_params.get(result_message);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(total_fee));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("成功")) {
            my_result = true;
        } else {
            log.error("[驰力]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[驰力]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：成功");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[驰力]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[驰力]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}