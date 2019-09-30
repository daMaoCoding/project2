package dc.pay.business.yunhuitong;

/**
 * ************************
 * @author tony 3556239829
 */

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.kspay.AESUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.TreeMap;

@ResponsePayHandler("YUNHUITONG")
public final class YunHuiTongPayResponseHandler extends PayResponseHandler {
      private final Logger log = LoggerFactory.getLogger(getClass());
      private static final String RESPONSE_PAY_MSG = "SUCCESS";
      private static final String SUCCESS = "SUCCESS";

      private static final String  amount = "amount"; // "10.00",
      private static final String  code = "code"; // "200",
      private static final String  bizMsg = "bizMsg"; // "操作成功",
      private static final String  bizCode = "bizCode"; // "1",
      private static final String  isCalFee = "isCalFee"; // "0",
      private static final String  serverRequestNo = "serverRequestNo"; // "727025930277462016",
      private static final String  requestNo = "requestNo"; // "20180824135710",
      private static final String  merchantNo = "merchantNo"; // "121808238077",
      private static final String  status = "status"; // "SUCCESS"

      private static final String  bizType = "bizType";  // "biz    Type": "PAY",
      private static final String data="data"; //         "data": "5CFE6391A91D08F0B8AD6704113DA6D01FED0AF7AD7FBF14E041332670ADD6DE9BAF7B7143291588B9371CEA112A23F67E22884559C24C47241F0678C182B33173C2A5520A2A26CEF890BABA98AE353AD8DA53BD2DE930A71858AF0D8E37F59BB59025224F6AD82251B36F4346E65DD777AB10BF2BAF943B7271B3BD8795B81AB945003C6885EBEDA4759132181E489FB03762D30C8C06F270538490497E1A4400B356B08EAFD331FEAE5A2A17AC01AB5BE329B5B7BCA3687EA7E511D82331D4B24F7B721F7BEBCC3E71775FDEA53BC9"
      private static  final String  orderId = "orderId";  //"orderId": "20180824135710",




    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty()|| !API_RESPONSE_PARAMS.containsKey(orderId) || StringUtils.isBlank(API_RESPONSE_PARAMS.get(orderId))   || !API_RESPONSE_PARAMS.containsKey(data) || StringUtils.isBlank(API_RESPONSE_PARAMS.get(data)) )
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(orderId);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[云汇通]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        // opstate=opstate值&orderid=orderid值&ovalue=ovalue值&parter=商家接口调用ID&key=商家
//        String paramsStr = String.format("opstate=%s&orderid=%s&ovalue=%s&parter=%s&key=%s",
//                params.get(opstate),
//                params.get(orderid),
//                params.get(ovalue),
//                params.get(parter),
//                channelWrapper.getAPI_KEY());
//        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
//        log.debug("[云汇通]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return "true";
    }



    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
        String  responseJSONStr = AESUtil.decrypt( api_response_params.get(data), channelWrapper.getAPI_KEY().substring(0, 16));
        Map<String, String> resultMap = JSON.parseObject(responseJSONStr,new TypeReference<TreeMap<String, String>>() {});
        boolean checkResult = false;
        if(resultMap.containsKey(code) && resultMap.get(code).equalsIgnoreCase("200")  &&   resultMap.get(bizCode).equalsIgnoreCase("1")  ){
            String payStatus = resultMap.get(status);
            String responseAmount =  HandlerUtil.getFen(resultMap.get(amount));
            boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
            if (checkAmount && payStatus.equalsIgnoreCase(SUCCESS)) {
                checkResult = true;
            } else {
                log.error("[云汇通]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatus + " ,支付金额：" + responseAmount + " ，应支付金额：" + amountDb);
            }
            log.debug("[云汇通]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + checkResult + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amountDb + ",第三方响应支付成功标志:" + payStatus + " ,计划成功：1");
        }





        return checkResult;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
      //  boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
      //  log.debug("[云汇通]-[响应支付]-4.验证MD5签名：" + result);
        return true;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[云汇通]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}