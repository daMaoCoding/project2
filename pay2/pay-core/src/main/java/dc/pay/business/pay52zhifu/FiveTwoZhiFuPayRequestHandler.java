package dc.pay.business.pay52zhifu;

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
 * @date 28 Sep 2019
 */
@RequestPayHandler("FIVETWOZHIFU")
public final class FiveTwoZhiFuPayRequestHandler extends PayRequestHandler {
  private static final Logger log = LoggerFactory.getLogger(FiveTwoZhiFuPayRequestHandler.class);


  private static final String customerid                ="customerid";
  private static final String sdcustomno           	    ="sdcustomno";
  private static final String orderamount           	="orderamount";
  private static final String cardno           			="cardno";
  private static final String noticeurl          		="noticeurl";
  private static final String backurl              		="backurl";
  private static final String mark            			="mark";
  private static final String zftype           			="zftype";
  private static final String device           			="device";
  private static final String ordertime           		="ordertime";
  
  private static final String signType            ="signType";
  private static final String sign                ="sign";
  private static final String key                 ="key";

  @Override
  protected Map<String, String> buildPayParam() throws PayException {
      Map<String, String> payParam = new TreeMap<String, String>() {
          {
              put(customerid, channelWrapper.getAPI_MEMBERID());
              put(sdcustomno,channelWrapper.getAPI_ORDER_ID());
              put(orderamount,channelWrapper.getAPI_AMOUNT());
              put(noticeurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
              put(cardno,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              put(backurl,channelWrapper.getAPI_WEB_URL());
              put(ordertime,System.currentTimeMillis()/1000+"");
              put(mark,UUID.randomUUID().toString().replaceAll("-", ""));
              put(zftype,"casher");
              put(device,"mobile");
          }
      };
      log.debug("[52pay支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
      return payParam;
  }

   protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
      String signSrc=String.format("%s%s%s%s%s%s%s%s",
    		  customerid+"="+api_response_params.get(customerid)+"&",
    		  sdcustomno+"="+api_response_params.get(sdcustomno)+"&",
    		  orderamount+"="+api_response_params.get(orderamount)+"&",
    		  cardno+"="+api_response_params.get(cardno)+"&",
    		  noticeurl+"="+api_response_params.get(noticeurl)+"&",
    		  backurl+"="+api_response_params.get(backurl)+"&",
    		  ordertime+"="+api_response_params.get(ordertime),
    		  channelWrapper.getAPI_KEY()
      );
      String paramsStr = signSrc.toString();
      String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr);
      log.debug("[52pay支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
      return signMD5;
  }

  protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
      payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
      HashMap<String, String> result = Maps.newHashMap();
      ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
      result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());  //.replace("method='post'","method='get'"));
      payResultList.add(result);
      log.debug("[52pay支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
      log.debug("[52pay支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
      return requestPayResult;
  }
}