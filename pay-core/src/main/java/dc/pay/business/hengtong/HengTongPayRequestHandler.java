package dc.pay.business.hengtong;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * ************************
 * @author beck 2229556569
 */

@RequestPayHandler("HENGTONG")
public final class HengTongPayRequestHandler extends PayRequestHandler {
	private static final Logger log = LoggerFactory.getLogger(HengTongPayRequestHandler.class);

	private static final String customer = "customer";
	private static final String amount = "amount";
	private static final String banktype = "banktype";
	private static final String orderid = "orderid";
	private static final String asynbackurl = "asynbackurl";
	private static final String request_time = "request_time";
	private static final String isqrcode = "isqrcode";
	private static final String israndom = "israndom";
	private static final String sign = "sign"; // 是 string 签名

	@Override
	protected Map<String, String> buildPayParam() throws PayException {
		Map<String, String> payParam = new TreeMap<String, String>();
		payParam.put(customer, channelWrapper.getAPI_MEMBERID());
		payParam.put(amount, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
		if (!HandlerUtil.isWebYlKjzf(channelWrapper)) {
			payParam.put(banktype, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
		}

		payParam.put(orderid, channelWrapper.getAPI_ORDER_ID());
		payParam.put(asynbackurl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
		payParam.put(request_time, DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
		payParam.put(isqrcode, "Y");

		log.debug("[恒通]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
		return payParam;
	}

	protected String buildPaySign(Map<String, String> params) throws PayException {

		StringBuilder sb = new StringBuilder();

		sb.append(customer + "=" + params.get(customer) + "&");
		if (!HandlerUtil.isWebYlKjzf(channelWrapper)) {
			sb.append(banktype + "=" + params.get(banktype) + "&");
		}
		sb.append(amount + "=" + params.get(amount) + "&");
		sb.append(orderid + "=" + params.get(orderid) + "&");
		sb.append(asynbackurl + "=" + params.get(asynbackurl) + "&");
		sb.append(request_time + "=" + params.get(request_time) + "&");
		sb.append("key=").append(this.channelWrapper.getAPI_KEY());

		String signStr = sb.toString();

		String pay_md5sign = HandlerUtil.getMD5UpperCase(signStr).toLowerCase();
		log.debug("[恒通]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
		return pay_md5sign;
	}

	protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign)
			throws PayException {
		payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
		
		Map result = Maps.newHashMap();
		String resultStr;
		try {
			if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)|| handlerUtil.isWapOrApp(channelWrapper)) {

				String htmlContent = HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam).toString();
				result.put(HTMLCONTEXT, htmlContent);

			} else {

				resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(),
						payParam, String.class, HttpMethod.POST).trim();
				JSONObject jsonResult = JSON.parseObject(resultStr);

				if (null != jsonResult && jsonResult.containsKey("code")
						&& "0".equalsIgnoreCase(jsonResult.getString("code"))) {
					if (StringUtils.isNotBlank(jsonResult.getString("qrinfo"))) {
						String qrinfo = jsonResult.getString("qrinfo");
						result.put(QRCONTEXT, qrinfo);
					}
				} else {
				    log.error("[恒通]-[请求支付]3.1.发送支付请求，及获取支付请求结果出错：{}", resultStr);
					throw new PayException(resultStr);
				}
			}
		} catch (Exception e) {
			log.error("[恒通]-[请求支付]3.2.发送支付请求，及获取支付请求结果出错：", e);
			throw new PayException(e.getMessage(), e);
		}
		
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		payResultList.add(result);
		log.debug("[恒通]-[请求支付]-3.3.发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));
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
		log.debug("[恒通]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
		return requestPayResult;
	}
}