package dc.pay.business.kuaitongbaozhifu;

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
import dc.pay.utils.HandlerUtil;
import org.apache.commons.lang.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
@ResponsePayHandler("KUAITONGBAOZHIFU")
public final class KuaiTongBaoZhiFuPayResponseHandler extends PayResponseHandler {
      //private final Logger log = LoggerFactory.getLogger(getClass());
      private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("success");

    private static final String   context ="context";

    private static final String   businessHead   ="businessHead"    ;    //
    private static final String   merchantNumber   ="merchantNumber"  ;   // "PAY000638000658",
    private static final String   sign   ="sign"   ;    // ftmh8z0hB+11vS6DNi7BUbqUOUTGZehHfq19UvSuFSpKCFgr6taHQ6t1T/a/cSORDsc7qgUHq35E3P5HOuqs6Q0+NQnZwPX2YtiyYvX2NSoankkfZOQnROqZ6ZDb9d2pRdGUqr9e5OqMuwEZ3Wfkdz2Y9c47ajw2qFoawE1n+wE="

    private static final String   businessContext   ="businessContext"    ;    //
    private static final String   amount   ="amount";    // "100",
    private static final String   orderNumber   ="orderNumber"   ;    // "20181029103307381612",
    private static final String   orderTime   ="orderTime"    ;    // "20181029103308",
    private static final String   payType   ="payType"    ;    // "WECHAT_NATIVE",
    private static final String   fee   ="fee"    ;    // "4",
    private static final String   orderStatus   ="orderStatus"    ;    // "SUC",
    private static final String   currency   ="currency"    ;    // "CNY",
    private static final String   payOrderNumber   ="payOrderNumber"    ;    // "O0120181029103307863329000638"



    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty() ||!API_RESPONSE_PARAMS.containsKey(context))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String decrypt=null;
        try {
             decrypt = RSAUtils.decryptByPrivateKey(context, channelWrapper.getAPI_KEY()); //channelWrapper ==null,第三方回调数据全部加密，无订单号查找私钥解密
        } catch (Exception e) {
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR_APIKEY);
        }
        boolean isVerify = RSAUtils.verify(decrypt, channelWrapper.getAPI_PUBLIC_KEY());
        if(!isVerify){  throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR_STATE);  }


        JSONObject businessContextJsonObject = JSON.parseObject(decrypt).getJSONObject(businessContext);
        String orderStatusS =businessContextJsonObject.getString(orderStatus);
        if(!orderStatusS.equalsIgnoreCase("SUC")) throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR_SIGN);


        String ordernumberR   =businessContextJsonObject.getString(orderNumber);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[快通宝支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        return "true";
    }



    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean checkResult = false;
        String decrypt = null;
        try {  decrypt = RSAUtils.decryptByPrivateKey(context, channelWrapper.getAPI_KEY()); } catch (Exception e) {  throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR_APIKEY);    }
        JSONObject businessContextJsonObject = JSON.parseObject(decrypt).getJSONObject(businessContext);
        String responseAmount  =businessContextJsonObject.getString(amount);
        //boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        boolean checkAmount =  HandlerUtil.isRightAmount(amountDb,responseAmount,"100");//第三方回调金额差额1元内
        if (checkAmount) {
            checkResult = true;
        } else {
        }
        log.debug("[快通宝支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + checkResult + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amountDb + " ,计划成功：1");
        return checkResult;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = "true".equalsIgnoreCase(signMd5);
        log.debug("[快通宝支付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[快通宝支付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}