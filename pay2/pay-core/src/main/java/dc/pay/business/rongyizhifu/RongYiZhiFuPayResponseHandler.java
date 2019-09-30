package dc.pay.business.rongyizhifu;

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
import dc.pay.utils.DesUtil;
import dc.pay.utils.HandlerUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLDecoder;
import java.util.Map;

@ResponsePayHandler("RONGYIZHIFU")
public final class RongYiZhiFuPayResponseHandler extends PayResponseHandler {
     private final Logger log = LoggerFactory.getLogger(getClass());
     private static final String RESPONSE_PAY_MSG = "SUCCESS";

     private static final String   MerchantNo ="MerchantNo";  //: "062717371849635104",
     private static final String   ErrCode ="ErrCode";  //: 200,
     private static final String   ErrMsg ="ErrMsg";  //: "ok",
     private static final String   Data ="Data";  //: "vnnjvmm8AgYTbkrNDP9b8Ve%2bDT0vrxRusTD3%2b%2f5Gs%2ftvZdXMnrIXiW7EH5NammKkqvYxiRYs5293SJtgH25hTpA7V020EBWWss652lAUMsdYRix%2flNUvAWqBWeo0X1ku2T6c5RuXz5iOiN%2fWPLhNa4sFzmDKSdgIoN7sDKfuNmcBIfmm6as3wTpmNonV9QvPiij%2f8Czt55ZT41%2bAYPykndCvXg2csl8LQTnzgeGVprgIjFfaIAxFFFIiDM%2bd%2bQgb959n2BiLFj4WgXLJSU5NNeqx%2fTcgx%2fA%2b9nwkgTj1WukukwjNwrjoe2GVEi4ZuewxIxf3fgE7B11J3BhHbbgAtw%3d%3d",
     private static final String   Sign ="Sign";  //: "9104A37F9B1387B3865D4CE7F95E1A42"


    private static final String    ResultCode = "ResultCode";  //: 200,
    private static final String    ResultMsg = "ResultMsg";  //: "操作成功",
    private static final String    MerchantOrderNumber = "MerchantOrderNumber";  //: "20180629112045",
    private static final String    PlatformOrderNumber = "PlatformOrderNumber";  //: "8062911201844428011",
    private static final String    OrderAmount = "OrderAmount";  //: 1000,
    private static final String    PlaceOrderTime = "PlaceOrderTime";  //: "2018-06-29 11:20:44",
    private static final String    PaymentTime = "PaymentTime";  //: "2018-06-29 11:24:29",
    private static final String    OrderStatus = "OrderStatus";  //: 2,
    private static final String    PType = "PType";  //: 11


    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty() ||StringUtils.isBlank(API_RESPONSE_PARAMS.get(MerchantNo)) || !"200".equalsIgnoreCase(API_RESPONSE_PARAMS.get(ErrCode)) )
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);

        String ordernumberR= getResponseParam().getString(MerchantOrderNumber);
        log.debug("[荣亿付支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        String paramsStr = String.format("Data=%s&ErrCode=%s&ErrMsg=%s&MerchantNo=%s&Key=%s",params.get(Data),params.get(ErrCode),params.get(ErrMsg),channelWrapper.getAPI_MEMBERID(),channelWrapper.getAPI_KEY());
        String  signResult = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[荣亿付支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signResult));
        return signResult;
    }



    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean checkResult = false;
        JSONObject responseParam = getResponseParam();
        if(responseParam.containsKey(ResultCode) && "200".equalsIgnoreCase(responseParam.getString(ResultCode))){
            String payStatus = responseParam.getString(OrderStatus);
            String responseAmount =   responseParam.getString(OrderAmount);
            boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
            if (checkAmount && payStatus.equalsIgnoreCase("2")) {
                checkResult = true;
            } else {
                log.error("[荣亿付支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatus + " ,支付金额：" + responseAmount + " ，应支付金额：" + amountDb);
            }
            log.debug("[荣亿付支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + checkResult + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amountDb + ",第三方响应支付成功标志:" + payStatus + " ,计划成功：1");
        }
        return checkResult;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(Sign).equalsIgnoreCase(signMd5);
        log.debug("[荣亿付支付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[荣亿付支付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }



    private JSONObject getResponseParam() throws PayException {
        String fullKey = handlerUtil.getApiKeyFromReqPayMemberId(API_RESPONSE_PARAMS.get(MerchantNo));
        String key = fullKey.substring(fullKey.length()-8);
        String iv = fullKey.substring(0,24);
        String RData = API_RESPONSE_PARAMS.get(Data);
        try {
            String data64Desc = URLDecoder.decode(RData, "utf-8");
            String desDecryptJson = DesUtil.desDecrypt64(data64Desc, key, iv);
            JSONObject jsonObject = JSON.parseObject(desDecryptJson);
           return  jsonObject;
        }catch (Exception e){
            throw new PayException("第三方返回数据处理错误");
        }
    }

}