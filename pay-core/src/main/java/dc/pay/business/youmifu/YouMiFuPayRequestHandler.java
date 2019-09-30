package dc.pay.business.youmifu;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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
import dc.pay.utils.Result;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Feb 20, 2018
 */
@RequestPayHandler("YOUMIFU")
public final class YouMiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(YouMiFuPayRequestHandler.class);

    //接口名字		apiName
    private static final String apiName		  ="apiName";
    //接口版本		apiVersion
    private static final String apiVersion	  ="apiVersion";
    //商户(合作伙伴)ID	platformID
    private static final String platformID	  ="platformID";
    //商户账号		merchNo
    private static final String merchNo		  ="merchNo";
    //商户订单号		orderNo
    private static final String orderNo		  ="orderNo";
    //交易日期		tradeDate
    private static final String tradeDate	  ="tradeDate";
    //订单金额		amt
    private static final String amt		      ="amt";
    //支付结果通知地址	merchUrl
    private static final String merchUrl	  ="merchUrl";
    //商户参数		merchParam
    private static final String merchParam	  ="merchParam";
    //交易摘要		tradeSummary
    private static final String tradeSummary  ="tradeSummary";
    //银行代码		不进行签名，支付系统根据该银行代码直接跳转银行网银，不输或输入的银行代码不存在则展示支付首页让用户选择支付方式。
    private static final String bankCode	  ="bankCode";
    //选择支付方式	不进行签名，根据选择的支付方式直接对应页面。不输入或选择支付方式不存在则认为是该商户所拥有的全部方式。
    private static final String choosePayType ="choosePayType";
    //客户端IP	customerIP	ans（..20）	必输，客户端ip地址
	private static final String customerIP	  ="customerIP";

    /**
     * 封装第三方所需要的参数
     * 
	 * @return
	 * @throws PayException
	 * @author andrew
	 * Feb 21, 2018
	 */
	@Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {        	
        		put(platformID, channelWrapper.getAPI_MEMBER_PLATFORMID());
        		put(orderNo,channelWrapper.getAPI_ORDER_ID());
        		put(merchNo, channelWrapper.getAPI_MEMBERID());
        		put(tradeDate, DateUtil.formatDateTimeStrByParam("yyyyMMdd"));
        		put(amt,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
        		put(merchUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
        		//merchParam可以为空，但必须存在！
          		put(merchParam,"");
           		//如：商品名称|商品数量
           		put(tradeSummary,"pay");
            	//直接
            	if (channelWrapper.getAPI_CHANNEL_BANK_NAME().endsWith("YOUMIFU_BANK_WEBWAPAPP_ZFB_SM") ||
            			channelWrapper.getAPI_CHANNEL_BANK_NAME().endsWith("YOUMIFU_BANK_WEBWAPAPP_WX_SM") ||
            			channelWrapper.getAPI_CHANNEL_BANK_NAME().endsWith("YOUMIFU_BANK_WEBWAPAPP_QQ_SM") ||
            			channelWrapper.getAPI_CHANNEL_BANK_NAME().endsWith("YOUMIFU_BANK_WEBWAPAPP_JD_SM") ||
            			channelWrapper.getAPI_CHANNEL_BANK_NAME().endsWith("YOUMIFU_BANK_WAP_ZFB_SM")
            			) {
            		put(apiName,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            		put(apiVersion,"1.0.0.0");
            		if (channelWrapper.getAPI_CHANNEL_BANK_NAME().endsWith("YOUMIFU_BANK_WAP_ZFB_SM")) {
            			put(bankCode,"ALSP");
            		}else {
            			//客户端IP	customerIP	ans（..20）	必输，客户端ip地址
            			put(customerIP,HandlerUtil.getRandomIp(channelWrapper));
            		}
            	}else if (channelWrapper.getAPI_CHANNEL_BANK_NAME().endsWith("_WY_KJZF")) {
            		put(apiName,"WAP_PAY_B2C");
            		put(apiVersion,"1.0.0.1");
            		//客户端IP	customerIP	ans（..20）	必输，客户端ip地址
            		put(customerIP,HandlerUtil.getRandomIp(channelWrapper));
            		put(choosePayType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            		put(bankCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            	}else if (channelWrapper.getAPI_CHANNEL_BANK_NAME().endsWith("YOUMIFU_BANK_WEBWAPAPP_YL_SM")) {
            		put(apiName,"WAP_PAY_B2C");
            		put(apiVersion,"1.0.0.1");            		
            		//客户端IP	customerIP	ans（..20）	必输，客户端ip地址
            		put(customerIP,HandlerUtil.getRandomIp(channelWrapper));
            		put(choosePayType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
				}else if (handlerUtil.isWY(channelWrapper) || channelWrapper.getAPI_CHANNEL_BANK_NAME().endsWith("YOUMIFU_BANK_WAP_WX_SM")) {
            		put(apiName,"WEB_PAY_B2C");
            		put(apiVersion,"1.0.0.1");
            		//客户端IP	customerIP	ans（..20）	必输，客户端ip地址
            		put(customerIP,HandlerUtil.getRandomIp(channelWrapper));
            		put(choosePayType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            		put(bankCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
				}else if (channelWrapper.getAPI_CHANNEL_BANK_NAME().endsWith("YOUMIFU_BANK_WAP_QQ_SM")) {
					put(apiName,"WAP_PAY_B2C");
            		put(apiVersion,"1.0.0.1");
            		//客户端IP	customerIP	ans（..20）	必输，客户端ip地址
            		put(customerIP,HandlerUtil.getRandomIp(channelWrapper));
            		put(choosePayType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            	}
            }
        };
        log.debug("[优米付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

    
    /**
     * 生成签名
     * 
     * @param api_response_params
     * @return
     * @throws PayException
     * @author andrew
     * Feb 21, 2018
     */
    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
    	//1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
    	//2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
		// 输入数据组织成字符串
		StringBuffer signSrc= new StringBuffer();
		signSrc.append(apiName+"=").append(api_response_params.get(apiName)).append("&");
		signSrc.append(apiVersion+"=").append(api_response_params.get(apiVersion)).append("&");
		signSrc.append(platformID+"=").append(api_response_params.get(platformID)).append("&");
		signSrc.append(merchNo+"=").append(api_response_params.get(merchNo)).append("&");
		signSrc.append(orderNo+"=").append(api_response_params.get(orderNo)).append("&");
		signSrc.append(tradeDate+"=").append(api_response_params.get(tradeDate)).append("&");
		signSrc.append(amt+"=").append(api_response_params.get(amt)).append("&");
		signSrc.append(merchUrl+"=").append(api_response_params.get(merchUrl)).append("&");
		signSrc.append(merchParam+"=").append(api_response_params.get(merchParam)).append("&");
		signSrc.append(tradeSummary+"=").append(api_response_params.get(tradeSummary));
		if (StringUtils.isNotBlank(api_response_params.get(customerIP))) {
			signSrc.append("&").append(customerIP+"=").append(api_response_params.get(customerIP));
		}
		signSrc.append(channelWrapper.getAPI_KEY());
		String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[优米付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }

    /**
     * 生成返回给RequestPayResult对象detail字段的值
     * 
     * @param payParam
     * @param pay_md5sign
     * @return
     * @throws PayException
     * @author andrew
     * Feb 21, 2018
     */
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
    	payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        String api_channel_bank_name = channelWrapper.getAPI_CHANNEL_BANK_NAME();
        String api_CHANNEL_BANK_URL = channelWrapper.getAPI_CHANNEL_BANK_URL();
        Map<String,String> result = Maps.newHashMap();
        if ((HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper)) && !channelWrapper.getAPI_CHANNEL_BANK_NAME().endsWith("YOUMIFU_BANK_WAP_ZFB_SM") ) {
			result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
        }else{
        	String resultStr = null;
        	try {
        		resultStr = RestTemplateUtil.postForm(api_CHANNEL_BANK_URL, payParam,"UTF-8").trim();
        	} catch (Exception e) {
        		log.error("[优米付]3.1.发送支付请求，及获取支付请求出错，订单号：" + channelWrapper.getAPI_ORDER_ID() + ",通道名称：" + api_channel_bank_name + ",postUrl:" + api_CHANNEL_BANK_URL + ",payForManagerCGIResultJsonObj" + e.getMessage(), e);
        		throw new PayException(e.getMessage(),e);
        	}
        	if (StringUtils.isBlank(resultStr)) {
        		log.error("[优米付]3.2.发送支付请求，获取支付请求返回值异常:返回null，入参数："+JSON.toJSONString(payParam));
        		throw new PayException("返回null，入参数："+JSON.toJSONString(payParam));
        	}
			if (resultStr.contains("失败") || resultStr.contains("不成功")) {
				log.error("[优米付]3.3.发送支付请求，获取支付请求返回值异常:"+resultStr);
				throw new PayException(resultStr);
			}
			Document document = null;
			try {
				document = Jsoup.parse(resultStr);
			} catch (Exception e) {
				log.error("[优米付]3.4.发送支付请求，获取支付请求返回值异常:"+resultStr);
				throw new PayException(resultStr);
			}
            if (HandlerUtil.isYL(channelWrapper)) {
            	Element formEl = document.getElementsByTag("form").first();
            	Map<String, String> secondPayParam = HandlerUtil.parseFormElement(formEl);
            	String root = "http://cashier.youmifu.com/";
            	Result secondServ = null;
            	try {
            		secondServ = HandlerUtil.sendToThreadPayServ(root+"cgi-bin/netpayment/pay_gate.cgi", secondPayParam, root+secondPayParam.get("action"));
				} catch (Exception e) {
					e.printStackTrace();
					log.error("[优米付]3.5.发送支付请求，获取支付请求返回值异常:"+e.getMessage());
					throw new PayException(e.getMessage());
				}
            	String body = secondServ.getBody();
				if (StringUtils.isBlank(body)) {
					log.error("[优米付]3.6.发送支付请求，获取支付请求返回值异常:返回null，入参数："+JSON.toJSONString(secondPayParam));
					throw new PayException(":返回null，入参数："+JSON.toJSONString(secondPayParam));
				}
//				try {
//					document = Jsoup.parse(body);
//				} catch (Exception e) {
//					log.error("[优米付]3.7.发送支付请求，获取支付请求返回值异常:"+resultStr);
//					throw new PayException(resultStr);
//				}
				
				Element form1 = Jsoup.parse(body).select("body [id=ylspPayForm").first();
				Elements inputs = form1.getElementsByTag("input");
				Map<String,String> map = new HashMap<>();
				map.put("orderId", inputs.select("[name='orderId']").val());
				map.put("channelType", inputs.select("[name='channelType']").val());
				map.put("amt", inputs.select("[name='amt']").val());
				map.put("merchNo", inputs.select("[name='merchNo']").val());
				map.put("__token", inputs.select("[name='__token']").val());
				map.put("bankCode", "UNSP");
				map.put("payType", "31");
				map.put("__long", "ZElJ4R73oucRitV4");
				map.put("m", "getCodeUrl");
				map.put("_", System.currentTimeMillis()+"");
				result.put("第三方返回2",body); //保存全部第三方信息，上面的拆开没必要
				String resultStr3 = RestTemplateUtil.sendByRestTemplate(root+"/standard/gateway/manager.cgi", map, String.class, HttpMethod.GET).trim();
				if (StringUtils.isBlank(resultStr3)) {
					log.error("[优米付]3.8.发送支付请求，获取支付请求返回值异常:返回null，入参数："+JSON.toJSONString(map));
					throw new PayException(":返回null，入参数："+JSON.toJSONString(map));
				}
				JSONObject parseObject = null;
				try {
					parseObject = JSON.parseObject(resultStr3);
				} catch (Exception e) {
					e.printStackTrace();
					log.error("[优米付]3.9.发送支付请求，获取支付请求返回值异常:"+e.getMessage());
					throw new PayException(e.getMessage());
				}
				String respCode = parseObject.getString("respCode");
				if (StringUtils.isBlank(respCode) || !"00".equals(respCode)) {
					log.error("[优米付]3.1.1.发送支付请求，获取支付请求返回值异常:"+resultStr3);
					throw new PayException(resultStr3);
				}
				String codeUrl = parseObject.getString("codeUrl");
				if (StringUtils.isBlank(codeUrl)) {
					log.error("[优米付]3.1.2.发送支付请求，获取支付请求返回值异常:"+resultStr3);
					throw new PayException(resultStr3);
				}
				result.put("第三方返回3",resultStr3); //保存全部第三方信息，上面的拆开没必要
				result.put(QRCONTEXT, new String(Base64.getDecoder().decode(codeUrl)));
            }else {
				String respcode = document.select("respcode").first().html();
				String respDesc = document.select("respDesc").first().html();
				if (StringUtils.isBlank(respcode) || !"00".equals(respcode) || respDesc.contains("失败")) {
					log.error("[优米付]3.1.3.发送支付请求，获取支付请求返回值异常:"+resultStr);
					throw new PayException(resultStr);
				}
				String codeUrl = document.select("codeUrl").first().html();
				if (StringUtils.isBlank(codeUrl)) {
					log.error("[优米付]3.1.4.发送支付请求，获取支付请求返回值异常:"+resultStr);
					throw new PayException(resultStr);
				}
				result.put(handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, new String(Base64.getDecoder().decode(codeUrl)));
			}
            result.put("第三方返回",resultStr); //保存全部第三方信息，上面的拆开没必要
        }
        List<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[优米付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
        return payResultList;
    }

    /**
     * 生成返回给前端使用的值
     * 
	 * @param resultListMap
	 * @return
	 * @throws PayException
	 * @author andrew
	 * Feb 21, 2018
	 */
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
        log.debug("[优米付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}