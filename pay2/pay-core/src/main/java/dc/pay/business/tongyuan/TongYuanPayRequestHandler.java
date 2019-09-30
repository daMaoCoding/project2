package dc.pay.business.tongyuan;

import java.util.ArrayList;
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
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Mar 12, 2018
 */
@RequestPayHandler("TONGYUAN")
public final class TongYuanPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(TongYuanPayRequestHandler.class);

    //版本号		version		varchar(5)			默认1.0
    private static final String version  ="version";
	//商户编号		customerid	int(8)				商户后台获取
	private static final String customerid  ="customerid";
	//商户订单号	sdorderno	varchar(20)		
	private static final String sdorderno  ="sdorderno";
	//订单金额		total_fee	decimal(10,2)			精确到小数点后两位，例如10.24
	private static final String total_fee  ="total_fee";
	//支付编号		paytype		varchar(10)			详见附录1
	private static final String paytype  ="paytype";
	//银行编号		bankcode	varchar(10)			网银直连不可为空，其他支付方式可为空	详见附录2
	private static final String bankcode  ="bankcode";
	//异步通知URL	notifyurl	varchar(50)			不能带有任何参数
	private static final String notifyurl  ="notifyurl";
	//同步跳转URL	returnurl	varchar(50)			不能带有任何参数
	private static final String returnurl  ="returnurl";
	//订单备注说明	remark		varchar(50)	Y		可为空
	private static final String remark  ="remark";
	//获取微信二维码	get_code	tinyint(1)	Y		如果只想获取被扫二维码，请设置get_code=1
	private static final String get_code  ="get_code";
		
    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
            	put(version, "1.0");
            	put(customerid, channelWrapper.getAPI_MEMBERID());
            	put(sdorderno,channelWrapper.getAPI_ORDER_ID());
            	put(total_fee,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            	if (HandlerUtil.isWY(channelWrapper)) {
            		put(paytype,"bank");
            		put(bankcode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
				}else {
					put(paytype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
				}
            	put(notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            	put(returnurl,channelWrapper.getAPI_WEB_URL());
            	//如果只想获取被扫二维码，请设置get_code=1
            	put(get_code, "1");
            }
        };
        log.debug("[通源]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map api_response_params) throws PayException {
		//1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
		//2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
    	//{value}要替换成接收到的值，{apikey}要替换成平台分配的接入密钥，可在商户后台获取
    	//version={value}&customerid={value}&total_fee={value}&sdorderno={value}&notifyurl={value}&returnurl={value}&{apikey}
		StringBuffer signSrc= new StringBuffer();
		signSrc.append(version+"=").append(api_response_params.get(version)).append("&");
		signSrc.append(customerid+"=").append(api_response_params.get(customerid)).append("&");
		signSrc.append(total_fee+"=").append(api_response_params.get(total_fee)).append("&");
		signSrc.append(sdorderno+"=").append(api_response_params.get(sdorderno)).append("&");
		signSrc.append(notifyurl+"=").append(api_response_params.get(notifyurl)).append("&");
		signSrc.append(returnurl+"=").append(api_response_params.get(returnurl)).append("&");
		signSrc.append(channelWrapper.getAPI_KEY());
		String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[通源]-[请求支付]-2.生成加密URL签名完成："+JSON.toJSONString(paramsStr));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
		payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map<String,String> result = Maps.newHashMap();
		if(HandlerUtil.isWapOrApp(channelWrapper)){
            StringBuffer htmlContent = HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
            //保存第三方返回值
            result.put(HTMLCONTEXT,htmlContent.toString());
        }else{
        	String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
        	if (StringUtils.isBlank(resultStr)) {
        		log.error("[通源]3.1.发送支付请求，获取支付请求返回值异常:返回空");
        		throw new PayException("第三方返回异常:返回空");
        	}
			if (!resultStr.contains("body")) {
				log.error("[通源]3.2.发送支付请求，获取支付请求返回值异常:"+resultStr);
				throw new PayException(resultStr);
			}
			result.put("第三方返回1",resultStr); //保存全部第三方信息，上面的拆开没必要
        	Document document = Jsoup.parse(resultStr);  //Jsoup.parseBodyFragment(html)
        	Element bodyEl = document.getElementsByTag("body").first();
        	Element formEl = bodyEl.getElementsByTag("a").first();
        	resultStr = RestTemplateUtil.sendByRestTemplate(formEl.attr("href"), payParam, String.class, HttpMethod.GET).trim();
            if (StringUtils.isBlank(resultStr)) {
            	log.error("[通源]3.1.发送支付请求，获取支付请求返回值异常:返回空");
            	throw new PayException("第三方返回异常:返回空");
            }
			if (!resultStr.contains("body")) {
				log.error("[通源]3.2.发送支付请求，获取支付请求返回值异常:"+resultStr);
				throw new PayException(resultStr);
			}
			result.put("第三方返回2",resultStr); //保存全部第三方信息，上面的拆开没必要
            if (channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("_WEBWAPAPP_QQ_SM")) {
            	document = Jsoup.parse(resultStr);  //Jsoup.parseBodyFragment(html)
				bodyEl = document.getElementsByTag("body").first();
				//按不同的请求接口，向不同的属性设置值
				result.put(HandlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, bodyEl.getElementById("hidUrl").attr("value"));
			}
            
//            else {
//				document = Jsoup.parse(resultStr);  //Jsoup.parseBodyFragment(html)
//				bodyEl = document.getElementsByTag("body").first();
//				formEl = bodyEl.getElementsByTag("form").first();
//				Map<String, String> secondPayParam = HandlerUtil.parseFormElement(formEl);
//				resultStr = RestTemplateUtil.postForm(secondPayParam.get("action"), secondPayParam,"UTF-8");
//				if (!resultStr.contains("img")) {
//					log.error("[通源]3.2.发送支付请求，获取支付请求返回值异常:"+resultStr);
//					throw new PayException(resultStr);
//				}
//				document = Jsoup.parse(resultStr);  //Jsoup.parseBodyFragment(html)
//				bodyEl = document.getElementsByTag("body").first();
//				formEl = bodyEl.getElementsByTag("img").first();
//				try {
//					System.out.println(QRCodeUtil.decodeByUrl(HttpUtil.getURLWithParam(HandlerUtil.UrlDecode(formEl.attr("src").toString()), null)));
//				} catch (UnsupportedEncodingException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//				//按不同的请求接口，向不同的属性设置值
//				result.put(HandlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, QRCodeUtil.decodeByUrl(formEl.attr("src").toString()));
//				result.put("第三方返回3",resultStr); //保存全部第三方信息，上面的拆开没必要
//			}
        }
		payResultList.add(result);
        log.debug("[通源]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
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
        log.debug("[通源]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}