package dc.pay.business.jiudingzhifu;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
import dc.pay.utils.Sha1Util;

/**
 * @author sunny
 * @date 14 Sep 2019
 */
@ResponsePayHandler("JIUDINGZHIFU")
public final class JiuDingZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String merId                   ="merId";
    private static final String orderId                 ="orderId";
    private static final String orderAmt                ="orderAmt";
    private static final String status                  ="status";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("success");

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(status);
        String ordernumberR = API_RESPONSE_PARAMS.get(orderId);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[九鼎支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
    	Map<String, String> sortMap = new TreeMap<>(api_response_params);
        StringBuffer sb = new StringBuffer();
        Iterator<String> iterator = sortMap.keySet().iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            String val = sortMap.get(key);
            if (StringUtils.isBlank(val) || key.equalsIgnoreCase("sign")) {
                continue;
            }
            sb.append(key).append("=").append(val).append("&");
        }
        sb.append("key=").append(channelWrapper.getAPI_MEMBERID().split("&")[1]);
        //生成待签名串
        String singStr = sb.toString();
        //生成加密串
        String sign = HandlerUtil.getMD5UpperCase(singStr);
        String publicKeys="MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAyNgjt9RdaTCnYFIilcYsYtOOGpnUoiTc0C6/xiwR4Zm1hT8mP7xkXaBXKirFFIWdJYYCoGKyazCumMyPS08SBRCqze3oZNc9x6eivXdRJCKlgL6F481EhRx7LS3Vn+lU/awUOXl8Sty9ZFi8P2YvzTfJAvLa7cwGvo/xFXcS/+RRIY0gl9O8azDjxm/REwZHJZQXP0Jiagckh9OOYEhpor8PyX1NKMdBiQnobe8rZQ6aeiQEujwniFqTZzfXcYxpNYTNhlwdZYeG0FuxpuXfdTHSY+AXXDBXMtERERXvQWMq6cv/7GF5kBORKgf/Mw4wVzfZUdhpDjKneYOZ/vsuOwIDAQAB";
        Boolean signFlag=SHA256WithRSAUtils.buildRSAverifyByPublicKey(sign,channelWrapper.getAPI_PUBLIC_KEY(),api_response_params.get("sign"));
        log.debug("[九鼎支付]-[响应支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signFlag));
        return String.valueOf(signFlag);
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        String payStatusCode = api_response_params.get(status);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(orderAmt));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        boolean checkAmount = HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差
        //1代表第三方支付成功
        if (checkAmount&&payStatusCode.equalsIgnoreCase("1")) {
            my_result = true;
        } else {
            log.error("[九鼎支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[九鼎支付-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = "true".equalsIgnoreCase(signMd5);
        log.debug("[九鼎支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[九鼎支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return  RESPONSE_PAY_MSG;
    }
}