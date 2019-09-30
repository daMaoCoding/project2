package dc.pay.business.niuniuzhifu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
 * 
 * 
 * @author sunny
 * Dec 18, 2018
 */
@RequestPayHandler("NIUNIUZHIFU")
public final class NiuNiuZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(NiuNiuZhiFuPayRequestHandler.class);

//    login_id			商户号			INT			否			签约牛牛搬运的唯一商户编号
//    create_time		请求时间			String		否			格式：unix_timestamp
//    create_ip			客户端IP			String(16)	否			不是商户请求的服务器IP,指的是商户的客户IPv4地址
//    nonce				随机数			INT(11)		否	
//    sign_type			签名类型			string		否			目前只支持MD5
//    sign				签名				String		否			签名信息支付业务参数
//    pay_type			支付类型			String(16)	否			见附件“支付类型“
//    order_type		订单类型			Int(1)		否			1为充值订单 2提现订单
//    order_sn			商户订单编号		String(32)	否			由商户内部生成的唯一订单编号, 最长不超过32字符只能由数字或字母组成
//    amount			交易金额			Double(10,2)	否		单位：元，两位小数,不能小于1.0，具体金额上限和下限根据实际情况制定
//    send_currency		货币类型			String(8)	否			客户打款币种，目前只支持人民币：cny
//    recv_currency		货币类型			String(8)	否			商户接收币种，目前只支持人民币：cny
//    extra				附加字段			String(128)	可空			附加数据，在查询和支付通知中原样返回，该字段主要用于商户携带订单的自定义数据
//    notify_url		后台通知回调URL	String(128)	否			需要带上http://或https://

  private static final String login_id               ="login_id";
  private static final String create_time            ="create_time";
  private static final String create_ip           	 ="create_ip";
  private static final String nonce           		 ="nonce";
  private static final String sign_type          	 ="sign_type";
  private static final String pay_type               ="pay_type";
  private static final String order_type             ="order_type";
  private static final String order_sn           	 ="order_sn";
  private static final String amount           	 	 ="amount";
  private static final String send_currency          ="send_currency";
  private static final String recv_currency          ="recv_currency";
  private static final String notify_url          	 ="notify_url";
  
  private static final String signType               ="signType";
  private static final String sign                   ="sign";
  private static final String api_secret             ="api_secret";
  


  @Override
  protected Map<String, String> buildPayParam() throws PayException {
      Map<String, String> payParam = new TreeMap<String, String>() {
          {
              put(login_id, channelWrapper.getAPI_MEMBERID());
              put(order_sn,channelWrapper.getAPI_ORDER_ID());
              put(amount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
              put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
              put(pay_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              put(create_time,System.currentTimeMillis()/1000+"");
              put(create_ip,channelWrapper.getAPI_Client_IP());
              put(nonce,getRandom(10));
              put(sign_type,"MD5");
              put(order_type,"1");
              put(send_currency,"cny");
              put(recv_currency,"cny");
          }
      };
      log.debug("[牛牛支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
      return payParam;
  }

   protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
      List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
      StringBuilder signSrc = new StringBuilder();
      for (int i = 0; i < paramKeys.size(); i++) {
    	  signSrc.append(paramKeys.get(i).toString().toLowerCase()).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
      }
      //最后一个&转换成#
      //signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "#" );
      signSrc.append(api_secret+"="+channelWrapper.getAPI_KEY());
      String paramsStr = signSrc.toString();
      String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
      log.debug("[牛牛支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
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
	            log.error("[牛牛支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(EMPTYRESPONSE);
	        }
	        resultStr = UnicodeUtil.unicodeToString(resultStr);
	        resultStr=resultStr.replaceAll("\\\\", "");
	        if (!resultStr.contains("{") || !resultStr.contains("}")) {
	            log.error("[牛牛支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        JSONObject resJson;
	        try {
	            resJson = JSONObject.parseObject(resultStr);
	        } catch (Exception e) {
	            e.printStackTrace();
	            log.error("[牛牛支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        if (null != resJson && resJson.containsKey("code") && resJson.getString("code").equals("0")) {
	            String code_url = resJson.getString("http_url");
	            result.put(JUMPURL, code_url);
	        }else {
	            log.error("[牛牛支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        payResultList.add(result);
      }
      log.debug("[牛牛支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
      log.debug("[牛牛支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
      return requestPayResult;
  }
  
  private static String getRandom(int length){
		String val = "";
		Random random = new Random();
		for (int i = 0; i < length; i++) {
			val += String.valueOf(random.nextInt(10));
		}
		return val;
	}

}