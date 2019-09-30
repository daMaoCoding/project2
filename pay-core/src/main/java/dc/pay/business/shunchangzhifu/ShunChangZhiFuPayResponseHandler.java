package dc.pay.business.shunchangzhifu;

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
@ResponsePayHandler("SHUNCHANGZHIFU")
public final class ShunChangZhiFuPayResponseHandler extends PayResponseHandler {
      //private final Logger log = LoggerFactory.getLogger(getClass());
      private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("success");


     private static final String  platform_trade_no = "platform_trade_no";  //  "": "A20181010133629353304",
     private static final String  orderid = "orderid";  //  "": "20181010133622",
     private static final String  transaction_id = "transaction_id";  //  "": "2018101022001472540572973384",
     private static final String  price = "price";  //  "": "1.00",
     private static final String  realprice = "realprice";  //  "": "1.00",
     private static final String  orderuid = "orderuid";  //  "": "1001286576",
     private static final String  attach = "attach";  //  "": "20181010133622",
     private static final String  key = "key";  //  "": "8eb64775803940b8a7e8459348ca7b45"

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(orderid);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[顺畅支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        // orderid + orderuid + platform_trade_no + price + realprice + token
        String paramsStr = String.format("%s%s%s%s%s%s",
                params.get(orderid),
                params.get(orderuid),
                params.get(platform_trade_no),
                params.get(price),
                params.get(realprice),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[顺畅支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }



    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean checkResult = false;
       // String payStatus = api_response_params.get(opstate);
        String responseAmount =  HandlerUtil.getFen(api_response_params.get(realprice));
        //boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        boolean checkAmount =  HandlerUtil.isRightAmount(amountDb,responseAmount,"100");//第三方回调金额差额1元内
        if (checkAmount ) {
            checkResult = true;
        } else {
            log.error("[顺畅支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",支付金额：" + responseAmount + " ，应支付金额：" + amountDb);
        }
        log.debug("[顺畅支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + checkResult + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amountDb + ",第三方响应支付成功标志: ,计划成功：1");
        return checkResult;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(key).equalsIgnoreCase(signMd5);
        log.debug("[顺畅支付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[顺畅支付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}