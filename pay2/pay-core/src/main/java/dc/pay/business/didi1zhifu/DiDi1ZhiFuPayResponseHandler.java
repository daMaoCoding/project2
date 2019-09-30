package dc.pay.business.didi1zhifu;

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
 * @author sunny
 * Dec 14, 2018
 */
@ResponsePayHandler("DIDI1ZHIFU")
public final class DiDi1ZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

//    字段名				类型				描述				必填				备注
//    orderSn			string			PDD订单号		否				下单成功返回PDD订单号
//    outTradeNo		string			订单号			是				用户唯一订单号
//    userAgent			string			客户浏览器		否				默认AlipayClient   微信MicroMessenger
//    appId				string			appId			是				平台唯一用户ID
//    money				string			订单金额			是				单位：元，保留两位小数
//    respMsg			string			返回信息			是	
//    respCode			string			返回码		
//    status			string			状态码			是				2=支付成功；7=创建时间；8=创建失败；9=支付超时；10=已发货；11=已收货
//    sign				string			签名				是	

    private static final String orderSn                   	  	="orderSn";
    private static final String outTradeNo                    	="outTradeNo";
    private static final String userAgent                  		="userAgent";
    private static final String appId                			="appId";
    private static final String money             				="money";
    private static final String respMsg             			="respMsg";
    private static final String respCode             			="respCode";
    private static final String status             				="status";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("success");

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(appId);
        String ordernumberR = API_RESPONSE_PARAMS.get(outTradeNo);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[迪迪支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
    	boolean signMD5=false;
  	    try {
  			signMD5 = RSA2Util.rsaCheck(api_response_params,channelWrapper.getAPI_PUBLIC_KEY());
  	    } catch (Exception e) {
  			e.printStackTrace();
  	    }
        log.debug("[迪迪支付]-[响应支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return String.valueOf(signMD5);
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        String payStatusCode = api_response_params.get(status);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(money));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        boolean checkAmount = HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差
        //1代表第三方支付成功
        if (checkAmount&&payStatusCode.equalsIgnoreCase("2")) {
            my_result = true;
        } else {
            log.error("[迪迪支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[迪迪支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：2");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = "true".equalsIgnoreCase(signMd5);
        log.debug("[迪迪支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[迪迪支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return  RESPONSE_PAY_MSG;
    }
}