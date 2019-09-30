package dc.pay.business.fengxie;

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
 * Oct 17, 2018
 */
@ResponsePayHandler("FENGXIE")
public final class FengXiePayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //字段名               字段说明      是否必填      备注
    //fx_merchant_id       商户号          是           唯一，有风携支付提供
    //fx_order_id          商户订单号      是           平台返回商户提交的订单号
    //fx_transaction_id    平台订单号      是           平台内部生成的订单号
    //fx_desc              商品名称        是           Utf-8编码
    //fx_order_amount      交易金额        是           商户下单时传递金额
    //fx_attch             附加信息        是           原样返回
    //fx_status_code       订单状态        是           【200代表支付成功】
    //fx_time              支付时间        是           支付成功时的时间，unix时间戳
    //fx_sign              数据签名        是           通过签名算法计算得出的签名值。
    private static final String fx_merchant_id                    ="fx_merchant_id";
    private static final String fx_order_id                       ="fx_order_id";
    private static final String fx_transaction_id                 ="fx_transaction_id";
//    private static final String fx_desc                           ="fx_desc";
    private static final String fx_order_amount                   ="fx_order_amount";
//    private static final String fx_attch                          ="fx_attch";
    private static final String fx_status_code                    ="fx_status_code";
//    private static final String fx_time                           ="fx_time";

    //fx_original_amount  商户提交的整数金额（int）  是   商户提交的整数金额（必须是整数，例如：100，300）
    private static final String fx_original_amount                ="fx_original_amount";
    
    //signature    数据签名    32    是    　
    private static final String signature  ="fx_sign";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(fx_merchant_id);
        String ordernumberR = API_RESPONSE_PARAMS.get(fx_order_id);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[风携]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        StringBuilder signStr = new StringBuilder();
//        签名方式(分隔符“|”)：【md5(md5(商户号|商户订单号|平台订单号|支付金额|商户请求金额|商户秘钥|订单状态))】
        signStr.append(api_response_params.get(fx_merchant_id)).append("|");
        signStr.append(api_response_params.get(fx_order_id)).append("|");
        signStr.append(api_response_params.get(fx_transaction_id)).append("|");
        signStr.append(api_response_params.get(fx_order_amount)).append("|");
        signStr.append(api_response_params.get(fx_original_amount)).append("|");
        signStr.append(channelWrapper.getAPI_KEY()).append("|");
        signStr.append(api_response_params.get(fx_status_code));
        String paramsStr =signStr.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase()).toLowerCase();
        log.debug("[风携]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //fx_status_code    订单状态    是   【200代表支付成功】
        String payStatusCode = api_response_params.get(fx_status_code);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(fx_order_amount));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        boolean checkAmount = handlerUtil.isAllowAmountt(db_amount, responseAmount, "60");
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("200")) {
            my_result = true;
        } else {
            log.error("[风携]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[风携]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：200");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[风携]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[风携]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}