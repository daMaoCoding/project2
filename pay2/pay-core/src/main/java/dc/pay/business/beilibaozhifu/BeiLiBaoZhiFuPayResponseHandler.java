package dc.pay.business.beilibaozhifu;

/**
 * ************************
 * @author tony 3556239829
 */

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.business.kuaitongbaozhifu.RSAUtils;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
@ResponsePayHandler("BEILIGBAOZHIFU")
public final class BeiLiBaoZhiFuPayResponseHandler extends PayResponseHandler {
      //private final Logger log = LoggerFactory.getLogger(getClass());
      private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("SUC");

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
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty() ||!API_RESPONSE_PARAMS.containsKey(context)   ||   !API_RESPONSE_PARAMS.containsKey(orderNumber))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR =API_RESPONSE_PARAMS.get(orderNumber);
        log.debug("[贝立宝支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        String decrypt=null;
        try {
            decrypt = RSAUtils.decryptByPrivateKey(params.get(context), channelWrapper.getAPI_KEY()); //channelWrapper ==null,第三方回调数据全部加密，无订单号查找私钥解密
        } catch (Exception e) {
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR_APIKEY);
        }
        boolean isVerify = RSAUtils.verify(decrypt, channelWrapper.getAPI_PUBLIC_KEY());
        if(!isVerify){  throw new PayException("回调数据验证失败");  }


        JSONObject businessContextJsonObject = JSON.parseObject(decrypt).getJSONObject(businessContext);
        String orderStatusS =businessContextJsonObject.getString(orderStatus);
        if(!orderStatusS.equalsIgnoreCase("SUC")) throw new PayException("回调订单完成状态错误");


        String responseAmount =businessContextJsonObject.getString(amount);
        boolean checkAmount =  HandlerUtil.isRightAmount(channelWrapper.getAPI_AMOUNT(),responseAmount,"100");//第三方回调金额差额1元内
        if(!checkAmount) throw new PayException("回调金额错误");

        return "true";
    }



    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
             return true;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = "true".equalsIgnoreCase(signMd5);
        log.debug("[贝立宝支付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[贝立宝支付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}