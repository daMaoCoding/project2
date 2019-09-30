package dc.pay.business.rongjinzhifu;

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
 * Dec 14, 2018
 */
@ResponsePayHandler("RONGJINFUZHIFU")
public final class RongJinFuZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

//    序号				参数名称				参数名			类型				可否为空				说明
//    1	APP编号			app_id				Number			必填	
//    2	商户订单号		order_id			String			必填	
//    3	支付流水号		pay_seq				String			必填				由支付平台生成，唯一，不超过30个字符
//    4	实付金额			pay_amt				Number			必填				建议商户系统校验实付的金额以确定充值的正确性
//    5	支付结果			pay_result			Number			必填				20—支付成功，其它为失败，目前值通知成功的订单
//    6	支付结果描述		result_desc			string			选填				支付结果描述
//    7	扩展参数			extends				String			选填				商户自定义参数或扩展参数
//    8	签名				sign				String			必填				参数机制（参见2.4	HTTP参数签名机制）参数组成（参见下面的签名参数说明）

    private static final String app_id                   	="app_id";
    private static final String order_id                    ="order_id";
    private static final String pay_seq                  	="pay_seq";
    private static final String pay_amt                		="pay_amt";
    private static final String pay_result             		="pay_result";
    private static final String result_desc                 ="result_desc";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("ok");

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(app_id);
        String ordernumberR = API_RESPONSE_PARAMS.get(order_id);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[融金付支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
    	String signSrc=String.format("%s%s%s%s%s%s", 
    			app_id+"="+api_response_params.get(app_id)+"&",
    			order_id+"="+api_response_params.get(order_id)+"&",
    			pay_seq+"="+api_response_params.get(pay_seq)+"&",
    			pay_amt+"="+api_response_params.get(pay_amt)+"&",
    			pay_result+"="+api_response_params.get(pay_result)+"&",
    			key+"="+HandlerUtil.getMD5UpperCase(channelWrapper.getAPI_KEY()).toLowerCase()
    	);
        String paramsStr = signSrc.toString();
        String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[融金付支付]-[响应支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return signMD5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        String payStatusCode = api_response_params.get(pay_result);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(pay_amt));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        boolean checkAmount = HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差
        //1代表第三方支付成功
        if (checkAmount&&payStatusCode.equalsIgnoreCase("20")) {
            my_result = true;
        } else {
            log.error("[融金付支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[融金付支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：20");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[融金付支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[融金付支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return  RESPONSE_PAY_MSG;
    }
}