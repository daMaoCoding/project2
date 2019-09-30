package dc.pay.business.supzhifu;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayRequestHandler;
import dc.pay.business.RequestPayResult;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.Sha1Util;
import dc.pay.utils.UnicodeUtil;
import dc.pay.utils.ValidateUtil;


/**
 * @author sunny
 * @date 23 Jul 2019
 */
@RequestPayHandler("SUPZHIFU")
public final class SupZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(SupZhiFuPayRequestHandler.class);

//    业务参数			参数名称			类型			必填			请求参数说明
//    amount			订单金额			BigDecimal	是			下单的金额
//    outOrderNo		订单号			String		是			商户订单的唯一标识符
//    orderDesc			订单描述			String		是			订单的描述信息
//    timestamp			请求时间戳		Long		是			13位，请求时间戳不能为空,且与网关时间差不能大于60秒
//    nonceStr			业务流水号		String		是			业务流水号不小于10位字符随机字符串用于保证签名的不可预测性
//    returnUrl			返回地址			String					成功支付订单的页面返回地址
//    notifyUrl			通知地址			String		是			成功支付后异步回调通知地址
//    appId				商户appId		String		是			商户appId
//    signature			签名				String		是			签名
//    payType			支付类型			String		是			支付类型:(ALIPAY、WXPAY、CLOUDPAY)
//    userUnqueNo		用户编号			String		是			用户唯一编号不能为空,填写用户的ID或者用户名，可加密后传值，用于订单申诉检查,请务必填写很重要
//    attach			附加参数			String		是			附加参数，商户自定义值

  private static final String amount               ="amount";
  private static final String outOrderNo           ="outOrderNo";
  private static final String orderDesc            ="orderDesc";
  private static final String timestamp            ="timestamp";
  private static final String nonceStr             ="nonceStr";
  private static final String returnUrl            ="returnUrl";
  private static final String notifyUrl            ="notifyUrl";
  private static final String appId           	   ="appId";
  private static final String payType              ="payType";
  private static final String userUnqueNo          ="userUnqueNo";
  private static final String attach          	   ="attach";
  
  private static final String signType            ="signType";
  private static final String sign                ="sign";
  private static final String key                 ="key";
  


  @Override
  protected Map<String, String> buildPayParam() throws PayException {
      Map<String, String> payParam = new TreeMap<String, String>() {
          {
              put(appId, channelWrapper.getAPI_MEMBERID());
              put(outOrderNo,channelWrapper.getAPI_ORDER_ID());
              put(amount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
              put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
              put(payType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              put(returnUrl,channelWrapper.getAPI_WEB_URL());
              put(timestamp,System.currentTimeMillis()+"");
              put(orderDesc,channelWrapper.getAPI_ORDER_ID());
              put(nonceStr,UUID.randomUUID().toString().replaceAll("-", ""));
              put(userUnqueNo,channelWrapper.getAPI_ORDER_ID());
              put(attach,channelWrapper.getAPI_ORDER_ID());
          }
      };
      log.debug("[sup支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
      return payParam;
  }

   protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
      String signMD5 = generateCreateSign(
    		  api_response_params.get(outOrderNo),
    		  api_response_params.get(amount),
    		  api_response_params.get(payType),
    		  api_response_params.get(attach),
    		  api_response_params.get(appId),
    		  api_response_params.get(timestamp),
    		  api_response_params.get(nonceStr),
    		  channelWrapper.getAPI_KEY()
    		  );
      log.debug("[sup支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
      return signMD5;
  }

  protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
      payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
      HashMap<String, String> result = Maps.newHashMap();
      ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
      if (1==2&&(HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)  ||  HandlerUtil.isWapOrApp(channelWrapper))) {
          result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());  //.replace("method='post'","method='get'"));
          payResultList.add(result);
      }else{
      	String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,null);
	        if (StringUtils.isBlank(resultStr)) {
	            log.error("[收米吧支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(EMPTYRESPONSE);
	        }
	        resultStr = UnicodeUtil.unicodeToString(resultStr);
	        resultStr=resultStr.replaceAll("\\\\", "");
	        if (!resultStr.contains("{") || !resultStr.contains("}")) {
	            log.error("[收米吧支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        JSONObject resJson;
	        try {
	            resJson = JSONObject.parseObject(resultStr);
	        } catch (Exception e) {
	            e.printStackTrace();
	            log.error("[收米吧支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        if (null != resJson && resJson.containsKey("code") && "1".equals(resJson.getString("code"))) {
	            String code_url = resJson.getString("data");
	            result.put(JUMPURL, code_url);
	        }else {
	            log.error("[收米吧支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        payResultList.add(result);
      }
      log.debug("[收米吧支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
      return payResultList;
  }

  protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
      RequestPayResult requestPayResult = new RequestPayResult();
      if (null != resultListMap && !resultListMap.isEmpty()) {
          if (resultListMap.size() == 1) {
              Map<String, String> resultMap = resultListMap.get(0);
              requestPayResult=  buildResult(resultMap, channelWrapper,requestPayResult);
          }
          if (ValidateUtil.requestesultValdata(requestPayResult)) {
              requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
          } else {
              throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
          }
      } else {
          throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
      }
      log.debug("[收米吧支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
      return requestPayResult;
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