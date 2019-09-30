package dc.pay.business.tongsao;

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
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Dec 5, 2017
 */
@ResponsePayHandler("TONGSAO")
public final class TongSaoPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //merchno		商户号	15	是	　
    private static final String merchno  ="merchno";
    //status		交易状态	1	是	0-未支付	1-支付成功		2-支付失败
    private static final String status  ="status";
    //traceno		商户流水号	30	是	商家的流水号
    private static final String traceno  ="traceno";
    //orderno		系统订单号	12	是	系统订单号,同上面接口的refno。
    private static final String orderno  ="orderno";
    //merchName	商户名称	30	是	　
    private static final String merchname  ="merchName";
    //amount		交易金额	12	是	单位/元
    private static final String amount  ="amount";
    //transDate	交易日期	10	是	　
    private static final String transdate  ="transDate";
    //transTime	交易时间	8	是	　
    private static final String transtime  ="transTime";
    //payType		支付方式	1	是	1-支付宝    2-微信    3-百度钱包    4-QQ钱包    5-京东钱包	
    private static final String paytype  ="payType";
    //openId	用户OpenId	50	否	支付的时候返回
    private static final String openid  ="openId";
    //signature	数据签名	32	是	　
    private static final String signature  ="signature";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(merchno);
        String ordernumberR = API_RESPONSE_PARAMS.get(traceno);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[通扫]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        StringBuilder signStr = new StringBuilder();
        signStr.append(amount+"=").append(api_response_params.get(amount)).append("&");
        signStr.append(merchname+"=").append(api_response_params.get(merchname)).append("&");
        signStr.append(merchno+"=").append(api_response_params.get(merchno)).append("&");
        if (null != api_response_params.get(openid)) {
        	signStr.append(openid+"=").append(api_response_params.get(openid)).append("&");
		}
        signStr.append(orderno+"=").append(api_response_params.get(orderno)).append("&");
        signStr.append(paytype+"=").append(api_response_params.get(paytype)).append("&");
        signStr.append(status+"=").append(api_response_params.get(status)).append("&");
        signStr.append(traceno+"=").append(api_response_params.get(traceno)).append("&");
        signStr.append(transdate+"=").append(api_response_params.get(transdate)).append("&");
        signStr.append(transtime+"=").append(api_response_params.get(transtime)).append("&");
        signStr.append(channelWrapper.getAPI_KEY());
        String paramsStr =signStr.toString();
        
//    	//使用对方返回的数据进行签名
//        String paramsStr = String.format(amount+"=%s&"+merchname+"=%s&"+merchno+"=%s&"+openid+"=%s&"+orderno+"=%s&"+paytype+"=%s&"+status+"=%s&"+traceno+"=%s&"+transdate+"=%s&"+transtime+"=%s&%s",
//                api_response_params.get(amount),
//                api_response_params.get(merchname),
//                api_response_params.get(merchno),
//                api_response_params.get(openid),
//                api_response_params.get(orderno),
//                api_response_params.get(paytype),
//                api_response_params.get(status),
//                api_response_params.get(traceno),
//                api_response_params.get(transdate),
//                api_response_params.get(transtime),
//                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[通扫]-[请求支付]-2.生成加密URL签名完成，参数：" + JSON.toJSONString(paramsStr) +" ,值："+ JSON.toJSONString(signMd5));
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean result = false;
        //status		交易状态	1	是	0-未支付	1-支付成功		2-支付失败
        String payStatusCode = api_response_params.get(status);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(amount));
        //amount数据库存入的是分 	第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("1")) {
            result = true;
        } else {
            log.error("[通扫]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[通扫]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[通扫]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[通扫]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}