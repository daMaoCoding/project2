package dc.pay.business.threevfu;

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
 * Jan 7, 2018
 */
@ResponsePayHandler("THREEVFU")
public final class ThreeVFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

	//字段名					参数						类型（字节长度）				是否必填		示例值			备注
	//商户订单号				channelOrderId			String(3~32)			是			123456789		商户订单号
	//我方订单号				orderId					String(19)				是			1000012345678912345	我方订单号
	//通知时间戳				timeStamp				String(13)				否			1516322720055		通知时间戳 
	//金额					totalFee				int(10)					是			100			单位为分
	//签名					sign					String(20~100)			是			47b8795f78fd51cebc1ad3d8c4483a1b	签名规则将在文档下方说明
	//状态					return_code				Int						是			0000	支付状态，0000表示支付成功
	private static final String channelOrderId			="channelOrderId";
	private static final String orderId					="orderId";
	private static final String timeStamp				="timeStamp";
	private static final String totalFee				="totalFee";
	private static final String return_code				="return_code";
	private static final String sign					="sign";
	
	private static final String key						="key";
  
    private static final String RESPONSE_PAY_MSG = "SUCCESS";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
//        String partnerR = API_RESPONSE_PARAMS.get(src_code);
        String ordernumberR = API_RESPONSE_PARAMS.get(channelOrderId);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[3V支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }
    
    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
		//1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
		//2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
		StringBuffer signSrc= new StringBuffer();
		signSrc.append(channelOrderId+"=").append(api_response_params.get(channelOrderId)).append("&");
		signSrc.append(key+"=").append(channelWrapper.getAPI_KEY()).append("&");
		signSrc.append(orderId+"=").append(api_response_params.get(orderId)).append("&");
		signSrc.append(timeStamp+"=").append(api_response_params.get(timeStamp)).append("&");
		signSrc.append(totalFee+"=").append(api_response_params.get(totalFee));
		String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr,"utf-8");
        log.debug("[3V支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean result = false;
        //return_code				Int					是			0000	支付状态，0000表示支付成功
        String payStatusCode = api_response_params.get(return_code);
        String responseAmount = api_response_params.get(totalFee);
        //amount数据库存入的是分 	第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //0代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("0000")) {
            result = true;
        } else {
            log.error("[3V支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[3V支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：0000");
        return result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("[3V支付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[3V支付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}