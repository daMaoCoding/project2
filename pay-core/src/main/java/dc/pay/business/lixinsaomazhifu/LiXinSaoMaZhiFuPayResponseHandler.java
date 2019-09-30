package dc.pay.business.lixinsaomazhifu;

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

@ResponsePayHandler("LIXINSAOMAZHIFU")
public final class LiXinSaoMaZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String pay_order         ="pay_order";     //必填，参与签名  支付订单号 ，系统订单或者第三方订单号
    private static final String mer_order         ="mer_order";     //必填，参与签名  商户订单号
    private static final String pay_way           ="pay_way";       //必填，参与签名  支付通道
    private static final String amount            ="amount";        //必填，参与签名  订单金额(分为单位)
    private static final String actual_amount     ="actual_amount"; //必填，参与签名  实际支付金额(分为单位)
    private static final String goods_name        ="goods_name";    //必填，参与签名  商品说明
    private static final String status            ="status";        //必填，参与签名  支付状态  2 为支付成功
    private static final String pay_succ_time     ="pay_succ_time"; //必填，参与签名  支付时间
    private static final String sign              ="sign";          //必填  MD5签名结果

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if(null==API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty()){
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        }
        String ordernumberR = API_RESPONSE_PARAMS.get(mer_order);
        if ( StringUtils.isBlank(ordernumberR)){
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        }
        log.debug("[立信扫码支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
//      sign=md5(pay_order&mer_orde&pay_way&amount&actual_amount&goods_name&status&pay_succ_time&key)
        String signSrc = String.format("%s&%s&%s&%s&%s&%s&%s&%s&%s",
                api_response_params.get(pay_order),
                api_response_params.get(mer_order),
                api_response_params.get(pay_way),
                api_response_params.get(amount),
                api_response_params.get(actual_amount),
                api_response_params.get(goods_name),
                api_response_params.get(status),
                api_response_params.get(pay_succ_time),
                channelWrapper.getAPI_KEY());

        String signMd5 = HandlerUtil.getMD5UpperCase(signSrc);
        log.debug("[立信扫码支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5));
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //支付状态  2 为支付成功
        String payStatusCode = api_response_params.get(status);
        String responseAmount = api_response_params.get(amount);
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("2")) {
            my_result = true;
        } else {
            log.error("[立信扫码支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[立信扫码支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：2");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("[立信扫码支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[立信扫码支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}