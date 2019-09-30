package dc.pay.business.ouchuang;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import dc.pay.base.processor.ChannelWrapper;
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
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Apr 11, 2018
 */
@RequestPayHandler("OUCHUANG")
public final class OuChuangPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(OuChuangPayRequestHandler.class);

	//merchantNo		String			否					商户号
	//key				String			否					商户密钥
	//nonce				String			否					随机字符（与获取签名时的保持一致）
	//timestamp			Long			否					时间戳（与获取签名时的保持一致）
	//sign				String			否					签名（大写,详情见1.5）
	private static final String merchantNo	  ="merchantNo";
	private static final String key			="key";
	private static final String nonce		="nonce";
	private static final String timestamp	  ="timestamp";
	private static final String sign		="sign";

	//accessToken		String			否						商户accessToken
	//outTradeNo		String			否						商户订单号
	//money				long			否						金额(分)
	//type				String			否		T0/T1			付款类型
	//body				String			否						商品描述
	//detail			String			否						商品详情
	//notifyUrl			String			否						后台通知地址
	//productId			String			否						商品ID
	private static final String accessToken		  ="accessToken";
	private static final String outTradeNo		  ="outTradeNo";
	private static final String money			="money";
	private static final String type			="type";
	private static final String body			="body";
	private static final String detail			="detail";
	private static final String notifyUrl		  ="notifyUrl";
	private static final String productId		  ="productId";
	//merchantIp	String	否		商户Ip（付款客户端的ip）
	private static final String merchantIp		  ="merchantIp";
	
	private static final String FLAG			  ="OUCHUANG:";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
    	String api_CHANNEL_BANK_NAME_URL = channelWrapper.getAPI_CHANNEL_BANK_URL();
    	if (null == api_CHANNEL_BANK_NAME_URL || !api_CHANNEL_BANK_NAME_URL.contains(",") || api_CHANNEL_BANK_NAME_URL.split(",").length != 2) {
    		log.error("[欧创聚合]-[请求支付]-1.1.组装请求url格式：获取accessToken url,支付下单url" );
    		throw new PayException("[欧创聚合]-[请求支付]-1.1.组装请求url格式：获取accessToken url,支付下单url" );
    	}
    	Map<String, String> payParam = new TreeMap<String, String>() {
    		{
    			String token = handlerUtil.getStrFromRedis(FLAG+channelWrapper.getAPI_MEMBERID());
    			if (StringUtils.isBlank(token)) {
    				JSONObject json = getAccessToken(channelWrapper);
    				token = json.get("accessToken").toString();
    				handlerUtil.saveStrInRedis(FLAG+channelWrapper.getAPI_MEMBERID(), token, Long.parseLong(json.get("expireTime").toString()));
    			}
    			put(accessToken, token);
    			put(outTradeNo,channelWrapper.getAPI_ORDER_ID());
    			put(money,  channelWrapper.getAPI_AMOUNT());
    			put(type,channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("_WAP_") ? "T1" : "T0");
    			put(body,"name");
    			put(detail,"test");
    			put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
    			if (channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("WAP_WX")) {
					put(merchantIp, HandlerUtil.getRandomIp(channelWrapper));
				}
    			put(productId,System.currentTimeMillis()+"");
    		}
    	};
    	log.debug("[欧创聚合]-[请求支付]-1.4.组装请求参数完成：" + JSON.toJSONString(payParam));
    	return payParam;
    }
    
    private JSONObject getAccessToken(ChannelWrapper channelWrapper) throws PayException {
    	Map<String, String> payParam = new TreeMap<String, String>() {
    		{
    			put(merchantNo, channelWrapper.getAPI_MEMBERID());
    			put(nonce,HandlerUtil.getRandomStr(10));
    			put(timestamp,  HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "yyyyMMddHHmmss"));
    		}
    	};
    	try {
			payParam.put(sign, buildPaySign(payParam));
			payParam.put(key, channelWrapper.getAPI_KEY());
		} catch (PayException e) {
			e.printStackTrace();
		}
    	log.debug("[欧创聚合]-[请求支付]-1.1.获取accessToken请求参数完成：" + JSON.toJSONString(payParam));
    	String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0], payParam);
		if (StringUtils.isBlank(resultStr)) {
			log.error("[欧创聚合]-[请求支付]-1.1.获取accessToken请求返回值异常:返回空"+",参数："+JSON.toJSONString(payParam));
			throw new PayException("返回空"+",参数："+JSON.toJSONString(payParam));
		}
		JSONObject resJson = JSONObject.parseObject(resultStr);
		if (resJson == null || !resJson.containsKey("success") || !"true".equals(resJson.getString("success"))) {
			log.error("[欧创聚合]-[请求支付]-1.2.获取accessToken请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
			throw new PayException(resultStr);
		}
		resJson = JSONObject.parseObject(resJson.getString("value"));
		if (!resJson.containsKey("accessToken") || StringUtils.isBlank(resJson.getString("accessToken"))) {
			log.error("[欧创聚合]-[请求支付]-1.3.获取accessToken请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
			throw new PayException(resultStr);
		}
		return resJson;
	}

	 protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
        		signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
			}
        }
        signSrc.append(key+"="+channelWrapper.getAPI_KEY());
		String paramsStr = signSrc.toString();
        String pay_md5sign = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[欧创聚合]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }

	protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
		Map<String,Object> map = new TreeMap<>();
		map.put(accessToken, payParam.get(accessToken));
		//请求时，accessToken参数需要清除
		payParam.remove(accessToken);
		map.put("param", payParam);
		log.debug("[欧创聚合]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(map));
		String api_CHANNEL_BANK_URL = channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[1];
		HashMap<String, String> result = Maps.newHashMap();
		String resultStr = RestTemplateUtil.postStr(api_CHANNEL_BANK_URL, JSON.toJSONString(map),MediaType.APPLICATION_JSON_UTF8_VALUE.toString(),"Keep-Alive",MediaType.APPLICATION_JSON_UTF8_VALUE.toString());
		if (StringUtils.isBlank(resultStr)) {
			log.error("[欧创聚合]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空"+",参数："+JSON.toJSONString(map));
			throw new PayException("返回空"+",参数："+JSON.toJSONString(payParam));
		}
		JSONObject resJson = JSONObject.parseObject(resultStr);
		if (resJson == null || !resJson.containsKey("success") || !"true".equals(resJson.getString("success"))) {
			log.error("[欧创聚合]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
			throw new PayException(resultStr);
		}
		result.put(HandlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, resJson.getString("value"));
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		payResultList.add(result);
		log.debug("[欧创聚合]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
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
        log.debug("[欧创聚合]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}