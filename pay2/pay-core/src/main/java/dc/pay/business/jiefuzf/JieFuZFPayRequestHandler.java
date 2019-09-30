package dc.pay.business.jiefuzf;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
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
import dc.pay.utils.RsaUtil;
import dc.pay.utils.SecurityRSAPay;
import dc.pay.utils.ValidateUtil;

/**
 * ************************
 * @author beck 2229556569
 */

@RequestPayHandler("JIEFUZF")
public final class JieFuZFPayRequestHandler extends PayRequestHandler {
	private static final Logger log = LoggerFactory.getLogger(JieFuZFPayRequestHandler.class);
	private static final String money = "amount";            //支付金额
	private static final String platform = "platform";      
	private static final String note = "note";
	private static final String bankCode = "bank_code";
	private static final String payType = "service_type";              //支付类型
	private static final String merchantId = "merchant_user";         //商户号
	private static final String orderNumber = "merchant_order_no";      //订单号
	private static final String riskLevel = "risk_level";
	private static final String notifyUrl = "callback_url";          //异步通知url
	private static final String sign = "sign";
	

	@Override
	protected Map<String, String> buildPayParam() throws PayException {
		Map<String, String> payParam = Maps.newHashMap();
		
		try{
		    String memberID = this.channelWrapper.getAPI_MEMBERID();
		    payParam.put("merchant_code", memberID);
	        String encData = this.encryptParams();
	        payParam.put("data", encData);    
		}catch(Exception e){
		    log.error("[捷付支付]-[请求支付]-1.1.url编码异常：{}",e.getMessage(),e);
		    throw new PayException(e.getMessage());
		}
		log.debug("[捷付支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
		return payParam;
	}

	@Override
	protected String buildPaySign(Map<String, String> params) throws PayException {
	    String data = params.get("data");
	    String sign = "";
	    try {
	        String privateKey = this.channelWrapper.getAPI_KEY();
	        sign = RsaUtil.signByPrivateKey(data, privateKey,"SHA1WithRSA");
        } catch (Exception e) {
            log.error("[捷付支付]-[请求支付]-2.1-签名出错：{}",e.getMessage(),e);
            throw new PayException(e.getMessage());
        }
	    
        log.debug("[捷付支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(sign));
        return sign;
	}

	@Override
	protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
		payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		Map result = Maps.newHashMap();
		String resultStr;        
		try {
			if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLWAP(channelWrapper)) {
				String htmlContent = HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam).toString();
				result.put(HTMLCONTEXT, htmlContent);
			}else {
			    String param = this.generatePostParamRequest(payParam.get("data"),pay_md5sign);
			    
				resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
				
			    JSONObject jsonData = JSON.parseObject(resultStr);
			    String data = jsonData.getString("data");
			    String status = jsonData.getString("status");
			    
			    if(!status.equalsIgnoreCase("1")){
			        log.error("[捷付支付]-[请求支付]3.1.发送支付请求，获取第三方数据失败。"+resultStr);
                    throw new PayException(resultStr);
			    }
			    
		        byte[] decryptBytes = Base64.decodeBase64(data);
		        byte[] privateKeyBytes = Base64.decodeBase64(channelWrapper.getAPI_KEY());
		        
		        //解密返回数据
			    String decryData = SecurityRSAPay.decryptByPrivateKey2(decryptBytes,privateKeyBytes);
			    
			    if(StringUtils.isBlank(decryData)){
			        log.error("[捷付支付]-[请求支付]3.2.发送支付请求，解密第三方返回数据失败。"+ resultStr);
                    throw new PayException(resultStr);
			    }
			    
		        jsonData = JSON.parseObject(decryData);
		        String qrcode = jsonData.getString("transaction_url");
		        
		        if(StringUtils.isBlank(qrcode)){
		            log.error("[捷付支付]-[请求支付]3.3.发送支付请求，二维码数据为空。"+ decryData);
                    throw new PayException(decryData);
		        }
		        
		        result.put(JUMPURL, qrcode);
			}
			payResultList.add(result);
			
		} catch (Exception e) {
			log.error("[捷付支付]-[请求支付]3.4.发送支付请求，及获取支付请求结果出错：", e);
			throw new PayException(e.getMessage(), e);
		}
		
		log.debug("[捷付支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));
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
		log.debug("[捷付支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
		return requestPayResult;
	}
	
	/**
	 * 加密参数
	 * */
	private String encryptParams() throws PayException{
	    
	    Map<String, String> payParam = Maps.newHashMap();
        
        payParam.put(money, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
        payParam.put(platform, "PC");
        payParam.put(note, "test");
        payParam.put(bankCode, "");
        payParam.put(payType, this.channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
        payParam.put(merchantId, channelWrapper.getAPI_MEMBERID());
        payParam.put(orderNumber, channelWrapper.getAPI_ORDER_ID());
        payParam.put(riskLevel, "1");
        payParam.put(notifyUrl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                
        String json = JSON.toJSONString(payParam);
        String data = null;
        try{
            data = RsaUtil.encryptToBase64(json,channelWrapper.getAPI_PUBLIC_KEY());
           
        }catch(Exception e){
            log.error("[捷付支付]-[请求支付]-5.加密数据出错：{}",e.getMessage(),e);
            throw new PayException(e.getMessage(),e);
        }
        
        return data;
	}
	
	/************************************************************************/
	private String generatePostParamRequest(String data, String sign) {
        StringBuilder sb = new StringBuilder();
        try {
            sb.append("merchant_code=").append(URLEncoder.encode(this.channelWrapper.getAPI_MEMBERID(), "UTF-8")).append("&");
            sb.append("data=").append(URLEncoder.encode(data, "UTF-8")).append("&");
            sb.append("sign=").append(URLEncoder.encode(sign, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            System.out.println(e);
        }

        return sb.toString();
	    
    }
	
	
}