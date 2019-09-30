package dc.pay.business.hengrun;

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
import dc.pay.utils.MapUtils;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Dec 18, 2018
 */
@ResponsePayHandler("HENGRUN")
public final class HengRunPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //NoticeParams  请求入参    String(2048)    Y   值为JSON格式的参数字符串数据
    private static final String NoticeParams                ="NoticeParams";
    //appID 平台唯一标识，即商户号 String(16)  Y   由平台分配的唯一标识编号
    private static final String appID                 ="appID";
    //outTradeNo    订单号 String(32)  Y   商户系统上的订单号
    private static final String outTradeNo                ="outTradeNo";
    //totalAmount   订单金额    String(14)  Y   单位为分
    private static final String totalAmount                ="totalAmount";
    //payCode   支付状态    String(16)  Y   0000：成功    0011：失败s    0099：未支付
    private static final String payCode              ="payCode";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "SUCCESS";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String myNoticeParams = API_RESPONSE_PARAMS.get(NoticeParams);
        JSONObject parseObject = null;
        try {
            parseObject = JSON.parseObject(myNoticeParams);
        } catch (Exception e) {
            throw new PayException(SERVER_MSG.REQUEST_PAY_CHANNEL_RESULT_ERROR);
        }
        String partnerR = parseObject.getString(appID);
        String ordernumberR = parseObject.getString(outTradeNo);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[恒润]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        Map<String, String> jsonToMap = HandlerUtil.jsonToMap(api_response_params.get(NoticeParams));
        List paramKeys = MapUtils.sortMapByKeyAsc(jsonToMap);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!signature.equals(paramKeys.get(i)) && StringUtils.isNotBlank(jsonToMap.get(paramKeys.get(i)))) {
//                signSrc.append(paramKeys.get(i)).append("=").append(jsonToMap.get(paramKeys.get(i))).append("&");
                signSrc.append(jsonToMap.get(paramKeys.get(i))).append("|");
            }
        }
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[恒润]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        Map<String, String> jsonToMap = HandlerUtil.jsonToMap(api_response_params.get(NoticeParams));
        boolean my_result = false;
        //payCode   支付状态    String(16)  Y   0000：成功        0011：失败        0099：未支付
        String payStatusCode = jsonToMap.get(payCode);
        String responseAmount = jsonToMap.get(totalAmount);
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("0000")) {
            my_result = true;
        } else {
            log.error("[恒润]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[恒润]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：0000");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        Map<String, String> jsonToMap = HandlerUtil.jsonToMap(api_response_params.get(NoticeParams));
        boolean my_result = jsonToMap.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[恒润]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[恒润]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}