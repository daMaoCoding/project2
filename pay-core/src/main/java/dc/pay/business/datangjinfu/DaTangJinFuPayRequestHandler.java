package dc.pay.business.datangjinfu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
 * @author beck Aug 02, 2018
 */
@RequestPayHandler("DATANGJINFU")
public final class DaTangJinFuPayRequestHandler extends PayRequestHandler {
	private static final Logger log = LoggerFactory.getLogger(DaTangJinFuPayRequestHandler.class);

	private static final String money = "money";               //支付金额
	private static final String client_ip = "ip";              //客户端IP
	private static final String merchantId = "group_id";         //商户号
	private static final String notify_url = "notify_url";
	private static final String orderNumber = "user_order_sn";      //订单号
	//private static final String payType = "";              //支付类型
	private static final String subject = "subject";
	

	@Override
	protected Map<String, String> buildPayParam() throws PayException {
		Map<String, String> payParam = new TreeMap<String, String>() {
			{
			    put(money, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
			    put(client_ip, channelWrapper.getAPI_Client_IP());
			    //put(client_ip, "110.87.70.9");
			    put(merchantId, channelWrapper.getAPI_MEMBERID());
			    put(notify_url, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
			    put(orderNumber, channelWrapper.getAPI_ORDER_ID());
				put(subject, "goods");
			}
		};
		log.debug("[大唐金服]-[请求支付]-1.组装请求参数完成：{}", JSON.toJSONString(payParam));
		return payParam;
	}

	protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
		String signMd5 = "暂无签名";
		log.debug("[大唐金服]-[请求支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5));
		return signMd5;
	}

	protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
		//payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		HashMap<String, String> result = Maps.newHashMap();

		try {

			if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
			    String html = HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam).toString();
				result.put(HTMLCONTEXT, html);
			
			} else {
				String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(),
						payParam, String.class, HttpMethod.POST).trim();
				JSONObject resJson = JSONObject.parseObject(resultStr);

				if (resJson != null && resJson.containsKey("statusCode")
						&& resJson.getString("statusCode").equalsIgnoreCase("200")) {
					String data = resJson.getString("data");
					JSONObject resJson2 = JSONObject.parseObject(data);
					String qrcode = resJson2.getString("qrcode");
					String sign = resJson2.getString("sign");
										
					//对返回的数据进行验签
					boolean vlidateDataResult = this.validPayData(qrcode, channelWrapper.getAPI_KEY(), sign);    
					if(!vlidateDataResult){
					    
					    log.error("[大唐金服]-[请求支付]3.1.支付数据返回验签结果出错：{}", resultStr);
	                    throw new PayException("支付数据返回验签结果出错："+resultStr);
					}
					
					result.put(handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, qrcode);
				} else {
				    log.error("[大唐金服]-[请求支付]3.1.发送支付请求，及获取支付请求结果出错：{}", resultStr);
					throw new PayException(resultStr);
				}
			}
			payResultList.add(result);

		} catch (Exception e) {
			log.error("[大唐金服]-[请求支付]3.2.发送支付请求，及获取支付请求结果出错：", e);
			throw new PayException(e.getMessage(), e);
		}
		log.debug("[大唐金服]-[请求支付]-3.3.发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));

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
		log.debug("[大唐金服]-[请求支付]-4.处理请求响应成功：{}", JSON.toJSONString(requestPayResult));
		return requestPayResult;
	}
	
	/**
	 * 验证第三方返回的数据
	 * */
	private boolean validPayData(String content,String md5Key,String sign) throws PayException{
	    String newSign = HandlerUtil.getMD5UpperCase(content+md5Key);
	    
	    boolean result = newSign.equalsIgnoreCase(sign);
	    
	    log.debug("[大唐金服]-[请求支付]-5.验证支付返回数据："+String.valueOf(result));
	    return result;
	    
	    
	}
}