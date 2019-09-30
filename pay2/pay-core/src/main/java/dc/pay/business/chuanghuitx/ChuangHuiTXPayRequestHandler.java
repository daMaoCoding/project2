package dc.pay.business.chuanghuitx;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import com.alibaba.fastjson.JSON;
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
import dc.pay.utils.RsaUtil;
import dc.pay.utils.ValidateUtil;

/**
 * ************************
 * @author beck 2229556569
 */

@RequestPayHandler("CHUANGHUITX")
public final class ChuangHuiTXPayRequestHandler extends PayRequestHandler {
	private static final Logger log = LoggerFactory.getLogger(ChuangHuiTXPayRequestHandler.class);

	private static final String merchantId = "merchant_code";                          //商户号
	private static final String payType = "service_type";                              //支付类型
	private static final String notifyUrl = "notify_url";                              //异步通知url
	private static final String interfaceVersion = "interface_version";                //版本号
	private static final String clientIp = "client_ip";
	private static final String inputCharset = "input_charset";
//	private static final String bankCode = "bank_code";
	private static final String signType = "sign_type";
	private static final String sign = "sign";
	private static final String orderNumber = "order_no";                              //订单号
	private static final String orderTime = "order_time";                              //订单时间
	private static final String money = "order_amount";                                //支付金额
	private static final String productName = "product_name";                          //接口服务名称
	
	@Override
	protected Map<String, String> buildPayParam() throws PayException {
		Map<String, String> payParam = Maps.newHashMap();
		
		payParam.put(merchantId, channelWrapper.getAPI_MEMBERID());
		
		payParam.put(notifyUrl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
		if(HandlerUtil.isWY(channelWrapper)){
		    payParam.put(interfaceVersion, "V3.0");
		    payParam.put(inputCharset, "UTF-8");
		    payParam.put(payType, "direct_pay");
		    //payParam.put(bankCode, this.channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
		}else{
		    payParam.put(interfaceVersion, "V3.1");
		    payParam.put(payType, this.channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
		}
				
		payParam.put(clientIp, this.channelWrapper.getAPI_Client_IP());
		payParam.put(signType, "RSA-S");
		payParam.put(orderNumber, channelWrapper.getAPI_ORDER_ID());
		payParam.put(orderTime, DateUtil.getCurDateTime());
		payParam.put(money, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
		payParam.put(productName, "goods");
		
		log.debug("[创汇天下]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
		return payParam;
	}

	@Override
	protected String buildPaySign(Map<String, String> params) throws PayException {
	    
	    StringBuilder sb = new StringBuilder();
	    List<String> paramKeys = MapUtils.sortMapByKeyAsc(params);
	    
	    for(int i=0;i<paramKeys.size();i++){
	        String keyName = paramKeys.get(i);
	        if(keyName.equalsIgnoreCase(signType) || keyName.equalsIgnoreCase(sign)) continue;
	        
	        String value = params.get(keyName);
	        if(!StringUtils.isBlank(value)) {
	            sb.append(keyName).append("=").append(value).append("&");
	        }
	    }
	    String signInfo = sb.toString();
	    signInfo = signInfo.substring(0, signInfo.length()-1);
	    String signStr="";
	    try {
	        signStr = RsaUtil.signByPrivateKey(signInfo,channelWrapper.getAPI_KEY());  // 签名
        } catch (Exception e) {
            log.error("[创汇天下]-[请求支付]-2.生成加密URL签名出错，签名出错：{}",e.getMessage(),e);
        }
        log.debug("[创汇天下]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(sign));
        return signStr;
	}

	@Override
	protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
		payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		Map result = Maps.newHashMap();
		String resultStr;        
		try {
			if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLWAP(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)
			        ) {
				String htmlContent = HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam).toString();
				result.put(HTMLCONTEXT, htmlContent);
			}else {
				resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
				
				if(HandlerUtil.isWapOrApp(channelWrapper)){         //如果为H5
				    String qrcode = this.resolveH5(resultStr);
				    result.put(JUMPURL, qrcode);
				}else{
				    String qrcode = this.resolveScan(resultStr);
				    result.put(QRCONTEXT, qrcode);
				}
			}
			payResultList.add(result);
			
		} catch (Exception e) {
			log.error("[创汇天下]-[请求支付]3.2.发送支付请求，及获取支付请求结果出错：", e);
			throw new PayException(e.getMessage(), e);
		}
		
		log.debug("[创汇天下]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));
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
		log.debug("[创汇天下]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
		return requestPayResult;
	}
	
	/**
	 * 解析扫码二维码内容
	 * */
	private String resolveScan(String xmlContent) throws PayException{
	    Document document = Jsoup.parse(xmlContent);
        
        String resp_code = document.getElementsByTag("resp_code").first().html();
        String result_code = document.getElementsByTag("result_code").first().html();
        if(resp_code.equalsIgnoreCase("SUCCESS") && result_code.equalsIgnoreCase("0")){
            String qrcode = document.getElementsByTag("qrcode").first().html();
            return qrcode;
            
        }else{
            log.error("[创汇天下]-[请求支付]-5.发送支付请求,二维码获取失败:"+xmlContent);
            throw new PayException(xmlContent);
        }
	}
	
	
	/**
     * 解析H5二维码内容
     * */
    private String resolveH5(String xmlContent) throws PayException{
        Document document = Jsoup.parse(xmlContent);
        
        String resp_code = document.getElementsByTag("resp_code").first().html();
        String result_code = document.getElementsByTag("result_code").first().html();
        if(resp_code.equalsIgnoreCase("SUCCESS") && result_code.equalsIgnoreCase("0")){
            String qrcode = document.getElementsByTag("payURL").first().html();
            qrcode = HandlerUtil.UrlDecode(qrcode);
            return qrcode;
        }else{
            log.error("[创汇天下]-[请求支付]-6.发送支付请求,二维码获取失败:"+xmlContent);
            throw new PayException(xmlContent);
        }
    }
	
}