package dc.pay.business.dufuzhifu;

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
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.ValidateUtil;


/**
 * @author sunny
 * 26 04, 2019
 */
@RequestPayHandler("DUFUZHIFU")
public final class DuFuZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(DuFuZhiFuPayRequestHandler.class);

//    参数					参数名称			类型（长度）		使用	说明  基本参数
//    merchant_code			商家号			String(10)		必填	商户签约时，都付平台分配的唯一商家号。举例：300001002001。
//    service_type			业务类型			String(10)		必选	固定值： alipay_h5api、weixin_h5api、qq_h5api
//    notify_url			服务器异步通知地址	String(200)		必选	支付成功后，都付平台会主动通知商家系统，商家系统必须指定接收通知的地址。 举例：https://api.chinalightpay.com/Notify_Url.jsp
//    interface_version		接口版本			String(10)		必选	接口版本，固定值：V3.1(必须大写)
//    client_ip				客户端IP			String(15)		必选	消费者创建交易时所使用机器的IP或者终端ip，最大长度为15个字符。举例：192.168.1.25
//    sign_type				签名方式			String(10)		必选	RSA或RSA-S，不参与签名
//    sign					签名				String			必选	签名数据，具体请见附录的签名规则定义。 业务参数
//    order_no				商户网站唯一订单号	String(64)		必选	商户系统订单号，由商户系统生成,保证其唯一性，最长100位,由字母、数字组成.举例：1000201666。
//    order_time			商户订单时间		Date			必选	商户订单时间，格式：yyyy-MM-dd HH:mm:ss，举例：2013-11-01 12:34:58
//    order_amount			商户订单总金额		Number(13,2)	必选	该笔订单的总金额，以元为单位，精确到小数点后两位。举例：12.01。
//    product_name			商品名称			String(100)		必选	商品名称，不超过100个字符。 举例：华硕G750Y47JX-BL。

  private static final String merchant_code               	="merchant_code";
  private static final String service_type           		="service_type";
  private static final String notify_url           			="notify_url";
  private static final String interface_version           	="interface_version";
  private static final String client_ip          			="client_ip";
  private static final String sign_type              		="sign_type";
  private static final String order_no            			="order_no";
  private static final String order_time           			="order_time";
  private static final String order_amount            		="order_amount";
  private static final String product_name            		="product_name";
  
  private static final String sign                ="sign";
  private static final String key                 ="key";

  @Override
  protected Map<String, String> buildPayParam() throws PayException {
      Map<String, String> payParam = new TreeMap<String, String>() {
          {
              put(merchant_code, channelWrapper.getAPI_MEMBERID());
              put(order_no,channelWrapper.getAPI_ORDER_ID());
              put(order_amount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
              put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
              put(service_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
             // put(returnUrl,channelWrapper.getAPI_WEB_URL());
              put(client_ip,channelWrapper.getAPI_Client_IP());
              put(interface_version,"V3.1");
              put(sign_type,"RSA-S");
              put(order_time,DateUtil.formatDateTimeStrByParam("yyyy-MM-dd HH:mm:ss"));
              put(product_name,channelWrapper.getAPI_ORDER_ID());
          }
      };
      log.debug("[都付支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
      return payParam;
  }

   protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
      String signSrc=String.format("%s%s%s%s%s%s%s%s%s",
    		  client_ip+"="+ api_response_params.get(client_ip)+"&",
    		  interface_version+"="+ api_response_params.get(interface_version)+"&",
    		  merchant_code+"="+ api_response_params.get(merchant_code)+"&",
    		  notify_url+"="+ api_response_params.get(notify_url)+"&",
    		  order_amount+"="+ api_response_params.get(order_amount)+"&",
    		  order_no+"="+ api_response_params.get(order_no)+"&",
    		  order_time+"="+ api_response_params.get(order_time)+"&",
    		  product_name+"="+ api_response_params.get(product_name)+"&",
    		  service_type+"="+ api_response_params.get(service_type)
      );
      String signMD5="";
	  try {
		signMD5 = RSAWithSoftware.signByPrivateKey(signSrc, channelWrapper.getAPI_KEY());
	  } catch (Exception e) {
		e.printStackTrace();
	  }
      log.debug("[都付支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
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
      	String resultStr = new HttpClientUtil().doPost(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, "utf-8");
        if (StringUtils.isBlank(resultStr)) {
            log.error("[都付支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(EMPTYRESPONSE);
        }
        Map<String, Object> rtnMap=null;
		try {
			rtnMap = ParseXMLUtils.xmlToMap(resultStr);
		} catch (Exception e) {
			log.error("[都付支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
		}
        if (null != rtnMap && rtnMap.containsKey("dinpay")) {
        	Map<String, Object> dinpayMap=(Map<String, Object>) rtnMap.get("dinpay");
        	if(null != dinpayMap && dinpayMap.containsKey("response")){
        		Map<String, Object> responseMap=(Map<String, Object>) dinpayMap.get("response");
        		if(null!=responseMap&&responseMap.containsKey("result_code")&&responseMap.get("result_code").equals("0")){
        			if(HandlerUtil.isWapOrApp(channelWrapper)){
        				String code_url = responseMap.get("payURL").toString();
        				String url=java.net.URLDecoder.decode(code_url);
                        result.put(JUMPURL, url);
        			}else{
        				String code_url = responseMap.get("qrcode").toString();
                        result.put(JUMPURL, code_url);
        			}
        			
        		}else{
        			log.error("[都付支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
        		}
        	}else{
        		log.error("[都付支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
        	}
        }else {
            log.error("[都付支付]-[请求支付]-3.5.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        payResultList.add(result);
      }
      log.debug("[都付支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
      log.debug("[都付支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
      return requestPayResult;
  }
  
  
}