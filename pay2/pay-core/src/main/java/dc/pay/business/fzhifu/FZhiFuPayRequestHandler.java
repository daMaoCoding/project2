package dc.pay.business.fzhifu;

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
 * 06 20, 2019
 */
@RequestPayHandler("FZHIFU")
public final class FZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(FZhiFuPayRequestHandler.class);

//    字段名				类型				描述				必填				备注
//    money				string			订单金额			是				单位：元，保留两位小数
//    outTradeNo		string			订单号			是				用户唯一订单号
//    userAgent			string			客户浏览器		否				默认AlipayClient 微信MicroMessenger
//    appId				string			appId			是				平台唯一用户ID
//    notifyUrl			string			回调地址			否	
//    sign				string			签名				是	

  private static final String money                ="money";
  private static final String outTradeNo           ="outTradeNo";
  private static final String appId           	   ="appId";
  private static final String notifyUrl            ="notifyUrl";
  private static final String userAgent            ="userAgent";
  
  //channelType string  渠道类型    否   拼多多=pdd（默认），  微店=wd，          淘宝=tb，          拼多多商家码=pddcode，如果不传，不用参与签名
  private static final String channelType            ="channelType";
  
  private static final String signType            ="signType";
  private static final String sign                ="sign";
  private static final String key                 ="key";

  @Override
  protected Map<String, String> buildPayParam() throws PayException {
      String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
      if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 2) {
          log.error("[F支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号appId&通道类型编码值（向第三方获取当前使用通道编码值）channelType" );
          throw new PayException("[F支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号appId&通道类型编码值（向第三方获取当前使用通道编码值）channelType" );
      }
      
      Map<String, String> payParam = new TreeMap<String, String>() {
          {
              put(appId, channelWrapper.getAPI_MEMBERID().split("&")[0]);
              put(channelType, channelWrapper.getAPI_MEMBERID().split("&")[1]);
              put(outTradeNo,channelWrapper.getAPI_ORDER_ID());
              put(money,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
              put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
              put(userAgent,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
          }
      };
      log.debug("[F支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
      return payParam;
  }

   protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
	  String signSrc="";
	  try {
		signSrc = RSA2Util.rsaSign(api_response_params, channelWrapper.getAPI_KEY());
	  } catch (Exception e) {
		e.printStackTrace();
	  }
      log.debug("[F支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signSrc));
      return signSrc;
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
	            log.error("[F支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(EMPTYRESPONSE);
	        }
	        resultStr = UnicodeUtil.unicodeToString(resultStr);
	        resultStr=resultStr.replaceAll("\\\\", "");
	        if (!resultStr.contains("{") || !resultStr.contains("}")) {
	            log.error("[F支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        JSONObject resJson;
	        try {
	            resJson = JSONObject.parseObject(resultStr);
	        } catch (Exception e) {
	            e.printStackTrace();
	            log.error("[F支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
            if (null != resJson && resJson.containsKey("code") && "200".equalsIgnoreCase(resJson.getString("code"))  && 
            (resJson.containsKey("result") && StringUtils.isNotBlank(resJson.getString("result")) && 
             resJson.getJSONObject("result").containsKey("url") && StringUtils.isNotBlank(resJson.getJSONObject("result").getString("url")))
            ){
//	        if (null != resJson && resJson.containsKey("code") && resJson.getString("code").equals("200")) {
//	        	JSONObject  resultJson = resJson.getJSONObject("result");
	        	String urlcode=HandlerUtil.UrlEncode("订单编号");
	        	if(HandlerUtil.isZfbSM(channelWrapper)){
	        		 result.put(QRCONTEXT, resJson.getJSONObject("result").getString("url").replace("订单编号", urlcode));
	        	}else{
	        		 result.put(JUMPURL, resJson.getJSONObject("result").getString("url").replace("订单编号", urlcode));
	        	}
	           
	        }else {
	            log.error("[F支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        payResultList.add(result);
      }
      log.debug("[F支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
      log.debug("[F支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
      return requestPayResult;
  }
}