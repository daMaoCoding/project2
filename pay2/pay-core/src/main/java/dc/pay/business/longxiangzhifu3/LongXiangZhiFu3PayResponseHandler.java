package dc.pay.business.longxiangzhifu3;

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
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Apr 2, 2019
 */
@ResponsePayHandler("LONGXIANGZHIFU3")
public final class LongXiangZhiFu3PayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //字段名             参数名                型别         必填          说明
    //交易状态           pay_state             N(1)         M 交易状态代码，详情请参考pay_summary0 :未交易1 :交易中2 :交易成功3 :交易失败
    //交易状态描述       pay_summary           S(128)       M 交易详细描述
    //厂商交易序号       trade_service_id      S(30)        M 回传厂商交易序号
    //用户代号           customer_id           S(30)        O 用户于厂商的唯一识别编号，申请时授权码有填才通知
    //付费方式           payment_type          S( 15)       M 直接以付費方式的名稱通知
    //金额               amount                N            M 交易金额
    //币别               currency              S(3)         M 直接以币别名称回传
    //交易申请时间       pay_start_time        S(20)        M 日期格式: "Y-m-d H:i:s"
    //交易完成时间       pay_end_time          S(20)        M 日期格式: "Y-m-d H:i:s"
    //验证码             token                 S(32)        M 参考附录C说明，長度為32
    private static final String pay_state                         ="pay_state";
    private static final String pay_summary                       ="pay_summary";
    private static final String trade_service_id                  ="trade_service_id";
//    private static final String customer_id                       ="customer_id ";
    private static final String payment_type                      ="payment_type";
    private static final String amount                            ="amount";
    private static final String currency                          ="currency";
    private static final String pay_start_time                    ="pay_start_time";
    private static final String pay_end_time                      ="pay_end_time";
    
    private static final String trade_seq                      ="trade_seq";
    
    //signature    数据签名    32    是    　
    private static final String signature  ="token";

//    private static final String RESPONSE_PAY_MSG = "{\"return_code\": 1,\"return_msg\": \"资料接收成功\"}";
//    private static final String RESPONSE_PAY_MSG = "{\"return_code\": 1}";
    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
//        String partnerR = API_RESPONSE_PARAMS.get(merchno);
        String ordernumberR = API_RESPONSE_PARAMS.get(trade_service_id);
//        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[龙亨支付3]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        StringBuilder signStr = new StringBuilder();
        signStr.append(api_response_params.get(pay_state));
        signStr.append(api_response_params.get(pay_summary));
        signStr.append(api_response_params.get(trade_seq));
        signStr.append(api_response_params.get(trade_service_id));
        signStr.append(api_response_params.get(payment_type));
        signStr.append(api_response_params.get(amount));
        signStr.append(api_response_params.get(currency));
        signStr.append(api_response_params.get(pay_start_time));
        signStr.append(api_response_params.get(pay_end_time));
        signStr.append(channelWrapper.getAPI_KEY());
        String paramsStr =signStr.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[龙亨支付3]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //交易状态           pay_state             N(1)         M 交易状态代码，详情请参考pay_summary   0 :未交易1 :交易中2 :交易成功3 :交易失败
        String payStatusCode = api_response_params.get(pay_state);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(amount));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        
        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差
        
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("2")) {
            my_result = true;
        } else {
            log.error("[龙亨支付3]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[龙亨支付3]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：2");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[龙亨支付3]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[龙亨支付3]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}