package dc.pay.business.mingshu;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Base64;
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
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.UnicodeUtil;
import dc.pay.utils.ValidateUtil;


/**
 * 
 * 
 * @author sunny
 * Dec 25, 2018
 */
@RequestPayHandler("MINGSHU")
public final class MingShuRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(MingShuRequestHandler.class);

//    字段名称			字段说明			类型	           必填	                 
//    version			版本号			string	  Y	
//    merId				商户号			string	  Y	
//    orderTime			订单提交时间		datetime  Y	格式为：yyyyMMddHHmmss
//    signType			签名方式			string	  Y	签名方式：MD5
//    charse			参数编码字符集		string	  Y	UTF-8
//    transactionId		商户订单号		String	  Y	商户提交的订单号，必须在自身账户交易中唯一
//    orderAmount		商户订单金额		decimal(10,2)	Y	例如10元，金额格式为10.00
//    payType			支付方式			String	  Y	对应后面的支付方式代码
//    cardType			卡类型			String	  Y	卡类型，01：借记卡02：信用卡，
//    bgUrl				异步通知地址		String	  Y	支付成功后台异步通知地址
//    bankcode			银行编号			String	  N	(payType=BANK)时使用
//    signData			加密字符			String	  Y	MD5加密 看下面5加密方法

  private static final String version                 ="version";
  private static final String merId                   ="merId";
  private static final String orderTime               ="orderTime";
  private static final String signType           	  ="signType";
  private static final String charse                  ="charse";
  private static final String transactionId           ="transactionId";
  private static final String orderAmount             ="orderAmount";
  private static final String payType                 ="payType";
  private static final String cardType                ="cardType";
  private static final String bgUrl              	  ="bgUrl";
  
  private static final String signData                ="signData";
  
  private static final String key        ="key";


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
    	Map<String, String> payParam = new TreeMap<String, String>() {
            {
            	put(version,"3.03");
            	put(merId,channelWrapper.getAPI_MEMBERID());
            	put(orderTime,HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "yyyyMMddHHmmss"));
            	put(signType,"MD5");
            	put(charse,"UTF-8");
            	put(transactionId,channelWrapper.getAPI_ORDER_ID());
            	put(orderAmount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            	put(payType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            	put(bgUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            	put(cardType,"01");
            }
        };
        log.debug("[明书支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
    	//签名规则
    	List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
    	StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        signSrc.append(key+"="+channelWrapper.getAPI_KEY());
        String paramsStr =signSrc.toString();
        String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr, "utf-8");
        log.debug("[明书支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return signMD5;
    }

     protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
       /*if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
        }*/
        	String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
	        if (StringUtils.isBlank(resultStr)) {
	            log.error("[明书支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        resultStr = UnicodeUtil.unicodeToString(resultStr);
	        JSONObject resJson=null;
	        try {
	            resJson = JSONObject.parseObject(resultStr);
	        } catch (Exception e) {
	            e.printStackTrace();
	            log.error("[明书支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        if (null != resJson && resJson.containsKey("code") && resJson.getString("code").equals("00")) {
	        	String html=URLDecoder.decode(resJson.get("codeHtml").toString());
	        	String codeHtml="";
				try {
					codeHtml = new String(Base64.getDecoder().decode(html.replace("\r\n", "")),"utf-8");
				} catch (UnsupportedEncodingException e) {
					log.error("[明书支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
					throw new PayException(resultStr);
				}
				
				if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
		            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
		            result.put(JUMPURL,codeHtml);
		        }else{
		        	result.put(QRCONTEXT,codeHtml);
		        }
	            
	        }else {
	            log.error("[明书支付]]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[明书支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[明书支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
    
}