package dc.pay.business.onepayzhifu;

/**
 * ************************
 * @author tony 3556239829
 */

import com.alibaba.fastjson.JSON;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.business.caifubao.StringUtil;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import org.apache.commons.lang.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
@ResponsePayHandler("ONEPAYZHIFU")
public final class OnePayZhiFuPayResponseHandler extends PayResponseHandler {
      private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("success");

    private static final String order_no = "order_no";       // "20190102140313971963",
    private static final String amount   = "amount";         // "2.00",
    private static final String status   = "status";         // "PS_PAYMENT_SUCCESS"

//  private static final String   payment_channel = "payment_channel";// "UNIONPAY",
//  private static final String   payment_id      = "payment_id";     // "190102140316491000",
//  private static final String   body            = "body";           // "null",
//  private static final String   app_id          = "app_id";         // "3799",
//  private static final String   finish_time     = "finish_time";    // "20190102140619",

    private static final String amountFee       = "amountFee";       // 金额    Number    NO    订单总金额
    private static final String merchantTradeId = "merchantTradeId"; // 商户订单号    Char(50)    NO    商户提供唯一订单号
    private static final String tradeStatus     = "tradeStatus";     // 支付结果    Char(10)    NO    支付成功:    PS_PAYMENT_SUCCESS    支付失败:    PS_PAYMENT_FAIL    处理中：    PS_GENERATE（请使用查询接口确认状态）
//  private static final String   version         = "version";         // 版本号     Char(2)    NO    1.0
//  private static final String   signType        = "signType";        // 签名类型    Char(5)    NO    RSA
//  private static final String   merchantId      = "merchantId";      // 商户号    Number    NO    易付提供唯一商户号
//  private static final String   pwTradeId       = "pwTradeId";       // 易付订单号    Char(50)    NO    易付提供唯一订单号
//  private static final String   currency        = "currency";        // 币种    Char(3)    NO    CNY
//  private static final String   payEndTime      = "payEndTime";      // 支付完成时间    Char(20)    NO    YYYY-MM-DD HH:MM:SS    以中国时区为准 UTC+8


    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(order_no);
        if (StringUtil.isBlank(ordernumberR)) {
            ordernumberR = API_RESPONSE_PARAMS.get(merchantTradeId);
        }
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[onepay支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        // opstate=opstate值&orderid=orderid值&ovalue=ovalue值&parter=商家接口调用ID&key=商家
        Boolean pay_md5sign = Boolean.FALSE;
        String serializationParam=SignatureUtil.generateSignContent(params);
        pay_md5sign =SignatureUtil.rsaYanQian(channelWrapper.getAPI_PUBLIC_KEY(),serializationParam,params.get("sign"),"RSA");
        log.debug("[onepay支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign.toString();
    }



    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean checkResult    = false;
        String  payStatus      = null;
        String  responseAmount = null;
        if (HandlerUtil.isYLKJ(channelWrapper)) {
            payStatus = api_response_params.get(tradeStatus);
            responseAmount = HandlerUtil.getFen(api_response_params.get(amountFee));
        } else {
            payStatus = api_response_params.get(status);
            responseAmount = HandlerUtil.getFen(api_response_params.get(amount));
        }

        boolean checkAmount =  HandlerUtil.isRightAmount(amountDb,responseAmount,"100");//第三方回调金额差额1元内
        if (checkAmount && payStatus.equalsIgnoreCase("PS_PAYMENT_SUCCESS")) {
            checkResult = true;
        } else {
            log.error("[onepay支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatus + " ,支付金额：" + responseAmount + " ，应支付金额：" + amountDb);
        }
        log.debug("[onepay支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + checkResult + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amountDb + ",第三方响应支付成功标志:" + payStatus + " ,计划成功：PS_PAYMENT_SUCCESS");
        return checkResult;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result =Boolean.valueOf(signMd5);
        log.debug("[onepay支付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[onepay支付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}