package dc.pay.business.paiyiyunjifeizhifu;


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
 * 
 * @author andrew
 * Aug 10, 2019
 */
@ResponsePayHandler("PAIYIYUNJIFEIZHIFU")
public final class PaiYiYunJiFeiZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //参数名 参数  加入签名    说明
    //商户号 partner Y   商户号
    private static final String partner                   ="partner";
    //商户订单号   orderid Y   上行过程中商户系统传入的orderid
    private static final String orderid                   ="orderid";
    //订单结果    opstate Y   0:支付成功，非0为支付失败
    private static final String opstate                   ="opstate";
    //订单金额    ovalue  Y   单位元（人民币）
    private static final String ovalue                   ="ovalue";
    //支付系统订单号 sysorderid  N   此次交易中支付接口系统内的订单ID
//    private static final String sysorderid                   ="sysorderid";
    //备注信息    attach  N   备注信息，上行中attach原样返回
    private static final String attach                   ="attach";
    //MD5签名   sign    N   32位小写MD5签名值
//    private static final String sign                   ="sign";

    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "ok";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(partner);
        String ordernumberR = API_RESPONSE_PARAMS.get(orderid);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[派易云计费支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        StringBuilder signStr = new StringBuilder();
        signStr.append(partner+"=").append(api_response_params.get(partner)).append("&");
        signStr.append(orderid+"=").append(api_response_params.get(orderid)).append("&");
        signStr.append(opstate+"=").append(api_response_params.get(opstate)).append("&");
        signStr.append(ovalue+"=").append(api_response_params.get(ovalue));
        signStr.append(channelWrapper.getAPI_KEY());
        String paramsStr =signStr.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[派易云计费支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //订单结果  opstate Y   0:支付成功，非0为支付失败
        String payStatusCode = api_response_params.get(opstate);
        //订单金额             ovalue             Y            订单实际支付金额，单位元
        String responseAmount = HandlerUtil.getFen(api_response_params.get(ovalue));
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        
        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差
        
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("0")) {
            my_result = true;
        } else {
            log.error("[派易云计费支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[派易云计费支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：0");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[派易云计费支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }
    
    @Override
    protected String responseSuccess() {
        log.debug("[派易云计费支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }

}