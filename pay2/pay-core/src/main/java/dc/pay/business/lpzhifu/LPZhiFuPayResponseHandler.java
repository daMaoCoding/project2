package dc.pay.business.lpzhifu;

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
 * @date 22 Jul 2019
 */
@ResponsePayHandler("LPZHIFU")
public final class LPZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

//    参数				参数名称 			类型（长度）		使用	说明
//    merchant_no		商户号			String(12)		必选	商户签约时Thirdpt分配的唯一商家编号
//    order_no			商户订单号		String(30)		必选	由商户平台生成的交易订单号，请确保其在商户平台的唯一性，其组成仅限于字母和数字
//    order_amount		订单金额			number			必选	实际支付金额，以元为单位，精确到小数点后两位
//    original_amount	原始订单金额		number			必选	调用支付接口时传给Thirdpt的订单金额	
//    upstream_settle	是否上游直接结算	String(1)		必选	1：是 0：否
//    result			支付结果			String(1)		必选	I：未支付 S：支付成功 F：支付失败U：处理中
//    pay_time			支付时间			String（14）		必选	格式：yyyyMMddHHmmss

    private static final String merchant_no                   	="merchant_no";
    private static final String order_no                    	="order_no";
    private static final String order_amount                    ="order_amount";
    private static final String original_amount                 ="original_amount";
    private static final String upstream_settle             	="upstream_settle";
    private static final String result                 			="result";
    private static final String pay_time              			="pay_time";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("200");

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(merchant_no);
        String ordernumberR = API_RESPONSE_PARAMS.get(order_no);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[LP支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
    	String signSrc=String.format("%s%s%s%s%s%s%s%s%s%s", 
    			merchant_no+"="+api_response_params.get(merchant_no)+"&",
    			order_no+"="+api_response_params.get(order_no)+"&",
    			order_amount+"="+api_response_params.get(order_amount)+"&",
      		    "original_amount"+"="+api_response_params.get("original_amount")+"&",
      		    "upstream_settle"+"="+api_response_params.get("upstream_settle")+"&",
      		    result+"="+api_response_params.get(result)+"&",
      		    pay_time+"="+api_response_params.get(pay_time)+"&",
      		   "trace_id"+"="+api_response_params.get("trace_id")+"&",
      		   "reserve"+"="+api_response_params.get("reserve")+"&",
      		   key+"="+channelWrapper.getAPI_KEY()
        );
        String paramsStr = signSrc.toString();
        String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[LP支付]]-[响应支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return signMD5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        String payStatusCode = api_response_params.get(result);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(order_amount));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        boolean checkAmount = HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差
        //1代表第三方支付成功
        if (checkAmount&&payStatusCode.equalsIgnoreCase("S")) {
            my_result = true;
        } else {
            log.error("[LP支付]]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[LP支付]]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：S");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[LP支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[LP支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return  RESPONSE_PAY_MSG;
    }
}