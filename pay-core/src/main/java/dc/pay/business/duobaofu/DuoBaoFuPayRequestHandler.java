package dc.pay.business.duobaofu;

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
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * ************************
 * @author beck 2229556569
 */

@RequestPayHandler("DUOBAOFU")
public final class DuoBaoFuPayRequestHandler extends PayRequestHandler {
	private static final Logger log = LoggerFactory.getLogger(DuoBaoFuPayRequestHandler.class);
	
	private static final String merchantId = "app_id";              //商户号
	private static final String payType = "channel";                //支付类型
	private static final String orderNumber = "order_sn";           //订单号
	private static final String money = "amount";                   //支付金额
	private static final String notifyUrl = "notify_url";           //异步通知url
	private static final String returnUrl = "return_url";           //同步通知url
	private static final String nonce_str = "nonce_str";            //8位随机数
	
	@Override
	protected Map<String, String> buildPayParam() throws PayException {
		Map<String, String> payParam = Maps.newHashMap();
		
		payParam.put(merchantId, channelWrapper.getAPI_MEMBERID());
		payParam.put(payType, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
		payParam.put(orderNumber, channelWrapper.getAPI_ORDER_ID());
		payParam.put(money, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
		payParam.put(notifyUrl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
		payParam.put(returnUrl, channelWrapper.getAPI_WEB_URL());
		payParam.put(nonce_str, HandlerUtil.getRandomStr(8));
		
		log.debug("[多宝付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
		return payParam;
	}

	@Override
	protected String buildPaySign(Map<String, String> params) throws PayException {

		StringBuilder sb = new StringBuilder();
		List<String> paramKeys = MapUtils.sortMapByKeyAsc(params);
		
		for(int i = 0; i<paramKeys.size();i++){
		    Object keyName = paramKeys.get(i);
		    String value = params.get(keyName);
		    if(!StringUtils.isBlank(value)){
		        sb.append(keyName).append("=").append(value).append("&");
		    }
		}
		sb.append("app_key=").append(this.channelWrapper.getAPI_KEY());

		String signStr = sb.toString();
		String pay_md5sign = HandlerUtil.getMD5UpperCase(signStr).toLowerCase();
		log.debug("[多宝付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
		return pay_md5sign;
	}

	protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
		payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		Map<String,String> result = Maps.newHashMap();
		String resultStr;
		try {
			if (HandlerUtil.isWY(channelWrapper) ||HandlerUtil.isYLWAP(channelWrapper)||HandlerUtil.isWapOrApp(channelWrapper)) {
				String htmlContent = HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam).toString();
				result.put(HTMLCONTEXT, htmlContent);
			}else {
				resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
				JSONObject jsonResult = JSON.parseObject(resultStr);
				if (null != jsonResult && jsonResult.containsKey("code") && "200".equalsIgnoreCase(jsonResult.getString("code"))) {
				    String dataStr = jsonResult.getString("data");
					if (StringUtils.isNotBlank(dataStr)) {
					    JSONObject dataJson = JSON.parseObject(dataStr);
					    String qrinfo = dataJson.getString("data");
						result.put(QRCONTEXT, qrinfo);
					}
				} else {
				    log.error("[多宝付]-[请求支付]3.1.发送支付请求，及获取支付请求结果出错：{}", resultStr);
					throw new PayException(resultStr);
				}
			}
			payResultList.add(result);
			
		} catch (Exception e) {
			log.error("[多宝付]-[请求支付]3.2.发送支付请求，及获取支付请求结果出错：", e);
			throw new PayException(e.getMessage(), e);
		}
		log.debug("[多宝付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));
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
		log.debug("[多宝付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
		return requestPayResult;
	}
	
}