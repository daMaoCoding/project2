package dc.pay.business.chuangfu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * May 15, 2018
 */
@RequestPayHandler("CHUANGFU")
public final class ChuangFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(ChuangFuPayRequestHandler.class);

	//参数名				类型			是否必填		说明
	//body				String（50）		M			商品的具体描述
	//charset			String			M			参数编码字符集
	//defaultbank		String			C			网银代码，当支付方式为bankPay时，该值为空；支付方式为directPay时该值必传，值见银行列表
	//isApp				String			C			接入方式，当该值传“app”时，表示app接入，返回二维码地址，需商户自行生成二维码；值为“web”时，表示web接入，直接在收银台页面上显示二维码；值为“H5”时，表示手机端html5接入，会在手机端唤醒支付app
	//merchantId		String			M			支付平台分配的商户ID
	//notifyUrl			String（128）		M			商户支付成功后，该地址将收到支付成功的异步通知信息，该地址收到的异步通知作为发货依据
	//orderNo			String(64)		M			商户订单号，务必确保在系统中唯一
	//paymentType		String(4)		M			支付类型，固定值为1
	//paymethod			String			M			支付方式，directPay：直连模式；bankPay：收银台模式
	//returnUrl			String（128）		M			支付成功跳转URL，仅适用于支付成功后立即返回商户界面。我司处理完请求后，将立即跳转并把处理结果返回给这个URL
	//service			String			M			固定值online_pay，表示网上支付
	//title				String（50）		M			商品的名称，请勿包含字符
	//totalFee			Number(13,2)	M			订单金额，单位为RMB元
	//signType			String			M			签名方式 ：SHA
	//sign				String			M			加签结果
	private static final String body			 ="body";
	private static final String charset			 ="charset";
	private static final String defaultbank		 ="defaultbank";
	private static final String isApp			 ="isApp";
	private static final String merchantId		 ="merchantId";
	private static final String notifyUrl		 ="notifyUrl";
	private static final String orderNo			 ="orderNo";
	private static final String paymentType		 ="paymentType";
	private static final String paymethod		 ="paymethod";
	private static final String returnUrl		 ="returnUrl";
	private static final String service			 ="service";
	private static final String title			 ="title";
	private static final String totalFee		 ="totalFee";
	private static final String signType		 ="signType";
	
	//参数名（ParameterName）		类型（Type）			是否必填（Required）			说明（Description）
	//smsText				String				 C					短信验证码，当支付方式为快捷支付时必填（SMS veri
	//userIp				String				 C					（h5必填）发起支付的持卡人IP（The IP who will pay for the order） 
	//appName				String				 C					（h5必填）发起请求的APP名称或WAP网站名（The request of the APP's or WAP website's name）
	//appMsg				String				 C					（h5必填）手机标识 有iOS android web 三个值（The phone mark）
	//appType				String				 C					（h5必填）客户端形式（The client type,  xed value: ios, android or wap）
	//backUrl				String（128）			 C					（h5必填）成功页面（Success page, when pay success, will open this URL）
//	private static final String smsText		 ="smsText";
	private static final String userIp		 ="userIp";
	private static final String appName		 ="appName";
	private static final String appMsg		 ="appMsg";
	private static final String appType		 ="appType";
	private static final String backUrl		 ="backUrl";
	
	//signature	数据签名	32	是	　
//	private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
            	put(body,"name");
            	put(charset,"UTF-8");
            	//defaultbank		String			C			网银代码，当支付方式为bankPay时，该值为空；支付方式为directPay时该值必传，值见银行列表
            	put(defaultbank, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            	//				String			C			接入方式，当该值传“app”时，表示app接入，返回二维码地址，需商户自行生成二维码；值为“web”时，表示web接入，直接在收银台页面上显示二维码；值为“H5”时，表示手机端html5接入，会在手机端唤醒支付app
            	if (handlerUtil.isWapOrApp(channelWrapper)) {
            		put(isApp, "H5");
            		put(userIp, handlerUtil.getRandomIp(channelWrapper));
            		put(appName, "http://www.baidu.com/");
            		put(appMsg, "ios");
            		put(appType, "ios");
            		put(backUrl, channelWrapper.getAPI_WEB_URL());
				}else if (handlerUtil.isWY(channelWrapper)) {
					put(isApp, "web");
				}else {
					put(isApp, "app");
				}
            	put(merchantId, channelWrapper.getAPI_MEMBERID());
            	put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            	put(orderNo,channelWrapper.getAPI_ORDER_ID());
            	put(paymentType,"1");
            	put(paymethod,"directPay");
            	put(returnUrl,channelWrapper.getAPI_WEB_URL());
            	put(service,"online_pay");
            	put(title,"1");
            	put(totalFee,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(signType,"SHA");
            }
        };
        log.debug("[创富]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
    	List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
    	StringBuilder signSrc = new StringBuilder();
    	for (int i = 0; i < paramKeys.size(); i++) {
    		if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i))) && !signType.equals(paramKeys.get(i))) {
    			signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
    		}
    	}
    	String paramsStr = signSrc.toString();
    	//去除最后一个&符
    	paramsStr = paramsStr.substring(0,paramsStr.length()-1);
    	paramsStr = paramsStr+channelWrapper.getAPI_KEY();
    	String signMd5 = null;
    	try {
    		signMd5 = Sha1Util.getSha1(paramsStr).toUpperCase();
    	} catch (Exception e) {
    		e.printStackTrace();
    		throw new PayException("签名异常，请查检参数！");
    	}
    	log.debug("[创富]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
    	return signMd5;
    }
    
	protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
		payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
		HashMap<String, String> result = Maps.newHashMap();
		String api_CHANNEL_BANK_URL = channelWrapper.getAPI_CHANNEL_BANK_URL()+"/"+payParam.get(merchantId)+"-"+payParam.get(orderNo);
		if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
			result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(api_CHANNEL_BANK_URL,payParam).toString());
		}else{
			String resultStr = RestTemplateUtil.postForm(api_CHANNEL_BANK_URL, payParam, "utf-8");
			if (StringUtils.isBlank(resultStr)) {
				log.error("[创富]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空"+",参数："+JSON.toJSONString(payParam));
				throw new PayException("返回空"+",参数："+JSON.toJSONString(payParam));
			}
			JSONObject resJson = JSONObject.parseObject(resultStr);
			if (!resJson.containsKey("respCode") || !"S0001".equals(resJson.getString("respCode"))) {
				log.error("[创富]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
				throw new PayException(resultStr);
			}
			result.put(QRCONTEXT, resJson.getString("codeUrl"));
		}
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		payResultList.add(result);
		log.debug("[创富]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
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
        log.debug("[创富]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}