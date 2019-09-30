package dc.pay.business.biterfu;

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
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * ************************
 * @author beck 2229556569
 */

@RequestPayHandler("BITERFU")
public final class BiterFuPayRequestHandler extends PayRequestHandler {
	private static final Logger log = LoggerFactory.getLogger(BiterFuPayRequestHandler.class);

	
	private static final String merchantId = "partner_id";         //商户号
	private static final String version = "version";               //版本号
	private static final String serviceName = "service_name";      //接口服务名称
	private static final String inputCharset = "input_charset";    //字符集
	private static final String signType = "sign_type";           //签名方式：目前支持 RSA、MD5 两种签名方式
	private static final String sign = "sign";                     //签名数据
	private static final String orderNumber = "out_trade_no";      //订单号
	private static final String money = "order_amount";            //支付金额
	private static final String outTradeTime = "out_trade_time";   //订单时间
	private static final String payType = "pay_type";              //支付类型
	private static final String bankCode = "bank_code";            //银行编码
	private static final String notifyUrl = "notify_url";          //异步通知url
	

	@Override
	protected Map<String, String> buildPayParam() throws PayException {
		Map<String, String> payParam = Maps.newHashMap();
		
		payParam.put(merchantId, channelWrapper.getAPI_MEMBERID());
		payParam.put(version, "V4.0.1");
		payParam.put(serviceName, "PTY_ONLINE_PAY");
		payParam.put(inputCharset, "UTF-8");
		payParam.put(signType, "MD5");
		payParam.put(orderNumber, channelWrapper.getAPI_ORDER_ID());
		payParam.put(money, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
		payParam.put(outTradeTime, DateUtil.getCurDateTime());
		payParam.put(payType, this.getPayType());
		payParam.put(bankCode, this.getBankCode());
		payParam.put(notifyUrl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
		
		log.debug("[比特付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
		return payParam;
	}

	@Override
	protected String buildPaySign(Map<String, String> params) throws PayException {
	    
	    StringBuilder sb = new StringBuilder();
	    List<String> paramKeys = MapUtils.sortMapByKeyAsc(params);
	    
	    for(int i=0;i<paramKeys.size();i++){
	        String keyName = paramKeys.get(i);
	        String value = params.get(keyName);
	        if(!StringUtils.isBlank(value)) {
	            sb.append(keyName).append("=").append(value).append("&");
	        }
	    }
	    
	    sb.append("key").append("=").append(channelWrapper.getAPI_KEY());
		String signStr = sb.toString();
		String pay_md5sign = HandlerUtil.getMD5UpperCase(signStr);
		log.debug("[比特付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
		return pay_md5sign;
	}

	@Override
	protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
		payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		Map result = Maps.newHashMap();
		String resultStr;        
		try {
			if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLWAP(channelWrapper)|| HandlerUtil.isYLKJ(channelWrapper)) {
				String htmlContent = HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam).toString();
				result.put(HTMLCONTEXT, htmlContent);
			}else {
				resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
				JSONObject jsonResult = JSON.parseObject(resultStr);
				if (null != jsonResult && jsonResult.containsKey("respCode") && jsonResult.getString("respCode").equalsIgnoreCase("RESPONSE_SUCCESS")) {
				    String respResult = jsonResult.getString("respResult");
				    jsonResult = JSON.parseObject(respResult);
				    String qrcode_url = jsonResult.getString("qrcode_url");
				    result.put(QRCONTEXT, qrcode_url);
				}else{
				    log.error("[比特付]-[请求支付]3.1.发送支付请求，及获取支付请求结果出错：{}", resultStr);
                    throw new PayException(resultStr);
				}
			}
			payResultList.add(result);
			
		} catch (Exception e) {
			log.error("[比特付]-[请求支付]3.3.发送支付请求，及获取支付请求结果出错：", e);
			throw new PayException(e.getMessage(), e);
		}
		
		log.debug("[比特付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));
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
		log.debug("[比特付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
		return requestPayResult;
	}
	
	/**
	 * 获取支付类型
	 * */
	private String getPayType() throws PayException{
	    return this.splitBankName()[0];
	}
	
	/**
	 * 获取银行代码
	 * */
	private String getBankCode() throws PayException{
	    return this.splitBankName()[1];
	    
	}
	
	/*
	 * 拆分银支付类型编码
	 * */
	private String[] splitBankName() throws PayException{
	    String[] payTypes = channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",");
	    if(payTypes.length<2){
	        String errorMsg = "[比特付]-[请求支付]-4.支付编码配置错误，配置格式：pay_type,bank_code";
	        log.error(errorMsg);
	        throw new PayException(errorMsg);
	    }
	    
	    return payTypes;
	}
	
}