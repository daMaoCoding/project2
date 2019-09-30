package dc.pay.business.feigezhifu3;

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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.util.Map;

@Slf4j
@ResponsePayHandler("FEIGEZHIFU3")
public final class FeiGeZhiFu3PayResponseHandler extends PayResponseHandler {
      //private final Logger log = LoggerFactory.getLogger(getClass());
      private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("success ");

     private static final String money ="money";  // "0.01",
     private static final String order ="order";  // "154260647497519752",
     private static final String remark ="remark";  // "154248020848995451",
     private static final String record ="record";  // "20181119134753737559",
     private static final String sdk_name ="sdk_name";  // "17815392543",
     private static final String sign ="sign";  // "e578a7012ec44a7c62e3d3ec1f22ea1a"

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(record);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[飞鸽支付3]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        //md5($order.$money.$record.$key.$remark.$sdk_name)
        String paramsStr = String.format("%s%s%s%s%s%s",
                params.get(order),
                params.get(money),
                params.get(record),
                channelWrapper.getAPI_KEY(),
                params.get(remark),
                params.get(sdk_name));
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[飞鸽支付3]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }



    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean checkResult = false;

        String responseAmount =  HandlerUtil.getFen(api_response_params.get(money));
        //boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        boolean checkAmount =  HandlerUtil.isRightAmount(amountDb,responseAmount,"100");//第三方回调金额差额1元内
        if (checkAmount) {
            checkResult = true;
        } else {
            log.error("[飞鸽支付3]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() +  " ,支付金额：" + responseAmount + " ，应支付金额：" + amountDb);
        }
        log.debug("[飞鸽支付3]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + checkResult + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amountDb );
        return checkResult;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("[飞鸽支付3]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[飞鸽支付3]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}