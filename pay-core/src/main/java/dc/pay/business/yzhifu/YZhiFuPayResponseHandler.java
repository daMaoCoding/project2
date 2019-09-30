package dc.pay.business.yzhifu;

/**
 * ************************
 * @author tony 3556239829
 */

import com.alibaba.fastjson.JSON;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import org.apache.commons.lang.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
@ResponsePayHandler("YZHIFU")
public final class YZhiFuPayResponseHandler extends PayResponseHandler {
      //private final Logger log = LoggerFactory.getLogger(getClass());
      private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("success");

     private static final String  order_id  = "order_id";   // "824",
     private static final String  out_order_id  = "out_order_id";   // "20181008150741",
     private static final String  price  = "price";   // "1.00",
     private static final String  realprice  = "realprice";   // "1.00",
     private static final String  type  = "type";   // "alipay",
     private static final String  paytime  = "paytime";   // "1538982530",
     private static final String  extend  = "extend";   // "20181008150741",
     private static final String  sign  = "sign";   // "5d264e77608bfbf623d6b65b65a75bb1"


    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(out_order_id);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[Y支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        // md5(md5(order_id+out_order_id+price+realprice+type+paytime+extend)+secretkey)
        String paramsStr = String.format("%s%s%s%s%s%s%s",
                params.get(order_id),
                params.get(out_order_id),
                params.get(price),
                params.get(realprice),
                params.get(type),
                params.get(paytime),
                params.get(extend));
        String md5_1 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        String signMd5 =HandlerUtil.getMD5UpperCase( md5_1.concat(channelWrapper.getAPI_KEY())).toLowerCase();
        log.debug("[Y支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }



    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean checkResult = false;
        String payStatus = api_response_params.get(sign);
        String responseAmount =  HandlerUtil.getFen(api_response_params.get(realprice));
        //boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        boolean checkAmount =  HandlerUtil.isRightAmount(amountDb,responseAmount,"100");//第三方回调金额差额1元内
        if (checkAmount) {
            checkResult = true;
        } else {
            log.error("[Y支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatus + " ,支付金额：" + responseAmount + " ，应支付金额：" + amountDb);
        }
        log.debug("[Y支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + checkResult + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amountDb + ",第三方响应支付成功标志:" + payStatus + " ,计划成功：1");
        return checkResult;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("[Y支付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[Y支付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}