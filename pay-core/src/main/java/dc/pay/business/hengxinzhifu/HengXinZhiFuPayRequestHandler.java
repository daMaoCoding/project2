package dc.pay.business.hengxinzhifu;

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
import dc.pay.utils.ValidateUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


/**
 * 
 * @author sunny
 * 04 13, 2019
 */
@RequestPayHandler("HENGXINZHIFU")
public final class HengXinZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(HengXinZhiFuPayRequestHandler.class);

//    序号		参数名			类型			必填			说明			示例			描述
//    1			mchId			long		是			商户ID		20001222	分配的商户号
//    2			appId			String(32)	是			应用ID		0ae8be35ff634e2abe94f5f32f6d5c4f	该商户创建的应用对应的ID
//    3			mchOrderNo		String(30)	是			商户订单号	20160427210604000490	商户生成的订单号
//    4			productId		String(24)	是			产品ID		8001		见分配的产品id
//    5			currency		String(3)	是			币种	cny		三位货币代码,人民币:cny
//    6			amount			int			是			支付金额		100	支付金额,单位分
//    7			notifyUrl		String(200)	是			支付结果回调URL	http://api.pay.org/notify.htm	支付结果回调URL
//    8			subject			String(64)	是			商品主题		测试商品1	商品主题
//    9			body			String(256)	是			商品描述信息	测试商品描述	商品描述信息
//    10		sign			String(32)	是			签名			C380BEC2BFD727A4B6845133519F3AD6	签名值，详见签名算法

  private static final String mchId               	="mchId";
  private static final String appId           		="appId";
  private static final String mchOrderNo            ="mchOrderNo";
  private static final String productId             ="productId";
  private static final String currency          	="currency";
  private static final String amount              	="amount";
  private static final String notifyUrl            	="notifyUrl";
  private static final String subject           	="subject";
  private static final String body           		="body";
  
  private static final String signType            ="signType";
  private static final String sign                ="sign";
  private static final String key                 ="key";
  
  @Override
  protected Map<String, String> buildPayParam() throws PayException {
	  if(channelWrapper.getAPI_MEMBERID().split("&").length!=3){
          throw new PayException("商户号格式错误，正确格式请使用&符号链接 商户号,应用ID和产品ID,如：商户号&应用ID&产品ID");
      }
      Map<String, String> payParam = new TreeMap<String, String>() {
          {
              put(mchId, channelWrapper.getAPI_MEMBERID().split("&")[0]);
              put(mchOrderNo,channelWrapper.getAPI_ORDER_ID());
              put(amount,channelWrapper.getAPI_AMOUNT());
              put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
              put(productId,channelWrapper.getAPI_MEMBERID().split("&")[2]);
              put(appId, channelWrapper.getAPI_MEMBERID().split("&")[1]);
              put(subject,channelWrapper.getAPI_ORDER_ID());
              put(currency,"cny");
              put(body,channelWrapper.getAPI_ORDER_ID());
          }
      };
      log.debug("[恒信支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
      return payParam;
  }

   protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
      List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
      StringBuilder signSrc = new StringBuilder();
      for (int i = 0; i < paramKeys.size(); i++) {
//          if (!pay_productname.equals(paramKeys.get(i)) && !pay_attach.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
          if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
          	signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
          }
      }
      //最后一个&转换成#
      //signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "#" );
      signSrc.append(key+"="+channelWrapper.getAPI_KEY());
      String paramsStr = signSrc.toString();
      String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr);
      log.debug("[恒信支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
      return signMD5;
  }

  protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
      payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
      HashMap<String, String> result = Maps.newHashMap();
      ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
      if (1==2&&(HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)  ||  HandlerUtil.isWapOrApp(channelWrapper))) {
    	  Map<String, String> postParam=Maps.newHashMap();
      	  postParam.put("params", JSON.toJSONString(payParam));
          result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),postParam).toString());  //.replace("method='post'","method='get'"));
          payResultList.add(result);
      }else{
    	Map<String, String> postParam=Maps.newHashMap();
    	postParam.put("params", JSON.toJSONString(payParam));
      	String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), postParam,null);
	        if (StringUtils.isBlank(resultStr)) {
	            log.error("[恒信支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(EMPTYRESPONSE);
	        }
	        if (!resultStr.contains("{") || !resultStr.contains("}")) {
	            log.error("[恒信支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        JSONObject resJson;
	        try {
	            resJson = JSONObject.parseObject(resultStr);
	        } catch (Exception e) {
	            e.printStackTrace();
	            log.error("[恒信支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        if (null != resJson && resJson.containsKey("retCode") && resJson.getString("retCode").equals("SUCCESS")) {
//                返回值里面有个 urlType 参数，0表示url，1表示html，你们根据返回值类型不同做不一样的处理即可，让你们技术做好适配：把这个发你技术
                JSONObject CodeJson = JSONObject.parseObject(resJson.getString("payParams"));
                if ("0".equalsIgnoreCase(CodeJson.getString("urlType"))) {
                    String code_url = CodeJson.getString("codeUrl");
		            result.put(JUMPURL, code_url);
                } else if ("1".equalsIgnoreCase(CodeJson.getString("urlType"))) {
                    String code_url = resJson.getString("codeUrl");
		            result.put(HTMLCONTEXT, code_url);
                }
//	        		JSONObject CodeJson = JSONObject.parseObject(resJson.getString("payParams"));
//		            String code_url = CodeJson.getString("codeUrl");
//		            result.put(JUMPURL, code_url);
//	        	}else
//	        	if("33".equalsIgnoreCase(channelWrapper.getAPI_MEMBERID().split("&")[2])){
//	        		JSONObject CodeJson = JSONObject.parseObject(resJson.getString("payParams"));
//		            String code_url = CodeJson.getString("codeUrl");
//		            result.put(JUMPURL, code_url);
//	        	}else if("6".equalsIgnoreCase(channelWrapper.getAPI_MEMBERID().split("&")[2])){
//		            String code_url = resJson.getString("codeUrl");
//		            result.put(HTMLCONTEXT, code_url);
//	        	}else if("32".equalsIgnoreCase(channelWrapper.getAPI_MEMBERID().split("&")[2])){
//	        		JSONObject CodeJson = JSONObject.parseObject(resJson.getString("payParams"));
//	        		String code_url = CodeJson.getString("codeUrl");
//	        		result.put(JUMPURL, code_url);
//	        	}else if("37".equalsIgnoreCase(channelWrapper.getAPI_MEMBERID().split("&")[2])){
//	        		JSONObject CodeJson = JSONObject.parseObject(resJson.getString("payParams"));
//	        		String code_url = CodeJson.getString("codeUrl");
//	        		result.put(JUMPURL, code_url);
//	        	}else if("40".equalsIgnoreCase(channelWrapper.getAPI_MEMBERID().split("&")[2])){
//	        		JSONObject CodeJson = JSONObject.parseObject(resJson.getString("payParams"));
//	        		String code_url = CodeJson.getString("codeUrl");
//	        		result.put(JUMPURL, code_url);
//	        	}



	        }else {
	            log.error("[恒信支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        payResultList.add(result);
      }
      log.debug("[恒信支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
      log.debug("[恒信支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
      return requestPayResult;
  }
}