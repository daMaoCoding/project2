package dc.pay.business.xinhtzhifu;

import java.security.PrivateKey;
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
import dc.pay.business.mengma.RSAUtil;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.AesAndBase64Util;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.Sha1Util;
import dc.pay.utils.UnicodeUtil;
import dc.pay.utils.ValidateUtil;


/**
 * 
 * 
 * @author sunny
 * Dec 18, 2018
 */
@RequestPayHandler("XINHTZHIFU")
public final class XinHTZhiFuPayRequestHandler extends PayRequestHandler {
  private static final Logger log = LoggerFactory.getLogger(XinHTZhiFuPayRequestHandler.class);

  private static final String mer_no                ="mer_no";
  private static final String mer_user_no           ="mer_user_no";
  private static final String trans_id           	="trans_id";
  private static final String pay_time           	="pay_time";
  private static final String notify_url          	="notify_url";
  private static final String front_url             ="front_url";
  private static final String body            		="body";
  private static final String spbill_create_ip      ="spbill_create_ip";
  private static final String version      			="version";
  private static final String sign_type      		="sign_type";
  private static final String data      			="data";
  
  private static final String money      			="money";
  private static final String pay_type      		="pay_type";
  private static final String trade_type      		="trade_type";
  
  private static final String signType            ="signType";
  private static final String sign                ="sign";
  private static final String key                 ="key";
  
  @Override
  protected Map<String, String> buildPayParam() throws PayException {
      Map<String, String> payParam = new TreeMap<String, String>() {
          {
              put(mer_no, channelWrapper.getAPI_MEMBERID().split("&")[0]);
              put(mer_user_no, channelWrapper.getAPI_MEMBERID().split("&")[1]);
              put(trans_id,channelWrapper.getAPI_ORDER_ID());
              put(money,channelWrapper.getAPI_AMOUNT());
              put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
              put(pay_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              put(front_url,channelWrapper.getAPI_WEB_URL());
              put(pay_time,System.currentTimeMillis()+"");
              put(sign_type,"RSA");
              put(body,channelWrapper.getAPI_ORDER_ID());
              put(spbill_create_ip,channelWrapper.getAPI_Client_IP());
              put(version,"1.0.0");
              put(trade_type,"T1");
          }
      };
      log.debug("[新HT支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
      return payParam;
  }

   protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
	  HashMap<String, String> dataMap = Maps.newHashMap();
	  dataMap.put(money, api_response_params.get(money));
	  dataMap.put(pay_type, api_response_params.get(pay_type));
	  dataMap.put(trade_type, api_response_params.get(trade_type));
	  String aesStr="";
	  try {
		//aesStr = AesAndBase64Util.Encrypt(HandlerUtil.mapToJson(dataMap), channelWrapper.getAPI_PUBLIC_KEY().split("&")[0]);
		aesStr=AESUtil.AESEncrypt(HandlerUtil.mapToJson(dataMap), channelWrapper.getAPI_PUBLIC_KEY().split("&")[0]);
	  } catch (Exception e) {
		e.printStackTrace();
	  }
	  api_response_params.put("data", aesStr);
	  List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
      StringBuilder signSrc = new StringBuilder();
      for (int i = 0; i < paramKeys.size(); i++) {
          if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
          	signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
          }
      }
      signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "" );
      String sign="";
	  try {
		PrivateKey privateKey = CryptoUtil.getRSAPrivateKeyByPriKeyStr(channelWrapper.getAPI_KEY(), CryptoUtil.keyAlgorithm);
		sign = CryptoUtil.digitalSign(signSrc.toString(), privateKey,CryptoUtil.signAlgorithm);
		//signMD5 = RSAUtil.encryptByPublicKey(signSrc.toString(), channelWrapper.getAPI_KEY());
	  } catch (Exception e) {
		e.printStackTrace();
	  }
      log.debug("[新HT支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(sign));
      return sign;
  }

  protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
      payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
      HashMap<String, String> result = Maps.newHashMap();
      ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
      result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());  //.replace("method='post'","method='get'"));
      payResultList.add(result);
      log.debug("[新HT支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
      log.debug("[新HT支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
      return requestPayResult;
  }
}