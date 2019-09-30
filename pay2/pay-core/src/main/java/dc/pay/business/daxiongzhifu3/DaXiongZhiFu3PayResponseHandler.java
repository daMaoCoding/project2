package dc.pay.business.daxiongzhifu3;

import com.alibaba.fastjson.JSON;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.business.daxiongzhifu.HmacSHA1Signature;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.SignatureException;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author andrew   @author Cobby
 * Aug 21, 2019
 */
@ResponsePayHandler("DAXIONGZHIFU3")
public final class DaXiongZhiFu3PayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String trade_status = "trade_status";//返回状态码      是    String    TRADE_SUCCESS    支付成功
    private static final String total_amount = "total_amount";//支付金额        是    String    1.0    单位：元
    private static final String out_trade_no = "out_trade_no";//商户订单号      是    String    20180731152231034308614215295961    商户订单号,64个字符以内、只能包含字母、数字、下划线；需保证在商户端不重复
//  private static final String  trade_no                ="trade_no"       ;//系统订单号    是    String    2018073121001004710200572763    支付宝系统订单号
//  private static final String  notify_id               ="notify_id"      ;//通知ID        是    String    fd2358b23f4a728298b4766da18916dlha    支付宝通知ID
//  private static final String  payment_time            ="payment_time"   ;//支付时间        是    String    2018-11-19 20:59:27    支付时间

    //signature    数据签名    32    是    　
    private static final String signature = "signature";

    private static final String RESPONSE_PAY_MSG = "SUCCESS";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR     = API_RESPONSE_PARAMS.get(trade_status);
        String ordernumberR = API_RESPONSE_PARAMS.get(out_trade_no);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[大熊支付3]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}", ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        List          paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc   = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!signature.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                String paramStr = null;
                try {
                    paramStr = URLEncoder.encode(api_response_params.get(paramKeys.get(i)), "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                signSrc.append(paramKeys.get(i)).append("=").append(paramStr).append("&");
            }
        }
        //删除最后一个字符
        signSrc.deleteCharAt(signSrc.length() - 1);
        String            paramsStr         = signSrc.toString();
        HmacSHA1Signature hmacSHA1Signature = new HmacSHA1Signature();
        String            signMd5           = null;
        try {
            signMd5 = hmacSHA1Signature.doSign(paramsStr, channelWrapper.getAPI_KEY(), "UTF-8");
        } catch (SignatureException e) {
            e.printStackTrace();
        }
        log.debug("[大熊支付3]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5));
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        // TRADE_SUCCESS :成功，其他失败
        String payStatusCode  = api_response_params.get(trade_status);
        String responseAmount = api_response_params.get(total_amount);

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount = HandlerUtil.isAllowAmountt(db_amount, responseAmount, "100");//我平台默认允许一元偏差

        //TRADE_SUCCESS 代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("TRADE_SUCCESS")) {
            my_result = true;
        } else {
            log.error("[大熊支付3]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[大熊支付3]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：TRADE_SUCCESS");
        return my_result;
    }


    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[大熊支付3]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[大熊支付3]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}