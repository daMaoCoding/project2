package dc.pay.business.dashizhifu;

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
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * @author sunny 05 04, 2019
 */
@RequestPayHandler("DASHIZHIFU")
public final class DaShiZhiFuPayRequestHandler extends PayRequestHandler {
	private static final Logger log = LoggerFactory.getLogger(DaShiZhiFuPayRequestHandler.class);

	// companyId 用户ID 由商务分配
	// userOrderId 用户自定义订单同步时候会返回
	// payType 支付方式
	// item 商品名
	// fee 价格 (单位分)
	// expire 超时时间(可选参数,单位:秒)
	// callbackUrl 前端回调地址(不是所有通道都能用)
	// syncUrl 异步通知地址
	// sign 签名=MD5(companyId_userOrderId_fee_用户密钥)
	// 小写参数之间用下划线连接md5(123_154158356330788039600_1000_8F532G0116509187) =
	// ef8efc3f4e7f40e3551fdad07efce80a
	// ip 终端用户的IP
	// mobile 手机号/或者用户在贵方系统中的唯一会员ID 仅在快捷支付时候需要使用
	// name 持卡人姓名,仅在快捷支付时候使用,可选
	// idCardNo 持卡人身份证号码,仅在快捷支付时候使用,可选

	private static final String companyId = "companyId";
	private static final String userOrderId = "userOrderId";
	private static final String payType = "payType";
	private static final String item = "item";
	private static final String fee = "fee";
	private static final String callbackUrl = "callbackUrl";
	private static final String syncUrl = "syncUrl";
	private static final String ip = "ip";
	private static final String expire = "expire";
	private static final String mobile = "mobile";

	private static final String signType = "signType";
	private static final String sign = "sign";
	private static final String key = "key";

	@Override
	protected Map<String, String> buildPayParam() throws PayException {
		Map<String, String> payParam = new TreeMap<String, String>() {
			{
				put(companyId, channelWrapper.getAPI_MEMBERID());
				put(userOrderId, channelWrapper.getAPI_ORDER_ID());
				put(fee, channelWrapper.getAPI_AMOUNT());
				put(syncUrl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
				put(payType, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
				put(callbackUrl, channelWrapper.getAPI_WEB_URL());
				put(ip, channelWrapper.getAPI_Client_IP());
				put(item, channelWrapper.getAPI_ORDER_ID());
				put(expire, "90");
				put(mobile, "48090");
			}
		};
		log.debug("[大师支付]-[请求支付]-1.组装请求参数完成：{}", JSON.toJSONString(payParam));
		return payParam;
	}

	protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
		String signSrc = String.format("%s_%s_%s_%s", api_response_params.get(companyId),
				api_response_params.get(userOrderId), api_response_params.get(fee), channelWrapper.getAPI_KEY());
		String paramsStr = signSrc.toString();
		String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
		log.debug("[大师支付]-[请求支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMD5));
		return signMD5;
	}

	protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign)
			throws PayException {
		payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
		HashMap<String, String> result = Maps.newHashMap();
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();

      	String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.GET);
	        if (StringUtils.isBlank(resultStr)) {
	            log.error("[大师支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(EMPTYRESPONSE);
	        }
	        if (!resultStr.contains("{") || !resultStr.contains("}")) {
	            log.error("[大师支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        JSONObject resJson;
	        try {
	            resJson = JSONObject.parseObject(resultStr);
	        } catch (Exception e) {
	            e.printStackTrace();
	            log.error("[大师支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        if (null != resJson && resJson.containsKey("result") && resJson.getString("result").equals("0")) {
	            String code_url = resJson.getString("param");
	            result.put(JUMPURL, code_url);
	        }else {
	            log.error("[大师支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        payResultList.add(result);
      
		log.debug("[大师支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));
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
		log.debug("[大师支付]-[请求支付]-4.处理请求响应成功：{}", JSON.toJSONString(requestPayResult));
		return requestPayResult;
	}
}