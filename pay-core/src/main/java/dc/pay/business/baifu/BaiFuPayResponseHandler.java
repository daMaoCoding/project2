package dc.pay.business.baifu;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.TreeMap;

@ResponsePayHandler("BAIFU")
public final class BaiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private static final String RESPONSE_PAY_MSG = "000000";
    private static final String  paramData = "paramData";      //:
    private static final String  goodsName =  "goodsName";          //: "goodsName",
    private static final String  merchantNo =  "merchantNo";         //: "DR180309152240127",
    private static final String  netwayCode =  "netwayCode";         //: "QQ",
    private static final String  orderNum =  "orderNum";             //: "BAIFU_QQ_SM-4j2Rs",
    private static final String  payAmount =  "payAmount";           //: "100",
    private static final String  payDate =  "payDate";               //: "2018-03-17 10:30:37",
    private static final String  resultCode =  "resultCode";         //: "00",
    private static final String  sign =  "sign";                     //: "79D9D050B1A3FDB341F64D0F9074DC36"

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty() || !API_RESPONSE_PARAMS.containsKey(paramData))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        JSONObject jsonObj = JSON.parseObject(API_RESPONSE_PARAMS.get(paramData));
        String ordernumberR = jsonObj.getString(orderNum);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[佰富]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }



    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        JSONObject jsonObj = JSON.parseObject(params.get(paramData));
        Map<String, String> metaSignMap = new TreeMap<String, String>();
        metaSignMap.put(goodsName, jsonObj.getString(goodsName));
        metaSignMap.put(merchantNo, jsonObj.getString(merchantNo));
        metaSignMap.put(netwayCode, jsonObj.getString(netwayCode));
        metaSignMap.put(orderNum, jsonObj.getString(orderNum));
        metaSignMap.put(payAmount, jsonObj.getString(payAmount));
        metaSignMap.put(payDate, jsonObj.getString(payDate));// 支付状态
        metaSignMap.put(resultCode, jsonObj.getString(resultCode));// yyyy-MM-dd
        String buildJsonParam = BaiFuPayUtil.buildJsonParam(metaSignMap,sign).concat(channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(buildJsonParam);
        log.debug("[佰富]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }





    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
        JSONObject jsonObj = JSON.parseObject(api_response_params.get(paramData));
        boolean result = false;
        String payStatus = jsonObj.getString(resultCode);
        String responseAmount = jsonObj.getString(payAmount);
        boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        if (checkAmount && payStatus.equalsIgnoreCase("00")) {
            result = true;
        } else {
            log.error("[佰富]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatus + " ,支付金额：" + responseAmount + " ，应支付金额：" + amountDb);
        }
        log.debug("[佰富]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amountDb + ",第三方响应支付成功标志:" + payStatus + " ,计划成功：1");
        return result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        JSONObject jsonObj = JSON.parseObject(api_response_params.get(paramData));
        boolean result = jsonObj.getString(sign).equalsIgnoreCase(signMd5);
        log.debug("[佰富]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[佰富]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}