package dc.pay.business.tangzhifu;

/**
 * ************************
 * @author tony 3556239829
 */

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.AES128ECB;
import dc.pay.utils.HandlerUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@ResponsePayHandler("TANGZHIFU")
public final class TangZhiFuPayResponseHandler extends PayResponseHandler {
     private final Logger log = LoggerFactory.getLogger(getClass());
     private static final String RESPONSE_PAY_MSG = "success";
     private static final String   Merorderno = "Merorderno";
     private static final String   Sign = "Sign";
     private static final String   Amountpaid  = "Amountpaid"; //支付金额
     private static final String   payStatus  = "payStatus";  //状态



    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(Merorderno);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[唐支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        String resultStr = "false";
        try {
            String responseJsonStr = params.get(Sign);
            if(StringUtils.isNotBlank(responseJsonStr)){
                responseJsonStr=responseJsonStr.replaceAll("%2B","+");
                String key = channelWrapper.getAPI_KEY();
                String encrypt =  AES128ECB.Decrypt(responseJsonStr,key);
                JSONObject jsonObject = JSON.parseObject(encrypt);
                if(null!=jsonObject && jsonObject.containsKey(Amountpaid) && jsonObject.containsKey(payStatus) ){
                    if("TRADE_SUCCESS".equalsIgnoreCase(jsonObject.getString(payStatus)) &&  HandlerUtil.getFen(jsonObject.getString(Amountpaid)).equalsIgnoreCase(channelWrapper.getAPI_AMOUNT())){
                        return "true";
                    }
                }
            }
        }catch (Exception e){
            return resultStr;
        }
        return resultStr;
    }



    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
         return true;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = Boolean.valueOf(signMd5);
        log.debug("[唐支付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[唐支付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}