package dc.pay.business.sdp;

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
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * ************************
 * @author beck 2229556569
 */

@RequestPayHandler("SDP")
public final class SdpPayRequestHandler extends PayRequestHandler {
	private static final Logger log = LoggerFactory.getLogger(SdpPayRequestHandler.class);

	
	private static final String merchantId = "pid";          //商户号
//	private static final String money = "money";                   //支付金额
	private static final String payType = "cmd";                   //支付类型
	private static final String des = "des";                   //支付类型
	
//	private static final String orderNumber = "orderNumber";       //订单号
//	private static final String notifyUrl = "notifyUrl";           //异步通知url
//	private static final String returnUrl = "returnUrl";           //同步通知url
//  private static final String signature = "signature";           //签名字段

	@Override
	protected Map<String, String> buildPayParam() throws PayException {
		Map<String, String> payParam = Maps.newHashMap();
		payParam.put(merchantId,channelWrapper.getAPI_MEMBERID());
		payParam.put(payType, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
				
		String key1 = this.getDesKey();
        String key2 = this.getDesVector();
        String md5key = this.getMD5Key();
        String xml = this.createXMLData();
        
        String md5 = MD5Encode.encode(xml+md5key);
        String xml2 = xml + md5.toLowerCase();
        
        String encryptData = "";
        
        try {
            encryptData = MyEncrypt.EncryptData(xml2, key1, key2);
        } catch (Exception e) {
           log.error("[Sdp]-[请求支付]1.加密XML失败：", e);
           throw new PayException(e.getMessage());
        }
		        
		payParam.put(des, encryptData);
		
		log.debug("[SdpPay]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
		return payParam;
	}

	protected String buildPaySign(Map<String, String> params) throws PayException {
	    return "";
	}

	protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
		//payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		Map result = Maps.newHashMap();
		String resultStr;
		try {
			if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
				String htmlContent = HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam).toString();
				result.put(HTMLCONTEXT, htmlContent);
			} else {
			    
				resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(),
						payParam, String.class, HttpMethod.POST).trim();
				JSONObject jsonResult = JSON.parseObject(resultStr);

				if (null != jsonResult && jsonResult.containsKey("success") && "true".equalsIgnoreCase(jsonResult.getString("success"))) {
					if (StringUtils.isNotBlank(jsonResult.getString("data"))) {
						String qrinfo = jsonResult.getString("data");
						result.put(QRCONTEXT, qrinfo);
					}
				} else {
				    log.error("[Sdp]-[请求支付]3.1.发送支付请求，及获取支付请求结果出错：{}", resultStr);
					throw new PayException(resultStr);
				}
			}
			
			payResultList.add(result);
			
		} catch (Exception e) {
			log.error("[SdpPay]-[请求支付]3.2.发送支付请求，及获取支付请求结果出错：", e);
			throw new PayException(e.getMessage(), e);
		}
		log.debug("[SdpPay]-[请求支付]-3.3.发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));
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
		log.debug("[SdpPay]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
		return requestPayResult;
	}
	
	/**
	 * 创建xml
	 * */
	private String createXMLData() throws PayException{
	    String xmlTemp = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
	    xmlTemp +=  "<message>";
    	    xmlTemp +=  "<cmd>6006</cmd>";
    	    xmlTemp +=  "<merchantid>{merchantid}</merchantid>";
    	    xmlTemp +=  "<language>zh-cn</language>";
        	    xmlTemp +=  "<userinfo>";
        	    xmlTemp +=  "<order>{order}</order>";
        	    xmlTemp +=  "<username>"+channelWrapper.getAPI_Client_IP().replaceAll("\\.","")+"O"+HandlerUtil.getRandomStr(3)+"</username>";
        	    xmlTemp +=  "<money>{money}</money>";
        	    xmlTemp +=   "<unit>1</unit>";
        	    xmlTemp +=   "<time>{time}</time>";
        	    xmlTemp +=   "<remark>remark</remark>";
        	    xmlTemp +=   "<backurl>{backurl}</backurl>";
        	    xmlTemp +=   "</userinfo>";
	    xmlTemp +=   "</message>";
	    
	    xmlTemp = xmlTemp.replace("{merchantid}", channelWrapper.getAPI_MEMBERID());
	    xmlTemp = xmlTemp.replace("{order}", channelWrapper.getAPI_ORDER_ID());
	    xmlTemp = xmlTemp.replace("{money}", HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
	    xmlTemp = xmlTemp.replace("{time}", DateUtil.formatDateTimeStrByParam(DateUtil.dateTimeString));
	    xmlTemp = xmlTemp.replace("{backurl}", channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
        
	    return xmlTemp;
	}
	
	/**
     * 获取md5Key
     * */
    private String getMD5Key() throws PayException{
        String items[] = this.splitAPIKey();
        return items[0];
    }
    
    /**
     * 获取des加密key
     * */
    private String getDesKey() throws PayException{
        String items[] = this.splitAPIKey();
        return items[1];
    }
    
    /**
     * 获取des加密向量
     * */
    private String getDesVector() throws PayException{
        String items[] = this.splitAPIKey();
        return items[2];
    }
    
	
	/**
	 * 分割秘钥
	 * */
	private String[] splitAPIKey() throws PayException {
	    String keyStr = this.channelWrapper.getAPI_KEY();
	    String[] keys = keyStr.split("&");
	    if(keys.length < 3){
	        String errorMsg = "[sdp]-[请求支付]-5. 秘钥填写错误，填写格式：【中间使用&分隔】：MD5Key&Key1&Key2";
            log.error(errorMsg);
            throw new PayException(errorMsg);
	    }
	    return keys;
	}
	
}
