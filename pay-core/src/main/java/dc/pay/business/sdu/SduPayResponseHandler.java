package dc.pay.business.sdu;

import java.util.HashMap;
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
import dc.pay.utils.MapUtils;

/**
 * 
 * @author sunny
 * Dec 14, 2018
 */
@ResponsePayHandler("SDU")
public final class SduPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

//    参数				参与签名				描述
//    total_amount		√					参数名称：商家订单金额 订单总金额，单位为分
//    out_trade_no		√					参数名称：商家订单号    商家网站生成的订单号，由商户保证其唯一性，由字母、数字、下划线组成。
//    trade_status		√					参数名：订单状态  取值为“SUCCESS”，代表订单交易成功

    private static final String total_amount                    ="total_amount";
    private static final String out_trade_no                    ="out_trade_no";
    private static final String trade_status                    ="trade_status";

    
    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("SUCCESS");

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(out_trade_no);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[sdu支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
    	//Map<String, String> paramKeys = MapUtils.recombinationMap(api_response_params,"out_trade_no","total_amount","trade_status");
    	Map<String, String> paramKeys =new HashMap<>();
    	paramKeys.put(out_trade_no, api_response_params.get(out_trade_no));
    	paramKeys.put(total_amount, api_response_params.get(total_amount));
    	paramKeys.put(trade_status, api_response_params.get(trade_status));
    	String signSrc=MapUtils.getSignStrByMap(paramKeys,true);
        signSrc=signSrc+channelWrapper.getAPI_KEY();
        String signMD5 = HandlerUtil.getMD5UpperCase(signSrc).toLowerCase();
        log.debug("[sdu支付]-[响应支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return signMD5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //returncode          交易状态         是            “SUCCESS” 为成功
        String payStatusCode = api_response_params.get(trade_status);
        String responseAmount = api_response_params.get(total_amount);
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        boolean checkAmount =  db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount&&payStatusCode.equalsIgnoreCase("SUCCESS")) {
            my_result = true;
        } else {
            log.error("[sdu支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[sdu支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：SUCCESS");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[sdu支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[sdu支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}