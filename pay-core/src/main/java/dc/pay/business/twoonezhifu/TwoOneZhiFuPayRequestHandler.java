package dc.pay.business.twoonezhifu;

import java.util.*;

import com.alibaba.fastjson.JSONObject;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayRequestHandler;
import dc.pay.utils.*;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import dc.pay.business.RequestPayResult;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import org.springframework.http.HttpMethod;

/**
 * @author cobby
 * Jan 28, 2019
 */
@RequestPayHandler("TWOONEZHIFU")
public final class TwoOneZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(TwoOneZhiFuPayRequestHandler.class);

	private static final String uid             ="uid";         //商户uid	int(11)	必填。您的商户唯一标识，注册后在我的账户里获得。
	private static final String qr_amount       ="qr_amount";   //价格	decimal(10,2)	必填。单位：元，支付渠道为银联时，需精确到两位小数(1.00)
	private static final String notify_url      ="notify_url";  //通知回调网址	string(255)	必填。用户支付成功后，我们服务器会主动发送一个post消息到这个网址。由您自定义。不要urlencode。例：http://2wxr.cn/notify
	private static final String return_url      ="return_url";  //跳转网址	string(255)	必填。用户支付成功后，我们会让用户浏览器自动跳转到这个网址。由您自定义。不要urlencode。例：http://2wxr.cn/return
	private static final String order_number    ="order_number";//商户自定义订单号	string(128)	必填。我们会据此判别是同一笔订单还是新订单。我们回调时，会带上这个参数。例：201701160027368885458
	private static final String order_uid       ="order_uid";   //商户自定义客户号	string(128)	必填。我们会显示在您后台的订单列表中，方便您看到是哪个用户的付款，方便后台对账。可以填用户名，也可以填您数据库中的用户uid。例：xxx, xxx@aaa.com
	private static final String type            ="type";        //支付渠道	int(11)	必填。1：支付宝； 2：微信； 3:银联
	private static final String key             ="key";         //秘钥	string(32)	必填。把使用到的所有参数，连密钥一起，按参数名字母升序排序。把参数值拼接在一起。做md5-32位加密，取字符串大写。得到key。网址类型的参数值不要urlencode。

	@Override
	protected Map<String, String> buildPayParam() throws PayException {

		Map<String, String> payParam = Maps.newHashMap();
		payParam.put(uid,channelWrapper.getAPI_MEMBERID());
		payParam.put(qr_amount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
		payParam.put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
		payParam.put(return_url,channelWrapper.getAPI_WEB_URL());
		payParam.put(order_number,channelWrapper.getAPI_ORDER_ID());
		payParam.put(order_uid,channelWrapper.getAPI_ORDER_ID());
		payParam.put(type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
		log.debug("[二一支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
		return payParam;
	}

	protected String buildPaySign(Map<String,String> api_response_params) throws PayException {

		String pay_md5signA = null;
		List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < paramKeys.size(); i++) {
			if(StringUtils.isBlank(api_response_params.get(paramKeys.get(i))) )  //
				continue;
			sb.append(api_response_params.get(paramKeys.get(i)));
		}
		sb.append(channelWrapper.getAPI_KEY());
		String signStr = sb.toString(); //.replaceFirst("&key=","")
		pay_md5signA = HandlerUtil.getMD5UpperCase(signStr);
		log.debug("[二一支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5signA));
		return pay_md5signA;

	}

	protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String md5sign) throws PayException {
		payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),md5sign);
		HashMap<String, String> result = Maps.newHashMap();
//	    String url = channelWrapper.getAPI_CHANNEL_BANK_URL();
		try {
			String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
			if (StringUtils.isBlank(resultStr)) {
				log.error("[二一支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
				throw new PayException(resultStr);
			}
			System.out.println("请求返回=========>"+resultStr);
			if (!resultStr.contains("{") || !resultStr.contains("}")) {
				log.error("[二一支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
				throw new PayException(resultStr);
			}
			JSONObject resJson;
			try {
				resJson = JSONObject.parseObject(resultStr);
			} catch (Exception e) {
				e.printStackTrace();
				log.error("[二一支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
				throw new PayException(resultStr);
			}
			//只取正确的值，其他情况抛出异常
//			"ret": 1,					"data": {						"qr_code": "HTTPS://QR.ALIPAY.COM/FKX00849HD6HRIS0QETICA",
			if (null != resJson && resJson.containsKey("ret") && "1".equalsIgnoreCase(resJson.getString("ret"))  && resJson.containsKey("data") && StringUtils.isNotBlank(resJson.getString("data"))) {
				String data = resJson.getString("data");
				resJson = JSONObject.parseObject(data);
				result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, resJson.getString( "qr_code"));
			}else {
				log.error("[二一支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
				throw new PayException(resultStr);
			}

		} catch (Exception e) {
			log.error("[二一支付]-[请求支付]-3.5.发送支付请求，及获取支付请求结果出错：", e);
			throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
		}
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		payResultList.add(result);
		log.debug("[二一支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
		log.debug("[二一支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
		return requestPayResult;
	}
}