package dc.pay.business.majizhifu;

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
 * @author sunny
 */
@RequestPayHandler("MAJIZHIFU")
public final class MaJiZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(MaJiZhiFuPayRequestHandler.class);

//    参数名称			参数含义				格式			出现要求			备注
//    txnType			报文类型				N2			M				01
//    txnSubType		报文子类				N2			M				21
//    secpVer			安全协议版本			AN3..16		M				icp3-1.1   （注意，旧版本的secpver = icp3-1.0）
//    secpMode			安全协议类型			AN4..8		M				固定值 perm
//    macKeyId			密钥识别				ANS1..16	M				密钥编号，由平台提供，现与商户号相同
//    orderDate			下单日期				N8			M				YYYYMMDD
//    orderTime			下单时间				N6			M				hhmmss
//    merId				商户代号				AN1..15		M				由平台分配的商户号
//    orderId			商户订单号			AN8..32		M				商户系统产生，同一商户同一交易日唯一
//    pageReturnUrl		交易结果页面通知地址	ANS1..256	M	
//    notifyUrl			交易结果后台通知地址	ANS1..128	M				交易结果以后台通知为准
//    productTitle		商品名称				ANS1..64	M				用已标注在支付页面主要的商品说明
//    txnAmt			交易金额				N1..12		M				单位为分，实际交易金额
//    currencyCode		交易币种				NS3			M				默认：156
//    bankNum			联行号				N8			O				用户支付卡所属银行所对应的英文代号，详见联行号对照表：  网银支付：不跳转收银台，直连银行必传
//    timeStamp			时间戳				N14			M				请带入报文(目前)时间，格式：YYYYMMDDhhmmss
//    mac				签名					M							请参考安全方案

  private static final String txnType               ="txnType";
  private static final String txnSubType            ="txnSubType";
  private static final String secpVer               ="secpVer";
  private static final String secpMode           	="secpMode";
  private static final String macKeyId          	="macKeyId";
  private static final String orderDate             ="orderDate";
  private static final String orderTime            	="orderTime";
  private static final String merId           		="merId";
  private static final String orderId           	="orderId";
  private static final String pageReturnUrl         ="pageReturnUrl";
  private static final String notifyUrl         	="notifyUrl";
  private static final String productTitle         	="productTitle";
  private static final String txnAmt         		="txnAmt";
  private static final String currencyCode         	="currencyCode";
  private static final String bankNum         		="bankNum";
  private static final String timeStamp         	="timeStamp";
  private static final String clientIp         		="clientIp";
  
  private static final String signType            ="signType";
  private static final String sign                ="sign";
  private static final String key                 ="k";
  


  @Override
  protected Map<String, String> buildPayParam() throws PayException {
      Map<String, String> payParam = new TreeMap<String, String>() {
          {
              put(merId, channelWrapper.getAPI_MEMBERID());
              put(orderId,channelWrapper.getAPI_ORDER_ID());
              put(txnAmt,channelWrapper.getAPI_AMOUNT());
              put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
              put(txnType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
              put(txnSubType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
              put(pageReturnUrl,channelWrapper.getAPI_WEB_URL());
              put(timeStamp,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
              put(productTitle,channelWrapper.getAPI_ORDER_ID());
              put(secpVer,"icp3-1.1");
              put(secpMode,"perm");
              put(currencyCode,"156");
              put(macKeyId,channelWrapper.getAPI_MEMBERID());
              put(orderDate,DateUtil.formatDateTimeStrByParam("yyyyMMdd"));
              put(orderTime,DateUtil.formatDateTimeStrByParam("HHmmss"));
              if(HandlerUtil.isWapOrApp(channelWrapper)){
            	  put(clientIp,channelWrapper.getAPI_Client_IP());
            	  put("sceneBizType","WAP");
            	  put("wapUrl","http://www.baidu.com");
            	  put("wapName","good");
              }
              if(HandlerUtil.isWY(channelWrapper)){
            	  put(bankNum,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[2]);
              }
          }
      };
      log.debug("[麻吉支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
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
      signSrc.append(key+"="+channelWrapper.getAPI_KEY());
      String paramsStr = signSrc.toString();
      String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
      log.debug("[麻吉支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
      return signMD5;
  }

  protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
      payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
      HashMap<String, String> result = Maps.newHashMap();
      ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
      if (HandlerUtil.isYLKJ(channelWrapper)||HandlerUtil.isWY(channelWrapper)||HandlerUtil.isYLSM(channelWrapper)) {
          result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());  //.replace("method='post'","method='get'"));
          payResultList.add(result);
      }else{
      	String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,null);
	        if (StringUtils.isBlank(resultStr)) {
	            log.error("[麻吉支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(EMPTYRESPONSE);
	        }
	        if (!resultStr.contains("{") || !resultStr.contains("}")) {
	            log.error("[麻吉支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        JSONObject resJson;
	        try {
	            resJson = JSONObject.parseObject(resultStr);
	        } catch (Exception e) {
	            e.printStackTrace();
	            log.error("[麻吉支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        if (null != resJson && resJson.containsKey("txnStatus") && resJson.getString("txnStatus").equals("01")) {
	        	if(HandlerUtil.isWapOrApp(channelWrapper)){
	        		String code_url = resJson.getString("codePageUrl");
	 	            result.put(JUMPURL, code_url);
	        	}else{
	        		String code_url = resJson.getString("codeImgUrl");
	 	            result.put(JUMPURL, code_url);
	        	}
	           
	        }else {
	            log.error("[麻吉支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        payResultList.add(result);
      }
      log.debug("[麻吉支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
      log.debug("[麻吉支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
      return requestPayResult;
  }
}