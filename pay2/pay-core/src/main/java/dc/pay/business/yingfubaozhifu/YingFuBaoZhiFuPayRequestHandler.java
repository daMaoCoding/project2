package dc.pay.business.yingfubaozhifu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.UnicodeUtil;
import dc.pay.utils.ValidateUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author Mikey
 * Jun 12, 2019
 */
@Slf4j
@RequestPayHandler("YINGFUBAOZHIFU")
public final class YingFuBaoZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(YingFuBaoZhiFuPayRequestHandler.class);
    /*
	变量名		字段名			必填	类型		示例值				描述
billNumero	商户单号		是		String(50)	order201712010001	商户上送订单号，保持唯一值。
money		交易金额		是		String(20)	1000				以分为单位，10.00元填入值为1000
currency	币种			是		String(10)	CNY					CNY-人民币，默认为CNY
goodsName	商品名称		是		String(20)						用于描述该笔交易或商品的主体信息
goodsDesc	商品描述		是		String(500)						用于描述该笔交易或商品的主体信息
payType		支付类型		是		String(20)	ALI_H5				详见”4.1：交易类型“
notifyUrl	异步通知地址		是		String(255)						支付结果后台异步通知地址。(不能含有’字符. 如果含有?&=字符, 必须先对该地址做URL编码)
merchantId	平台商户号		是		String(32)	PAY10017681024		商户在平台注册与使用的商户编号
askTime		请求时间		是		String(14)	20180303161616		暂时只支持RSA，必须大写的RSA
sign		签名信息		是		String							使用商户证书对报文签名后值
*/
	private static final String billNumero	   = "billNumero";	    //商户上送订单号，保持唯一值。
	private static final String money		   = "money";			//以分为单位，10.00元填入值为1000
	private static final String currency	   = "currency";		//CNY-人民币，默认为CNY
	private static final String goodsName	   = "goodsName";		//用于描述该笔交易或商品的主体信息
	private static final String goodsDesc	   = "goodsDesc";		//用于描述该笔交易或商品的主体信息
	private static final String payType		   = "payType";			//详见”4.1：交易类型“
	private static final String notifyUrl	   = "notifyUrl";		//支付结果后台异步通知地址。(不能含有’字符. 如果含有?&=字符, 必须先对该地址做URL编码)
	private static final String merchantId	   = "merchantId";		//商户在平台注册与使用的商户编号
	private static final String askTime		   = "askTime";			//暂时只支持RSA，必须大写的RSA
	private static final String sign		   = "sign";			//使用商户证书对报文签名后值
	private static final String version		   = "version";
	private static final String orderCreateIp  = "orderCreateIp";
	private static final String context  	   = "context";

	
	
	/**
	 *	 參數封裝
	 */
	@Override
	protected Map<String, String> buildPayParam() throws PayException {
		Map<String, String> payParam = Maps.newHashMap();
		
		payParam.put(billNumero,channelWrapper.getAPI_ORDER_ID());
		payParam.put(money,channelWrapper.getAPI_AMOUNT());
		payParam.put(currency,"CNY");
		payParam.put(goodsName,channelWrapper.getAPI_ORDER_ID());
		payParam.put(goodsDesc,channelWrapper.getAPI_ORDER_ID());
		payParam.put(payType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
		payParam.put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
		payParam.put(merchantId, channelWrapper.getAPI_MEMBERID());
		payParam.put(askTime,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
	
	    log.debug("[盈付宝支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
	    return payParam;
	}
	
	/**
	 * 	簽名製作
	 */
	protected String buildPaySign(Map<String, String> params) throws PayException {
		String pay_md5sign = null;
		
		net.sf.json.JSONObject resulthead = new net.sf.json.JSONObject();
		resulthead.put(merchantId, params.get(merchantId));
		resulthead.put(version, "V1.1.0");
		resulthead.put(askTime, params.get(askTime));
		
		net.sf.json.JSONObject resultbody = new net.sf.json.JSONObject();
		resultbody.put(billNumero, params.get(billNumero)); 
		resultbody.put(currency, params.get(currency)); 
		resultbody.put(goodsDesc, params.get(goodsDesc)); 
		resultbody.put(goodsName, params.get(goodsName));
		resultbody.put(money, params.get(money));
		resultbody.put(notifyUrl, params.get(notifyUrl));
		resultbody.put(orderCreateIp, channelWrapper.getAPI_Client_IP());
		resultbody.put(payType, params.get(payType));
		
		try {
			pay_md5sign = RSAUtils.verifyAndEncryptionToString(resultbody, resulthead, channelWrapper.getAPI_KEY() , channelWrapper.getAPI_PUBLIC_KEY());
		} catch (Exception e) {
			e.printStackTrace();
		}
		log.debug("[盈付宝支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
		return pay_md5sign;
	}
	
	/**
	 * 	發送請求
	 */
	protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
	    HashMap<String, String> result = Maps.newHashMap();
	    ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
	    net.sf.json.JSONObject jsonParam = new net.sf.json.JSONObject();
	    jsonParam.put(context, pay_md5sign);
	    String resultStr = null;
	    if ((HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper))) {
			HashMap<String, String> payMap = Maps.newHashMap();
			payMap.put(context, pay_md5sign);
			result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payMap).toString().replace("method='post'","method='get'"));  //.replace("method='post'","method='get'"));
			payResultList.add(result);
	    }else{
			try {
		        resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), jsonParam );
		        resultStr = UnicodeUtil.unicodeToString(resultStr);
		        JSONObject resJson = JSONObject.parseObject(resultStr);
				if (null != resJson && resJson.containsKey("success") && resJson.getString("success").equals("true")) {
					String context = resJson.getString("context");
					String decrypt = RSAUtils.decryptByPrivateKey(context, channelWrapper.getAPI_KEY());
					JSONObject resultJson = JSONObject.parseObject(decrypt);
					if (null != resultJson && resultJson.containsKey("businessContext")
							&& null != resultJson.getJSONObject("businessContext")
							&& resultJson.getJSONObject("businessContext").containsKey("payurl")
							&& StringUtils.isNotBlank(resultJson.getJSONObject("businessContext").getString("payurl"))
							&& !ValidateUtil
									.isHaveChinese(resultJson.getJSONObject("businessContext").getString("payurl"))) {
						if (HandlerUtil.isWapOrApp(channelWrapper)) {
							result.put(JUMPURL, resultJson.getJSONObject("businessContext").getString("payurl"));
						} else {
							result.put(QRCONTEXT, resultJson.getJSONObject("businessContext").getString("payurl"));
						}
						payResultList.add(result);
					} else {
						throw new PayException(JSON.toJSONString(resultStr));
					}
				} else {
					throw new PayException(JSON.toJSONString(resultStr));
				}
			} catch (Exception e) {
				log.error("[盈付宝支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号："
						+ channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
				throw new PayException(JSON.toJSONString(resultStr));
			}
	    }
	     log.debug("[盈付宝支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
	     return payResultList;
	}
	
	/**
	 * 	封裝結果
	 */
	protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
	    RequestPayResult requestPayResult = new RequestPayResult();
	    if (null != resultListMap && !resultListMap.isEmpty()) {
	        if (resultListMap.size() == 1) {
	            Map<String, String> resultMap = resultListMap.get(0);
	            requestPayResult = buildResult(resultMap,channelWrapper,requestPayResult);
	        }
	        if (ValidateUtil.requestesultValdata(requestPayResult)) {
	            requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
	        } else {
	            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
	        }
	    } else {
	        throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
	    }
	    log.debug("[盈付宝支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
	    return requestPayResult;
	}
}