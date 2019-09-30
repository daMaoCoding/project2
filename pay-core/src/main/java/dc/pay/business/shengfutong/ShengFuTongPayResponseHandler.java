package dc.pay.business.shengfutong;

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
import org.apache.shiro.codec.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@ResponsePayHandler("SHENGFUTONGZHIFU")
public final class ShengFuTongPayResponseHandler extends PayResponseHandler {
     private final Logger log = LoggerFactory.getLogger(getClass());
     private static final String RESPONSE_PAY_MSG = "0000";


    private static final String  resp="resp";// "eyJyZXN1bHRjb2RlIjoiMTAwMiIsInJlc3VsdG1zZyI6IuaUr+S7mOaIkOWKn++8gSIsInR4bmFtdCI6MTAwMCwibWVyaWQiOiI4OTkxOTQxMSIsIm9yZGVyaWQiOiIyMDE4MDgzMTExMTIxNSIsInF1ZXJ5aWQiOiJIMTk0MjAxODA4MzExMTEyMTUyNzMwMDE2MiIsInBheXRpbWUiOiIyMDE4LTA4LTMxIDExOjIxOjUzIn0=",
    private static final String  sign="sign";// "e886e3b21ee2f2cd64ae85b008679f3a"


    //处理中回调
//     private static final String  resultcode = "resultcode";  // "1002",
//     private static final String  resultmsg = "resultmsg";  // "支付成功！",
//     private static final String  txnamt = "txnamt";  // 1000,
//     private static final String  merid = "merid";  // "89919411",
//     private static final String  orderid = "orderid";  // "20180831111215",
//     private static final String  queryid = "queryid";  // "H1942018083111121527300162",
//     private static final String  paytime = "paytime";  // "2018-08-31 11:21:53"


    //模拟回调
     private static final String  resultcode = "resultcode";  // "0000",
     private static final String  resultmsg = "resultmsg";  // "支付成功",
     private static final String  txnamt = "txnamt";  // 10000,
     private static final String  merid = "merid";  // "89919411",
     private static final String  orderid = "orderid";  // "orderid",
     private static final String  queryid = "queryid";  // "96106a7f0b974357bb4d7236826aa5d9",
     private static final String  paytime = "paytime";  // "2018-08-31 11:40:13"


    //对方表示 0000 1002都表示对方已收到钱，见需求聊天截图
    private  JSONObject getResp(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        JSONObject respJsonObject = JSON.parseObject(Base64.decodeToString(API_RESPONSE_PARAMS.get(resp)));
        if(null!=respJsonObject && respJsonObject.containsKey(resultcode) && (respJsonObject.getString(resultcode).equalsIgnoreCase("0000") || respJsonObject.getString(resultcode).equalsIgnoreCase("1002"))){
            return respJsonObject;
        }else{
            throw new PayException("回调状态不正常，非0000 非1002 "+respJsonObject.toJSONString());
        }
    }

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty() || !API_RESPONSE_PARAMS.containsKey(resp) || StringUtils.isBlank(API_RESPONSE_PARAMS.get(resp)))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        JSONObject respJsonObject = getResp(API_RESPONSE_PARAMS);
        String ordernumberR = respJsonObject.getString(orderid);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[盛付通支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        String signMd5 = HandlerUtil.getMD5UpperCase(params.get(resp)+channelWrapper.getAPI_KEY()).toLowerCase();
        log.debug("[盛付通支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }



    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
        JSONObject respJsonObject = getResp(API_RESPONSE_PARAMS);
        boolean checkResult = false;
        String payStatus = respJsonObject.getString(resultcode);
        String responseAmount =  respJsonObject.getString(txnamt);
        boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        if (checkAmount && (payStatus.equalsIgnoreCase("0000") || payStatus.equalsIgnoreCase("1002"))) {
            checkResult = true;
        } else {
            log.error("[盛付通支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatus + " ,支付金额：" + responseAmount + " ，应支付金额：" + amountDb);
        }
        log.debug("[盛付通支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + checkResult + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amountDb + ",第三方响应支付成功标志:" + payStatus + " ,计划成功：1");
        return checkResult;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("[盛付通支付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[盛付通支付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}