package dc.pay.business.chaofan;

import java.util.List;
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
import dc.pay.utils.MapUtils;

/**
 * 
 * @author sunny
 * Dec 14, 2018
 */
@ResponsePayHandler("CHAOFAN")
public final class ChaoFanPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

//    参数名称	      参数含义	长度	是否必填	备注
//    version	         版本号	10	是	固定值：1.0
//    merchantId	商户ID	10	是	商户在中心的唯一标识
//    orderNo	      商户订单号	40	是	商户提交的订单号
//    tradeDate	   通知交易日期	8	是	通知交易日期，格式：yyyyMMdd
//    tradeTime	   通知交易时间	6	是	通知交易时间，格式：HHmmss
//    amount	   订单金额	10	是	订单金额 （单位：分）
//    attach	   商户附加信息	60	否	商户扩展信息，原样返回
//    resultCode	交易结果	5	是	0表示成功，非0表示失败
//    sign	              签名数据	32	是	32位小写的组合加密验证串

    private static final String version                       ="version";
    private static final String merchantId                    ="merchantId";
    private static final String orderNo                       ="orderNo";
    private static final String tradeDate                     ="tradeDate";
    private static final String tradeTime                     ="tradeTime";
    private static final String amount                        ="amount";
    private static final String resultCode                    ="resultCode";
    
    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("success");

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(merchantId);
        String ordernumberR = API_RESPONSE_PARAMS.get(orderNo);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.info("[超凡支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
    	api_response_params.put(key, channelWrapper.getAPI_KEY());
    	List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        paramKeys.remove(signature);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
//            if (!pay_productname.equals(paramKeys.get(i)) && !pay_attach.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
            if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        //最后一个&转换成#
        //signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "#" );
        //signSrc.append(key +"="+ channelWrapper.getAPI_KEY());
        signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "" );
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[超凡支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //returncode          交易状态         是            “00” 为成功
        String payStatusCode = api_response_params.get(resultCode);
        String responseAmount = api_response_params.get(amount);
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //实际支付金额  有可能比提交金额少0.01-0.05
        boolean checkAmount =  db_amount.equalsIgnoreCase(responseAmount);//第三方回调金额差额0.05元内
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("0")) {
            my_result = true;
        } else {
            log.error("[超凡支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.info("[超凡支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：0");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.info("[超凡支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.info("[超凡支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}