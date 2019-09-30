package dc.pay.business.lpzhifu;

import java.io.UnsupportedEncodingException;
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
 * @date 22 Jul 2019
 */
@RequestPayHandler("LPZHIFU")
public final class LPZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(LPZhiFuPayRequestHandler.class);

//  字段 			说明 			类型 			必填 			备注
//  appId 		商户标 识		String 		是 			您的商户唯一标识，登陆后台系统 在个人中心获得。
//  payMethod 	支付方 式		String 		是			支付方式，固定枚举值。 2001：支付宝 1001:微信
//  notifyUrl 	支付成功回调地址String 	是			用户支付成功后，会做异步通知， 我们服务器会主动发送 post 消 息到这个地址。由调用者自定义。 
//  returnUrl		支付成功跳转地址String 	是			用户支付成功后，可以让用户浏览器自动跳转到这个网址。由调用者 自定义。
//  outTradeNo	商户支付单标识String 		是			系统会据此判别是同一笔订单还是新订单。回调时，会返回这个参数。
//  signType 		签名方 式		String 		是		       使用 MD5 签名。默认值MD5sign 签名后 数据String 是把使用到的所有参数，按参数名字 母升序排序。
//  amount 		订单金 额		String 		是 			单位：元。精确小数点后 2 位。
//  nonceStr 		随机字 符串	String 		是			 随机字符串。
//  timestamp 	时间戳		 String 	是 			当前时间戳。

  private static final String version               ="version";
  private static final String merchant_no           ="merchant_no";
  private static final String order_no           	="order_no";
  private static final String goods_name            ="goods_name";
  private static final String order_amount          ="order_amount";
  private static final String backend_url           ="backend_url";
  private static final String frontend_url          ="frontend_url";
  private static final String pay_mode           	="pay_mode";
  private static final String bank_code           	="bank_code";
  private static final String card_type           	="card_type";
  private static final String reserve           	="reserve";
  
  private static final String signType            ="signType";
  private static final String sign                ="sign";
  private static final String key                 ="key";

  @Override
  protected Map<String, String> buildPayParam() throws PayException {
      Map<String, String> payParam=null;
      try {
		payParam = new TreeMap<String, String>() {
	      {
	          put(merchant_no, channelWrapper.getAPI_MEMBERID());
	          put(order_no,channelWrapper.getAPI_ORDER_ID());
	          put(order_amount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
	          put(backend_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
	          put(bank_code,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	          put(frontend_url,channelWrapper.getAPI_WEB_URL());
	          if(HandlerUtil.isYLWAP(channelWrapper)){
	        	  put(pay_mode,"12");
	          }else{
	        	  put(pay_mode,"01");
	          }
	          put(goods_name,java.util.Base64.getEncoder().encodeToString(channelWrapper.getAPI_ORDER_ID().getBytes("UTF-8")));
	          put(version,"v1");
	          put(card_type,"0");
	          put(reserve,"");
	      }
		  };
      } catch (UnsupportedEncodingException e) {
		e.printStackTrace();
      }
      log.debug("[LP支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
      return payParam;
  }

   protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
      String signSrc=String.format("%s%s%s%s%s%s%s%s%s%s%s%s", 
    		  version+"="+api_response_params.get(version)+"&",
    		  merchant_no+"="+api_response_params.get(merchant_no)+"&",
    		  order_no+"="+api_response_params.get(order_no)+"&",
    		  goods_name+"="+api_response_params.get(goods_name)+"&",
    		  order_amount+"="+api_response_params.get(order_amount)+"&",
    		  backend_url+"="+api_response_params.get(backend_url)+"&",
    		  frontend_url+"="+api_response_params.get(frontend_url)+"&",
    		  reserve+"="+api_response_params.get(reserve)+"&",
    		  pay_mode+"="+api_response_params.get(pay_mode)+"&",
    		  bank_code+"="+api_response_params.get(bank_code)+"&",
    		  card_type+"="+api_response_params.get(card_type)+"&",
    		  key+"="+channelWrapper.getAPI_KEY()
      );
      String paramsStr = signSrc.toString();
      String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
      log.debug("[LP支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
      return signMD5;
  }

  protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
      payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
      HashMap<String, String> result = Maps.newHashMap();
      ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
      result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());  //.replace("method='post'","method='get'"));
      payResultList.add(result);
      log.debug("[LP支付]]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
      log.debug("[LP支付]]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
      return requestPayResult;
  }
}