package dc.pay.business.yipinfu;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
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
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * ************************
 * @author beck 2229556569
 */

@RequestPayHandler("YIPINFU")
public final class YiPinFuPayRequestHandler extends PayRequestHandler {
	private static final Logger log = LoggerFactory.getLogger(YiPinFuPayRequestHandler.class);

	private static final String appId = "appId";
	private static final String money = "money";
	private static final String payType = "payType";
	private static final String orderNumber = "orderNumber";
	private static final String notifyUrl = "notifyUrl";
	private static final String returnUrl = "returnUrl";
	//private static final String signature = "signature"; 

	@Override
	protected Map<String, String> buildPayParam() throws PayException {
		Map<String, String> payParam = Maps.newHashMap();
		payParam.put(appId, channelWrapper.getAPI_MEMBERID());
		payParam.put(orderNumber, channelWrapper.getAPI_ORDER_ID());
		payParam.put(money, HandlerUtil.getYuanWithoutZero(channelWrapper.getAPI_AMOUNT()));
		payParam.put(notifyUrl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
		payParam.put(payType, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
		payParam.put(returnUrl, channelWrapper.getAPI_WEB_URL());
		
		log.debug("[一品付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
		return payParam;
	}

	protected String buildPaySign(Map<String, String> params) throws PayException {

		StringBuilder sb = new StringBuilder();
		sb.append(params.get(appId)).append("&");
		sb.append(params.get(money)).append("&");
		sb.append(params.get(payType)).append("&");
		sb.append(params.get(orderNumber)).append("&");
		sb.append(params.get(notifyUrl)).append("&");
		sb.append(params.get(returnUrl)).append("&");
		sb.append(this.channelWrapper.getAPI_KEY());

		String signStr = sb.toString();
		String pay_md5sign = HandlerUtil.getMD5UpperCase(signStr);
		log.debug("[一品付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
		return pay_md5sign;
	}

	protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
		payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		Map result = Maps.newHashMap();
		String resultStr;
		try {
			if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWebWyKjzf(channelWrapper)) {
				String htmlContent = HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam).toString();
				result.put(HTMLCONTEXT, htmlContent);
			}else if(HandlerUtil.isYLKJ(channelWrapper)){    
			    resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
                JSONObject jsonResult = JSON.parseObject(resultStr);
                if (null != jsonResult && jsonResult.containsKey("pay_Status") && "100".equalsIgnoreCase(jsonResult.getString("pay_Status"))) {
                    String qrinfo = jsonResult.getString("pay_Code");
                    result.put(JUMPURL, qrinfo);
                }else {
                    log.error("[一品付]-[请求支付]3.1.发送支付请求，及获取支付请求结果出错：{}", resultStr);
                    throw new PayException(resultStr);
                }
			} else {
			    
				resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(),
						payParam, String.class, HttpMethod.POST).trim();
				JSONObject jsonResult = JSON.parseObject(resultStr);

				if (null != jsonResult && jsonResult.containsKey("success") && "true".equalsIgnoreCase(jsonResult.getString("success"))) {
					if (StringUtils.isNotBlank(jsonResult.getString("data"))) {
						String qrinfo = jsonResult.getString("data");
						result.put(QRCONTEXT, qrinfo);
					}
				} else {
				    log.error("[一品付]-[请求支付]3.1.发送支付请求，及获取支付请求结果出错：{}", resultStr);
					throw new PayException(resultStr);
				}
			}
			
			payResultList.add(result);
			
		} catch (Exception e) {
			log.error("[一品付]3.发送支付请求，及获取支付请求结果出错：", e);
			throw new PayException(e.getMessage(), e);
		}
		log.debug("[一品付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));
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
		log.debug("[一品付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
		return requestPayResult;
	}
	
}