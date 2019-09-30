package dc.pay.business.dianfupayzhifu;

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
 * July 25, 2019
 */
@ResponsePayHandler("DIANFUPAYZHIFU")
public final class DianFuPayZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String orderId    = "orderId";// 商户订单号    VARCHAR(32)    必填    32位以下的订单号
    private static final String totalAmt   = "totalAmt";// 支付金额     Number(13,1)    必填    格式：12.0 保留1位小数
    private static final String status     = "status";// 订单状态     VARCHAR(2)    必填    Y:支付成功，N:支付失败
    private static final String trandeNo   = "trandeNo";// 交易订单号   VARCHAR(30)    必填    交易订单号
    private static final String encodeType = "encodeType";// 签名方式     VARCHAR(2)    必填    md5加密:md5
    private static final String trandeTime = "trandeTime";// 处理订单时间  VARCHAR(14)    必填    yyyyMMddHHmmss
    private static final String attach     = "attach";// 商户数据包    VARCHAR(200)        对应订单支付接口的【商户数据包—Attach】参数

    private static final String key       = "key";
    //signature    数据签名    32    是    　
    private static final String signature = "signature";

    private static final String RESPONSE_PAY_MSG = "000000";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR     = API_RESPONSE_PARAMS.get(trandeNo);
        String ordernumberR = API_RESPONSE_PARAMS.get(orderId);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[点付支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}", ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        //MD5.sign(orderId=XXX&totalAmt=XXX&status=XXX&trandeNo=XXX&encodeType=md5&trandeTime=XXX&attach=XXX&key=XXX)
        StringBuilder signStr = new StringBuilder();
        signStr.append(orderId + "=").append(api_response_params.get(orderId)).append("&");
        signStr.append(totalAmt + "=").append(api_response_params.get(totalAmt)).append("&");
        signStr.append(status + "=").append(api_response_params.get(status)).append("&");
        signStr.append(trandeNo + "=").append(api_response_params.get(trandeNo)).append("&");
        signStr.append(encodeType + "=").append(api_response_params.get(encodeType)).append("&");
        signStr.append(trandeTime + "=").append(api_response_params.get(trandeTime)).append("&");
        signStr.append(attach + "=").append(api_response_params.get(attach)).append("&");
        signStr.append(key + "=").append(channelWrapper.getAPI_KEY());
        String paramsStr = signStr.toString();

        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[点付支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5));
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        // Y :成功，其他失败
        String payStatusCode  = api_response_params.get(status);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(totalAmt));

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount = HandlerUtil.isAllowAmountt(db_amount, responseAmount, "100");//我平台默认允许一元偏差

        //Y 代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("Y")) {
            my_result = true;
        } else {
            log.error("[点付支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[点付支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：Y");
        return my_result;
    }


    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[点付支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[点付支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}