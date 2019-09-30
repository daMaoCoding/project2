package dc.pay.business.hefusecond;

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
 * May 26, 2018
 */
@ResponsePayHandler("HEFUSECOND")
public final class HeFuSecondPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

	//outOrderNo         外部订单号
	//platformOrderId    平台订单号
	//status             订单状态
	//acquireDate        提单时间
	//completedDate      完成时间
	//payAmount          实际支付金额
	//sign              签名
	private static final String outOrderNo     		="outOrderNo";
//	private static final String platformOrderId		="platformOrderId";
	private static final String status         		="status";
//	private static final String acquireDate    		="acquireDate";
//	private static final String completedDate  		="completedDate";
	private static final String payAmount      		="payAmount";
	private static final String merchantKey      	="merchantKey";

    //signature	数据签名	32	是	　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "T";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
//        String partnerR = API_RESPONSE_PARAMS.get(merchno);
        String ordernumberR = API_RESPONSE_PARAMS.get(outOrderNo);
//        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[合付2]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
    	api_response_params.put(merchantKey, channelWrapper.getAPI_KEY());
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!signature.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
        		signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
			}
        }
        String paramsStr = signSrc.toString();
        //去除最后一个&符
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr.substring(0,paramsStr.length()-1)).toLowerCase();
        log.debug("[合付2]-[响应支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5) );
        return signMd5;
    }
    
    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
    	boolean result = false;
    	//status		-SUCCESS            成功
    	String payStatusCode = api_response_params.get(status);
    	String responseAmount = HandlerUtil.getFen(api_response_params.get(payAmount));
    	//db_amount数据库存入的是分 	第三方返回的responseAmount是元
//    	boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
    	//业主同意：@16  主 主管  他们这家支付宝单笔限额就10-500元以内，  每次支付金额偏差就在1元钱以内的啊
    	boolean checkAmount =  HandlerUtil.isRightAmount(db_amount,responseAmount,"100");//第三方回调金额差额1元内
    	//1代表第三方支付成功
    	if (checkAmount && payStatusCode.equalsIgnoreCase("SUCCESS")) {
    		result = true;
    	} else {
    		log.error("[合付2]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
    	}
    	log.debug("[合付2]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：SUCCESS");
    	return result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[合付2]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[合付2]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}