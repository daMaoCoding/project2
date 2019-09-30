package dc.pay.business.xinkkzhifu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import dc.pay.utils.Sha1Util;
import dc.pay.utils.UnicodeUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 
 * @author sunny Dec 18, 2018
 */
@RequestPayHandler("XINKKZHIFU")
public final class XinKKZhiFuPayRequestHandler extends PayRequestHandler {
	private static final Logger log = LoggerFactory.getLogger(XinKKZhiFuPayRequestHandler.class);

//	参数名称				参数编码			是否必填				字段长度				描述和样例
//	商户编号				merchId			是					定Int(12)			商户编号是高户在支付平台上开设的商户号码为12位数字，如：126804357570
//	支付类型				payType			是					变String(32)			微信公众号参数定为wxwap；微信扫码参数定为wxqrcode；微信H5参数定为wxhtml；支付宝扫码参数定为aliqrcode；支付宝参数定为aliwap；银联参数定为ylpay;
//	交易金额				amount			是					变Int(3)				订单的资金总额，单位为 RMB-分。大于或等于100的数字
//	订单时间				timestamp		是					定Int(10)			格式：Unix时间戳，精确到秒,请用北京时间，时间误差超过1小时会抛弃此订单
//	订单编号				orderNumber		是					定String(32)			确保唯一,长度不超过32，尽量随机生成，例如：MD5(随机字符串+时间戳+随机字符串)
//	前端通知地址			synchUrl		是					变String(255)		支付成功之后调起的前端界面URL，确保外网可以访问,并且视情况进行url encode (咨询运营),不允许带!@#+等，例如qq+v&c?!22
//	异步通知地址			asynchUrl		是					变String(255)		支付成功之后将异步返回给到商户服务端！确保外网可以访问
//	签名参数				sign			是					定String(32)			详见"签名说明"
//	下单ip				addrIp			是					变String(255)		下单用户的ip
//	透传参数				other			否					变String(255)		商户附加信息，只允许数字字母下划线

	private static final String merchId 				= "merchId";
	private static final String payType 				= "payType";
	private static final String amount 					= "amount";
	private static final String timestamp 				= "timestamp";
	private static final String orderNumber 			= "orderNumber";
	private static final String synchUrl 				= "synchUrl";
	private static final String asynchUrl 				= "asynchUrl";
	private static final String addrIp 					= "addrIp";
	private static final String other 					= "other";

	private static final String signType = "signType";
	private static final String sign = "sign";
	private static final String key = "key";

	@Override
	protected Map<String, String> buildPayParam() throws PayException {
		Map<String, String> payParam = new TreeMap<String, String>() {
			{
				put(merchId, channelWrapper.getAPI_MEMBERID());
				put(orderNumber, channelWrapper.getAPI_ORDER_ID());
				put(amount, channelWrapper.getAPI_AMOUNT());
				put(asynchUrl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
				put(payType, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
				put(synchUrl, channelWrapper.getAPI_WEB_URL());
				put(timestamp, System.currentTimeMillis()/1000 + "");
				put(addrIp,channelWrapper.getAPI_Client_IP());
			}
		};
		log.debug("[新KK支付]-[请求支付]-1.组装请求参数完成：{}", JSON.toJSONString(payParam));
		return payParam;
	}

	protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
		String signSrc=String.format("%s%s%s%s%s%s",
				amount+api_response_params.get(amount),
				merchId+api_response_params.get(merchId),
				orderNumber+api_response_params.get(orderNumber),
				payType+api_response_params.get(payType),
				timestamp+api_response_params.get(timestamp),
				 channelWrapper.getAPI_KEY()
		);
		String paramsStr = signSrc.toString();
		String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
		log.debug("[新KK支付]-[请求支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMD5));
		return signMD5;
	}

	protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign)
			throws PayException {
		payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
		HashMap<String, String> result = Maps.newHashMap();
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		result.put(HTMLCONTEXT,HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam).toString().replace("method='post'","method='get'")); // .replace("method='post'","method='get'"));
		payResultList.add(result);
		log.debug("[新KK支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));
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
		log.debug("[新KK支付]-[请求支付]-4.处理请求响应成功：{}", JSON.toJSONString(requestPayResult));
		return requestPayResult;
	}
}