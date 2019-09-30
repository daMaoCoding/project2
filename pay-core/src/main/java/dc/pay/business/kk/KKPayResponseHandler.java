package dc.pay.business.kk;

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

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * May 31, 2018
 */
@ResponsePayHandler("KK")
public final class KKPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

	//字段				输入项名称			属性		长度		注释					
	//versionId			服务版本号			必输		1		 1.0当前				
	//transType			交易类别			必输		4		默认填写 0008				
	//asynNotifyUrl		异步通知URL		必输		200		结果返回URL，1.7接口用到。支付系统处理完请求后，将处理结果返回给这个URL	
	//synNotifyUrl		同步返回URL		必输		120		针对该交易的交易状态同步通知接收URL	
	//merId				商户编号			必输		30							
	//orderStatus		订单状态			必输		2		01支付成功 00未支付 02支付处理中	
	//orderAmount		支付金额			必输					
	//prdOrdNo			商户订单号			必输		30			
	//payId				支付平台订单号		必输		32			
	//payTime			支付成功时间		必输		14		yyyyMMddHHmmss
	//signType			加密方式			必输		3		Md5默认	
	//signData			加密数据			必输		500		signTgpe为MD5时：将把所有参数按名称a-z排序,并且按key=value格式用“&”符号拼接起来,遇到key为空值的参数不参加签名，在字符串的最后还需拼接上MD5加密key，如果字符串中有中文在MD5加密时还需用UTF-8编码
//	private static final String versionId		="versionId";
//	private static final String transType		="transType";
//	private static final String asynNotifyUrl	="asynNotifyUrl";
//	private static final String synNotifyUrl	="synNotifyUrl";
	private static final String merId			="merId";
	private static final String orderStatus		="orderStatus";
	private static final String orderAmount		="orderAmount";
	private static final String prdOrdNo		="prdOrdNo";
//	private static final String payId			="payId";
//	private static final String payTime			="payTime";
//	private static final String signType		="signType";
    private static final String signature  ="signData";

    private static final String RESPONSE_PAY_MSG = "SUCCESS";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(merId);
        String ordernumberR = API_RESPONSE_PARAMS.get(prdOrdNo);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[kk]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!signature.equalsIgnoreCase(paramKeys.get(i).toString()) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
        		signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
			}
        }
        signSrc.append("key=" + channelWrapper.getAPI_KEY());
		String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[kk]-[响应支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean result = false;
        //01支付成功 00未支付 02支付处理中
        String payStatusCode = api_response_params.get(orderStatus);
        String responseAmount = api_response_params.get(orderAmount);
        //db_amount数据库存入的是分 	第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("01")) {
            result = true;
        } else {
            log.error("[kk]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[kk]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：01");
        return result;
    }


    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[kk]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[kk]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}