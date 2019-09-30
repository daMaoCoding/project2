package dc.pay.business.supzhifu;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
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
 * @date 23 Jul 2019
 */
@ResponsePayHandler("SUPZHIFU")
public final class SupZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

//    业务参数		参数名称			类型				参数说明
//    orderNo		订单号			String			我方订单唯一标识符
//    outOrderNo	外部订单号		String			商户订单唯一标识符
//    money			金额				Integer			订单金额
//    payType		订单类型			String			订单类型
//    status		订单状态			String			订单状态
//    attach		订单附加参数		String			商户自定义数据
//    timestamp		时间戳			Long			异步响应的时间戳
//    nonceStr		业务流水号		String			10位以上的业务流水随机字符串
//    signature		签名				String			参数签名，收到异步回调通知时，请务必验证签名有效性

    private static final String outOrderNo                   ="outOrderNo";
    private static final String money                    	 ="money";
    private static final String status                  	 ="status";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="signature";

    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("SUCCESS");

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(outOrderNo);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[sup支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
    	String signMD5 = generateCreateSign(
      		  api_response_params.get(outOrderNo),
      		  api_response_params.get(money),
      		  api_response_params.get("payType"),
      		  api_response_params.get("attach"),
      		  channelWrapper.getAPI_MEMBERID(),
      		  api_response_params.get("timestamp"),
      		  api_response_params.get("nonceStr"),
      		  channelWrapper.getAPI_KEY()
      		  );
        log.debug("[sup支付]-[响应支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return signMD5;
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
        if (checkAmount&&payStatusCode.equalsIgnoreCase("SUCCESS")) {
            my_result = true;
        } else {
            log.error("[sup支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[sup支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：SUCCESS");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[sup支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[sup支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return  RESPONSE_PAY_MSG;
    }
    
    
    public static String generateCreateSign(String outOrderNo, String amount,String payType, String attach,
            String appId, String timestamp, String nonceStr, String secret) throws PayException {

  		Map<String,String> params=new HashMap<>();
  		params.put("outOrderNo",outOrderNo);
  		params.put("amount",amount);
  		params.put("payType",payType);
  		params.put("attach",attach);
  		
  		List<String> paramKeys = MapUtils.sortMapByKeyAsc(params);
  		
  		StringBuilder requestUrl = new StringBuilder("?");
  		for (String key : paramKeys) {
  			requestUrl.append(key).append("=");
  		try{
  			requestUrl.append(java.net.URLEncoder.encode(params.get(key), "UTF-8"));
  		}catch (UnsupportedEncodingException e){
  		requestUrl.append(params.get(key));
  		}
  		requestUrl.append("&");
  		}
  		
  		String requestParamsEncode= requestUrl.replace(requestUrl.lastIndexOf("&"), requestUrl.length(), "").toString();
  		
  		String md5Value = HandlerUtil.getMD5UpperCase(requestParamsEncode + appId + timestamp + nonceStr).toLowerCase();
  		String orginSignature = HandlerUtil.getMD5UpperCase(md5Value + secret).toUpperCase();
  		return orginSignature;
  }
}