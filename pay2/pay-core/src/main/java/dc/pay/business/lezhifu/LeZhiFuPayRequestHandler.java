package dc.pay.business.lezhifu;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

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
import dc.pay.utils.qr.QRCodeUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 *
 * @author andrew
 * Jan 22, 2018
 */
@RequestPayHandler("LEZHIFU")
public final class LeZhiFuPayRequestHandler extends PayRequestHandler {
	private static final Logger log = LoggerFactory.getLogger(LeZhiFuPayRequestHandler.class);

	private static final String QRCONTEXT = "QrContext";
	private static final String QRURL = "QrUrl";
	private static final String HTMLCONTEXT = "HtmlContext";
	private static final String PARSEHTML = "parseHtml";

	//系统给定的商户号
	private static final String account  ="account";
	private static final String order  ="order";
	private static final String paytype  ="paytype";
	private static final String type  ="type";
	//0.01单位：元，个别测试最低金额为1元
	private static final String money  ="money";
	//    'body' =>"vip",
	private static final String body  ="body";
	//    'ext' =>"123",
	private static final String ext  ="ext";
	//请给出有效的地址 地址请使用url编码,地址后可以携带自定义get(?a=1&b=2)参数。地址请使用url编码
	private static final String notify  ="notify";
	//请给出有效的地址,地址后可以携带自定义get(?a=1&b=2)参数。地址请使用url编码.
	private static final String callback  ="callback";
	//必填参数，按A-Z顺序排序
	private static final String sign  ="sign";

	/**
	 * 封装第三方所需要的参数
	 *
	 * @return
	 * @throws PayException
	 * @author andrew
	 * Jan 23, 2018
	 */
	@Override
	protected Map<String, String> buildPayParam() throws PayException {
		String api_CHANNEL_BANK_NAME_FlAG = channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG();
		Map<String, String> payParam = new TreeMap<String, String>() {
			{
				put(account, channelWrapper.getAPI_MEMBERID());
				put(order,channelWrapper.getAPI_ORDER_ID() );
				put(money,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
				try {
					put(notify,HandlerUtil.UrlEncode(channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL()));
					put(callback,HandlerUtil.UrlEncode(channelWrapper.getAPI_WEB_URL()));
				} catch (Exception e) {
				}
				if (HandlerUtil.isWY(channelWrapper) && !channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("KJZF")) {
					put(paytype,"wy");
					if(HandlerUtil.isOrderFromWapOrApp(channelWrapper.getAPI_ORDER_FROM())){
						put(type,"1005");
					}else{
						put(type,api_CHANNEL_BANK_NAME_FlAG);
					}
				}else {
					put(paytype,api_CHANNEL_BANK_NAME_FlAG);
				}
			}
		};
		log.debug("[乐支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
		return payParam;
	}

	/**
	 * 生成签名
	 *
	 * @param api_response_params
	 * @return
	 * @throws PayException
	 * @author andrew
	 * Jan 23, 2018
	 */
	protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
		List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
		StringBuilder signSrc = new StringBuilder();
		for (int i = 0; i < paramKeys.size(); i++) {
			if (null != api_response_params.get(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i))) && !type.equals(paramKeys.get(i))) {
				signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
			}
		}
		signSrc.append(channelWrapper.getAPI_KEY());
		String paramsStr = signSrc.toString();
		String pay_md5sign = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
		log.debug("[乐支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
		return pay_md5sign;
	}

	/**
	 * 生成返回给RequestPayResult对象detail字段的值
	 *
	 * @param payParam
	 * @param pay_md5sign
	 * @return
	 * @throws PayException
	 * @author andrew
	 * Jan 23, 2018
	 */
	protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
		payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
		List<Map<String, String>> payResultList = Lists.newArrayList();
		String api_CHANNEL_BANK_URL = channelWrapper.getAPI_CHANNEL_BANK_URL();
		String api_CHANNEL_BANK_NAME = channelWrapper.getAPI_CHANNEL_BANK_NAME();
		Map<String,String> result = Maps.newHashMap();
		try {
			String tmpStr = RestTemplateUtil.postForm(api_CHANNEL_BANK_URL, payParam,"UTF-8");
			if (null == tmpStr || StringUtils.isBlank(tmpStr)) {
				log.error("[乐支付]3.1.发送支付请求，获取支付请求返回值异常:返回空");
				throw new PayException("第三方返回异常:返回空");
			}
			JSONObject jsonObject = JSONObject.parseObject(tmpStr);
			if ( !jsonObject.containsKey("code") || !"1".equals(jsonObject.getString("code").toString().trim())) {
				log.error("[乐支付]3.2.发送支付请求，获取支付请求返回值异常:"+tmpStr);
				throw new PayException(tmpStr);
			}
			if ( !jsonObject.containsKey("msg") || !"ok".equals(jsonObject.getString("msg").toString().trim())) {
				log.error("[乐支付]3.3.发送支付请求，获取支付请求返回值异常:"+tmpStr);
				throw new PayException(tmpStr);
			}
			if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper)) {
				result.put(JUMPURL, jsonObject.getString("payurl"));
			}else {
				result.put(QRCONTEXT, (api_CHANNEL_BANK_NAME.contains("WX")) ? QRCodeUtil.decodeByUrl(jsonObject.get("payurl").toString()) : jsonObject.get("payurl").toString());
			}
			result.put("第三方返回", tmpStr);
			payResultList.add(result);
		} catch (Exception e) {
			log.error("[乐支付]3.4.发送支付请求，及获取支付请求出错，订单号：" + channelWrapper.getAPI_ORDER_ID() + ",通道名称：" + api_CHANNEL_BANK_NAME + ",postUrl:" + api_CHANNEL_BANK_URL + ",payForManagerCGIResultJsonObj" + e.getMessage(), e);
			throw new PayException(e.getMessage(),e);
		}

		log.debug("[乐支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
		return payResultList;
	}

	/**
	 * 生成返回给前端使用的值
	 *
	 * @param resultListMap
	 * @return
	 * @throws PayException
	 * @author andrew
	 * Jan 23, 2018
	 */
	protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
		RequestPayResult requestPayResult = new RequestPayResult();
		if (CollectionUtils.isEmpty(resultListMap) || resultListMap.size() != 1) {
			throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
		}
		Map<String, String> qrMap = resultListMap.get(0);
		if (null != qrMap && qrMap.containsKey(QRCONTEXT)) {
			requestPayResult.setRequestPayQRcodeContent(qrMap.get(QRCONTEXT));
		}else if (null != qrMap && qrMap.containsKey(JUMPURL)) {
			requestPayResult.setRequestPayJumpToUrl(qrMap.get(JUMPURL));
		}else if (null != qrMap && qrMap.containsKey(HTMLCONTEXT)) {
			requestPayResult.setRequestPayHtmlContent(qrMap.get(HTMLCONTEXT));
		}
		requestPayResult.setRequestPayamount(channelWrapper.getAPI_AMOUNT());
		requestPayResult.setRequestPayOrderId(channelWrapper.getAPI_ORDER_ID());
		requestPayResult.setRequestPayOrderCreateTime(HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "yyyy-MM-dd HH:mm:ss"));
		requestPayResult.setRequestPayQRcodeURL(null);
		requestPayResult.setRequestPayChannelBankName(channelWrapper.getAPI_CHANNEL_BANK_NAME());
		if (!ValidateUtil.requestesultValdata(requestPayResult)) {
			throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
		}
		requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
		log.debug("[乐支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
		return requestPayResult;
	}
}