package dc.pay.business.fengshang;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

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
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * May 30, 2018
 */
@RequestPayHandler("FENGSHANG")
public final class FengShangPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(FengShangPayRequestHandler.class);

    //4.2预支付接口
    private static final String pretransfer				="http://pay.onwindservice.com/transfer/pretransfer";
    //4.3建立会话接口
    private static final String createsession			="http://pay.onwindservice.com/order/createsession";
    
	//4.2预支付接口
	//用户发起扫码支付时，合作方需要将扫码支付信息推送到扫码支付平台进行预支付，预支付成功返回交易流水号:
	//测试环境：
	//生产环境：http:// pay.onwindservice.com /transfer/pretransfer
	//中文域名				对应DTD元素			类型					请求			应答			说明
	//商户编号				merchantno			VARCHAR(11)			M						扫描支付平台为商户分配的唯一编号
	//应用编号				appno				VARCHAR(11)			M						商户应用编号
	//商户订单号			merchantorder		VARCHAR(64)			M						商户订单号
	//交易金额				amount				VARCHAR(20)			M						单位为:元（精确到0.01）
	//币种				currency			VARCHAR(3)			M						
	//商品编号				itemno				VARCHAR(64)			M						
	//商品名称				itemname			VARCHAR（64）			M						
	//用户编号				customerno			VARCHAR(32)			M						用户在合作方的编号
	//异步通知地址			notifyurl			VARCHAR(128)		M						
	//能力方式				paymode				VARCHAR(2)			M						1-SDK,2-扫码，3-H5，4-网关,5-快捷支付
	//交易流水号			serialno			VARCHAR(32)						M			
	//4.3建立会话接口
	//测试环境：
	//生产环境：http:// pay.onwindservice.com /order/createsession
	//中文域名				对应DTD元素			类型					请求			应答			说明
	//平台交易流水号			serialno			VARCHAR(32)			M						扫描支付平台为商户分配的唯一编号
	//支付方式				paytype				VARCHAR(11)			O						指定支付方式,1-微信，2-支付宝，3-网关，4-QQ
	//支付渠道编号			paychannel			VARCHAR(11)			O						指定支付渠道编号
	//订单信息				transinfo											M			
	//支付方式列表			paytypelist											M			
	//会话标识				sessionid											M			
	//4.4 下单接口
	//测试环境：
	//生产环境：http:// pay.onwindservice.com /order/transorder
	//中文域名				对应DTD元素			类型					请求			应答			说明
	//会话标识				sessionid			VARCHAR				M						会话标识
	//支付方式				paytype				VARCHAR(11)			M						指定支付方式1-微信，2-支付宝，3-网关，4-QQ
	//支付渠道编号			payinfo				VARCHAR				O						附加支付内容，网关必填银行代码，参考附录 4.8银行代码列表如：中国银行：BOC,微信wap扩展字段，传真实下单用户IP
	//订单信息				orderinfo			VARCHAR（32）						M			若是扫码方式，商户在此报文里Base64解码解析payparam字段，取json串里的url生成二维码，网银支付方式，同样要解析payparam字段，取json串里的url，然后跳转到网银页面进行支付
	//支付方式列表			resultinfo			VARCHAR							O			支付渠道返回内容
	private static final String merchantno			="merchantno";
	private static final String appno				="appno";
	private static final String merchantorder		="merchantorder";
	private static final String amount				="amount";
	private static final String currency			="currency";
	private static final String itemno				="itemno";
	private static final String itemname			="itemname";
	private static final String customerno			="customerno";
	private static final String notifyurl			="notifyurl";
	private static final String paymode				="paymode";
	private static final String serialno			="serialno";
	private static final String paytype				="paytype";
//	private static final String paychannel			="paychannel";
//	private static final String transinfo			="transinfo";
//	private static final String paytypelist			="paytypelist";
	private static final String sessionid			="sessionid";
//	private static final String paytype				="paytype";
	private static final String payinfo				="payinfo";
//	private static final String orderinfo			="orderinfo";
//	private static final String resultinfo			="resultinfo";

	//signature	数据签名	32	是	　
//	private static final String signature  ="signature";

	private String pretransfer() throws PayException {
		Map<String, String> payParam = new TreeMap<String, String>() {
			{
				put(merchantno, channelWrapper.getAPI_MEMBERID().split("&")[0]);
				put(appno, channelWrapper.getAPI_MEMBERID().split("&")[1]);
				put(merchantorder,channelWrapper.getAPI_ORDER_ID());
				put(amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
				put(currency,"RMB");
				put(itemno, HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "yyyyMMddHHmm"));
				put(itemname,"name");
				put(customerno,channelWrapper.getAPI_MEMBERID().split("&")[0]);
				put(notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
				put(paymode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
			}
		};
		String data = "";
		try {
			String signMd5 = RsaUtil.signByPrivateKey(JSON.toJSONString(payParam), channelWrapper.getAPI_KEY());
			data = "transdata=" + URLEncoder.encode(JSON.toJSONString(payParam), "utf-8") + "&sign="+ URLEncoder.encode(signMd5, "utf-8") + "&signtype=" + "RSA";
		} catch (Exception e) {
			log.error("[风上]-[请求支付]-pretransfer().1.生成加密URL签名出错，签名出错：{}",e.getMessage(),e);
			throw new PayException(e.getMessage(),e);
		}
		String resultStr = RestTemplateUtil.postStr(pretransfer,data,MediaType.APPLICATION_JSON_UTF8_VALUE,"Keep-Alive",MediaType.APPLICATION_JSON_UTF8_VALUE.toString());
		if (StringUtils.isBlank(resultStr)) {
			log.error("[风上]-[请求支付]-pretransfer().2.发送支付请求，获取支付请求返回值异常:返回空"+",参数："+JSON.toJSONString(payParam));
			throw new PayException("返回空"+",参数："+JSON.toJSONString(payParam));
		}
		Map<String, String> resJson = HandlerUtil.urlToMap(resultStr);
		String transdata = resJson.get("transdata");
		if (!resJson.containsKey("transdata") || StringUtils.isBlank(transdata)) {
			log.error("[风上]-[请求支付]-pretransfer().3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
			throw new PayException(resultStr);
		}
		return JSONObject.parseObject(handlerUtil.UrlDecode(transdata)).getString("serialno");
	}
	
	private String createsession() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
            	put(serialno, pretransfer());
            }
        };
        String data = "";
        try {
        	String signMd5 = RsaUtil.signByPrivateKey(JSON.toJSONString(payParam), channelWrapper.getAPI_KEY());
        	data = "transdata=" + URLEncoder.encode(JSON.toJSONString(payParam), "utf-8") + "&sign="+ URLEncoder.encode(signMd5, "utf-8") + "&signtype=" + "RSA";
		} catch (Exception e) {
			log.error("[风上]-[请求支付]-createsession().1.生成加密URL签名出错，签名出错：{}",e.getMessage(),e);
			throw new PayException(e.getMessage(),e);
		}
        String resultStr = RestTemplateUtil.postStr(createsession,data,MediaType.APPLICATION_JSON_UTF8_VALUE,"Keep-Alive",MediaType.APPLICATION_JSON_UTF8_VALUE.toString());
        if (StringUtils.isBlank(resultStr)) {
			log.error("[风上]-[请求支付]-createsession().2.发送支付请求，获取支付请求返回值异常:返回空"+",参数："+JSON.toJSONString(payParam));
			throw new PayException("返回空"+",参数："+JSON.toJSONString(payParam));
		}
        Map<String, String> resJson = HandlerUtil.urlToMap(resultStr);
		String transdata = resJson.get("transdata");
		if (!resJson.containsKey("transdata") || StringUtils.isBlank(transdata)) {
			log.error("[风上]-[请求支付]-createsession().3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
			throw new PayException(resultStr);
		}
		return JSONObject.parseObject(handlerUtil.UrlDecode(transdata)).getString("sessionid");
	}
	
    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(sessionid, createsession());
                put(paytype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
//                put(payinfo,handlerUtil.isWY(channelWrapper) ? channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1] : "");
                if (channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("FENGSHANG_BANK_WAP_WX_SM")) {
//                	put(payinfo,handlerUtil.getRandomIp(channelWrapper));
                	put(payinfo,channelWrapper.getAPI_Client_IP());
				}else {
					put(payinfo,handlerUtil.isWY(channelWrapper) ? channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1] : "");
				}
            }
        };
        log.debug("[风上]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

	protected String buildPaySign(Map params) throws PayException {
		String signMd5="";
		try {
			signMd5 = RsaUtil.signByPrivateKey(JSON.toJSONString(params),channelWrapper.getAPI_KEY());	// 签名
		} catch (Exception e) {
			log.error("[风上]-[请求支付]-2.生成加密URL签名出错，签名出错：{}",e.getMessage(),e);
			throw new PayException(e.getMessage(),e);
		}
		//String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
		log.debug("[风上]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
		return signMd5;
	}

	protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		try {
			String data = "transdata=" + URLEncoder.encode(JSON.toJSONString(payParam), "utf-8") + "&sign="+ URLEncoder.encode(pay_md5sign, "utf-8") + "&signtype=" + "RSA";
			String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(),data,MediaType.APPLICATION_JSON_UTF8_VALUE,"Keep-Alive",MediaType.APPLICATION_JSON_UTF8_VALUE.toString());
			if (StringUtils.isBlank(resultStr)) {
				log.error("[风上]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空"+",参数："+JSON.toJSONString(payParam));
				throw new PayException("返回空"+",参数："+JSON.toJSONString(payParam));
			}
			if (!resultStr.contains("transdata")) {
				log.error("[风上]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + resultStr + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
				throw new PayException(resultStr);
			}
			Map<String, String> resJson = HandlerUtil.urlToMap(resultStr);
			String transdata = resJson.get("transdata");
			if (StringUtils.isBlank(transdata)) {
				log.error("[风上]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resJson) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
				throw new PayException(JSON.toJSONString(resJson));
			}
			String resultinfo = JSONObject.parseObject(handlerUtil.UrlDecode(transdata)).getString("resultinfo");
			if (StringUtils.isBlank(resultinfo)) {
				log.error("[风上]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(transdata) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
				throw new PayException(transdata);
			}
			byte[] decodeBase64 = Base64.decodeBase64(JSONObject.parseObject(resultinfo).getString("payparam").getBytes("utf-8"));
			String json = new String(decodeBase64, "utf-8");
			if (StringUtils.isBlank(json)) {
				log.error("[风上]-[请求支付]-3.5.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultinfo) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
				throw new PayException(resultinfo);
			}
			String url = JSONObject.parseObject(json).getString("url");
			if (StringUtils.isBlank(url)) {
				log.error("[风上]-[请求支付]-3.6.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(json) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
				throw new PayException(json);
			}
//			{"type":"wap","url":""}
			HashMap<String, String> result = Maps.newHashMap();
			result.put((handlerUtil.isWY(channelWrapper) || handlerUtil.isWapOrApp(channelWrapper)) ? JUMPURL : QRCONTEXT,url );
			payResultList.add(result);
		} catch (Exception e) {
			log.error("[风上]-[请求支付]-3.7.发送支付请求，及获取支付请求结果出错：", e);
			throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
		}
		log.debug("[风上]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
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
        log.debug("[风上]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}