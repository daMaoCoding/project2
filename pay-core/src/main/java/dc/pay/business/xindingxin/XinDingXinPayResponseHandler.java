package dc.pay.business.xindingxin;

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
@ResponsePayHandler("XINDINGXIN")
public final class XinDingXinPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

//    参数名					参数					说明
//    商户订单号				orderid				上行过程中商户系统传入的orderid。
//    订单结果				opstate				0：支付成功 其余值都是支付失败
//    订单提交金额			orderamt			单位元,如1.00
//    实际支付金额			ovalue				单位元，如0.98
//    新鼎信订单号			sysorderid			此次订单过程中新鼎信接口系统内的订单Id
//    订单时间				completiontime		此次订单过程中新鼎信接口系统内的订单结束时间。如2010/04/05 21:50:58
//    备注信息				attach				备注信息，上行中attach原样返回
//    订单结果说明			msg					订单结果说明
//    MD5签名				sign				对orderid={}&opstate={}&ovalue={}key进行MD5加密，key是商户密钥

    private static final String orderid                    ="orderid";
    private static final String opstate                    ="opstate";
    private static final String orderamt                   ="orderamt";
    private static final String ovalue                	   ="ovalue";
    private static final String sysorderid             	   ="sysorderid";
    private static final String completiontime             ="completiontime";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("ok");

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(orderid);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[新鼎信支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
    	String signSrc=String.format("%s%s%s%s",
    			orderid+"="+api_response_params.get(orderid)+"&",
    			opstate+"="+api_response_params.get(opstate)+"&",
    			ovalue+"="+api_response_params.get(ovalue),
    			channelWrapper.getAPI_KEY()
    	);
        String paramsStr = signSrc.toString();
        String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[新鼎信支付]-[响应支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return signMD5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        String payStatusCode = api_response_params.get(opstate);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(orderamt));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount&&payStatusCode.equalsIgnoreCase("0")) {
            my_result = true;
        } else {
            log.error("[新鼎信支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[新鼎信支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：0");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[新鼎信支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[新鼎信支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return  RESPONSE_PAY_MSG;
    }
}