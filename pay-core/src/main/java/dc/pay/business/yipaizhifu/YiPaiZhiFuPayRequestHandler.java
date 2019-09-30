package dc.pay.business.yipaizhifu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

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
import dc.pay.utils.ValidateUtil;

/**
 * 
 * @author beck Aug 21, 2018
 */
@RequestPayHandler("YIPAIZHIFU")
public final class YiPaiZhiFuPayRequestHandler extends PayRequestHandler {
	private static final Logger log = LoggerFactory.getLogger(YiPaiZhiFuPayRequestHandler.class);

	@Override
	protected Map<String, String> buildPayParam() throws PayException {
		Map<String, String> payParam = Maps.newHashMap();
		payParam.put("parter", channelWrapper.getAPI_MEMBERID());
		payParam.put("money", HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
		payParam.put("requestId", channelWrapper.getAPI_ORDER_ID());
		payParam.put("requestIp", channelWrapper.getAPI_Client_IP());
		payParam.put("paybank", channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());//TODO:
		payParam.put("requestTime",DateUtil.formatDateTimeStrByParam(DateUtil.dateTimeString2));
		payParam.put("goodsName", "goods");
		payParam.put("returnUrl", channelWrapper.getAPI_WEB_URL());
		payParam.put("notifyUrl", channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
		//payParam.put("bankCode", "ICBC"); //TODO:
				
		log.debug("[亿拍支付]-[请求支付]-1.组装请求参数完成：{}", JSON.toJSONString(payParam));
		return payParam;
	}

	@Override
	protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
	    StringBuilder signSrc = new StringBuilder();
	    List<String> paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
	    for (int i = 0; i < paramKeys.size(); i++) {
            String keyName = paramKeys.get(i);
            String value = api_response_params.get(keyName);
            if (StringUtils.isNotBlank(value)) {
                if(!keyName.equalsIgnoreCase(paramKeys.get(paramKeys.size()-1))){
                    signSrc.append(keyName).append("=").append(value).append("&");
                }else {
                    signSrc.append(keyName).append("=").append(value);    
                }
            }
        }
	    
		signSrc.append(channelWrapper.getAPI_KEY());
		String paramsStr = signSrc.toString();
		
		String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
		log.debug("[亿拍支付]-[请求支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5));
		return signMd5;
	}

	@Override
	protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign)
			throws PayException {
		payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		HashMap<String, String> result = Maps.newHashMap();

		try {

		    String resultStr = "";
		    
		    if(channelWrapper.getAPI_CHANNEL_BANK_NAME().equalsIgnoreCase("YIPAIZHIFU_BANK_WAP_ZFB_SM")){    //如果为支付宝wap
                resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.GET);
                result = this.getZFBWAPFormUrl(resultStr);
            }else{
    			if (HandlerUtil.isWY(channelWrapper)||HandlerUtil.isWapOrApp(channelWrapper) ||HandlerUtil.isWebWxGZH(channelWrapper)||HandlerUtil.isYLKJ(channelWrapper)) {
    			    String html = HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam).toString();
    			    html = html.replace("method='post'", "method='get'");
    				result.put(HTMLCONTEXT, html);
    				
    			} else{
    			    resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.GET);
    			    JSONObject jsonObj = JSON.parseObject(resultStr);
    	            String rescode = jsonObj.getString("rescode");
    	            
    	            if(!rescode.equalsIgnoreCase("0000")){
    	                log.error("[亿拍支付]-[请求支付]3.1.返回数据异常{}", resultStr);
    	                throw new PayException(resultStr);
    	            }
    	            
    	            String qrcode = jsonObj.getString("qrcode");
    	            result.put(QRCONTEXT, qrcode);
    			}
            }
			payResultList.add(result);

		} catch (Exception e) {
			log.error("[亿拍支付]-[请求支付]3.2.发送支付请求，及获取支付请求结果出错：", e);
			throw new PayException(e.getMessage(), e);
		}
		log.debug("[亿拍支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));

		return payResultList;
	}

	@Override
	protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
		RequestPayResult requestPayResult = new RequestPayResult();
		if (null != resultListMap && !resultListMap.isEmpty()) {
			if (resultListMap.size() == 1) {
				Map<String, String> resultMap = resultListMap.get(0);
				requestPayResult = buildResult(resultMap, channelWrapper, requestPayResult);
			}
			if (ValidateUtil.requestesultValdata(requestPayResult)) {
				requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
			} else {
				throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
			}
		} else {
			throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
		}
		log.debug("[亿拍支付]-[请求支付]-4.处理请求响应成功：{}", JSON.toJSONString(requestPayResult));
		return requestPayResult;
	}
	
	
	/**
	 * 由于支付宝支付WAP（返回json）和wap（返回form同时跳转到APP）第三方会交替使用，故做特殊处理
	 * */
	private HashMap<String, String> getZFBWAPFormUrl(String content) throws PayException{
	    if(StringUtils.isBlank(content)) return null;
	    
	    HashMap<String, String> result = Maps.newHashMap();
	    
	    if(content.indexOf("<form") !=-1){ //表单跳转
	        Document doc = Jsoup.parse(content);
	        Element formElement = doc.getElementsByTag("form").first();
	        String action = formElement.attr("action");
	        Map<String, String> inputs = HandlerUtil.parseFormElement(formElement);
	        
	        String html = HandlerUtil.getHtmlContent(action,inputs).toString();
	        result.put(HTMLCONTEXT, html);
	        
	    }else{     //json
	        JSONObject jsonObj = JSON.parseObject(content);
	        String rescode = jsonObj.getString("rescode");
	        
	        if(!rescode.equalsIgnoreCase("0000")){
	            log.error("[亿拍支付]-[请求支付]5.返回数据异常{}", content);
	            throw new PayException(content);
	        }
	        
	        String qrcode = jsonObj.getString("qrcode");
	        
	        result.put(JUMPURL, qrcode);
	        
	    }
	    
	    return result;
	}
	
}