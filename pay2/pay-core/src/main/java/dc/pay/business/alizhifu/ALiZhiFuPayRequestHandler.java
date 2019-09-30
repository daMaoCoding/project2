package dc.pay.business.alizhifu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
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
import dc.pay.utils.ValidateUtil;


/**
 * @author sunny
 * @date 19 Sep 2019
 */
@RequestPayHandler("ALIZHIFU")
public final class ALiZhiFuPayRequestHandler extends PayRequestHandler {
  private static final Logger log = LoggerFactory.getLogger(ALiZhiFuPayRequestHandler.class);

  private static final String merchantid               	="merchantid";
  private static final String wayid           			="wayid";
  private static final String waytype           		="waytype";
  private static final String merchantorder           	="merchantorder";
  private static final String userid          			="userid";
  private static final String money              		="money";
  private static final String backurl            		="backurl";
  private static final String notifyurl           		="notifyurl";
  private static final String extparam           		="extparam";
  private static final String clientip           		="clientip";
  private static final String signtype           		="signtype";
  
  private static final String key                 ="key";
  
  @Override
  protected Map<String, String> buildPayParam() throws PayException {
	  String memberId = channelWrapper.getAPI_MEMBERID();
      if (StringUtils.isEmpty(memberId)|| !memberId.contains("&") || memberId.split("&").length != 2) {
          throw new PayException("[阿里付]-[请求支付]-“商户号”输入数据格式为【中间使用&分隔】：商户号&通道类型编码" );
      }
      Map<String, String> payParam = new TreeMap<String, String>() {
          {
              put(merchantid, channelWrapper.getAPI_MEMBERID().split("&")[0]);
              put(merchantorder,channelWrapper.getAPI_ORDER_ID());
              put(money,channelWrapper.getAPI_AMOUNT());
              put(notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
              put(wayid,channelWrapper.getAPI_MEMBERID().split("&")[1]);
              put(backurl,channelWrapper.getAPI_WEB_URL());
              put(extparam,channelWrapper.getAPI_ORDER_ID());
              put(waytype,channelWrapper.getAPI_MEMBERID().split("&")[1]);
              put(userid,channelWrapper.getAPI_ORDER_ID());
              put(clientip,channelWrapper.getAPI_Client_IP());
              put(signtype,"MD5");
          }
      };
      log.debug("[阿里付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
      return payParam;
  }

   protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
      List<?> paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
      StringBuilder signSrc = new StringBuilder();
      for (int i = 0; i < paramKeys.size(); i++) {
          if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
          	signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
          }
      }
      signSrc.append(key+"="+channelWrapper.getAPI_KEY());
      String paramsStr = signSrc.toString();
      String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
      log.debug("[阿里付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
      return signMD5;
  }

  protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
      payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
      HashMap<String, String> result = Maps.newHashMap();
      ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
      result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());  //.replace("method='post'","method='get'"));
      payResultList.add(result);
      log.debug("[阿里付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
      log.debug("[阿里付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
      return requestPayResult;
  }
}