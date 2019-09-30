package dc.pay.business.fengsheng;

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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayRequestHandler;
import dc.pay.business.RequestPayResult;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.UnicodeUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 
 * @author beck Aug 11, 2018
 */
@RequestPayHandler("FENGSHENG")
public final class FengShengPayRequestHandler extends PayRequestHandler {
	private static final Logger log = LoggerFactory.getLogger(FengShengPayRequestHandler.class);

	@Override
	protected Map<String, String> buildPayParam() throws PayException {
		Map<String, String> payParam = Maps.newHashMap();
		payParam.put("version", "3.0");
		payParam.put("method", "fsapp.online.interface");
		payParam.put("partner", channelWrapper.getAPI_MEMBERID());
		payParam.put("banktype", channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
		payParam.put("paymoney", HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
		payParam.put("ordernumber", channelWrapper.getAPI_ORDER_ID());
		payParam.put("callbackurl", channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
		payParam.put("hrefbackurl", channelWrapper.getAPI_CHANNEL_BANK_URL());
		payParam.put("isshow", "1"); //默认为1，跳转到网关页面进行扫码，如设为0，则网关只返回二维码图片地址供用户自行调用
//		if(HandlerUtil.isWapOrApp(channelWrapper)){
//		    payParam.put("isshow", "0");    
//		}
		
		log.debug("[锋胜]-[请求支付]-1.组装请求参数完成：{}", JSON.toJSONString(payParam));
		return payParam;
	}

	protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
	    StringBuilder signSrc = new StringBuilder();
	    signSrc.append("version=").append(api_response_params.get("version")).append("&");
	    signSrc.append("method=").append(api_response_params.get("method")).append("&");
	    signSrc.append("partner=").append(api_response_params.get("partner")).append("&");
	    signSrc.append("banktype=").append(api_response_params.get("banktype")).append("&");
	    signSrc.append("paymoney=").append(api_response_params.get("paymoney")).append("&");
	    signSrc.append("ordernumber=").append(api_response_params.get("ordernumber")).append("&");
	    signSrc.append("callbackurl=").append(api_response_params.get("callbackurl"));
		signSrc.append(channelWrapper.getAPI_KEY());
		String paramsStr = signSrc.toString();
		
		String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
		log.debug("[锋胜]-[请求支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5));
		return signMd5;
	}

	protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign)
			throws PayException {
		payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		HashMap<String, String> result = Maps.newHashMap();

		try {

//		    if (HandlerUtil.isWY(channelWrapper) ||HandlerUtil.isWebWxGZH(channelWrapper)||HandlerUtil.isYLKJ(channelWrapper)||HandlerUtil.isWapOrApp(channelWrapper)) {
//		        //	result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam).toString());
//		        String url = HttpUtil.getURLWithParam(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
//		        result.put(JUMPURL,url);
//		    } else {
//		        String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.GET);
//		        String qrcode = this.handleScan(resultStr);
//		        result.put(QRCONTEXT ,qrcode);
//		        
//		    }
            String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.GET);
            if (StringUtils.isBlank(resultStr)) {
                log.error("[锋胜]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
                //log.error("[通扫]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
                //throw new PayException("返回空,参数："+JSON.toJSONString(map));
            }
            String qrcode = this.handleScan(resultStr);
            result.put(handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT ,qrcode);
            payResultList.add(result);

		} catch (Exception e) {
			log.error("[锋胜]-[请求支付]3.2.发送支付请求，及获取支付请求结果出错：", e);
			throw new PayException(e.getMessage(), e);
		}
		log.debug("[锋胜]-[请求支付]-3.3.发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));

		return payResultList;
	}

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
		log.debug("[锋胜]-[请求支付]-4.处理请求响应成功：{}", JSON.toJSONString(requestPayResult));
		return requestPayResult;
	}
	
	/**
	 * 处理扫码
	 * */
	private String handleScan(String html) throws PayException{
	    Document doc = Jsoup.parse(html);
	    Element formEle = doc.getElementsByTag("form").first();
	    
	    if(formEle == null){
	        log.error("[锋胜]-[请求支付]5.发送支付请求，及获取支付请求结果出错：{}",html);
            throw new PayException(html);
	    }
	    
	    String formAction = formEle.attr("action");
	    Map<String,String> hidesInput = HandlerUtil.parseFormElement(formEle);
	    String html2 = RestTemplateUtil.sendByRestTemplate(formAction, hidesInput, String.class, HttpMethod.POST);
	    html2 = UnicodeUtil.unicodeToString(html2);
	    doc=Jsoup.parse(html2);
	    Element show_qrcode = doc.getElementById("show_qrcode");
	    if(show_qrcode == null){
	        log.error("[锋胜]-[请求支付]6.发送支付请求，及获取支付请求结果出错：{}",html2);
            throw new PayException(html2);
	    }
	    
	    String src = show_qrcode.attr("src");
	    if(StringUtils.isBlank(src)){
	        log.error("[锋胜]-[请求支付]7.发送支付请求，及获取支付请求结果出错：{}",html2);
            throw new PayException(html2);
	    }
	    
	    String[] srcItems = src.split("d=");
	    if(srcItems.length<2){
	        log.error("[锋胜]-[请求支付]8.发送支付请求，及获取支付请求结果出错：{}",html2);
            throw new PayException(html2);
	    }
	    
	    String qrcode = srcItems[1];
	    return qrcode;
	    
	    
	}
	
}