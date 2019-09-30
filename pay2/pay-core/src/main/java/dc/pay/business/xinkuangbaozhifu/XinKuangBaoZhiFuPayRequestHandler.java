package dc.pay.business.xinkuangbaozhifu;

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
 * @date 13 Sep 2019
 */
@RequestPayHandler("XINKUANGBAOZHIFU")
public final class XinKuangBaoZhiFuPayRequestHandler extends PayRequestHandler {
  private static final Logger log = LoggerFactory.getLogger(XinKuangBaoZhiFuPayRequestHandler.class);

  private static final String merchant_order_uid               	="merchant_order_uid";
  private static final String merchant_order_sn           		="merchant_order_sn";
  private static final String merchant_order_money           	="merchant_order_money";
  private static final String merchant_order_channel            ="merchant_order_channel";
  private static final String merchant_order_date          		="merchant_order_date";
  private static final String merchant_order_callbak_confirm_duein              ="merchant_order_callbak_confirm_duein";
  private static final String url            					="url";
  
  private static final String apikey                 ="apikey";
  
  @Override
  protected Map<String, String> buildPayParam() throws PayException {
      Map<String, String> payParam = new TreeMap<String, String>() {
          {
              put(merchant_order_uid, channelWrapper.getAPI_MEMBERID());
              put(merchant_order_sn,channelWrapper.getAPI_ORDER_ID());
              put(merchant_order_money,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
              put(merchant_order_callbak_confirm_duein,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
              put(merchant_order_channel,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              put(merchant_order_date,DateUtil.formatDateTimeStrByParam("yyyy-MM-dd HH:mm:ss"));
          }
      };
      log.debug("[新狂暴支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
      return payParam;
  }

   protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
	      List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
	      StringBuilder signSrc = new StringBuilder();
	      for (int i = 0; i < paramKeys.size(); i++) {
	          if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
	          	signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
	          }
	      }
	      //最后一个&转换成#
	      signSrc.append(apikey+"="+channelWrapper.getAPI_KEY());
	      String paramsStr = signSrc.toString().toUpperCase();
	      String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
	      log.debug("[新狂暴支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
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
	            log.error("[新狂暴支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(EMPTYRESPONSE);
	        }
	        if (!resultStr.contains("{") || !resultStr.contains("}")) {
	            log.error("[新狂暴支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        JSONObject resJson;
	        try {
	            resJson = JSONObject.parseObject(resultStr);
	        } catch (Exception e) {
	            e.printStackTrace();
	            log.error("[新狂暴支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        if (null != resJson && resJson.containsKey("code") && resJson.getString("code").equals("200")) {
	        	JSONObject data=JSONObject.parseObject(resJson.getString("data"));
	            String code_url = data.getString("url");
	            result.put(JUMPURL, code_url);
	        }else {
	            log.error("[新狂暴支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        payResultList.add(result);
      }
      log.debug("[新狂暴支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
      log.debug("[新狂暴支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
      return requestPayResult;
  }
}