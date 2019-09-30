package dc.pay.business.beijingezhifu;

import com.alibaba.fastjson.JSON;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author Cobby
 * June 14, 2018
 */
@ResponsePayHandler("BEIJINGEZHIFU")
public final class BeiJingEZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String partner     = "partner";     //     是     string     商户号
    private static final String ordernumber = "ordernumber"; //     是     string     商户订单号
    private static final String orderstatus = "orderstatus"; //     是     string     支付状态（1=支付，0=未支付）
    private static final String paymoney    = "paymoney";    //     是     string     支付金额
//  private static final String sysnumber    ="sysnumber";   //     是     string     系统订单号
//  private static final String attach       ="attach";      //     是     string     备注

    // sign    是     string     签名
    private static final String sign = "sign";

    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("ok");

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR     = API_RESPONSE_PARAMS.get(partner);
        String ordernumberR = API_RESPONSE_PARAMS.get(ordernumber);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[北京E支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        //partner=1&ordernumber=20180926155113&orderstatus=1&paymoney=10
        String paramsStr = String.format("partner=%s&ordernumber=%s&orderstatus=%s&paymoney=%s%s",
                api_response_params.get(partner),
                api_response_params.get(ordernumber),
                api_response_params.get(orderstatus),
                api_response_params.get(paymoney),
                channelWrapper.getAPI_KEY());
        String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[北京E支付]-[响应支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return String.valueOf(signMD5);
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result      = false;
        String  payStatusCode  = api_response_params.get(orderstatus);
        String  responseAmount = HandlerUtil.getFen(api_response_params.get(paymoney));
        boolean checkAmount    =  HandlerUtil.isRightAmount(db_amount,responseAmount,"100");//第三方回调金额差额1元内
        //1代表第三方支付成功
        if (checkAmount&&payStatusCode.equalsIgnoreCase("1")) {
            my_result = true;
        } else {
            log.error("[北京E支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[北京E支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("[北京E支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[北京E支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return  RESPONSE_PAY_MSG;
    }
}