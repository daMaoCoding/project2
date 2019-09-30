package dc.pay.business.abczhifu;

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
 */
@RequestPayHandler("ABCZHIFU")
public final class ABCZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(ABCZhiFuPayRequestHandler.class);

//    参数名称				参数含义			是否必填				参与签名			参数说明
//    merchantNo			商户号			是					是				平台分配商户号
//    paySign				支付标识			是					是				参考文档底部:支付标识
//    amount				支付金额			是					是				精确到分,例如:10.00
//    merchantOrderNo		商户订单号		是					是				商户订单号保证唯一
//    nonceStr				随机字符串		是					是				32-64位字符,请勿包含特殊字符
//    signType				签名方式			是					是				HMAC_SHA256或RSA
//    notifyUrl				服务器通知地址		是					是				需要urlencode编码
//    callbackUrl			前台页面通知地址	是					是				需要urlencode编码
//    extra					附加字段			否					否				此字段在返回时按原样返回 (中文需要url编码),最大长度124
//    signMsg				签名字符串		是					否				签名算法参考文档下方:签名算法

  private static final String merchantNo               	="merchantNo";
  private static final String paySign           		="paySign";
  private static final String amount           			="amount";
  private static final String merchantOrderNo           ="merchantOrderNo";
  private static final String nonceStr          		="nonceStr";
  private static final String signType              	="signType";
  private static final String notifyUrl            		="notifyUrl";
  private static final String callbackUrl           	="callbackUrl";
  
  private static final String sign                ="sign";
  private static final String key                 ="key";

  @Override
  protected Map<String, String> buildPayParam() throws PayException {
	  Map<String, String> payParam =new TreeMap<String, String>(){
      {
          put(merchantNo, channelWrapper.getAPI_MEMBERID());
          put(merchantOrderNo,channelWrapper.getAPI_ORDER_ID());
          put(amount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
          put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
          put(paySign,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
          put(callbackUrl,channelWrapper.getAPI_WEB_URL());
          put(nonceStr,UUID.randomUUID().toString().replaceAll("-", ""));
          put(signType,"HMAC_SHA256");
      }
      };
      log.debug("[ABC支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
      return payParam;
  }

   protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
      List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
      StringBuilder signSrc = new StringBuilder();
      for (int i = 0; i < paramKeys.size(); i++) {
          if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
        	  if(paramKeys.get(i).equals(notifyUrl)||paramKeys.get(i).equals(callbackUrl)){
        		  signSrc.append(paramKeys.get(i)).append("=").append(HandlerUtil.UrlEncode(api_response_params.get(paramKeys.get(i)))).append("&");
        	  }else{
        		  signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
        	  }
          	
          }
      }
      //最后一个&转换成#
      signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "" );
      String paramsStr = signSrc.toString();
      String signMD5 =HMACSHA256.sha256_HMAC(paramsStr, channelWrapper.getAPI_KEY()).toUpperCase();
      log.debug("[ABC支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
      return signMD5;
  }

  protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
      payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
      HashMap<String, String> result = Maps.newHashMap();
      ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
      result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));  //.replace("method='post'","method='get'"));
      payResultList.add(result);
      log.debug("[ABC支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
      log.debug("[ABC支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
      return requestPayResult;
  }

}