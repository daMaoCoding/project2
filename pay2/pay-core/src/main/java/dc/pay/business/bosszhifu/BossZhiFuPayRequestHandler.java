package dc.pay.business.bosszhifu;

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
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 
 * @author beck Aug 13, 2018
 */
@RequestPayHandler("BOSSZHIFU")
public final class BossZhiFuPayRequestHandler extends PayRequestHandler {
	private static final Logger log = LoggerFactory.getLogger(BossZhiFuPayRequestHandler.class);

	@Override
	protected Map<String, String> buildPayParam() throws PayException {
		Map<String, String> payParam = Maps.newHashMap();
		payParam.put("mchNo", channelWrapper.getAPI_MEMBERID());
		payParam.put("paytype", channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
		payParam.put("money", HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
		payParam.put("tradeno", channelWrapper.getAPI_ORDER_ID());
		payParam.put("notify_url", channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
		payParam.put("returnurl", channelWrapper.getAPI_WEB_URL());
		payParam.put("remark", "remark");
		payParam.put("hrefbackurl", channelWrapper.getAPI_CHANNEL_BANK_URL());
		String timeMillis = String.valueOf(System.currentTimeMillis() / 1000);
		payParam.put("time",timeMillis ); 
		
		log.debug("[BOSS支付]-[请求支付]-1.组装请求参数完成：{}", JSON.toJSONString(payParam));
		return payParam;
	}

	protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
	    StringBuilder signSrc = new StringBuilder();
	    signSrc.append("mchNo=").append(api_response_params.get("mchNo")).append("&");
	    signSrc.append("money=").append(api_response_params.get("money")).append("&");
	    signSrc.append("notify_url=").append(api_response_params.get("notify_url")).append("&");
	    signSrc.append("paytype=").append(api_response_params.get("paytype")).append("&");
	    signSrc.append("remark=").append(api_response_params.get("remark")).append("&");
	    signSrc.append("returnurl=").append(api_response_params.get("returnurl")).append("&");
	    signSrc.append("tradeno=").append(api_response_params.get("tradeno")).append("&");
	    signSrc.append("time=").append(api_response_params.get("time")).append("&");
		signSrc.append("key=").append(channelWrapper.getAPI_KEY());
		String paramsStr = signSrc.toString();
		
		String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
		log.debug("[BOSS支付]-[请求支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5));
		return signMd5;
	}

	protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign)
			throws PayException {
		payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		HashMap<String, String> result = Maps.newHashMap();

		try {

		    String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
            if (StringUtils.isBlank(resultStr)) {
                log.error("[BOSS支付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(payParam));
                throw new PayException("返回空,参数："+JSON.toJSONString(payParam));
            }
            JSONObject resJson = JSON.parseObject(resultStr);
            //只取正确的值，其他情况抛出异常
            if(null !=resJson && resJson.containsKey("code") && "200".equalsIgnoreCase(resJson.getString("code")) && StringUtils.isNotBlank(resJson.getString("data"))){
            	JSONObject dataJson = JSON.parseObject(resJson.getString("data"));
            	if(StringUtils.isNotBlank(dataJson.getString("payurl"))) {
            		result.put(JUMPURL, HandlerUtil.UrlDecode(dataJson.getString("payurl")));
            	}else {
            		log.error("[BOSS支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                	throw new PayException(resultStr);
            	}
            }else {
            	log.error("[BOSS支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            	throw new PayException(resultStr);
            }    
			
			payResultList.add(result);

		} catch (Exception e) {
			log.error("[BOSS支付]-[请求支付]3.4.发送支付请求，及获取支付请求结果出错：", e);
			throw new PayException(e.getMessage(), e);
		}
		log.debug("[BOSS支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));

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
		log.debug("[BOSS支付]-[请求支付]-4.处理请求响应成功：{}", JSON.toJSONString(requestPayResult));
		return requestPayResult;
	}
	
}