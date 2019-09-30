package dc.pay.business.jinfazhifu;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
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
 * May 1, 2019
 */
@ResponsePayHandler("JINFAZHIFU")
public final class JinFaZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String pay_order         ="pay_order"; // 平台订单号  Y    Y    平台生成的订单号,维度唯一
    private static final String order_id          ="order_id";  // 商户订单号  Y    Y    商户提交的自己订单号，保持唯一值。
    private static final String price             ="price";     // 支付金额    Y    Y    金额（单位:分）
    private static final String pay_type          ="pay_type";  // 支付类型    Y    Y    支付类型,参考附录
    private static final String code              ="code";      // 订单状态    Y    Y    00:支付成功/01:失败
    private static final String timestamp         ="timestamp"; // 支付时间    Y    Y    格式（yyyyMMddHHmmss）

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";
    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        JSONObject paramsMap = getParamsMap(API_RESPONSE_PARAMS);
        String ordernumberR = paramsMap.getString(order_id);
        if ( StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[金发支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        JSONObject paramsMap = getParamsMap(API_RESPONSE_PARAMS);
        // pay_order=MLNBT30598&order_id=pay151&price=10000&pay_type=wx&code=00&timestamp=20190430102522&key=
        StringBuilder signStr = new StringBuilder();
        signStr.append(pay_order + "=").append(paramsMap.getString(pay_order)).append("&");
        signStr.append(order_id + "=").append(paramsMap.getString(order_id)).append("&");
        signStr.append(price + "=").append(paramsMap.getString(price)).append("&");
        signStr.append(pay_type + "=").append(paramsMap.getString(pay_type)).append("&");
        signStr.append(code + "=").append(paramsMap.getString(code)).append("&");
        signStr.append(timestamp + "=").append(paramsMap.getString(timestamp)).append("&");
        signStr.append(key+"=").append(channelWrapper.getAPI_KEY());
        String paramsStr =signStr.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[金发支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        JSONObject paramsMap = getParamsMap(api_response_params);
        boolean my_result = false;
        //status        交易状态    1    是    0-未支付    00-支付成功        2-支付失败
        String payStatusCode = paramsMap.getString(code);
        String responseAmount = paramsMap.getString(price);

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        // 00 代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("00")) {
            my_result = true;
        } else {
            log.error("[金发支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[金发支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：00");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        JSONObject paramsMap = getParamsMap(api_response_params);
        boolean my_result = paramsMap.getString(signature).equalsIgnoreCase(signMd5);
        log.debug("[金发支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[金发支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }

    public JSONObject getParamsMap(Map<String, String> params) {
        JSONObject jsonObject = null;
         for(String key : params.keySet()){
            if (StringUtils.isNotBlank(key)){
                jsonObject = JSONObject.parseObject(key);
                break;
            }
          }
        return jsonObject;
    }
}