package dc.pay.business.longxingzhifu;

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
@ResponsePayHandler("LONGXINGZHIFU")
public final class LongXingZhiFuPayResponseHandler extends PayResponseHandler {
	private final Logger log = LoggerFactory.getLogger(getClass());
/*	
	参数名称		变量名			类型长度			说明
	订单状态		status		int(1)			1:成功，其他失败
	商户编号		customerid	int(8)	
	平台订单号		sdpayno		varchar(20)	
	商户订单号		sdorderno	varchar(20)	
	交易金额		total_fee	decimal(10,2)	最多两位小数
	支付类型		paytype		varchar(20)	
	订单备注说明		remark		varchar(50)		原样返回
	md5验证签名串	sign		varchar(32)		参照签名方法
*/

	private static final String RESPONSE_PAY_MSG  	= "success";	
	private static final String  status		 = "status";		//int(1)			1:成功，其他失败
	private static final String  customerid	 = "customerid";	//int(8)	
	private static final String  sdpayno	 = "sdpayno";		//varchar(20)	
	private static final String  sdorderno	 = "sdorderno";		//varchar(20)	
	private static final String  total_fee	 = "total_fee";		//decimal(10,2)		最多两位小数
	private static final String  paytype	 = "paytype";		//varchar(20)	
	private static final String  remark		 = "remark";		//varchar(50)		原样返回
	private static final String  sign		 = "sign";			//varchar(32)		参照签名方法
	/**
	 *	 取得訂單單號
	 */
    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String postdata = API_RESPONSE_PARAMS.get("postdata");
        Map<String ,String > parseMap = (Map) JSON.parse(postdata);
        String orderId = parseMap.get(sdorderno);
        if (StringUtils.isBlank(orderId) )
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[龙杏支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + orderId);
        return orderId;
    }

    /**
     * 	生成加密URL签名
     */
    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        String postdata = params.get("postdata");
        Map<String ,String > parseMap = (Map) JSON.parse(postdata);
        StringBuilder signStr = new StringBuilder();
        signStr.append(customerid+"=").append(parseMap.get(customerid)).append("&");
        signStr.append(status+"=").append(parseMap.get(status)).append("&");
        signStr.append(sdpayno+"=").append(parseMap.get(sdpayno)).append("&");
        signStr.append(sdorderno+"=").append(parseMap.get(sdorderno)).append("&");
        signStr.append(total_fee+"=").append(parseMap.get(total_fee)).append("&");
        signStr.append(paytype+"=").append(parseMap.get(paytype)).append("&");
        signStr.append(channelWrapper.getAPI_KEY());
        String paramsStr =signStr.toString();
        String pay_md5sign = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[龙杏支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }
    
    /**
     * 	金額及狀態驗證
     */
    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean checkResult = false;
        String postdata = api_response_params.get("postdata");
        Map<String ,String > parseMap = (Map) JSON.parse(postdata);
        String payStatus = parseMap.get(status);
        String responseAmount = HandlerUtil.getFen(parseMap.get(total_fee));
        boolean checkAmount =  HandlerUtil.isAllowAmountt(amountDb,responseAmount,"100");//我平台默认允许一元偏差
        if (checkAmount && payStatus.equalsIgnoreCase("1")) {	//1:成功，其他失败
            checkResult = true;
        } else {
            log.error("[龙杏支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatus + " ,支付金额：" + responseAmount + " ，应支付金额：" + amountDb);
        }
        log.debug("[龙杏支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + checkResult + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amountDb + ",第三方响应支付成功标志:" + payStatus + " ,计划成功：1");
        return checkResult;
    }

    /**
     * 	验证MD5签名
     */
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        String postdata = api_response_params.get("postdata");
        Map<String ,String > parseMap = (Map) JSON.parse(postdata);
        boolean result = parseMap.get(sign).equalsIgnoreCase(signMd5);
        log.debug("[龙杏支付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    /**
     * 	收到消息返回内容
     */
    @Override
    protected String responseSuccess() {
        log.debug("[龙杏支付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}