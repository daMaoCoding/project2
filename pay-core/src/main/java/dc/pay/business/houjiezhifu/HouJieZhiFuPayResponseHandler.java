package dc.pay.business.houjiezhifu;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;

import dc.pay.business.caifubao.StringUtil;
import dc.pay.utils.HmacSha256Util;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;


/**
 * @author Cobby
 * Jan 31, 2019
 */
@ResponsePayHandler("HOUJIEZHIFU")
public final class HouJieZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

// “appOrderId”: //由商家系统内部⽣成的订单ID(必填，且这个appOrderId也要在商家系统内保证全局永	久唯⼀，否则会发⽣订单混乱问题)
// "orderAmount": //必填，订单的⾦额（如果商家是⾃定义币种，则为商家的币种数量）
// "orderCoinSymbol": "CNY", //本次订单中下单⾦额的单位
// "orderStatus": //订单状态
    private static final String appOrderId                  ="appOrderId";
    private static final String orderAmount                 ="orderAmount";
    private static final String orderStatus                 ="orderStatus";
    private static final String statusReason                ="statusReason";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  =    "jrddSignContent";

    private static final String RESPONSE_PAY_MSG = "SUCCESS";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {


        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(appOrderId);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[后捷支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {

	    StringBuilder signSrc = new StringBuilder();
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!signature.equals(paramKeys.get(i))) {
	            try {
		            String encode="";
		            if (StringUtils.isNotBlank(String.valueOf(params.get(paramKeys.get(i))))){
			            encode = URLEncoder.encode(String.valueOf(params.get(paramKeys.get(i))), "UTF-8");
		            }
		            signSrc.append(paramKeys.get(i)).append("=").append(encode).append("&");
	            } catch (UnsupportedEncodingException e) {
		            e.printStackTrace();
	            }
            }
        }
	    //删除最后一个字符
	    signSrc.deleteCharAt(signSrc.length()-1);
        String paramsStr = signSrc.toString();
	    String signMd5 = HmacSha256Util.digest(paramsStr, channelWrapper.getAPI_KEY()).toLowerCase();
        log.debug("[后捷支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //status        orderStatus=7 成功
        String payStatusCode = api_response_params.get(orderStatus);
        String payStatusReason = api_response_params.get(statusReason);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(orderAmount));

        //tony(Tony) 01-17 15:09:28
        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
//        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("7")) {
            my_result = true;
	        log.debug("[后捷支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：7");
        } else if (checkAmount && payStatusCode.equalsIgnoreCase("5")&&payStatusReason.equalsIgnoreCase("19")){
	        my_result = true;
	        log.debug("[后捷支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusReason + " ,计划成功：19");
        }else {
	        log.error("[后捷支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        return my_result;
    }

    
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
	    boolean my_result =false;
	    if (api_response_params.containsKey(signature) && StringUtils.isNotBlank(api_response_params.get(signature))){
		    my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
	    }
        log.debug("[后捷支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }


    @Override
    protected String responseSuccess() {
        log.debug("[后捷支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}