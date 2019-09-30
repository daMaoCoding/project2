package dc.pay.business.juhemazhifu;

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

/**
 * 
 * @author andrew
 * Jul 27, 2019
 */
@ResponsePayHandler("JUHEMAZHIFU")
public final class JuHeMaZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //返回名称    返回类型    返回说明
    //mch_uid integer 商户ID
    private static final String mch_uid                ="mch_uid";
    //out_trade_no    string  商户订单号
    private static final String out_trade_no                ="out_trade_no";
    //transaction_id  string  平台订单号
    private static final String transaction_id                ="transaction_id";
    //total_fee   double  订单金额
    private static final String total_fee                ="total_fee";
    //real_fee    double  用户实际支付金额
    private static final String real_fee                ="real_fee";
    //pay_type    integer 支付类型：1微信、2支付宝、3未知、4银联支付
    private static final String pay_type                ="pay_type";
    //status  string  订单状态：1已支付、0未付款
    private static final String status                ="status";
    //timestamp   long    服务器系统时间戳毫秒（需于本地时间校验（北京时间），相差正负5分钟）
    private static final String timestamp                ="timestamp";
    //paytime string  支付时间
    private static final String paytime                ="paytime";
    //sign    string  服务端签名
//    private static final String sign                ="sign";
    
//    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
       //String api_KEY = channelWrapper.getAPI_KEY();
       //if (null == api_KEY || !api_KEY.contains("-") || api_KEY.split("-").length != 2) {
       //    log.error("[聚合码支付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //    throw new PayException("[聚合码支付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //}
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(mch_uid);
        String ordernumberR = API_RESPONSE_PARAMS.get(out_trade_no);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[聚合码支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        StringBuilder signStr = new StringBuilder();
        signStr.append(mch_uid+"=").append(api_response_params.get(mch_uid)).append("&");
        signStr.append(out_trade_no+"=").append(api_response_params.get(out_trade_no)).append("&");
        signStr.append(pay_type+"=").append(api_response_params.get(pay_type)).append("&");
        signStr.append(paytime+"=").append(api_response_params.get(paytime)).append("&");
        signStr.append(real_fee+"=").append(api_response_params.get(real_fee)).append("&");
        signStr.append(status+"=").append(api_response_params.get(status)).append("&");
        signStr.append(timestamp+"=").append(api_response_params.get(timestamp)).append("&");
        signStr.append(total_fee+"=").append(api_response_params.get(total_fee)).append("&");
        signStr.append(transaction_id+"=").append(api_response_params.get(transaction_id));
        signStr.append(channelWrapper.getAPI_KEY());
        String paramsStr =signStr.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[聚合码支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //status    string  订单状态：1已支付、0未付款
        String payStatusCode = api_response_params.get(status);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(real_fee));

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        // 1 代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("1")) {
            my_result = true;
        } else {
            log.error("[聚合码支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[聚合码支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[聚合码支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[聚合码支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}