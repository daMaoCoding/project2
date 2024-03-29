package dc.pay.business.shengyingfu;

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
 * Apr 21, 2018
 */
@ResponsePayHandler("SHENGYINGFU")
public final class ShengYingFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

	//必填			字段说明			字段名			最大长度			备注
	//是			交易日期			tradeDate			10
	//是			交易时间			tradeTime			8
	//是			商户号			merchNo				15
	//是			交易金额			tradeAmount			12				单位/元
	//是			商户流水号			tradeNo				30				商户流水号
	//是			支付方式			pt					1	
	//是			系统订单号			channelNo			12				系统订单号
	//是			交易状态			tradeStatus			1				0-未支付	1-支付成功	2-支付失败
	//否			备注				remark				50				订单备注，系统将原样返回
	//是			数据签名			sign				32
//	private static final String tradeDate	 ="tradeDate";
//	private static final String tradeTime	 ="tradeTime";
	private static final String merchNo	 	 ="merchNo";
	private static final String tradeAmount	 ="tradeAmount";
	private static final String tradeNo	 	 ="tradeNo";
//	private static final String pt		 	 ="pt";
//	private static final String channelNo	 ="channelNo";
	private static final String tradeStatus	 ="tradeStatus";
//	private static final String remark		 ="remark";
    //signature	数据签名	32	是	　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "SUCCESS";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(merchNo);
        String ordernumberR = API_RESPONSE_PARAMS.get(tradeNo);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[盛盈付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!signature.equalsIgnoreCase((String) paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
        		signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
			}
        }
        signSrc.append(channelWrapper.getAPI_KEY());
		String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[盛盈付]-[响应支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean result = false;
        //tradeStatus	1		0-未支付	1-支付成功	2-支付失败
        String payStatusCode = api_response_params.get(tradeStatus);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(tradeAmount));
        //db_amount数据库存入的是分 	第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("1")) {
            result = true;
        } else {
            log.error("[盛盈付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[盛盈付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return result;
    }


    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[盛盈付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }


    @Override
    protected String responseSuccess() {
        log.debug("[盛盈付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}