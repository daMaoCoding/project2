package dc.pay.business.dingsheng88;

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
import dc.pay.utils.RsaUtil;
import dc.pay.utils.Sha1Util;

/**
 * @author sunny
 * @date 16 Aug 2019
 */
@ResponsePayHandler("DINGSHENGBABA")
public final class DingShengBaBaPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String merchant                    ="merchant";
    private static final String customno                    ="customno";
    private static final String money                  		="money";
    private static final String state                		="state";
    private static final String qrtype                		="qrtype";
    private static final String sendtime                	="sendtime";
    private static final String orderno                		="orderno";
    private static final String paytime                		="paytime";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("OK");

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(merchant);
        String ordernumberR = API_RESPONSE_PARAMS.get(customno);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[鼎盛88支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
    	String signSrc=String.format("%s%s%s%s%s%s%s%s",
    			merchant+"="+api_response_params.get(merchant)+"&",
    			qrtype+"="+api_response_params.get(qrtype)+"&",
    			customno+"="+api_response_params.get(customno)+"&",
    			sendtime+"="+api_response_params.get(sendtime)+"&",
    			orderno+"="+api_response_params.get(orderno)+"&",
    			money+"="+api_response_params.get(money)+"&",
    			paytime+"="+api_response_params.get(paytime)+"&",
    			state+"="+api_response_params.get(state)
        );
        String paramsStr = signSrc.toString();
        //String publicKeys="MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAgdzYNdAGrykkCdhjAuntmaN6CVuiOQyBvErxju3eb3OLbGVfuyWB+0hKIhLL52hEyLxeE9rF4hpcNW057BatCOl3/IaMNjygf90a3plOgbQX3XWAZg6SiO3hl9NHzkmJqV66xStxkMDQ/QxZ+tWNaXybEJo1yQgyQzTNBjeXwaUSkB+JpP3m51bQI+QT5Jn2FJUiGlkMnkHTaKfKmRpPBsb1s1qSvk5VBDrn7eYJ7u6tP05TKI2tc0cUxsliGBVE0I+DA0A4r3LgHRC9/WlWaLgyEFuF9JKTCWYa1kZCyK4LwywAdbsBfkyybTm9WglfkQOv9cVp/ZSBpUuC3dA+DwIDAQAB";
        Boolean signMD5 = RsaUtil.validateSignByPublicRSA2(paramsStr,channelWrapper.getAPI_KEY(),api_response_params.get(signature));
        log.debug("[鼎盛88支付]-[响应支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return String.valueOf(signMD5);
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        String payStatusCode = api_response_params.get(state);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(money));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        boolean checkAmount = HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差
        //1代表第三方支付成功
        if (checkAmount&&payStatusCode.equalsIgnoreCase("1")) {
            my_result = true;
        } else {
            log.error("[鼎盛88支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[鼎盛88支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：10000");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[鼎盛88支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[鼎盛88支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return  RESPONSE_PAY_MSG;
    }
    
}