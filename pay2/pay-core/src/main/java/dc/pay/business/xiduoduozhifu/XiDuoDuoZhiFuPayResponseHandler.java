package dc.pay.business.xiduoduozhifu;

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
import dc.pay.utils.Sha1Util;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Feb 27, 2019
 */
@ResponsePayHandler("XIDUODUOZHIFU")
public final class XiDuoDuoZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //名称  类型  参数说明
    //merAcount   String(32)  商户标识
    private static final String merAccount                ="merAccount";
    //payOrderNo  String(50)  平台唯一流水号
//    private static final String payOrderNo                ="payOrderNo";
    //orderNo String(32)  商户唯一订单号
    private static final String orderNo                ="orderNo";
    //amount  String  订单金额，以元为单位
    private static final String amount                ="amount";
    //orderStatus String(20)  订单状态    SUCCESS("交易成功"),    FAILED("交易失败")
    private static final String orderStatus                ="orderStatus";
    //sign    String(200) 签名信息，商户使用SHA1对sign以外的其他参数进行字母排序后串成字符串进行签名，如sign=SHA1(amount+…+merKey)
//    private static final String sign                 ="sign";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "SUCCESS";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
       //String api_KEY = channelWrapper.getAPI_KEY();
       //if (null == api_KEY || !api_KEY.contains("-") || api_KEY.split("-").length != 2) {
       //    log.error("[喜多多支付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //    throw new PayException("[喜多多支付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //}
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(merAccount);
        String ordernumberR = API_RESPONSE_PARAMS.get(orderNo);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[喜多多支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        Map<String, String> jsonToMap = handlerUtil.jsonToMap(api_response_params.get("data"));
        List paramKeys = MapUtils.sortMapByKeyAsc(jsonToMap);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!signature.equals(paramKeys.get(i)) && StringUtils.isNotBlank(jsonToMap.get(paramKeys.get(i)))) {
//                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
                signSrc.append(jsonToMap.get(paramKeys.get(i)));
            }
        }
        signSrc.append(api_key);
        String paramsStr = signSrc.toString();
//        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        String signMd5 = Sha1Util.getSha1(paramsStr);
        log.debug("[喜多多支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        Map<String, String> jsonToMap = handlerUtil.jsonToMap(api_response_params.get("data"));
        
        boolean my_result = false;
        //orderStatus   String(20)  订单状态        SUCCESS("交易成功"),        FAILED("交易失败")
        String payStatusCode = jsonToMap.get(orderStatus);
        String responseAmount = HandlerUtil.getFen(jsonToMap.get(amount));

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        //boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("SUCCESS")) {
            my_result = true;
        } else {
            log.error("[喜多多支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[喜多多支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：SUCCESS");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        Map<String, String> jsonToMap = handlerUtil.jsonToMap(api_response_params.get("data"));
        boolean my_result = jsonToMap.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[喜多多支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[喜多多支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}