package dc.pay.business.xinduofutong;

import java.util.List;
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
 * @author andrew
 * Aug 26, 2019
 */
@ResponsePayHandler("XINDUOFUTONG")
public final class XinDuoFuTongPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //POST发送JSON进行通知
    //参数名 描述  必填  数据类型    说明
    //resultStatus    状态码 是   int 0为成功，其余均为失败
    private static final String resultStatus                ="resultStatus";
    //resultMsg   状态描述    否   string(128) resultStatus不为0时返回字符串
//    private static final String resultMsg                ="resultMsg";
    //resultCode  状态码 是   string(32)  resultStatus=0时返回字符串SUCCESS；其余返回状态说明
//    private static final String resultCode                ="resultCode";
    //以下内容在resultCode=0时, resultData有返回值
    private static final String resultData                ="resultData";

    //mch_code    商户标识    是   string(32)  商户编号由我司统一分配
    private static final String mch_code                ="mch_code";
    //order_no    商户订单编号  是   string(32)  商户系统中必须唯一
    private static final String order_no                ="order_no";
    //pay_amount  提交支付金额  是   decimal 商户订单金额，以元为单位，            第三方支付与real_amount一致
//    private static final String pay_amount                ="pay_amount";
    //real_amount 实际支付金额  是   decimal 订单成交金额，以元为单位，            第三方支付与pay_amount一致
    private static final String real_amount                ="real_amount";
    //status  订单状态    是   int status=1时能确认支付成功
    private static final String status                ="status";
    //sign    参数签名    是   string(32)  MD5全参数签名，小写
//    private static final String sign                ="sign";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
       //String api_KEY = channelWrapper.getAPI_KEY();
       //if (null == api_KEY || !api_KEY.contains("-") || api_KEY.split("-").length != 2) {
       //    log.error("[新多福通]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //    throw new PayException("[新多福通]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //}
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        if (!"0".equals(API_RESPONSE_PARAMS.get(resultStatus)))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_PARAM_ERROR);
      Map<String, String> jsonToMap = handlerUtil.jsonToMap(API_RESPONSE_PARAMS.get(resultData));
        
        String partnerR = jsonToMap.get(mch_code);
        String ordernumberR = jsonToMap.get(order_no);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[新多福通]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
//        Map<String, String> jsonToMap = handlerUtil.jsonToMap(api_response_params.get(data));
        Map<String, String> jsonToMap = handlerUtil.jsonToMap(api_response_params.get(resultData));
        List paramKeys = MapUtils.sortMapByKeyAsc(jsonToMap);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!signature.equals(paramKeys.get(i)) && StringUtils.isNotBlank(jsonToMap.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(jsonToMap.get(paramKeys.get(i))).append("&");
            }
        }
        signSrc.append(key +"="+ channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[新多福通]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        Map<String, String> jsonToMap = handlerUtil.jsonToMap(api_response_params.get(resultData));
        
        boolean my_result = false;
        //status    订单状态    是   int status=1时能确认支付成功
        String payStatusCode = jsonToMap.get(status);
        String responseAmount = HandlerUtil.getFen(jsonToMap.get(real_amount));

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        // 1 代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("1")) {
            my_result = true;
        } else {
            log.error("[新多福通]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[新多福通]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return my_result;
    }

    
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        Map<String, String> jsonToMap = handlerUtil.jsonToMap(api_response_params.get(resultData));
        boolean my_result = jsonToMap.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[新多福通]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[新多福通]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}