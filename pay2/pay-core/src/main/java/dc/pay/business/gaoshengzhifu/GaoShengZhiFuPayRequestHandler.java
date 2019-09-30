package dc.pay.business.gaoshengzhifu;

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
import com.alibaba.fastjson.serializer.SerializerFeature;
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
@RequestPayHandler("GAOSHENGZHIFU")
public final class GaoShengZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(GaoShengZhiFuPayRequestHandler.class);

//    参数			字符长度			描述											必填
//    appId			String(16)		商户号（我方高盛提供）							是
//    payType		String(16)		扫码支付网关:（二维码地址生成二维码图片进行扫码支付）支付宝填写ZFB；微信填写WX；QQ钱包填写：QQ;
//    nonceStr		String(14)		随机数：唯一性	是
//    outTradeNo	String(20)		订单号：唯一性	是
//    totalAmount	String(14)		金额（单位：分）   例：1元=100	是
//    goodsInfo		String(20)		商品信息(字符长度限定20字符，禁止出现标点符号，特殊符号，可能影响商户交易成功率)	是
//    notifyUrl		String(128)		交易异步通知地址：我方通知商户接收交易结果地址	是
//    returnUrl		String(128)		回显地址：若无，则与异步通知地址相同即可	是
//    requestIp		String(15)		商户真实交易请求IP：(格式：112.224.69.132)：禁止使用127.0.0.1，通道校验非商户正式ip，则影响商户成功率	是
//    sign			String(32)		签名（字母大写）	是

  private static final String appId               ="appId";
  private static final String payType             ="payType";
  private static final String nonceStr            ="nonceStr";
  private static final String outTradeNo          ="outTradeNo";
  private static final String totalAmount         ="totalAmount";
  private static final String goodsInfo           ="goodsInfo";
  private static final String notifyUrl           ="notifyUrl";
  private static final String returnUrl           ="returnUrl";
  private static final String requestIp           ="requestIp";
  private static final String reqData           ="reqData";
  
  private static final String signType            ="signType";
  private static final String sign                ="sign";
  private static final String key                 ="key";
  
  @Override
  protected Map<String, String> buildPayParam() throws PayException {
      Map<String, String> payParam = new TreeMap<String, String>() {
          {
              put(appId, channelWrapper.getAPI_MEMBERID());
              put(outTradeNo,channelWrapper.getAPI_ORDER_ID());
              put(totalAmount,channelWrapper.getAPI_AMOUNT());
              put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
              put(payType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              put(returnUrl,channelWrapper.getAPI_WEB_URL());
              put(goodsInfo,channelWrapper.getAPI_ORDER_ID());
              put(nonceStr,UUID.randomUUID().toString().replaceAll("-", ""));
              put(requestIp,channelWrapper.getAPI_Client_IP());
          }
      };
      log.debug("[高盛支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
      return payParam;
  }

   protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
      List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
      StringBuilder signSrc = new StringBuilder();
      JSONObject resJson=new JSONObject();
      for (int i = 0; i < paramKeys.size(); i++) {
          if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
        	  resJson.put(paramKeys.get(i).toString(), api_response_params.get(paramKeys.get(i)));
          }
      }
      String paramJson=JSONObject.toJSONString(resJson,SerializerFeature.SortField.MapSortField);
      String paramsStr = paramJson+channelWrapper.getAPI_KEY();
      String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr);
      log.debug("[高盛支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
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
    	HashMap<String, String> postMap = Maps.newHashMap();
    	postMap.put(reqData, HandlerUtil.simpleMapToJsonStr(payParam));
      	String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), postMap,null);
	        if (StringUtils.isBlank(resultStr)) {
	            log.error("[高盛支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(EMPTYRESPONSE);
	        }
	        try {
				resultStr = new String(resultStr.getBytes("ISO-8859-1"), "UTF-8");
			} catch (UnsupportedEncodingException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
	        if (!resultStr.contains("{") || !resultStr.contains("}")) {
	            log.error("[高盛支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        JSONObject resJson;
	        try {
	            resJson = JSONObject.parseObject(resultStr);
	        } catch (Exception e) {
	            e.printStackTrace();
	            log.error("[高盛支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        if (null != resJson && resJson.containsKey("resultCode") && resJson.getString("resultCode").equals("00")) {
	        	if(HandlerUtil.isWEBWAPAPP_SM(channelWrapper)){
	        		String code_url = resJson.getString("qrCode");
		            result.put(QRCONTEXT, code_url);
	        	}
	            if(HandlerUtil.isWapOrApp(channelWrapper)){
	            	String code_url = resJson.getString("qrCode");
		            result.put(JUMPURL, code_url);
	            }
	        }else {
	            log.error("[高盛支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        payResultList.add(result);
      }
      log.debug("[高盛支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
      log.debug("[高盛支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
      return requestPayResult;
  }
}