package dc.pay.business.xinguaishouzhifu;

import com.alibaba.fastjson.JSON;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;


/**
 * @author Cobby
 * July 24, 2019
 */
@ResponsePayHandler("XINGUAISHOUZHIFU")
public final class XinGuaiShouZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String merchant_code = "merchant_code";// Int(10)    √    参数名称：商户ID    商户签约时，MonsterPay分配给商户的唯一身份标识    例如：1111110166或者11180
    private static final String money         = "money";// Number(6)    √    参数名称：商户订单金额 以元为单位，精确到小数点后两位.例如：12.01
    private static final String order_sn      = "order_sn";// String(64)    √    参数名称：商户订单号商户网站生成的订单号，由商户保证其唯一性，由字母、数字、下划线组成。
    private static final String status        = "status";// String(64)    √    参数名：订单状态    取值为“1”，代表订单交易成功
    //  private static final String submit_money           ="submit_money"    ;// Number(6)    √    参数名称：商户原始提交订单金额以元为单位，精确到小数点后两位.例如：12.01
//  private static final String trade_sn               ="trade_sn"        ;// String(64)    √    参数名称：上游订单号    MonsterPay流水产生的订单号。
//  private static final String payment_time           ="payment_time"    ;// String    √    参数名称：商户订单时间    时间格式：10位时间戳
//  private static final String remark                 ="remark"          ;// Date    √    参数名称：回传参数    商户如果支付请求是传递了该参数，则通知商户支付成功时会回传该参数不为空参与签名
    //signature    数据签名    32    是    　
    private static final String signature     = "sign";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR     = API_RESPONSE_PARAMS.get(merchant_code);
        String ordernumberR = API_RESPONSE_PARAMS.get(order_sn);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[新怪兽支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}", ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        List          paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc   = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!signature.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        //删除最后一个字符
        signSrc.deleteCharAt(signSrc.length() - 1);
        String signMd5 = signSrc.toString();
        log.debug("[新怪兽支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5));
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        // 1 :成功，其他失败
        String payStatusCode  = api_response_params.get(status);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(money));

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount = HandlerUtil.isAllowAmountt(db_amount, responseAmount, "100");//我平台默认允许一元偏差

        //1 代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("1")) {
            my_result = true;
        } else {
            log.error("[新怪兽支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[新怪兽支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        byte[]  publicKey = Base64.decodeBase64(channelWrapper.getAPI_PUBLIC_KEY());
        byte[]  bytes     = Base64.decodeBase64(api_response_params.get(signature));
        boolean my_result = false;
        try {
            my_result = RSACoder.verify(signMd5.getBytes(), publicKey, bytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        log.debug("[新怪兽支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[新怪兽支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}