package dc.pay.business.zhifutong;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.HmacSha256Util;
import dc.pay.utils.MapUtils;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Jul 11, 2018
 */
@ResponsePayHandler("ZHIFUTONG")
public final class ZhiFuTongPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //参数名             必选      参数值       最大长度        备注
    //appid              是                     （分配）        商户分配的appid
    //pay_type           是                     1               支付方式。1：h5支付，2：PC支付。默认为h5支付
    //timestamp          是                     11              发送请求时间戳。11位
    //nonce_str          是                     32              随机字符串
    //sign               是                                     签名
    //biz_content        是                                     异步通知商户的业务参数，内容为json字符串
    private static final String appid                        ="appid";
//    private static final String pay_type                     ="pay_type";
//    private static final String timestamp                    ="timestamp";
//    private static final String nonce_str                    ="nonce_str";
    private static final String biz_content                  ="biz_content";
    //异步通知业务参数（biz_content）
    //payway                 1.支付宝（目前只支持1）
    //trade_no               支付内部交易号
    //out_trade_no           统一下单时提交的商户订单号
    //amount                 用户实际支付的金额
    //params                 额外参数。提交什么返回什么。
    //trade_status           交易状态（3：交易成功，4：交易超时关闭）
    //request_id             请求号，每次请求都不同
//    private static final String payway                               ="payway";
//    private static final String trade_no                             ="trade_no";
    private static final String out_trade_no                         ="out_trade_no";
    private static final String amount                               ="amount";
//    private static final String params                               ="params";
    private static final String trade_status                         ="trade_status";
//    private static final String request_id                           ="request_id";

    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(appid);
        JSONObject parseObject = null;
        try {
            parseObject = JSON.parseObject(API_RESPONSE_PARAMS.get(biz_content));
        } catch (Exception e) {
            throw new PayException(SERVER_MSG.REQUEST_PAY_CHANNEL_RESULT_ERROR);
        }
        String ordernumberR = parseObject.getString(out_trade_no);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[支付通]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!signature.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        signSrc.append("key=" + api_key);
        String paramsStr = signSrc.toString();
        String signMd5 = HmacSha256Util.digest(paramsStr, api_key).toLowerCase();
        log.debug("[支付通]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
    	JSONObject parseObject = JSON.parseObject(API_RESPONSE_PARAMS.get(biz_content));
        boolean my_result = false;
        //trade_status           交易状态（TRADE_SUCCESS）
        String payStatusCode = parseObject.getString(trade_status);
        String responseAmount = HandlerUtil.getFen(parseObject.getString(amount));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("TRADE_SUCCESS")) {
            my_result = true;
        } else {
            log.error("[支付通]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[支付通]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：TRADE_SUCCESS");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[支付通]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[支付通]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}