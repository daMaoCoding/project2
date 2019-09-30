package dc.pay.business.yiqifuer;

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
@ResponsePayHandler("YIQIFUERZHIFU")
public final class YiQiFuErPayResponseHandler extends PayResponseHandler {
      //private final Logger log = LoggerFactory.getLogger(getClass());
      private static final String RESPONSE_PAY_MSG = jsonResponsePayMsg("{\"errorcode\":\"200\",\"msg\":\"成功\"}");

     private static final String  errorcode = "errorcode";    // "200",
     private static final String  msg = "msg";    // "成功",
     private static final String  data = "data";    //
     private static final String  mid = "mid";    // "10828",
     private static final String  oid = "oid";    // "20181011142356",
     private static final String  amt = "amt";    // 1,
     private static final String  tamt = "tamt";    // 0.92,
     private static final String  way = "way";    // 3,
     private static final String  code = "code";    // 100,
     private static final String  remark = "remark";    // "20181011142356",
     private static final String  sign = "sign";    // "d2ef1f463af378146e7cb691625b2ca1"


    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        if(!HandlerUtil.oneSizeJsonMapToMap(API_RESPONSE_PARAMS).containsKey("data"))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        JSONObject data = JSON.parseObject(HandlerUtil.oneSizeJsonMapToMap(API_RESPONSE_PARAMS).get("data"));
        String ordernumberR = data.getString(oid);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[易起付2]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        JSONObject data = JSON.parseObject(HandlerUtil.oneSizeJsonMapToMap(API_RESPONSE_PARAMS).get("data"));
        // $mid.$oid.$amt.$way.$code.$secretk
        String paramsStr = String.format("%s%s%s%s%s%s",
                data.get(mid),
                data.get(oid),
                data.get(amt),
                data.get(way),
                data.get(code),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[易起付2]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }



    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
        JSONObject data = JSON.parseObject(HandlerUtil.oneSizeJsonMapToMap(API_RESPONSE_PARAMS).get("data"));
        boolean checkResult = false;
        String payStatus = data.getString(code);
        String responseAmount =  HandlerUtil.getFen(data.getString(tamt));
        //boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        boolean checkAmount =  HandlerUtil.isRightAmount(amountDb,responseAmount,"100");//第三方回调金额差额1元内
        if (checkAmount && payStatus.equalsIgnoreCase("100")) {
            checkResult = true;
        } else {
            log.error("[易起付2]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatus + " ,支付金额：" + responseAmount + " ，应支付金额：" + amountDb);
        }
        log.debug("[易起付2]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + checkResult + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amountDb + ",第三方响应支付成功标志:" + payStatus + " ,计划成功：1");
        return checkResult;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        JSONObject data = JSON.parseObject(HandlerUtil.oneSizeJsonMapToMap(API_RESPONSE_PARAMS).get("data"));
        boolean result = data.getString(sign).equalsIgnoreCase(signMd5);
        log.debug("[易起付2]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[易起付2]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}