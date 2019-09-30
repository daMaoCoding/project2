package dc.pay.business.hongfuzhifu;

import com.alibaba.fastjson.JSON;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RsaUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author cobby
 * May 22, 2019
 */
@ResponsePayHandler("HONGFUZHIFU")
public final class HongFuZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String merchant_code         ="merchant_code";     //     String(12)    √    参数名称：商家号    商户签约时，掌付分配给商家的唯一身份标识。例如：800003004321
    private static final String order_no              ="order_no";          //     String(64)    √    参数名称：商家订单号    商家网站生成的订单号，由商户保证其唯一性，由字母、数字、下划线组成。
    private static final String order_amount          ="order_amount";      //     Number(13,2)  √    参数名称：商家订单金额    以元为单位，精确到小数点后两位.例如：12.01
    private static final String trade_status          ="trade_status";      //     String(7)     √    参数名：订单状态    取值为“SUCCESS”，代表订单交易成功
    private static final String bank_seq_no           ="bank_seq_no";
    private static final String extra_return_param    ="extra_return_param";//     String(100)    ×    参数名称：回传参数    商户如果支付请求是传递了该参数，则通知商户支付成功时会回传该参数
    private static final String interface_version     ="interface_version"; //     String(10)    √    参数名称：接口版本    固定值：V3.0(大写)
    private static final String notify_id             ="notify_id";         //     String(100)   √    参数名：通知校验ID    此版本不需要校验，但是参数依然保留
    private static final String notify_type           ="notify_type";       //     String(14)    √    参数名：通知方式    固定值：offline_notify
    private static final String order_time            ="order_time";        //     Date          √    参数名称：商家订单时间    时间格式：yyyy-MM-dd HH:mm:ss
    private static final String trade_no              ="trade_no";          //     String(30)    √    参数名：订单号
    private static final String trade_time            ="trade_time";        //     Date          √    参数名：掌付订单时间    格式：yyyy-MM-dd HH:mm:ss
    private static final String sign_type             ="sign_type";         //     String(10)    √    参数名称：签名方式1.取值为：RSA或RSA-S2.该字段不参与签名

    //sign    数据签名    32    是    　
    private static final String sign            ="sign";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {

        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(order_no);
        if ( StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[鸿付支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        StringBuilder signStr = new StringBuilder();
        if(null != params.get(bank_seq_no) && StringUtils.isNotBlank(params.get(bank_seq_no))) {
            signStr.append("bank_seq_no=").append(params.get(bank_seq_no)).append("&");
        }
        if(null !=params.get(extra_return_param) && StringUtils.isNotBlank(params.get(extra_return_param))) {
            signStr.append("extra_return_param=").append(params.get(extra_return_param)).append("&");
        }
        signStr.append("interface_version=").append(params.get(interface_version)).append("&");
        signStr.append("merchant_code=").append(params.get(merchant_code)).append("&");
        signStr.append("notify_id=").append(params.get(notify_id)).append("&");
        signStr.append("notify_type=").append(params.get(notify_type)).append("&");
        signStr.append("order_amount=").append(params.get(order_amount)).append("&");
        signStr.append("order_no=").append(params.get(order_no)).append("&");
        signStr.append("order_time=").append(params.get(order_time)).append("&");
        signStr.append("trade_no=").append(params.get(trade_no)).append("&");
        signStr.append("trade_status=").append(params.get(trade_status)).append("&");
        signStr.append("trade_time=").append(params.get(trade_time));

        String signInfo =signStr.toString();
        boolean result = false;

        if("RSA-S".equals(params.get(sign_type))){ // sign_type = "RSA-S"
            String wpay_public_key = channelWrapper.getAPI_PUBLIC_KEY();
            result = RsaUtil.validateSignByPublicKey(signInfo, wpay_public_key, params.get(sign));    // 验签   signInfo财猫快汇返回的签名参数排序， wpay_public_key财猫快汇公钥， wpaySign财猫快汇返回的签名
        }

        log.debug("[鸿付支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(result));
        return String.valueOf(result);
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //status        交易状态    1    是    0-未支付    1-支付成功        2-支付失败
        String payStatusCode = api_response_params.get(trade_status);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(order_amount));

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("SUCCESS")) {
            my_result = true;
        } else {
            log.error("[鸿付支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[鸿付支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：SUCCESS");
        return my_result;
    }



    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        Boolean my_result =   Boolean.valueOf(signMd5);
        log.debug("[鸿付支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }


    @Override
    protected String responseSuccess() {
        log.debug("[鸿付支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}