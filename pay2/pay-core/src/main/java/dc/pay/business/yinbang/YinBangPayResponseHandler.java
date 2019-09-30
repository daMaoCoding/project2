package dc.pay.business.yinbang;

/**
 * ************************
 * @author tony 3556239829
 */

import com.alibaba.fastjson.JSON;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.*;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@ResponsePayHandler("YINBANG")
public final class YinBangPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private static final String RESPONSE_PAY_MSG = "SUCCESS";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);

        String encParam = API_RESPONSE_PARAMS.get("encParam");
        String merId = API_RESPONSE_PARAMS.get("merId");
        String version = API_RESPONSE_PARAMS.get("version");
        String sign = API_RESPONSE_PARAMS.get("sign");
        String orderId = API_RESPONSE_PARAMS.get("orderId");

//
//        Map<String, String> resultMap = new HashMap<String, String>();
//        //验签和解密
//        JsonResult verifyResult = YinBangUtil.verifyAndDecrypt(encParam, sign, channelWrapper.getAPI_PUBLIC_KEY(), channelWrapper.getAPI_KEY());
//        if (!"1000".equals(verifyResult.getRespCode())) {
//            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
//        }
//        try {
//            resultMap = GsonUtil.fromJson(verifyResult.getData().toString(), Map.class);
//        } catch (Exception e) {
//            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
//        }
//        String code = resultMap.get("respCode"); // 返回码返回1000表示成功。当respCode为1000时，订单数据才有效。
//        if (!"1000".equals(code)) {
//            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
//        }
//
//        String respCode = resultMap.get("respCode"); // 返回码返回1000表示成功。当respCode为1000时，订单数据才有效。
//         orderId = resultMap.get("orderId"); // 商户订单号
//        String payOrderId = resultMap.get("payOrderId"); // 支付订单号
//        String order_state = resultMap.get("order_state"); // 订单状态
//        String money = resultMap.get("money"); // 交易金额
//        String payReturnTime = resultMap.get("payReturnTime"); // 付款时间
//        String selfParam = resultMap.get("selfParam"); // 自定义参数
//        String payType = resultMap.get("payType"); // 支付方式
//        String payTypeDesc = resultMap.get("payTypeDesc"); // 支付方式描述


        if (StringUtils.isBlank(orderId))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[银邦]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + orderId);
        return orderId;
    }



    @Override
    protected String buildPaySign(Map<String, String> payParam, String api_key) throws PayException {
        log.debug("[银邦]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString("wait....."));
        return "pay_md5sign";
    }




    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> payParam, String amount) throws PayException {
        String encParam = payParam.get("encParam");
        String merId = payParam.get("merId");
        String version = payParam.get("version");
        String sign = payParam.get("sign");
        String orderId = payParam.get("orderId");
        Map<String, String> resultMap = new HashMap<String, String>();
        //验签和解密
        JsonResult verifyResult = YinBangUtil.verifyAndDecrypt(encParam, sign, channelWrapper.getAPI_PUBLIC_KEY(), channelWrapper.getAPI_KEY());
        if (!"1000".equals(verifyResult.getRespCode())) {
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        }
        try {
            resultMap = GsonUtil.fromJson(verifyResult.getData().toString(), Map.class);
        } catch (Exception e) {
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        }
        String respCode = resultMap.get("respCode"); // 返回码返回1000表示成功。当respCode为1000时，订单数据才有效。
        orderId = resultMap.get("orderId"); // 商户订单号
        String payOrderId = resultMap.get("payOrderId"); // 支付订单号
        String order_state = resultMap.get("order_state"); // 订单状态
        String money = resultMap.get("money"); // 交易金额
        String payReturnTime = resultMap.get("payReturnTime"); // 付款时间
        String selfParam = resultMap.get("selfParam"); // 自定义参数
        String payType = resultMap.get("payType"); // 支付方式
        String payTypeDesc = resultMap.get("payTypeDesc"); // 支付方式描述
        boolean result = false;
        boolean checkAmount = amount.equalsIgnoreCase(money);
        if (checkAmount && order_state.equalsIgnoreCase("1003")) {
            result = true;
        } else {
            log.error("[银邦]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + order_state + " ,支付金额：" + money + " ，应支付金额：" + amount);
        }
        log.debug("[银邦]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + money + " ,数据库金额：" + amount + ",第三方响应支付成功标志:" + order_state + " ,计划成功：1000");
        return result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
       // boolean result = api_response_params.get("sign").equalsIgnoreCase(signMd5);
        //log.debug("[银邦]-[响应支付]-4.验证MD5签名：" + result);
       // return result;
        return true;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[银邦]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}