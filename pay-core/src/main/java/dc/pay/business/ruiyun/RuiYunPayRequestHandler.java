package dc.pay.business.ruiyun;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
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
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.HttpUtil;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;
import dc.pay.utils.qr.QRCodeUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Mar 12, 2018
 */
@RequestPayHandler("RUIYUN")
public final class RuiYunPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(RuiYunPayRequestHandler.class);
	
	//参数名			参数			可空	加入签名	说明
	//商户ID			parter		N	Y		商户id，由分配
	private static final String parter  ="parter";
	//银行类型			type		N	Y		银行类型，具体请参考附录1
	private static final String type  ="type";
	//金额			value		N	Y		单位元（人民币），2位小数，最小支付金额为0.02
	private static final String value  ="value";
	//商户订单号		orderid		N	Y		商户系统订单号，该订单号将作为接口的返回数据。该值需在商户系统内唯一，系统暂时不检查该值是否唯一
	private static final String orderid  ="orderid";
	//下行异步通知地址	callbackurl	N	Y		下行异步通知过程的返回地址，需要以http://开头且没有任何参数
	private static final String callbackurl  ="callbackurl";
	//支付用户IP		payerIp		Y	N		用户在下单时的真实IP，接口将会判断玩家支付时的ip和该值是否相同。若不相同，接口将提示用户支付风险
	private static final String payerIp  ="payerIp";
	//备注消息			attach		Y	N		备注信息，下行中会原样返回。若该值包含中文，请注意编码
	private static final String attach  ="attach";
	//MD5签名			sign		N	-		32位小写MD5签名值，GB2312编码
	private static final String signature  ="signature";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(parter, channelWrapper.getAPI_MEMBERID());
                if (HandlerUtil.isWebWyKjzf(channelWrapper)) {
                	//2087	网银快捷PC		2088	网银快捷wap
                	put(type,HandlerUtil.isOrderFromWapOrApp(channelWrapper.getAPI_ORDER_FROM()) ? "2088" : "2087");
				}else {
					put(type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
				}
                put(value,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(orderid,channelWrapper.getAPI_ORDER_ID());
                put(callbackurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                //支付用户IP			payerIp		Y	N		用户在下单时的真实IP，接口将会判断玩家支付时的ip和该值是否相同。若不相同，接口将提示用户支付风险
                //备注消息			attach		Y	N		备注信息，下行中会原样返回。若该值包含中文，请注意编码
            }
        };
        log.debug("[瑞云]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map api_response_params) throws PayException {
		//1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
		//2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
    	//parter={}&type={}&value={}&orderid ={}&callbackurl={}key
		StringBuffer signSrc= new StringBuffer();
		signSrc.append(parter+"=").append(api_response_params.get(parter)).append("&");
		signSrc.append(type+"=").append(api_response_params.get(type)).append("&");
		signSrc.append(value+"=").append(api_response_params.get(value)).append("&");
		signSrc.append(orderid+"=").append(api_response_params.get(orderid)).append("&");
		signSrc.append(callbackurl+"=").append(api_response_params.get(callbackurl));
		signSrc.append(channelWrapper.getAPI_KEY());
		String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr,"GB2312").toLowerCase();
        log.debug("[瑞云]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
		payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		HashMap<String, String> result = Maps.newHashMap();
		if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper)) {
			result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
		}else{
			if (channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("WX_SM")) {
				String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.GET);
				if (StringUtils.isBlank(resultStr)) {
					log.error("[瑞云]3.1.发送支付请求，获取支付请求返回值异常:返回空");
					throw new PayException("返回空");
				}
				if (!resultStr.contains("form")) {
					log.error("[瑞云]3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
					throw new PayException(resultStr);
				}
				result.put("第三方返回1", resultStr);
				Document document = Jsoup.parse(resultStr);  //Jsoup.parseBodyFragment(html)
				Element bodyEl = document.getElementsByTag("body").first();
				Element formEl = bodyEl.getElementsByTag("form").first();
				Map<String, String> secondPayParam = HandlerUtil.parseFormElement(formEl);
				resultStr = RestTemplateUtil.sendByRestTemplateRedirect("http://pay.hhrwlb.top/"+secondPayParam.get("action"), secondPayParam, String.class, HttpMethod.GET);
				if (StringUtils.isBlank(resultStr)) {
					log.error("[瑞云]3.1.发送支付请求，获取支付请求返回值异常:返回空");
					throw new PayException("返回空");
				}
				document = Jsoup.parse(resultStr);
				bodyEl = document.getElementsByTag("body").first();
				result.put(QRCONTEXT,  bodyEl.getElementById("hidUrl").attr("value"));
				result.put("第三方返回2", resultStr);
			}else{
				String decodeByUrl = null;
				try {
					decodeByUrl = QRCodeUtil.decodeByUrl(HttpUtil.getURLWithParam(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam));
				} catch (Exception e) {
					log.error("[瑞云]3.解析二维码，及获取支付请求结果：" + decodeByUrl + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
					throw new PayException(JSON.toJSONString(payParam));
				}
				result.put(QRCONTEXT, decodeByUrl);
				result.put("第三方返回1", decodeByUrl);
			}
		}
		payResultList.add(result);
		log.debug("[瑞云]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
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
        log.debug("[瑞云]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
	
}