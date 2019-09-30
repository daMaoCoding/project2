package dc.pay.business.jisufu;

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
 * 
 * 
 * @author sunny
 * Dec 18, 2018
 */
@RequestPayHandler("JISUFU")
public final class JiSuFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(JiSuFuPayRequestHandler.class);

//  参数 					类型 				说明 				允许为空
//  shopAccountId 		字符串 			商家ID 			否
//  shopUserId 			字符串 			商家⽤户ID 		否
//  amountInString 		字符串 			订单⾦额，单位元，如：0.01表示⼀分钱； 否
//  payChannel 			字符串 ⽀付宝：alipay, ⽀付宝转银⾏：bank否

  private static final String shopAccountId                		="shopAccountId";
  private static final String shopUserId            		 	="shopUserId";
  private static final String amountInString           	 		="amountInString";
  private static final String payChannel            		 	="payChannel";
  private static final String sign           				 	="sign";
  private static final String shopNo               		 		="shopNo";
  private static final String shopCallbackUrl              		="shopCallbackUrl";
  private static final String returnUrl            		 		="returnUrl";
  private static final String target             		 	 	="target";
  private static final String key                 ="key";
    


  @Override
  protected Map<String, String> buildPayParam() throws PayException {
      Map<String, String> payParam = new TreeMap<String, String>() {
          {
              put(shopAccountId, channelWrapper.getAPI_MEMBERID());
              put(shopNo,channelWrapper.getAPI_ORDER_ID());
              put(amountInString,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
              put(shopCallbackUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
              put(payChannel,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              put(returnUrl,channelWrapper.getAPI_WEB_URL());
              put(shopUserId,"0");
              if(HandlerUtil.isWapOrApp(channelWrapper)){
              	 put(target,"1");
              }else if(HandlerUtil.isWEBWAPAPP_SM(channelWrapper)){
              	 put(target,"2");
              }
          }
      };
      log.debug("[极速付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
      return payParam;
  }

   protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
      String signSrc=String.format("%s", 
      		api_response_params.get(shopAccountId)+
      		api_response_params.get(shopUserId)+
      		api_response_params.get(amountInString)+
      		api_response_params.get(shopNo)+
      		api_response_params.get(payChannel)+
      		channelWrapper.getAPI_KEY()
      		);
      String paramsStr = signSrc.toString();
      String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
      log.debug("[极速付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
      return signMD5;
  }

  protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
      payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
      HashMap<String, String> result = Maps.newHashMap();
      ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
      result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());  //.replace("method='post'","method='get'"));
      payResultList.add(result);
      log.debug("[极速付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
      log.debug("[极速付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
      return requestPayResult;
  }
}