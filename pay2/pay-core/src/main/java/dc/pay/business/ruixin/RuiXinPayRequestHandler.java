package dc.pay.business.ruixin;

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
 *
 * 
 * @author kevin
 * Jul 24, 2018
 */
@RequestPayHandler("RUIXIN")
public final class RuiXinPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(RuiXinPayRequestHandler.class);

    private static final String      parter	  	  	= "parter";                         
    private static final String      type	  		= "type";                       
    private static final String      value	  		= "value";                       
    private static final String      orderid	    = "orderid";                        
    private static final String      callbackurl	= "callbackurl";                         
    private static final String      payerIp	    = "payerIp";                        
    private static final String      attach	  		= "attach";                       
    private static final String      sign	  	    = "sign";                         

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
            	put(parter,channelWrapper.getAPI_MEMBERID());
                put(type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(value,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(orderid,channelWrapper.getAPI_ORDER_ID());
                put(callbackurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            }
        };
        log.debug("[瑞信]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
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
	    log.debug("[瑞信]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
	    return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        Map result = Maps.newHashMap();
        //System.out.println("请求地址=========>"+channelWrapper.getAPI_CHANNEL_BANK_URL());
    	//System.out.println("请求参数=========>"+JSON.toJSONString(payParam));
        try {
        	if (HandlerUtil.isWapOrApp(channelWrapper)||channelWrapper.getAPI_CHANNEL_BANK_NAME().endsWith("_WEBWAPAPP_ZFB_SM")) {
    			result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
    		}else{
				String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.GET);
				//log.debug("resultStr==========>"+resultStr);
				if (StringUtils.isBlank(resultStr)) {
					log.error("[瑞信]-[请求支付]-3.1第一次发送支付请求，获取支付请求返回值异常:返回空");
					throw new PayException("返回空,参数："+JSON.toJSONString(payParam));
				}
				if (!resultStr.contains("form")) {
					log.error("[瑞信]-[请求支付]-3.2第一次发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
					throw new PayException(resultStr);
				}
				result.put("第三方返回1", resultStr);
				Document document = Jsoup.parse(resultStr);  //Jsoup.parseBodyFragment(html)
				Element bodyEl = document.getElementsByTag("body").first();
				Element formEl = bodyEl.getElementsByTag("form").first();
				Map<String, String> secondPayParam = HandlerUtil.parseFormElement(formEl);
				//log.debug("secondPayParam==========>"+secondPayParam);
				resultStr = RestTemplateUtil.sendByRestTemplateRedirect(secondPayParam.get("action"), secondPayParam, String.class, HttpMethod.GET);
				//log.debug("secondResult========>"+resultStr);
				if (StringUtils.isBlank(resultStr)) {
					log.error("[瑞信]-[请求支付]-3.3第二次发送支付请求，获取支付请求返回值异常:返回空");
					throw new PayException("返回空,参数："+JSON.toJSONString(payParam));
				}
				document = Jsoup.parse(resultStr);
				bodyEl = document.getElementsByTag("body").first();
				if(StringUtils.isBlank(bodyEl.getElementById("hidUrl").attr("value"))) {
					log.error("[瑞信]-[请求支付]-3.4第二次发送支付请求，获取hidUrl值为空");
					throw new PayException(resultStr);
				}
				result.put(QRCONTEXT,  bodyEl.getElementById("hidUrl").attr("value"));
				result.put("第三方返回2", resultStr);
    		}
        } catch (Exception e) {
        	log.error("[瑞信]-[请求支付]-3.5.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null,参数："+JSON.toJSONString(payParam),e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		payResultList.add(result);
        log.debug("[瑞信]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
        return payResultList;
    }

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
        log.debug("[瑞信]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }

}