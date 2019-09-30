package dc.pay.business.abczhifu;

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
@ResponsePayHandler("ABCZHIFU")
public final class ABCZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

//    参数名称			参数含义			参与签名			参数说明
//    responseCode		响应码			是				正常0000
//    responseRemark	响应备注			是	
//    merchantNo		商户号			是				平台分配商户号
//    paySign			支付类型			是				参考文档底部:支付标识
//    sysOrderId		平台订单号		是				商户可保存此订单号,订单查询优先使用此订单号
//    merchantOrderNo	商户订单号		是	
//    nonceStr			随机字符串		是	
//    signType			签名方式			是				HMAC_SHA256或RSA(以请求接口的签名方式返回)
//    amount			支付金额			是	
//    payTime			支付时间			是				格式:20190630092845
//    payStatus			支付状态			是				支付成功:SUCCESS,等待支付:WAITTING,无此订单:NO_ORDER
//    extra				附加字段			否				商户订单生成时附加数据
//    signMsg			签名字符串		否				签名算法参考文档下方:签名算法

    private static final String responseCode                   ="responseCode";
    private static final String merchantNo                     ="merchantNo";
    private static final String merchantOrderNo                ="merchantOrderNo";
    private static final String amount                		   ="amount";
    private static final String payStatus                	   ="payStatus";
    private static final String responseRemark                 ="responseRemark";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="signMsg";

    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("SUCCESS");

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(merchantNo);
        String ordernumberR = API_RESPONSE_PARAMS.get(merchantOrderNo);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[ABC支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
    	List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
    	paramKeys.remove(signature);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
            	if(paramKeys.get(i).equals(responseRemark)){
            		signSrc.append(paramKeys.get(i)).append("=").append(HandlerUtil.UrlEncode(api_response_params.get(paramKeys.get(i)))).append("&");
            	}else{
            		signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            	}
            }
        }
        //最后一个&转换成#
        signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "" );
        String paramsStr = signSrc.toString();
        String signMD5 =HMACSHA256.sha256_HMAC(paramsStr, channelWrapper.getAPI_KEY()).toUpperCase();
        log.debug("[ABC支付]-[响应支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return signMD5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        String payStatusCode = api_response_params.get(payStatus);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(amount));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        boolean checkAmount = HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差
        //1代表第三方支付成功
        if (checkAmount&&payStatusCode.equalsIgnoreCase("SUCCESS")) {
            my_result = true;
        } else {
            log.error("[ABC支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[ABC支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：SUCCESS");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[ABC支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[ABC支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return  RESPONSE_PAY_MSG;
    }
}