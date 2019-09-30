package dc.pay.business.qianfu;

import java.util.ArrayList;
import java.util.HashMap;
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
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 
 * @author beck Aug 02, 2018
 */
@RequestPayHandler("QIANFU")
public final class QianFuPayRequestHandler extends PayRequestHandler {
	private static final Logger log = LoggerFactory.getLogger(QianFuPayRequestHandler.class);

	private static final String app_id = "app_id";
	private static final String channel = "channel";
	private static final String order_sn = "order_sn";
	private static final String amount = "amount";
	private static final String notify_url = "notify_url";
	private static final String return_url = "return_url";
	private static final String nonce_str = "nonce_str";

	@Override
	protected Map<String, String> buildPayParam() throws PayException {
		Map<String, String> payParam = new TreeMap<String, String>() {
			{
				put(app_id, channelWrapper.getAPI_MEMBERID());
				put(channel, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
				put(order_sn, channelWrapper.getAPI_ORDER_ID());
				put(amount, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
				put(notify_url, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
				put(return_url, channelWrapper.getAPI_WEB_URL());
				put(nonce_str, handlerUtil.getRandomStr(32));
			}
		};
		log.debug("[钱付]-[请求支付]-1.组装请求参数完成：{}", JSON.toJSONString(payParam));
		return payParam;
	}

	protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
		// 1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
		// 2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
		List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
		StringBuilder signSrc = new StringBuilder();
		for (int i = 0; i < paramKeys.size(); i++) {
			if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
				signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i)))
						.append("&");
			}
		}
		signSrc.append("app_key=" + channelWrapper.getAPI_KEY());
		String paramsStr = signSrc.toString();
		String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
		log.debug("[钱付]-[请求支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5));
		return signMd5;
	}

	protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign)
			throws PayException {
		payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		HashMap<String, String> result = Maps.newHashMap();

		try {

			if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
				result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam)
						.toString().replace("method='post'", "method='post'"));
				payResultList.add(result);
			} else {
				String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(),
						payParam, String.class, HttpMethod.POST).trim();
				JSONObject resJson = JSONObject.parseObject(resultStr);

				if (resJson != null && resJson.containsKey("code")
						&& resJson.getString("code").equalsIgnoreCase("200")) {
					String data = resJson.getString("data");
					JSONObject resJson2 = JSONObject.parseObject(data);
					result.put(handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT,
							resJson2.getString("data"));
					payResultList.add(result);
				} else {
				    log.error("[钱付]-[请求支付]3.1.发送支付请求，及获取支付请求结果出错：{}", resultStr);
					throw new PayException(resultStr);
				}
			}

		} catch (Exception e) {
			log.error("[钱付]-[请求支付]3.2.发送支付请求，及获取支付请求结果出错：", e);
			throw new PayException(e.getMessage(), e);
		}
		log.debug("[钱付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));

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
		log.debug("[钱付]-[请求支付]-4.处理请求响应成功：{}", JSON.toJSONString(requestPayResult));
		return requestPayResult;
	}
}