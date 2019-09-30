package dc.pay.business.yishengfu;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.apache.tomcat.util.codec.binary.Base64;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * May 4, 2018
 */
@RequestPayHandler("YISHENGFU")
public final class YiShengFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(YiShengFuPayRequestHandler.class);

	//变量名称		变量名			长度定义		说明
	//接口名字		apiName			ans(.30)		必输	，取值：取值：“WECHAT_PAY”
	//接口版本		apiVersion		ans(7)			必输	取值：“1.0.0.0”
	//商户(合作伙伴)ID	platformID		ans(..16)		必输	由支付系统统一分配
	//商户账号		merchNo			ans(..32)		必输	由支付系统统一分配
	//商户订单号		orderNo			n(..32)			必输	支付的订单号，由商户系统生成，同一交易日期内不允许重复
	//交易日期		tradeDate		n(8)			必输，由商户系统生成	YYYYMMDD年月日
	//订单金额		amt			n(12,2)			必输	保留2位小数，单位：元	例如：12.02、12.00、12
	//支付结果通知地址	merchUrl		ans(..128)		必输	接收支付通知的URL地址，自身不能带商户参数,如	http://www.merchant.com/handler.jsp
	//交易摘要		tradeSummary		ans（..120）		必输	，对支付的简单说明	如：商品名称|商品数量
	//超时时间		overTime		ans（..8）		选输,	单位秒
	//客户端IP		customerIP		ans（..20）		必输，客户端ip地址
	//签名			signMsg			ans（..300）		必输，商户对交易数据的签名，签名通过API生成。
	private static final String apiName		  ="apiName";
	private static final String apiVersion		="apiVersion";
	private static final String platformID		="platformID";
	private static final String merchNo		  ="merchNo";
	private static final String orderNo		  ="orderNo";
	private static final String tradeDate		 ="tradeDate";
	private static final String amt			="amt";
	private static final String merchUrl		="merchUrl";
	private static final String tradeSummary	="tradeSummary";
	private static final String overTime	="overTime";
	private static final String customerIP	="customerIP";
	
	private static final String merchParam	="merchParam";
	private static final String bankCode	="bankCode";
	private static final String choosePayType	="choosePayType";
	@Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
            	put(apiName,HandlerUtil.isWY(channelWrapper) || handlerUtil.isYLKJ(channelWrapper)? "WEB_PAY_B2C" : channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            	put(apiVersion,"1.0.0.0");
            	put(platformID, channelWrapper.getAPI_MEMBER_PLATFORMID());
            	put(merchNo, channelWrapper.getAPI_MEMBERID());
            	put(orderNo,channelWrapper.getAPI_ORDER_ID());
            	//yyyyMMdd
            	put(tradeDate, DateUtil.formatDateTimeStrByParam("yyyyMMdd"));
                put(amt,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(merchUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                //merchParam可以为空，但必须存在！
                put(merchParam,"abcd");
                //如：商品名称|商品数量
                put(tradeSummary,"pay");
                //银行代码				bankCode			n(..8)				不进行签名，支付系统根据该银行代码直接跳转银行网银，不输或输入的银行代码不存在则展示支付首页让用户选择支付方式。
                //(工行ICBC)(农行ABC)(中行BOC)(建行CCB) (交行COMM)(招行CMB)(浦发SPDB)(兴业CIB)(民生CMBC)(广发GDB)(中信CNCB)(光大 CEB)(华夏HXB)(邮储PSBC)(平安PAB)(北京BOBJ)(宁波BONB)
                if (HandlerUtil.isWY(channelWrapper)) {
                	put(bankCode, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                	put(choosePayType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                }else if (channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("_WAP_ZFB_SM")) {
                	put(bankCode, "ALSP");
				}else{
					put(choosePayType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
					put(overTime,"600");
					put(customerIP,handlerUtil.getRandomIp(channelWrapper));
				}
            }
        };
        log.debug("[亿胜付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }
    
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
		if (StringUtils.isNotBlank(api_response_params.get(overTime))) {
			signSrc.append("&").append(overTime+"=").append(api_response_params.get(overTime));
		}
		if (StringUtils.isNotBlank(api_response_params.get(customerIP))) {
			signSrc.append("&").append(customerIP+"=").append(api_response_params.get(customerIP));
		}
		signSrc.append(channelWrapper.getAPI_KEY());
		String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[亿胜付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }
    
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
    	payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        Map<String,String> result = Maps.newHashMap();
        if (HandlerUtil.isWY(channelWrapper)) {
			result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
        }else{
            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8").trim();
            if (StringUtils.isBlank(resultStr)) {
            	log.error("[亿胜付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空"+",参数："+JSON.toJSONString(payParam));
            	throw new PayException("第三方返回异常:返回空"+",参数："+JSON.toJSONString(payParam));
            }
			if (!resultStr.contains("<respCode>00</respCode>")) {
				log.error("[亿胜付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
				throw new PayException(resultStr);
			}
			Document document = Jsoup.parse(resultStr);
			Element first = document.getElementsByTag("codeUrl").first();
			if (!first.hasText()) {
				log.error("[亿胜付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
				throw new PayException(resultStr);
			}
			result.put(handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, new String(Base64.decodeBase64(first.text())));
        }
        List<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[亿胜付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
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
        log.debug("[亿胜付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}