package dc.pay.business.wanbaofu2zhifu;

import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.alibaba.fastjson.JSON;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;

/**
 * @author Cobby
 * Mar 25, 2019
 */
@ResponsePayHandler("WANBAOFU2ZHIFU")
public final class WanBaoFu2ZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private static final String RESPONSE_PAY_MSG = "ErrCode=0";

    private static final String  P_UserId  ="P_UserId";// "100000",
    private static final String  P_OrderId  ="P_OrderId";// "20180804171611",
    private static final String  P_CardId  ="P_CardId";// "",
    private static final String  P_CardPass  ="P_CardPass";// "",
    private static final String  P_FaceValue  ="P_FaceValue";// "0.23000",
    private static final String  P_ChannelId  ="P_ChannelId";// "89",
    private static final String  P_PayMoney  ="P_PayMoney";// "0.23",
    private static final String  P_ErrCode  ="P_ErrCode";// "0",
    private static final String  P_PostKey  ="P_PostKey";// "22e8425b6e2a667e238b6933cab24615",


    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(P_OrderId);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[万宝付2支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        // P_UserId|P_OrderId|P_CardId|P_CardPass|P_FaceValue|P_ChanneHd|P_PayMoney|P_ErrCode|Key
        String paramsStr = String.format("%s|%s|%s|%s|%s|%s|%s|%s|%s",
                params.get(P_UserId),
                params.get(P_OrderId),
                params.get(P_CardId),
                params.get(P_CardPass),
                params.get(P_FaceValue),
                params.get(P_ChannelId),
                params.get(P_PayMoney),
                params.get(P_ErrCode),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[万宝付2支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }



    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean checkResult = false;
        String payStatus = api_response_params.get(P_ErrCode);
        String responseAmount =  HandlerUtil.getFen(api_response_params.get(P_PayMoney));
        boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        if (checkAmount && payStatus.equalsIgnoreCase("0")) {
            checkResult = true;
        } else {
            log.error("[万宝付2支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatus + " ,支付金额：" + responseAmount + " ，应支付金额：" + amountDb);
        }
        log.debug("[万宝付2支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + checkResult + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amountDb + ",第三方响应支付成功标志:" + payStatus + " ,计划成功：0");
        return checkResult;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(P_PostKey).equalsIgnoreCase(signMd5);
        log.debug("[万宝付2支付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[万宝付2支付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}