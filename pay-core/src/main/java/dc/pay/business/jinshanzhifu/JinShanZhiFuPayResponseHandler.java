package dc.pay.business.jinshanzhifu;

import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSONObject;
import dc.pay.utils.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;


@ResponsePayHandler("JINSHANZHIFU")
public final class JinShanZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String status           ="status";       //status	    状态	     	是	3支付成功，其他情况不会通知
    private static final String request_no       ="request_no";   //request_no	商户订单号	是	原样返回
    private static final String traceno          ="traceno";      //request_time商户请求时间戳	是	原样返回，商户下单时的时间戳
    private static final String request_time     ="request_time"; //pay_time	支付成功时间戳	是
    private static final String order_no         ="order_no";     //order_no	平台处理单号	是
    private static final String amount           ="amount";       //amount	    金额		    是	实际支付金额
    private static final String merchant_no      ="merchant_no";  //merchant_no	商户号		是	原样返回
    private static final String nonce_str        ="nonce_str";    //nonce_str	随机串		是
    private static final String call_nums        ="call_nums";    //call_nums	通知次数		是	平台在未收到商户接口返回 SUCCESS 字串的情况下，会持续通知5次，每次间隔分别是0，1，2，3，4分钟
    private static final String sign             ="sign";         //sign	    签名		    是

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {

        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        JSONObject data = JSONObject.parseObject(API_RESPONSE_PARAMS.get("data"));
        String orderId = data.getString(request_no);
        if ( StringUtils.isBlank(orderId))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[金山支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,orderId);
        return orderId;
    }


    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        JSONObject data = JSONObject.parseObject(api_response_params.get("data"));
        Map mapTypes = JSON.parseObject(data.toJSONString());
        List paramKeys = MapUtils.sortMapByKeyAsc(mapTypes);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!sign.equals(paramKeys.get(i)) && StringUtils.isNotBlank(mapTypes.get(paramKeys.get(i))+"")) {
                signSrc.append(paramKeys.get(i)).append("=").append(mapTypes.get(paramKeys.get(i))).append("&");
            }
        }

        signSrc.append("key" +"="+ channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[金山支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //status 支付状态  2支付中 3支付成功，只有3时才可以确认支付成功，其他状态均可视为支付中
        JSONObject data = JSONObject.parseObject(api_response_params.get("data"));
        String payStatusCode = data.getString(status);
        String responseAmount = HandlerUtil.getFen(data.getString(amount));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("3")) {
            my_result = true;
        } else {
            log.error("[金山支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[金山支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：3");
        return my_result;
    }




    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        JSONObject data = JSONObject.parseObject(api_response_params.get("data"));
        boolean my_result = data.getString(sign).equalsIgnoreCase(signMd5);
        log.debug("[金山支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }



    @Override
    protected String responseSuccess() {
        log.debug("[金山支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}