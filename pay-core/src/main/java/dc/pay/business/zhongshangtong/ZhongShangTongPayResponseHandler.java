package dc.pay.business.zhongshangtong;

/**
 * ************************
 * @author tony 3556239829
 */

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

@ResponsePayHandler("ZHONGSHANGTONG")
public final class ZhongShangTongPayResponseHandler extends PayResponseHandler {
    private final Logger log =  LoggerFactory.getLogger(getClass());

            private static final String   returncode    = "returncode"  ;// -> "1"
            private static final String   userid    = "userid"  ;// -> "1374"
            private static final String   orderid    = "orderid"  ;// -> "ZHONGSHANGTONG_QQ_SM-OQF5O"
            private static final String   money    = "money"  ;// -> "2.00"
            private static final String   sign    = "sign"  ;// -> "8fd67d61ca4382169026c97692dfcaa9"
            private static final String   sign2    = "sign2"  ;// -> "e0a3b0ae1544f4ac267eca30a829d607"
            private static final String   ext    = "ext"  ;// ->
            private static final String  RESPONSE_PAY_MSG   = "ok";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        String memberId = null;
        String orderId = null;
        if(null==API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty() ||!API_RESPONSE_PARAMS.containsKey(orderid)) {
            log.error("[中商通支付]1.获取支付通道响应信息中的订单号错误，"+ JSON.toJSONString(API_RESPONSE_PARAMS));
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        }

         memberId = API_RESPONSE_PARAMS.get(userid);
         orderId = API_RESPONSE_PARAMS.get(orderid);

        if(StringUtils.isBlank(memberId) || StringUtils.isBlank(orderId)){
            log.error("[中商通支付]1.获取支付通道响应信息中的订单号错误，"+ JSON.toJSONString(API_RESPONSE_PARAMS));
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        }
        log.debug("[中商通支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成："+ orderId);
        return orderId;
    }

    @Override
    protected String buildPaySign(Map<String, String> payParam, String api_key) throws PayException {
        // sign = md5( tolower(returncode={}&userid={}&orderid={}&keyvalue={}))
        // sign2 = md5(tolower(money={}&returncode={}&userid={}&orderid={}&keyvalue={}))
        String paramsStr1 = String.format("returncode=%s&userid=%s&orderid=%s&keyvalue=%s",
                payParam.get(returncode),  payParam.get(userid), payParam.get(orderid),channelWrapper.getAPI_KEY()
        );
        String sign1 = HandlerUtil.getMD5UpperCase(paramsStr1).toLowerCase();

        String paramsStr2 = String.format("money=%s&returncode=%s&userid=%s&orderid=%s&keyvalue=%s",
                payParam.get(money), payParam.get(returncode),  payParam.get(userid), payParam.get(orderid),channelWrapper.getAPI_KEY()
        );
        String sign2 = HandlerUtil.getMD5UpperCase(paramsStr2).toLowerCase();

        log.debug("[中商通支付]-[请求支付]-2.生成加密URL签名完成：" + sign1.concat(",").concat(sign2));
        return sign1.concat(",").concat(sign2);
    }


    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params,String amount) throws PayException {
        boolean result = false;
        boolean checkAmount = amount.equalsIgnoreCase( HandlerUtil.getFen(api_response_params.get(money)));
        if(checkAmount && StringUtils.isNotBlank( api_response_params.get(returncode)) &&  api_response_params.get(returncode).equalsIgnoreCase("1")  ){
            result = true;
        }else{
            log.error("[中商通支付]-[响应支付]金额及状态验证错误,订单号："+channelWrapper.getAPI_ORDER_ID()+",第三方支付状态："+api_response_params.get(returncode) +" ,支付金额："+HandlerUtil.getFen(api_response_params.get(money))+" ，应支付金额："+amount);
        }
        log.debug("[中商通支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果："+ result+" ,金额验证："+checkAmount+" ,responseAmount="+HandlerUtil.getFen(api_response_params.get(money)) +" ,数据库金额："+amount+",第三方响应支付成功标志:"+api_response_params.get(returncode)+" ,计划成功：1");
        return result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params,String signMd5) {
        String signMsg1     = api_response_params.get(sign);
        String signMsg2    = api_response_params.get(sign2);
        boolean result     = signMsg1.equalsIgnoreCase(signMd5.split(",")[0]) &&  signMsg2.equalsIgnoreCase(signMd5.split(",")[1]) ;
        log.debug("[中商通支付]-[响应支付]-4.验证MD5签名："+ result);
        return result;
    }
    @Override
    protected String responseSuccess() {
        log.debug("[中商通支付]-[响应支付]-5.第三方支付确认收到消息返回内容："+ RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}