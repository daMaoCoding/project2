package dc.pay.business.futongzhifu;

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
 * 
 * 
 * @author sunny
 * Dec 18, 2018
 */
@RequestPayHandler("FUTONGZHIFU")
public final class FuTongZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(FuTongZhiFuPayRequestHandler.class);

//    参数名				必选				类型				中文域名			说明
//    merchant_code		M				string			商户编号			平台分配的唯一编号
//    appno_no			M				string			应用编号			平台分配的唯一应用编号
//    order_no			M				string			商户订单号		商户订单号，不可重复，最长30位
//    order_amount		M				Number			交易金额	单位：元（精确到0.01）
//    order_time		M				string			交易时间	YYYYMMDDHHmmss
//    product_name		M				string			产品名称	请尽量不要传固定值，否则会影响成功率；请尽量不要带空格。传近真实的随机中文名。
//    product_code		M				string(24)		产品编号	不能带中文

  private static final String merchant_code               ="merchant_code";
  private static final String appno_no           		  ="appno_no";
  private static final String order_no           			="order_no";
  private static final String order_amount           		="order_amount";
  private static final String order_time          			="order_time";
  private static final String product_name              	="product_name";
  private static final String product_code            		="product_code";
  private static final String user_no           			="user_no";
  private static final String notify_url           			="notify_url";
  private static final String pay_type           			="pay_type";
  private static final String return_url           			="return_url";
  private static final String merchant_ip           	    ="merchant_ip";
  
  private static final String signType            ="signType";
  private static final String sign                ="sign";
  private static final String key                 ="key";
  


  @Override
  protected Map<String, String> buildPayParam() throws PayException {
      Map<String, String> payParam = new TreeMap<String, String>() {
          {
              put(merchant_code, channelWrapper.getAPI_MEMBERID().split("&")[0]);
              put(appno_no, channelWrapper.getAPI_MEMBERID().split("&")[1]);
              put(order_no,channelWrapper.getAPI_ORDER_ID());
              put(order_amount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
              put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
              put(pay_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              put(order_time,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
              put(product_name,channelWrapper.getAPI_ORDER_ID());
              put(product_code,channelWrapper.getAPI_ORDER_ID());
              put(user_no,channelWrapper.getAPI_ORDER_ID());
              put(return_url,channelWrapper.getAPI_WEB_URL());
              put(merchant_ip,channelWrapper.getAPI_Client_IP());
          }
      };
      log.debug("[富通支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
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
      signSrc.append(key+"="+channelWrapper.getAPI_KEY());
      String paramsStr = signSrc.toString();
      String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr);
      log.debug("[富通支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
      return signMD5;
  }

  protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
      HashMap<String, String> result = Maps.newHashMap();
      ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
      if (1==2&&(HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)  ||  HandlerUtil.isWapOrApp(channelWrapper))) {
          result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());  //.replace("method='post'","method='get'"));
          payResultList.add(result);
      }else{
    	HashMap<String, String> postMap = Maps.newHashMap();
    	postMap.put("sign", pay_md5sign);
    	postMap.put("signtype", "MD5");
    	postMap.put("transdata", HandlerUtil.mapToJson(payParam));
      	String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), postMap);
	        if (StringUtils.isBlank(resultStr)) {
	            log.error("[富通支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(EMPTYRESPONSE);
	        }
	        if (!resultStr.contains("{") || !resultStr.contains("}")) {
	            log.error("[富通支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        JSONObject resJson;
	        try {
	            resJson = JSONObject.parseObject(resultStr);
	        } catch (Exception e) {
	            e.printStackTrace();
	            log.error("[富通支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        if (null != resJson && resJson.containsKey("payment") && resJson.getString("payment").equals("true")) {
	            String code_url = resJson.getString("payUrl");
	            result.put(JUMPURL, code_url);
	        }else {
	            log.error("[富通支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        payResultList.add(result);
      }
      log.debug("[富通支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
      log.debug("[富通支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
      return requestPayResult;
  }
}