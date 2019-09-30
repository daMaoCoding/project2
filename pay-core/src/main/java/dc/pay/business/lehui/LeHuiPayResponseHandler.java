package dc.pay.business.lehui;

import java.io.UnsupportedEncodingException;
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
 * Dec 5, 2018
 */
@ResponsePayHandler("LEHUI")
public final class LeHuiPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //参数名                类型            必填          说明
    //resultCode            String          是            通信返回码 00-成功 -10-失败
    //message               String          是            返回说明 success-正确
    //merchantID            String          是            商户号
    //tradeNo               String          是            乐汇订单号
    //outTradeNo            String          是            商家订单号（由商家自行生产）
    //payMoney              Int             是            订单总金额，单位为分
    //payTime               String          是            支付时间（yyyyMMddHHmmss）
    //status                String          是            支付结果  000：支付成功   002：支付失败
    //remark                String          否            自定义字段（如果商户下单传入此参数，异步通知参数会加入此参数并加入签名；下单未传入，异步通知不会加入此参数）
    //sign                  String          是            参数签名字符串
//    private static final String resultCode                      ="resultCode";
//    private static final String message                         ="message";
    private static final String merchantID                      ="merchantID";
    private static final String tradeNo                         ="tradeNo";
    private static final String outTradeNo                      ="outTradeNo";
    private static final String payMoney                        ="payMoney";
    private static final String payTime                         ="payTime";
    private static final String status                          ="status";
//    private static final String remark                          ="remark";
//    private static final String sign                            ="sign";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "SUCCESS";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(merchantID);
        String ordernumberR = API_RESPONSE_PARAMS.get(outTradeNo);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[乐汇]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        StringBuilder signStr = new StringBuilder();
        signStr.append(api_response_params.get(merchantID)).append("&");
        signStr.append(api_response_params.get(tradeNo)).append("&");
        signStr.append(api_response_params.get(outTradeNo)).append("&");
        signStr.append(api_response_params.get(payMoney)).append("&");
        signStr.append(api_response_params.get(payTime)).append("&");
        signStr.append(api_response_params.get(status)).append("&");
        signStr.append(channelWrapper.getAPI_KEY());
        String paramsStr =signStr.toString();
        String signMd5 = null;
        try {
            signMd5 = HandlerUtil.getMD5UpperCase(URLEncoder.encode(paramsStr,"UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            log.error("[乐汇]-[响应支付]-2.生成加密URL签名出错，签名出错：{}",e.getMessage(),e);
            throw new PayException(e.getMessage(),e);
        }
        log.debug("[乐汇]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //status    String  是   支付结果          000：支付成功        002：支付失败
        String payStatusCode = api_response_params.get(status);
        String responseAmount = api_response_params.get(payMoney);
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("000")) {
            my_result = true;
        } else {
            log.error("[乐汇]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[乐汇]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：000");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[乐汇]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[乐汇]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}