package dc.pay.business.baijiezhifu;

import java.io.UnsupportedEncodingException;
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
@ResponsePayHandler("BAIJIEZHIFU")
public final class BaiJieZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

//    参数名					含义						说明
//    account_name			商户登录名（会员名），识别是哪个会员名回调过来的请求	admin
//    pay_time				支付成功的时间戳（10位）	1529775437
//    status				支付状态，支付状态只有成功一个状态（success）	success
//    amount				支付金额	1.00
//    out_trade_no			订单信息，在发起订单时附加的信息，如用户名，充值订单号等字段参数	2018062312410711888
//    trade_no				交易流水号，由系统生成的交易流水号	018062312410729584
//    fees					手续费，本次回调过程产生的手续费用（已经在平台账户中扣除）	004
//    sign					签名算法，在支付时进行签名算法，详见《支付签名算法》	d92eff67b3be05f5e61502e96278d01b
//    callback_time			回调时间，在回调时产生的时间戳（10位）	1529775437
//    type					当前订单支付类型，1为微信，2为支付宝	1
//    account_key			商户KEY（S_KEY）	该值为空，忽略

    private static final String account_name                   ="account_name";
    private static final String pay_time                       ="pay_time";
    private static final String status                         ="status";
    private static final String amount                		   ="amount";
    private static final String out_trade_no             	   ="out_trade_no";
    private static final String trade_no                 	   ="trade_no";
    private static final String fees              			   ="fees";
    private static final String callback_time              	   ="callback_time";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("success");

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(out_trade_no);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[百捷支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
    	String signSrc=String.format("%s%s", 
      		  api_response_params.get(amount),
      		  api_response_params.get(out_trade_no)
        );
        String paramsStr = signSrc.toString();
        String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        byte[] signByte=null;
  	    try {
  		  signByte = Sign.encry_RC4_byte(signMD5.getBytes("UTF-8"), channelWrapper.getAPI_KEY());
  	    } catch (UnsupportedEncodingException e) {
  		e.printStackTrace();
  	    }
        String signStr=HandlerUtil.getMD5UpperCase(signByte).toLowerCase();
        log.debug("[百捷支付]-[响应支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signStr));
        return signStr;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        String payStatusCode = api_response_params.get(status);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(amount));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        boolean checkAmount = HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差
        //1代表第三方支付成功
        if (checkAmount&&payStatusCode.equalsIgnoreCase("success")) {
            my_result = true;
        } else {
            log.error("[百捷支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[百捷支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：success");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[百捷支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[百捷支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return  RESPONSE_PAY_MSG;
    }
}