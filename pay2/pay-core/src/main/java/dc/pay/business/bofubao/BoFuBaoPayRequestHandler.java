package dc.pay.business.bofubao;

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

@RequestPayHandler("BOFUBAO")
public final class BoFuBaoPayRequestHandler extends PayRequestHandler {
	private static final Logger log = LoggerFactory.getLogger(BoFuBaoPayRequestHandler.class);

	private static final String service = "service";               //服务类型
	private static final String version = "version";               //版本号
	private static final String merchantId = "mch_id";             //商户号
	private static final String orderNumber = "out_order_no";       //订单号
	private static final String subject = "subject";               //订单描述
	private static final String money = "total_fee";               //支付金额
	private static final String payType = "pay_type";              //支付类型
	private static final String notifyUrl = "notify_url";           //异步通知url
	private static final String clientip = "clientip";             //客户ip
	private static final String returnUrl = "return_url";           //同步通知url
	private static final String nonce_str = "nonce_str";           //8位随机数


	@Override
	protected Map<String, String> buildPayParam() throws PayException {
		Map<String, String> payParam = Maps.newHashMap();
		payParam.put(service, "pay");
		payParam.put(version, "1.0");
		payParam.put(merchantId, channelWrapper.getAPI_MEMBERID());
		payParam.put(orderNumber, channelWrapper.getAPI_ORDER_ID());
		payParam.put(subject, "goods");
		payParam.put(money, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
		if(HandlerUtil.isWY(channelWrapper)){
		    payParam.put(payType, this.getPayType());
		    payParam.put("open_bank_code", this.getOpenBankCode());
		}else{
		    payParam.put(payType, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());    
		}
		
		payParam.put(notifyUrl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
		payParam.put(clientip, channelWrapper.getAPI_Client_IP());
		payParam.put(returnUrl, channelWrapper.getAPI_WEB_URL());
		payParam.put(nonce_str, HandlerUtil.getRandomStr(8));
		
		log.debug("[博付宝]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
		return payParam;
	}

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
		sb.append("key=").append(this.channelWrapper.getAPI_KEY());

		String signStr = sb.toString();
		String pay_md5sign = HandlerUtil.getMD5UpperCase(signStr);
		log.debug("[博付宝]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
		return pay_md5sign;
	}

	protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
		payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		Map result = Maps.newHashMap();
		String resultStr;
		try {
			if (HandlerUtil.isWY(channelWrapper) ||HandlerUtil.isYLWAP(channelWrapper)||HandlerUtil.isWapOrApp(channelWrapper)) {
				String htmlContent = HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam).toString();
				result.put(HTMLCONTEXT, htmlContent);
			}else {
				resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
				JSONObject jsonResult = JSON.parseObject(resultStr);
				if (null != jsonResult && jsonResult.containsKey("code_status") && "1".equalsIgnoreCase(jsonResult.getString("code_status"))) {
					if (StringUtils.isNotBlank(jsonResult.getString("code_url"))) {
						String qrinfo = jsonResult.getString("code_url");
						result.put(QRCONTEXT, qrinfo);
					}else{
					    log.error("[博付宝]-[请求支付]3.1.发送支付请求，未获取到二维码数据：{}",resultStr);
					    throw new PayException(resultStr);
					}
				} else {
				    log.error("[博付宝]-[请求支付]3.2.发送支付请求，及获取支付请求结果出错：{}", resultStr);
					throw new PayException(resultStr);
				}
			}
			payResultList.add(result);
			
		} catch (Exception e) {
			log.error("[博付宝]3.3.发送支付请求，及获取支付请求结果出错：", e);
			throw new PayException(e.getMessage(), e);
		}
		log.debug("[博付宝]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));
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
		log.debug("[博付宝]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
		return requestPayResult;
	}
	
	/**
	 * 获取支付类型
	 * */
	private String getPayType() throws PayException{
	    return this.splitWYBankCode()[0];
	}
	
	/**
	 * 获取银行代码
	 * */
	private String getOpenBankCode()throws PayException{
	    return this.splitWYBankCode()[1];
	} 
	
	/**
	 * 分割网银银行代码
	 * */
	private String[] splitWYBankCode() throws PayException{
	    String[] items = this.channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",");
	    if(items.length<2) {
	        String errorMsg = "[博付宝]-[请求支付]-4.支付编码配置错误，配置格式：pay_type,open_bank_code";
            log.error(errorMsg);
            throw new PayException(errorMsg);
	    }
	    return items;
	}
	
}