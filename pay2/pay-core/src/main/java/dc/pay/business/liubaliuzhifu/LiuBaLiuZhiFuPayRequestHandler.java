package dc.pay.business.liubaliuzhifu;

import java.util.ArrayList;
import java.util.HashMap;
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
 * 
 * @author beck Aug 22, 2018
 */
@RequestPayHandler("LIUBALIUZHIFU")
public final class LiuBaLiuZhiFuPayRequestHandler extends PayRequestHandler {
	private static final Logger log = LoggerFactory.getLogger(LiuBaLiuZhiFuPayRequestHandler.class);

	@Override
	protected Map<String, String> buildPayParam() throws PayException {
		Map<String, String> payParam = Maps.newHashMap();
		payParam.put("notifyUrl", channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
		payParam.put("outOrderNo", channelWrapper.getAPI_ORDER_ID());
		payParam.put("goodsClauses", "goods");
		payParam.put("tradeAmount", HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
		payParam.put("code", channelWrapper.getAPI_MEMBERID());
		payParam.put("payCode", channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());		
		log.debug("[686支付]-[请求支付]-1.组装请求参数完成：{}", JSON.toJSONString(payParam));
		return payParam;
	}

	@Override
	protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
	    StringBuilder signSrc = new StringBuilder();
	     List<String> paramsKeys = MapUtils.sortMapByKeyAsc(api_response_params);
	     for (String key : paramsKeys) {
	         String paramsVal = api_response_params.get(key);
	         signSrc.append(key).append("=").append(paramsVal).append("&");
        }
		signSrc.append("key=").append(channelWrapper.getAPI_KEY());
		String paramsStr = signSrc.toString();
		String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
		log.debug("[686支付]-[请求支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5));
		return signMd5;
	}

	@Override
	protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign)throws PayException {
		payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		HashMap<String, String> result = Maps.newHashMap();

		try {
			if (1==2 && HandlerUtil.isWY(channelWrapper)&& HandlerUtil.isWapOrApp(channelWrapper) && HandlerUtil.isWebWxGZH(channelWrapper)&& HandlerUtil.isYLKJ(channelWrapper)) {
				result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam).toString());
			} else{
			    String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
			    if (StringUtils.isBlank(resultStr)) {
		            log.error("[686支付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空 ,参数："+JSON.toJSONString(payParam));
		            throw new PayException("返回空数据："+resultStr);
		        }
			    JSONObject jsonData = JSON.parseObject(resultStr);
			    String payState = jsonData.getString("payState");
			    String url = jsonData.getString("url");
			    if((!StringUtils.isBlank(payState) && !payState.equalsIgnoreCase("success")) || StringUtils.isBlank(url)){
			        log.error("[686支付]-[请求支付]-3.2.发送支付请求，获取支付请求返回值异常：{}",resultStr);
                    throw new PayException(resultStr);
			    }

			    if(HandlerUtil.isWapOrApp(channelWrapper)){
					result.put(JUMPURL, url);
				}else{
					result.put(QRCONTEXT, url);
				}


			}
			
			payResultList.add(result);

		} catch (Exception e) {
			log.error("[686支付]-[请求支付]3.3.发送支付请求，及获取支付请求结果出错：", e);
			throw new PayException(e.getMessage(), e);
		}
		log.debug("[686支付]-[请求支付]-3..发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));

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
		log.debug("[686支付]-[请求支付]-4.处理请求响应成功：{}", JSON.toJSONString(requestPayResult));
		return requestPayResult;
	}
	
}