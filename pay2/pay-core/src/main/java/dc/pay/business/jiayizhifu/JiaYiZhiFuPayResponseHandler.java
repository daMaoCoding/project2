package dc.pay.business.jiayizhifu;

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
import dc.pay.utils.RsaUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.TreeMap;

@ResponsePayHandler("JIAYIZHIFU")
public final class JiaYiZhiFuPayResponseHandler extends PayResponseHandler {
     private final Logger log = LoggerFactory.getLogger(getClass());
     private static final String RESPONSE_PAY_MSG = "0";

     private static final String  amount ="amount";  // "100",
     private static final String  goodsName ="goodsName";  // "IPhone-X",
     private static final String  merNo ="merNo";  // "60000124",
     private static final String  netway ="netway";  // "E_BANK_CCB",
     //private static final String  orderNum ="orderNum";  // "20180828140720",
     private static final String  payDate ="payDate";  // "20180828140838",
     private static final String  payResult ="payResult";  // "00",
     private static final String  sign ="sign";  // "E73C25EF4167A8AC9D0A1D5736EB240C"


    private static final String data = "data";  // "GXKevvol4dKEUHsqPgKa1kNmYmPSNgtWzwsZ+g4ArwJrylkgFVUF082rnkM5X1kveI/gtm9kgcX0LmqvsqA+7bBm/6R2+kq6Znp9IdPu2a17l+cyfzmGuDCP+Q9k63bugM+l7JwkRUC7CadyEbbdXRYRWnF9hjSGNAIJRg4IDyJkJuFo7+h19JGQB4QYJL8ycykIugg4a/KKAwidovDWcPYN1bfzPkNxlX5Pv3eOaagdwccAvMUTEyd60QL7cwEnd1ulVgVKEyEStMQxRiOGBZSb3P/UvtPsIv0uJ01bJFk/aXeJS63J9tcZGyOiGSkhutXWFoePOJutG89cHQVjrA==",
    private static final String  merchNo = "merchNo";  // "60000124",
    private static final String  orderNum = "orderNum";  // "20180828140720"





    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(orderNum);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[嘉亿支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        String deTxt = RsaUtil.dencipher(channelWrapper.getAPI_KEY(), params.get(data));
        JSONObject jsonObj = JSON.parseObject(deTxt);
        Map<String, String> metaSignMap = new TreeMap<String, String>();
        metaSignMap.put(merNo, jsonObj.getString(merNo));
        metaSignMap.put(netway, jsonObj.getString(netway));
        metaSignMap.put(orderNum, jsonObj.getString(orderNum));
        metaSignMap.put(amount, jsonObj.getString(amount));
        metaSignMap.put(goodsName, jsonObj.getString(goodsName));
        metaSignMap.put(payResult, jsonObj.getString(payResult));// 支付状态
        metaSignMap.put(payDate, jsonObj.getString(payDate));// yyyyMMddHHmmss
        String jsonStr = HandlerUtil.mapToJson(metaSignMap);
        String signMd5 = HandlerUtil.getMD5UpperCase(jsonStr+channelWrapper.getAPI_MEMBERID().split("&")[1]);
        log.debug("[嘉亿支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }



    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean checkResult = false;
        String deTxt = RsaUtil.dencipher(channelWrapper.getAPI_KEY(), api_response_params.get(data));
        Map<String, String> response_params = JSONObject.toJavaObject(JSON.parseObject(deTxt), Map.class);

        String payStatus = response_params.get(payResult);
        String responseAmount =   response_params.get(amount);
        boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        if (checkAmount && payStatus.equalsIgnoreCase("00")) {
            checkResult = true;
        } else {
            log.error("[嘉亿支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatus + " ,支付金额：" + responseAmount + " ，应支付金额：" + amountDb);
        }
        log.debug("[嘉亿支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + checkResult + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amountDb + ",第三方响应支付成功标志:" + payStatus + " ,计划成功：1");
        return checkResult;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        String deTxt = RsaUtil.dencipher(channelWrapper.getAPI_KEY(), api_response_params.get(data));
        Map<String, String> response_params = JSONObject.toJavaObject(JSON.parseObject(deTxt), Map.class);
        boolean result = response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("[嘉亿支付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[嘉亿支付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}