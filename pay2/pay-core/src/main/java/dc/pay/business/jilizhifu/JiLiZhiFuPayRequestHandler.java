package dc.pay.business.jilizhifu;

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
 * 05 27, 2019
 */
@RequestPayHandler("JILIZHIFU")
public final class JiLiZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(JiLiZhiFuPayRequestHandler.class);

//    入参请求参数
//    order_number				必填				订单号
//    api_id					必填				商户号
//    order_money				必填				订单金额（元）
//    order_type				必填				订单方式（附录1）
//    call_back					必填				异步通知地址
//    user_name					必填				会员标识
//    href_back					选填				同步通知地址
//    attach					选填				备注
//    sign						必填				MD5签名
//    bank_code					选填				在线网银必填（附录2）


  private static final String order_number               ="order_number";
  private static final String api_id           			 ="api_id";
  private static final String order_money           	 ="order_money";
  private static final String order_type           		 ="order_type";
  private static final String call_back          		 ="call_back";
  private static final String user_name              	 ="user_name";
  private static final String href_back            		 ="href_back";
  
  private static final String signType            ="signType";
  private static final String sign                ="sign";
  private static final String key                 ="key";

  @Override
  protected Map<String, String> buildPayParam() throws PayException {
      Map<String, String> payParam = new TreeMap<String, String>() {
          {
              put(api_id, channelWrapper.getAPI_MEMBERID());
              put(order_number,channelWrapper.getAPI_ORDER_ID());
              put(order_money,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
              put(call_back,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
              put(order_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              put(href_back,channelWrapper.getAPI_WEB_URL());
              put(user_name,System.currentTimeMillis()+"");
          }
      };
      log.debug("[吉利支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
      return payParam;
  }

   protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
      String signSrc=String.format("%s%s%s%s%s%s", 
    		  api_id+"="+api_response_params.get(api_id)+"&",
    		  order_type+"="+api_response_params.get(order_type)+"&",
    		  order_money+"="+api_response_params.get(order_money)+"&",
    		  order_number+"="+api_response_params.get(order_number)+"&",
    		  call_back+"="+api_response_params.get(call_back),
    		  channelWrapper.getAPI_KEY()
      );
      String paramsStr = signSrc.toString();
      String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
      log.debug("[吉利支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
      return signMD5;
  }

  protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
      payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
      HashMap<String, String> result = Maps.newHashMap();
      ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
      result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());  //.replace("method='post'","method='get'"));
      payResultList.add(result);
      log.debug("[吉利支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
      log.debug("[吉利支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
      return requestPayResult;
  }
}