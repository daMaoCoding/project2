package dc.pay.business.sihaiyun;

import java.util.List;
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
import dc.pay.utils.MapUtils;
import dc.pay.utils.Sha1Util;

/**
 * 
 * @author sunny
 * 03 21, 2019
 */
@ResponsePayHandler("SIHAIYUN")
public final class SiHaiYunPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

//    参数名称	参数含义		是否必填		参数长度		参数类型			参数说明	签名顺序
//    p1_MerId	商户编号		是			Max(11)		Int				商户在系统的唯一身份标识.获取方式请联系客服	0
//    r0_Cmd	业务类型		是			Max(20)		String			固定值“Buy”.	1
//    r1_Code	支付状态码	是			Max(11)		Int				1：成功，4：失败	2
//    r2_TrxId	平台流水号	是			Max(50)		String			平台流水号	3


    private static final String p1_MerId                   	="p1_MerId";
    private static final String r0_Cmd                     	="r0_Cmd";
    private static final String r1_Code                    	="r1_Code";
    private static final String r2_TrxId                   	="r2_TrxId";
    private static final String r3_Amt             			="r3_Amt";
    private static final String r4_Cur                 		="r4_Cur";
    private static final String r5_Pid              		="r5_Pid";
    private static final String r6_Order              		="r6_Order";
    private static final String r7_Uid              		="r7_Uid";
    private static final String r8_MP              			="r8_MP";
    private static final String r9_BType              		="r9_BType";
    private static final String rp_PayDate              	="rp_PayDate";
    private static final String hmac              			="hmac";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="hmac";

    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("success");

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(p1_MerId);
        String ordernumberR = API_RESPONSE_PARAMS.get(r6_Order);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[四海云付支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
    	String signSrc=String.format("%s%s%s%s%s%s%s%s%s%s%s",
    			api_response_params.get(p1_MerId),
    			api_response_params.get(r0_Cmd),
    			api_response_params.get(r1_Code),
    			api_response_params.get(r2_TrxId),
    			api_response_params.get(r3_Amt),
    			api_response_params.get(r4_Cur),
    			api_response_params.get(r5_Pid),
    			api_response_params.get(r6_Order),
    			api_response_params.get(r7_Uid),
    			api_response_params.get(r8_MP),
    			api_response_params.get(r9_BType)
    	);
        String paramsStr = signSrc.toString();
        String signMD5 = DigestUtil1.hmacSign(paramsStr, channelWrapper.getAPI_KEY());
        log.debug("[四海云付支付]-[响应支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return signMD5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        String payStatusCode = api_response_params.get(r1_Code);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(r3_Amt));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount&&payStatusCode.equalsIgnoreCase("1")) {
            my_result = true;
        } else {
            log.error("[四海云付支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[四海云付支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[四海云付支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[四海云付支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return  RESPONSE_PAY_MSG;
    }
}