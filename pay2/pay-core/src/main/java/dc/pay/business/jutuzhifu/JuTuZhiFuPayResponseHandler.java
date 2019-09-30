package dc.pay.business.jutuzhifu;

import com.alibaba.fastjson.JSON;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;


/**
 * @author Cobby
 * July 10, 2019
 */
@ResponsePayHandler("JUTUZHIFU")
public final class JuTuZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String mch_id       = "mch_id";            // 商户号     是 String(32) 系统分配的商户号
    private static final String result_code  = "result_code";       // 业务结果   是 String(16) SUCCESS/FAIL
    private static final String real_amount  = "real_amount";       // 实付金额   是 Decimal(16,2)本次交易实际支付的金额，单位为人民币(元)
    private static final String mch_order_no = "mch_order_no";      // 商户订单号  是 String(64) 商户系统的订单号，与请求一致。
//  private static final String method              ="method";            // 接口名称    是 String(32) alipay.sqm.h5
//  private static final String version             ="version";           // 版本信息    否 String(8) 1.0
//  private static final String charset             ="charset";           // 字符集      否 String(8) 若不填写，则默认为 UTF-8
//  private static final String sign_type           ="sign_type";         // 签名方式    否 String(8) 签名类型，与商户分配的密钥类            型一致
//  private static final String nonce_str           ="nonce_str";         // 随机字符串   是 String(32) 随机字符串，不长于 32 位
//  private static final String attach              ="attach";            // 附加数据     否 String(128) 如果下单时有数据，则会将原数 据带上
//  private static final String err_code            ="err_code";          // 错误代码     否 String(32) 详见错误码列表
//  private static final String err_code_des        ="err_code_des";      // 错误代码描述  否 String(128) 错误信息描述
//  private static final String cur_code            ="cur_code";          // 货币种类     否 String(8) 货币类型，符合 ISO4217 标准的三位字母代码，默认人 民币CNY
//  private static final String total_amount        ="total_amount";      // 订单金额     是 Decimal(16,2)本次交易支付的订单金额，单位为人民币（元）
//  private static final String bank_order_no       ="bank_order_no";     // 支付宝交易号  否 String(64) 支付宝交易号
//  private static final String channel_order_no    ="channel_order_no";  // 通道订单号    否 String(64) 通道订单号，取决于通道是否返回订单号
//  private static final String order_no            ="order_no";          // 系统订单号    是 String(64) 系统订单号
//  private static final String order_finish_time   ="order_finish_time"; // 支付完成时间  是 String(14) 支付完成时间， 格式为yyyyMMddhhmmss

    //signature    数据签名    32    是    　
    private static final String signature = "sign";

    private static final String RESPONSE_PAY_MSG = "SUCCESS";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR     = API_RESPONSE_PARAMS.get(mch_id);
        String ordernumberR = API_RESPONSE_PARAMS.get(mch_order_no);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[聚图支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}", ordernumberR);
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
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5   = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[聚图支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5));
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        // SUCCESS :成功，其他失败
        String payStatusCode  = api_response_params.get(result_code);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(real_amount));

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount = HandlerUtil.isAllowAmountt(db_amount, responseAmount, "100");//我平台默认允许一元偏差

        // SUCCESS 代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("SUCCESS")) {
            my_result = true;
        } else {
            log.error("[聚图支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[聚图支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：SUCCESS");
        return my_result;
    }


    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[聚图支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[聚图支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}