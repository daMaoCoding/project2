package dc.pay.business.pipifuzhifu;

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
 * June 10, 2019
 */
@ResponsePayHandler("PIPIFUZHIFU")
public final class PiPiFuZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private static final String zp_mer_id             ="zp_mer_id";           //    商户号    是    商户号
    private static final String zp_order_id           ="zp_order_id";         //    商户订单号    是     商户提交的订单号
    private static final String zp_transaction_id     ="zp_transaction_id";   //    平台订单号    是    真皮平台内部生成的订单号
//  private static final String zp_desc               ="zp_desc";             //    商品名称    是    Utf-8编码
    private static final String zp_order_amount_real  ="zp_order_amount_real";//    实际交易金额(float)    是    用户实际用户实际付款款金额(都为两位小数的金额，例:99.98,499.91)，  建议入账请按实际金额入账为妥
    private static final String zp_order_amount       ="zp_order_amount";     //    商户提交的整数金额（int）     是    商户提交的整数金额（必须是整数，例如:100,300）
//  private static final String zp_attch              ="zp_attch";            //    附加信息    是    原样返回
    private static final String zp_status_code        ="zp_status_code";      //    订单状态    是    【200代表支付成功】
//  private static final String zp_datetime           ="zp_datetime";         //    支付时间    是    支付成功时的时间，unix时间戳
//  private static final String zp_timezone           ="zp_timezone";         //    时区    是    时区

    //signature    数据签名    32    是    　
    private static final String signature  ="zp_sign";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {

        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(zp_order_id);
        if ( StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[皮皮付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        //zp_mer_id=商户号&zp_order_amount=请求金额&zp_order_amount_real=实际金额&zp_order_id=商户订单号&zp_status_code=订单状态&zp_transaction_id=平台订单号
        String signMd5 = String.format("zp_mer_id=%s&zp_order_amount=%s&zp_order_amount_real=%s&zp_order_id=%s&zp_status_code=%s&zp_transaction_id=%s",
                api_response_params.get(zp_mer_id),
                api_response_params.get(zp_order_amount),
                api_response_params.get(zp_order_amount_real),
                api_response_params.get(zp_order_id),
                api_response_params.get(zp_status_code),
                api_response_params.get(zp_transaction_id));
        log.debug("[皮皮付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //status        交易状态    1    是    0-未支付    200-支付成功        2-支付失败
        String payStatusCode = api_response_params.get(zp_status_code);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(zp_order_amount_real));

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        // 200 代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("200")) {
            my_result = true;
        } else {
            log.error("[皮皮付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[皮皮付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：200");
        return my_result;
    }

    
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = RSAUtils.verifyByPublicKey(api_response_params.get(signature), channelWrapper.getAPI_PUBLIC_KEY(), signMd5);
        log.debug("[皮皮付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[皮皮付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}