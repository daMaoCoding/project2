package dc.pay.business.xpzhifu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

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
import dc.pay.utils.UnicodeUtil;
import dc.pay.utils.ValidateUtil;


/**
 * 
 * 
 * @author sunny
 * Dec 18, 2018
 */
@RequestPayHandler("XPZHIFU")
public final class XPZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XPZhiFuPayRequestHandler.class);

//    字段名				变量名			类型				说明					必填
//    商户id				appId			Sting			平台分配给商户的id		是
//    当前时间戳			time			Int				当前北京时间的时间戳10位数	是
//    签名值				sign			Sting			请求的数据的签名		是
//    业务数据			data			String			业务相关的数据	是
    
//    字段名				变量名			类型				说明					必填
//    支付商				payProvider		int				1：支付宝				是
//    订单金额			amount			Float			单位元，精确到元保留两位小数，如105.00、111.00	是
//    交易标题			subject			Sting			支付订单的标题	是
//    商户单号			orderId			String			商户订单号，必须唯一	是
//    支付成功后回调地址	notifyUrl		string			包含http://的地址，支付成功发送回调信息到该地址	是
//    支付成功跳转的地址	returnUrl		string			该参数预留，暂时未开通 包含http://的地址，支付成功后跳转到该地址	否

    private static final String appId               ="appId";
    private static final String time           		="time";
    private static final String data           		="data";
    private static final String sign                ="sign";
    
    private static final String payProvider         ="payProvider";
    private static final String amount         		="amount";
    private static final String subject         	="subject";
    private static final String orderId         	="orderId";
    private static final String notifyUrl         	="notifyUrl";
    private static final String returnUrl         	="returnUrl";
    
    private static final String key                 ="key";
    


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
    	Map<String, String> payParam = new TreeMap<String, String>() {
    		{
    			put(payProvider, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
    			put(amount, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
    			put(subject, channelWrapper.getAPI_ORDER_ID());
    			put(orderId, channelWrapper.getAPI_ORDER_ID());
    			put(notifyUrl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
    			put(returnUrl, channelWrapper.getAPI_WEB_URL());
    		}
    	};
        Map<String, String> dataParam = new TreeMap<String, String>() {
            {
                put(appId, channelWrapper.getAPI_MEMBERID());
                put(time,System.currentTimeMillis()/1000+"");
                put(data,HandlerUtil.mapToJson(payParam));
            }
        };
        log.debug("[XP支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return dataParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
    	String signSrc=String.format("%s|%s|%s|%s", 
    			api_response_params.get(appId),
    			api_response_params.get(data),
    			api_response_params.get(time),
    			channelWrapper.getAPI_KEY()
    			);
        String paramsStr = signSrc.toString();
        String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[XP支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return signMD5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        if (1==2&&(HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)  ||  HandlerUtil.isWapOrApp(channelWrapper))) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());  //.replace("method='post'","method='get'"));
            payResultList.add(result);
        }else{
	        String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,null);
	        if (StringUtils.isBlank(resultStr)) {
	            log.error("[XP支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(EMPTYRESPONSE);
	        }
	        resultStr = UnicodeUtil.unicodeToString(resultStr);
	        resultStr=resultStr.replaceAll("\\\\", "");
	        if (!resultStr.contains("{") || !resultStr.contains("}")) {
	            log.error("[XP支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        resultStr=resultStr.replace("\"{", "{").replace("}\"", "}");
	        JSONObject resJson;
	        try {
	            resJson = JSONObject.parseObject(resultStr);
	        } catch (Exception e) {
	            e.printStackTrace();
	            log.error("[XP支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        if (null != resJson && resJson.containsKey("code") && resJson.getString("code").equals("success")) {
	        	JSONObject code_url = resJson.getJSONObject("data");
	            result.put(JUMPURL , code_url.getString("payUrl"));
	        }else {
	            log.error("[XP支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        payResultList.add(result);
        }
        log.debug("[XP支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[XP支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}