package dc.pay.business.jiebaozhifu;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
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
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.Sha1Util;
import dc.pay.utils.UnicodeUtil;
import dc.pay.utils.ValidateUtil;


/**
 * @author sunny
 * 05 13, 2019
 */
@RequestPayHandler("JIEBAOZHIFU")
public final class JieBaoZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(JieBaoZhiFuPayRequestHandler.class);

//    参数名称				参数含义				是否必填			参数说明
//    serviceuri			服务					是				YF0001
//    nonceStr				随机串				是				随机字符串
//    agentId				商户编号				是				统一提供的商户编号
//    orderNo				订单号				是				下游提供的唯一订单号
//    amount				金额					是				订单金额，单位 分
//    payType				银行卡类型			是				12(固定值，微信h5)
//    ip					ip					是				发起支付的ip地址
//    notifyurl				通知地址				是				异步通知地址
//    returnurl				同步返回地址			否				同步返回地址
//    remark				订单描述信息			是	
//    sign					签名					是				签名字段，nonceStr + agentId + orderNo + payType + amount
    
  private static final String serviceuri               			="serviceuri";
  private static final String nonceStr           				="nonceStr";
  private static final String agentId           				="agentId";
  private static final String orderNo           				="orderNo";
  private static final String amount          					="amount";
  private static final String payType          					="payType";
  private static final String ip          						="ip";
  private static final String notifyurl          				="notifyurl";
  private static final String returnurl          				="returnurl";
  private static final String remark          					="remark";
  private static final String cardType          			    ="cardType";
  private static final String bankCode          			    ="bankCode";
  private static final String bankName          			    ="bankName";
  
  private static final String sign                ="sign";
  private static final String key                 ="key";
  
  @Override
  protected Map<String, String> buildPayParam() throws PayException {
      Map<String, String> payParam = new TreeMap<String, String>() {
          {
              put(agentId, channelWrapper.getAPI_MEMBERID());
              put(orderNo,channelWrapper.getAPI_ORDER_ID());
              put(amount,channelWrapper.getAPI_AMOUNT());
              put(notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
             
              put(nonceStr,UUID.randomUUID().toString().replaceAll("-", ""));
              put(ip,channelWrapper.getAPI_Client_IP());
              put(returnurl,channelWrapper.getAPI_WEB_URL());
              put(remark,channelWrapper.getAPI_ORDER_ID());
              if(HandlerUtil.isWY(channelWrapper)){
            	  put(cardType,"1");
            	  put(bankCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
            	  put(bankName,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
            	  put(serviceuri,"YF0001");
              }else{
            	  put(serviceuri,"YY0001");
            	  put(payType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              }
          }
      };
      log.debug("[捷豹支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
      return payParam;
  }

   protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
	      String signSrc="";
	      if(HandlerUtil.isWY(channelWrapper)){
	    	  signSrc=String.format("%s%s%s%s%s%s", 
		  	   			api_response_params.get(nonceStr),
		  	   			api_response_params.get(agentId),
		  	   			api_response_params.get(orderNo),
		  	   			api_response_params.get(ip),
		  	   			api_response_params.get(cardType),
		  	   			api_response_params.get(amount)
		  	   	   ); 
	      }else{
	    	  signSrc=String.format("%s%s%s%s%s", 
	  	   			api_response_params.get(nonceStr),
	  	   			api_response_params.get(agentId),
	  	   			api_response_params.get(orderNo),
	  	   			api_response_params.get(payType),
	  	   			api_response_params.get(amount)
	  	   	   );
	      }
	      String paramsStr = signSrc.toString();
	      String signMD5 = MD5Util.MD5(paramsStr, channelWrapper.getAPI_KEY()).toLowerCase();
	      log.debug("[捷豹支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
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
	            log.error("[捷豹支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(EMPTYRESPONSE);
	        }
	        resultStr = UnicodeUtil.unicodeToString(resultStr);
	        resultStr=resultStr.replaceAll("\\\\", "");
	        if (!resultStr.contains("{") || !resultStr.contains("}")) {
	            log.error("[捷豹支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        JSONObject resJson;
	        try {
	            resJson = JSONObject.parseObject(resultStr);
	        } catch (Exception e) {
	            e.printStackTrace();
	            log.error("[捷豹支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        if (null != resJson && resJson.containsKey("code") && resJson.getString("code").equals("200")) {
	        	JSONObject data = resJson.getJSONObject("data");
	        	if(null != data && data.containsKey("status") && data.getString("status").equals("10000")){
	        		if(HandlerUtil.isWY(channelWrapper)){
	        			result.put(HTMLCONTEXT, data.getString("data"));
	        		}else{
	        			if(HandlerUtil.isWxSM(channelWrapper)){
	        				result.put(QRCONTEXT, data.getString("data"));
	        			}else{
	        				result.put(JUMPURL, data.getString("data"));
	        			}
	        		}
	        		
	        	}else{
	        		log.error("[捷豹支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
		            throw new PayException(resultStr);
	        	}
	        }else {
	            log.error("[捷豹支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        payResultList.add(result);
      }
      log.debug("[捷豹支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
      log.debug("[捷豹支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
      return requestPayResult;
  }
  
  
  
}