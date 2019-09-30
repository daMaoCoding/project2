package dc.pay.business.yezhiyinshang;

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
@RequestPayHandler("YEZHIYINSHANG")
public final class YeZhiYinShangPayRequestHandler extends PayRequestHandler {
  private static final Logger log = LoggerFactory.getLogger(YeZhiYinShangPayRequestHandler.class);

  private static final String merchantNo               ="merchantNo";
  private static final String merchantName             ="merchantName";
  private static final String payKey           		   ="payKey";
  private static final String payWayCode           	   ="payWayCode";
  private static final String orderNo          		   ="orderNo";
  private static final String payGateWay               ="payGateWay";
  private static final String productName              ="productName";
  private static final String orderPrice               ="orderPrice";
  private static final String returnUrl           	   ="returnUrl";
  private static final String notifyUrl           	   ="notifyUrl";
  private static final String orderPeriod              ="orderPeriod";
  private static final String ismobile                 ="ismobile";
  private static final String orderDate                ="orderDate";
  private static final String orderTime                ="orderTime";
  
  private static final String signType            ="signType";
  private static final String sign                ="sign";
  private static final String paySecret                ="paySecret";
  


  @Override
  protected Map<String, String> buildPayParam() throws PayException {
      String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
      if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 2) {
          log.error("[椰子银商支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号merchantNo&APPIDpayKey" );
          throw new PayException("[椰子银商支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号merchantNo&APPID" );
      }
      Map<String, String> payParam = new TreeMap<String, String>() {
          {
              put(merchantNo, channelWrapper.getAPI_MEMBERID().split("&")[0]);
              put(merchantName, channelWrapper.getAPI_MEMBERID());
              put(orderNo,channelWrapper.getAPI_ORDER_ID());
              put(orderPrice,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
              put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
              put(payWayCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              put(returnUrl,channelWrapper.getAPI_WEB_URL());
              put(orderDate,DateUtil.formatDateTimeStrByParam("yyyy-MM-dd HH:mm:ss"));
              put(orderTime,DateUtil.formatDateTimeStrByParam("yyyy-MM-dd HH:mm:ss"));
              put(payKey,channelWrapper.getAPI_MEMBERID().split("&")[1]);
              put(payGateWay,"10002");
              put(productName,channelWrapper.getAPI_ORDER_ID());
              put(orderPeriod,"10");
//              put(ismobile,handlerUtil.isWEBWAPAPP_SM(channelWrapper) ? "1" : "0");
              //老马🐴 2019/8/19 15:53:53
              //你都传1 我们有做自适应
              put(ismobile,"1");
          }
      };
      log.debug("[椰子银商支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
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
      signSrc.append(paySecret+"="+channelWrapper.getAPI_KEY());
      String paramsStr = signSrc.toString();
      String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr);
      log.debug("[椰子银商支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
      return signMD5;
  }

  protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
      payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
      HashMap<String, String> result = Maps.newHashMap();
      ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
      result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());  //.replace("method='post'","method='get'"));
      payResultList.add(result);
      log.debug("[椰子银商支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
      log.debug("[椰子银商支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
      return requestPayResult;
  }
}