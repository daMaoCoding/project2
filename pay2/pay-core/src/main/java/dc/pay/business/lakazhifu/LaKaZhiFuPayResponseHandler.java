package dc.pay.business.lakazhifu;

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
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author Mikey
 * Jun 12, 2019
 */
@Slf4j
@ResponsePayHandler("LAKAZHIFU")
public final class LaKaZhiFuPayResponseHandler extends PayResponseHandler {
	private final Logger log = LoggerFactory.getLogger(getClass());
	
/*
	参数				参数名称	
	orderId			订单 id	
	paymentAmount	支付金额	
	message			说明信息	充值成功
	sign			签名（根据平台的私钥签名）	
*/    

	private static final String RESPONSE_PAY_MSG  		 = "success";	
    private static final String mer_id                   = "mer_id";           // 商户号
    private static final String businessnumber           = "businessnumber";   // 业务订单号
    private static final String status                   = "status";           // 交易状态
    private static final String amount                   = "amount";           // 交易金额 单位 分
    private static final String sign_type                = "sign_type";        // 签名算法类型
    private static final String signature  				 = "sign";

	/**
	 *	 取得訂單單號
	 */
    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(mer_id);
        String ordernumberR = API_RESPONSE_PARAMS.get(businessnumber);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[拉卡支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }

    /**
     * 	生成加密URL签名
     */
    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!signature.equals(paramKeys.get(i)) && !sign_type.equals(paramKeys.get(i)) && StringUtils.isNotBlank(params.get(paramKeys.get(i)))) {
                sb.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i))).append("&");
            } 
        }
		sb.append(channelWrapper.getAPI_KEY());
        String signStr = sb.toString();
        pay_md5sign = HandlerUtil.getMD5UpperCase(signStr);	//进行MD5运算，再将得到的字符串所有字符转换为大写
        log.debug("[拉卡支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }
    
    /**
     * 	金額及狀態驗證
     */
    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean checkResult = false;
        String payStatus = api_response_params.get(status);
        String responseAmount = api_response_params.get(amount);
        boolean checkAmount =  HandlerUtil.isAllowAmountt(amountDb,responseAmount,"100");//我平台默认允许一元偏差
        if (checkAmount && payStatus.equalsIgnoreCase("成功")) {
            checkResult = true;
        } else {
            log.error("[拉卡支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatus + " ,支付金额：" + responseAmount + " ，应支付金额：" + amountDb);
        }
        log.debug("[拉卡支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + checkResult + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amountDb + ",第三方响应支付成功标志:" + payStatus + " ,计划成功：成功");
        return checkResult;
    }

    /**
     * 	验证MD5签名
     */
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[拉卡支付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    /**
     * 	收到消息返回内容
     */
    @Override
    protected String responseSuccess() {
        log.debug("[拉卡支付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}