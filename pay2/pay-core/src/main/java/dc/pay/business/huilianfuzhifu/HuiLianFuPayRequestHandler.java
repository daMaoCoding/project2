package dc.pay.business.huilianfuzhifu;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
import dc.pay.utils.ValidateUtil;


/**
 * @author sunny
 * 04 17, 2019
 */
@RequestPayHandler("HUILIANFU")
public final class HuiLianFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(HuiLianFuPayRequestHandler.class);

//    字段名			变量名		必填			类型				示例值描述
//    商户号			merchNo		是			String(32)		由汇联中国分配给商户的商户唯一编码
//    商户单号		orderNo		是			String(23)		4392849234723987	商户上送订单号，保持唯一值。
//    交易金额		amount		是			String(20)		10.00	以元为单位,交易金额必须随机增加小数，确保支付安全，如支付10.00元，需随机生成10.01，10.07等金额 
//    币种			currency	是			String(20)		CNY	目前只支持CNY
//    支付类型		outChannel	是			String(10)		qq	详见附录4.1
//    银行编号		bankCode	是			String(10)		1001当支付类型为网关支付时，需要传该参数详见附录4.3支付类型为：pzfb,pwx,pysf可填任意值
//    订单标题		title		是			String(20)		消费	用于描述该笔交易的主题
//    商品描述		product		是			String(500)		消费	用于描述该笔交易商品的主体信息
//    商品备注		memo		是			String(500)		消费	用于描述该笔交易或商品的主体信息
//    同步回调地址	returnUrl	是			String(255)		http://abc.cn/returnUrl	商户服务器用来接收同步通知的http地址
//    异步通知地址	notifyUrl	是			String(255)		http://abc.cn/notifyUrl	商户服务器用来接收异步通知的http地址
//    下单时间		reqTime		是			string(128)		20170808161616	满足格式yyyyMMddHHmmss的下单时间
//    客户标识		userId		是			String(32)		12345	用来标识商户系统中的用户唯一编码，可用于单用户限额等控制0-9数字组成的字符串，保证唯一性

  private static final String merchNo               ="merchNo";
  private static final String orderNo           	="orderNo";
  private static final String amount           		="amount";
  private static final String currency           	="currency";
  private static final String outChannel          	="outChannel";
  private static final String bankCode              ="bankCode";
  private static final String title            		="title";
  private static final String product           	="product";
  private static final String memo           		="memo";
  private static final String returnUrl           	="returnUrl";
  private static final String notifyUrl           	="notifyUrl";
  private static final String reqTime           	="reqTime";
  private static final String userId           		="userId";
  private static final String context           	="context";
  private static final String encryptType           ="encryptType";
  
  private static final String signType            ="signType";
  private static final String sign                ="sign";
  private static final String key                 ="key";
  


  @Override
  protected Map<String, String> buildPayParam() throws PayException {
      Map<String, String> payParam = new TreeMap<String, String>() {
          {
              put(merchNo, channelWrapper.getAPI_MEMBERID());
              put(orderNo,channelWrapper.getAPI_ORDER_ID());
              if(HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()).endsWith(".00")){
            	  double randomAt=Math.random();
            	  if(randomAt==0||randomAt<=0.01){
            		  put(amount,new DecimalFormat("0.00").format(Double.parseDouble(HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()))+0.01)); 
            	  }else{
            		  put(amount,new DecimalFormat("0.00").format(Double.parseDouble(HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()))+randomAt));
            	  }
              }else{
            	  put(amount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
              }
              put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
              put(outChannel,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              put(returnUrl,channelWrapper.getAPI_WEB_URL());
              put(product,channelWrapper.getAPI_ORDER_ID());
              put(title,channelWrapper.getAPI_ORDER_ID());
              put(currency,"CNY");
              put(bankCode,"1");
              put(memo,channelWrapper.getAPI_ORDER_ID());
              put(reqTime,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
              put(userId,System.currentTimeMillis()/100000+"");
          }
      };
      log.debug("[汇联付支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
      return payParam;
  }

   protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
	   JSONObject sendContextJson = new JSONObject();
		sendContextJson.put("merchNo", api_response_params.get(merchNo));
		sendContextJson.put("orderNo", api_response_params.get(orderNo));
		sendContextJson.put("outChannel", api_response_params.get(outChannel));
		sendContextJson.put("title", api_response_params.get(title));
		sendContextJson.put("product", api_response_params.get(product));
		sendContextJson.put("memo", api_response_params.get(memo));
		sendContextJson.put("amount", api_response_params.get(amount));
		sendContextJson.put("currency", api_response_params.get(currency));
		sendContextJson.put("returnUrl", api_response_params.get(returnUrl));
		sendContextJson.put("notifyUrl", api_response_params.get(notifyUrl));
		sendContextJson.put("reqTime", api_response_params.get(reqTime));
		sendContextJson.put("userId", api_response_params.get(userId));
		String content = sendContextJson.toJSONString()+channelWrapper.getAPI_KEY();
		MessageDigest messageDigest=null;
		try {
			messageDigest = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		messageDigest.update(content.getBytes());
		String sendSign = HexUtil.byte2hex(messageDigest.digest());
      log.debug("[汇联付支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(sendSign));
      return sendSign;
  }

  protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
      HashMap<String, String> result = Maps.newHashMap();
      ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
      if (1==2&&(HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)  ||  HandlerUtil.isWapOrApp(channelWrapper))) {
          result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());  //.replace("method='post'","method='get'"));
          payResultList.add(result);
      }else{
    	JSONObject requestParamsJson = new JSONObject();
    	String resultStr="";
		try {
			JSONObject sendContextJson = new JSONObject();
			sendContextJson.put("merchNo", payParam.get(merchNo));
			sendContextJson.put("orderNo", payParam.get(orderNo));
			sendContextJson.put("outChannel", payParam.get(outChannel));
			sendContextJson.put("title", payParam.get(title));
			sendContextJson.put("product", payParam.get(product));
			sendContextJson.put("memo", payParam.get(memo));
			sendContextJson.put("amount", payParam.get(amount));
			sendContextJson.put("currency", payParam.get(currency));
			sendContextJson.put("returnUrl", payParam.get(returnUrl));
			sendContextJson.put("notifyUrl", payParam.get(notifyUrl));
			sendContextJson.put("reqTime", payParam.get(reqTime));
			sendContextJson.put("userId", payParam.get(userId));
			requestParamsJson.put("context", JSON.toJSONBytes(sendContextJson));
			requestParamsJson.put("sign", pay_md5sign);
			requestParamsJson.put("encryptType", "MD5");
			resultStr = RequestUtils.doPostJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), requestParamsJson.toJSONString(), "utf-8");
		} catch (Exception e3) {
			e3.printStackTrace();
		}
      	//String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(),requestParamsJson.toJSONString());
        if (StringUtils.isBlank(resultStr)) {
            log.error("[汇联付支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(EMPTYRESPONSE);
        }
        if (!resultStr.contains("{") || !resultStr.contains("}")) {
            log.error("[汇联付支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        JSONObject resJson;
        try {
            resJson = JSONObject.parseObject(resultStr);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[汇联付支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        if (null != resJson && resJson.containsKey("code") && resJson.getString("code").equals("0")) {
            try {
            	byte[] recvContext = resJson.getBytes("context");
        		String recvSource = new String(recvContext,"UTF-8");
        		JSONObject	sourceJson = JSONObject.parseObject(recvSource);
        		 result.put(JUMPURL, sourceJson.getString("code_url"));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
        }else {
            log.error("[汇联付支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        payResultList.add(result);
      }
      log.debug("[汇联付支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
      log.debug("[汇联付支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
      return requestPayResult;
  }
}