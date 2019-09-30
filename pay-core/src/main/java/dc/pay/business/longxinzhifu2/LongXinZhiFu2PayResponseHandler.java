package dc.pay.business.longxinzhifu2;

import com.alibaba.fastjson.JSON;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author Cobby
 * July 07, 2019
 */
@ResponsePayHandler("LONGXINZHIFU2")
public final class LongXinZhiFu2PayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String customerid = "customerid";  //   商户编号
    private static final String sdpayno    = "sdpayno";     //   商户订单号
    private static final String paytype    = "paytype";     //   支付类型
    private static final String status     = "status";      //   1:成功，其他失败
    private static final String sdorderno  = "sdorderno";   //   商户订单号
    private static final String total_fee  = "total_fee";   //   最多两位小数

    //signature    数据签名    32    是    　
    private static final String signature = "sign";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {

        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);

        String              postdata     = API_RESPONSE_PARAMS.get("postdata");
        Map<String, String> parseMap     = (Map) JSON.parse(postdata);
        String              ordernumberR = parseMap.get(sdorderno);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[龙鑫支付2]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}", ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        String              postdata = api_response_params.get("postdata");
        Map<String, String> parseMap = (Map) JSON.parse(postdata);
//        customerid={value}&status={value}&sdpayno={value}&sdorderno={value}&total_fee={value}&paytype={value}&{apikey}
        StringBuilder signStr = new StringBuilder();
        signStr.append(customerid + "=").append(parseMap.get(customerid)).append("&");
        signStr.append(status + "=").append(parseMap.get(status)).append("&");
        signStr.append(sdpayno + "=").append(parseMap.get(sdpayno)).append("&");
        signStr.append(sdorderno + "=").append(parseMap.get(sdorderno)).append("&");
        signStr.append(total_fee + "=").append(parseMap.get(total_fee)).append("&");
        signStr.append(paytype + "=").append(parseMap.get(paytype)).append("&");
        signStr.append(channelWrapper.getAPI_KEY());
        String paramsStr = signStr.toString();
        String signMd5   = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[龙鑫支付2]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5));
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        String              postdata  = api_response_params.get("postdata");
        Map<String, String> parseMap  = (Map) JSON.parse(postdata);
        boolean             my_result = false;
        //status        交易状态    1    是    0-未支付    1-支付成功        2-支付失败
        String payStatusCode  = parseMap.get(status);
        String responseAmount = HandlerUtil.getFen(parseMap.get(total_fee));

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount = HandlerUtil.isAllowAmountt(db_amount, responseAmount, "100");//我平台默认允许一元偏差

        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("1")) {
            my_result = true;
        } else {
            log.error("[龙鑫支付2]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[龙鑫支付2]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return my_result;
    }


    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        String              postdata  = api_response_params.get("postdata");
        Map<String, String> parseMap  = (Map) JSON.parse(postdata);
        boolean             my_result = parseMap.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[龙鑫支付2]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[龙鑫支付2]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}