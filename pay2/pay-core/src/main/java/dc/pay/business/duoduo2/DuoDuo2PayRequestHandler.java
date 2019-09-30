package dc.pay.business.duoduo2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
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
import dc.pay.utils.ValidateUtil;
import dc.pay.utils.qr.QRCodeUtil;

/**
 * ************************
 * @author beck 2229556569
 */

@RequestPayHandler("DUODUO2")
public final class DuoDuo2PayRequestHandler extends PayRequestHandler {
	private static final Logger log = LoggerFactory.getLogger(DuoDuo2PayRequestHandler.class);

	
	private static final String merchantId = "MerId";              //商户号
	private static final String orderNumber = "OrdId";             //订单号
	private static final String money = "OrdAmt";                  //支付金额
	private static final String payType = "PayType";               //支付类型，默认DT
	private static final String curCode = "CurCode";               //支付币种，默认CNY
	private static final String bankCode = "BankCode";              //支付类型
	private static final String productInfo = "ProductInfo";
	private static final String remark = "Remark";
	private static final String returnUrl = "ReturnURL";           //同步通知url
	private static final String notifyUrl = "NotifyURL";           //异步通知url
	private static final String signType = "SignType";
	//private static final String clientip = "clientip";             //客户ip
	


	@Override
	protected Map<String, String> buildPayParam() throws PayException {
		Map<String, String> payParam = Maps.newHashMap();
		
		payParam.put(merchantId, channelWrapper.getAPI_MEMBERID());
		payParam.put(orderNumber, channelWrapper.getAPI_ORDER_ID());
		payParam.put(money, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
		payParam.put(payType, "DT");
		payParam.put(curCode, "CNY");
		payParam.put(bankCode, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
		payParam.put(productInfo, "GOODSNAME");
		payParam.put(remark, "remark");
		payParam.put(returnUrl, channelWrapper.getAPI_WEB_URL());
		payParam.put(notifyUrl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
		payParam.put(signType, "MD5");
		
		log.debug("[多多2]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
		return payParam;
	}

	@Override
	protected String buildPaySign(Map<String, String> params) throws PayException {
	    
	    StringBuilder sb = new StringBuilder();
	    sb.append(merchantId).append("=").append(params.get(merchantId)).append("&");
	    sb.append(orderNumber).append("=").append(params.get(orderNumber)).append("&");
	    sb.append(money).append("=").append(params.get(money)).append("&");
	    sb.append(payType).append("=").append(params.get(payType)).append("&");
	    sb.append(curCode).append("=").append(params.get(curCode)).append("&");
	    sb.append(bankCode).append("=").append(params.get(bankCode)).append("&");
	    sb.append(productInfo).append("=").append(params.get(productInfo)).append("&");
	    sb.append(remark).append("=").append(params.get(remark)).append("&");
	    sb.append(returnUrl).append("=").append(params.get(returnUrl)).append("&");
	    sb.append(notifyUrl).append("=").append(params.get(notifyUrl)).append("&");
	    sb.append(signType).append("=").append(params.get(signType)).append("&");
	    sb.append("MerKey").append("=").append(channelWrapper.getAPI_KEY());
        
		String signStr = sb.toString();
		String pay_md5sign = HandlerUtil.getMD5UpperCase(signStr).toLowerCase();
		log.debug("[多多2]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
		return pay_md5sign;
	}

	@Override
	protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
		payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		Map result = Maps.newHashMap();
		        
		try {
			if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLWAP(channelWrapper) 
			        ||this.channelWrapper.getAPI_CHANNEL_BANK_NAME().equalsIgnoreCase("DUODUO2_BANK_WAP_ZFB_SM")) {
				String htmlContent = HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam).toString();
				result.put(HTMLCONTEXT, htmlContent);
			}else {
			    String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
				Document document = Jsoup.parse(resultStr);
				Elements imgUrls = document.select("[name='imgUrl']");
				
				if(imgUrls.size() == 0){
				    String errorMsg = "[多多2]-[请求支付] 3.1:找不到二维码。第三方返回html："+resultStr;
				    log.error(errorMsg);
				    throw new PayException(errorMsg);
				}
				
				String base64Img = imgUrls.first().attr("value");
				if(StringUtils.isBlank(base64Img)){
				    String errorMsg = "[多多2]-[请求支付] 3.2:二维码内容为空。第三方返回html："+resultStr;
				    throw new PayException(errorMsg);
				}
				
				String qrinfo = QRCodeUtil.decodeByBase64(base64Img);
				result.put(QRCONTEXT, qrinfo);
				
			}
			payResultList.add(result);
			
		} catch (Exception e) {
			log.error("[多多2]-[请求支付]3.3.发送支付请求，及获取支付请求结果出错：", e);
			throw new PayException(e.getMessage(), e);
		}
		
		log.debug("[多多2]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));
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
		log.debug("[多多2]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
		return requestPayResult;
	}
	
	
}