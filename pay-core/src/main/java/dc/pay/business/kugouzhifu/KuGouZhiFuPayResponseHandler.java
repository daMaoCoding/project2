package dc.pay.business.kugouzhifu;

import com.alibaba.fastjson.JSON;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Cobby
 * June 04, 2019
 */
@ResponsePayHandler("KUGOUZHIFU")
public final class KuGouZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String amount           ="amount";        //    true    number    198.00    到帐金额
    private static final String order_no         ="order_no";      //    true    String    2019325521251    订单编号
    private static final String plat_num         ="plat_num";      //    true    String    商户订单号
    private static final String app_id           ="app_id";        //    True    String    2019027    商户号
    private static final String sign_type        ="sign_type";     //    True    string    RSA    签名类型
    private static final String pay_type         ="pay_type";      //    true    string    wechat    支付类型
    private static final String status           ="status";        //    True    String    SUCCESS    支付状态
    private static final String back_status      ="back_status";   //    True    String    WAIT_BACK    返单状态
    private static final String complete_time    ="complete_time"; //    True    string    2019-05-18    确认打款时间
    private static final String sign             ="sign";          //    True    string    RSA    签名类型

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(app_id);
        String ordernumberR = API_RESPONSE_PARAMS.get(plat_num);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[酷狗支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
//        amount  ;order_no ; plat_num ;  app_id ; sign_type ; pay_type ; status ; back_status ; complete_time
        Map<String,String > params = new LinkedHashMap<>();
        params.put(amount ,api_response_params.get(amount));
        params.put(order_no ,api_response_params.get(order_no));
        params.put(plat_num ,api_response_params.get(plat_num));
        params.put(app_id ,api_response_params.get(app_id));
        params.put(sign_type ,api_response_params.get(sign_type));
        params.put(pay_type ,api_response_params.get(pay_type));
        params.put(status ,api_response_params.get(status));
        params.put(back_status ,api_response_params.get(back_status));
        params.put(complete_time ,api_response_params.get(complete_time));
        String signMd5 = JSON.toJSONString(params);

        log.debug("[酷狗支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //status        交易状态    1    是    0-未支付    1-支付成功        2-支付失败
        String payStatusCode = api_response_params.get(status);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(amount));

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        // SUCCESS 代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("SUCCESS")) {
            my_result = true;
        } else {
            log.error("[酷狗支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[酷狗支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：SUCCESS");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        String signstr = api_response_params.get(sign);
        boolean my_result = RSASignature.doCheck(signMd5, signstr, channelWrapper.getAPI_PUBLIC_KEY());
        log.debug("[酷狗支付]-[响应支付]-4.RSA公钥校验签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[酷狗支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }


}