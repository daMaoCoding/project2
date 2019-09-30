package dc.pay.business.kezhizhifu;

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
import dc.pay.utils.RsaUtil;
import dc.pay.utils.Sha1Util;
import dc.pay.utils.UnicodeUtil;
import dc.pay.utils.ValidateUtil;


/**
 * 
 * 
 * @author sunny
 * Dec 18, 2018
 */
@RequestPayHandler("KEZHIZHIFU")
public final class KeZhiZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(KeZhiZhiFuPayRequestHandler.class);

  private static final String serverNum               ="serverNum";
  private static final String amount           		  ="amount";
  private static final String trans_number            ="trans_number";
  private static final String type           		  ="type";
  private static final String encryptData          	  ="encryptData";
  private static final String backType                ="backType";
  private static final String notify            	  ="notify";
  
  private static final String signType            ="signType";
  private static final String sign                ="sign";
  private static final String key                 ="key";
  
  @Override
  protected Map<String, String> buildPayParam() throws PayException {
      Map<String, String> payParam = new TreeMap<String, String>() {
          {
              put(serverNum, channelWrapper.getAPI_MEMBERID());
              put(trans_number,channelWrapper.getAPI_ORDER_ID());
              put(amount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()).replace(".00", ""));
              put(type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
          }
      };
      log.debug("[壳子支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
      return payParam;
  }

   protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
      String signSrc=String.format("%s%s%s%s",
    		  amount+"="+api_response_params.get(amount)+"&",  
    		  serverNum+"="+api_response_params.get(serverNum)+"&",  
    		  trans_number+"="+api_response_params.get(trans_number)+"&",  
    		  type+"="+api_response_params.get(type) 
      );
      String paramsStr = signSrc.toString();
      String signMD5="";
	try {
		signMD5 = RSASignature.sign(paramsStr, channelWrapper.getAPI_KEY());
		//signMD5=RsaUtil.signByPrivateKey(paramsStr, privateKey);
	} catch (Exception e) {
		e.printStackTrace();
	}
      log.debug("[壳子支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
      return signMD5;
  }

  protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
      HashMap<String, String> result = Maps.newHashMap();
      ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
      if (1==2&&(HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)  ||  HandlerUtil.isWapOrApp(channelWrapper))) {
          result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());  //.replace("method='post'","method='get'"));
          payResultList.add(result);
      }else{
    	String signSrc=String.format("%s%s%s%s",
        		  amount+"="+payParam.get(amount)+"&",  
        		  serverNum+"="+payParam.get(serverNum)+"&",  
        		  trans_number+"="+payParam.get(trans_number)+"&",  
        		  type+"="+payParam.get(type) 
        );
    	String cipher="";
    	try {
    		  //data=RsaUtil.encryptToBase64(signSrc, channelWrapper.getAPI_PUBLIC_KEY());
    		   // 对所传参数进行utf-8编码并加密
    	      byte[] cipherData = RSAEncrypt.encrypt(RSAEncrypt.loadPublicKeyByStr(channelWrapper.getAPI_PUBLIC_KEY()), signSrc.getBytes("UTF-8"));
    	      cipher = Base64.encode(cipherData);
    		  //cipher=RsaUtil.encryptToBase64(signSrc, publicKey);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
    	HashMap<String, String> postMap = Maps.newHashMap();
    	postMap.put("notify", channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
    	postMap.put("backType", "json");
    	postMap.put("encryptData",HandlerUtil.UrlEncode(cipher));
    	postMap.put("rsaSign",HandlerUtil.UrlEncode(pay_md5sign));
    	//签名和密文必须编码
        String Padata = "rsaSign=" + HandlerUtil.UrlEncode(pay_md5sign) + "&encryptData="
                + HandlerUtil.UrlEncode(cipher)+"&notify="+channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL()+"&backType=json";
      	//String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), postMap,null);
    	String resultStr=HttpUtil.postData(channelWrapper.getAPI_CHANNEL_BANK_URL(), Padata);
	        if (StringUtils.isBlank(resultStr)) {
	            log.error("[壳子支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(EMPTYRESPONSE);
	        }
	        if (!resultStr.contains("{") || !resultStr.contains("}")) {
	            log.error("[壳子支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        JSONObject resJson;
	        try {
	            resJson = JSONObject.parseObject(resultStr);
	        } catch (Exception e) {
	            e.printStackTrace();
	            log.error("[壳子支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        if (null != resJson && resJson.containsKey("code") && resJson.getString("code").equals("200")) {
	        	JSONObject data = resJson.getJSONObject("data");
	            result.put(JUMPURL, data.getString("payUrl").replace("订单编号", "%e8%ae%a2%e5%8d%95%e7%bc%96%e5%8f%b7"));
	        }else {
	            log.error("[壳子支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        payResultList.add(result);
      }
      log.debug("[壳子支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
      log.debug("[壳子支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
      return requestPayResult;
  }
}