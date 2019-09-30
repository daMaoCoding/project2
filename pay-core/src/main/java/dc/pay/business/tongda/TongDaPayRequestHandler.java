package dc.pay.business.tongda;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.UnicodeUtil;
import dc.pay.utils.ValidateUtil;

/**
 *
 * 
 * @author kevin
 * AUG 20, 2018
 */
@RequestPayHandler("TONGDA")
public final class TongDaPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(TongDaPayRequestHandler.class);

    private static final String      pay_memberid	  	  = "pay_memberid";                         
    private static final String      pay_orderid	  	  = "pay_orderid";                       
    private static final String      pay_amount	  		  = "pay_amount";                       
    private static final String      pay_applydate	      = "pay_applydate";                         
    private static final String      pay_notifyurl	      = "pay_notifyurl";                         
    private static final String      pay_callbackurl	  = "pay_callbackurl";                         
    private static final String      pay_bankcode	  	  = "pay_bankcode";                        
    private static final String      pay_bankname	      = "pay_bankname";                          
    private static final String      jrpayname	  	  	  = "jrpayname";                       
    private static final String      pay_md5sign	      = "pay_md5sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
            	put(pay_memberid,channelWrapper.getAPI_MEMBERID());
                put(pay_orderid,channelWrapper.getAPI_ORDER_ID());
                put(pay_amount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(pay_applydate,HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "yyyy-MM-dd HH:mm:ss"));
                put(pay_notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(pay_callbackurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(jrpayname,"json");
                put(pay_bankcode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());	
            }
        };
        log.debug("[通达]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if(StringUtils.isBlank(api_response_params.get(paramKeys.get(i))))  
                continue;
            sb.append(paramKeys.get(i)).append("=>").append(api_response_params.get(paramKeys.get(i))).append("&");
            
        }
        sb.append("key=" + channelWrapper.getAPI_KEY());
        String signStr = sb.toString();
        String pay_md5sign = HandlerUtil.getMD5UpperCase(signStr);
        log.debug("[通达]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        Map<String,String> result = Maps.newHashMap();
        try {
        	String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
        	
        	if (StringUtils.isBlank(resultStr)) {
                log.error("[通达]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(payParam));
                throw new PayException("返回空,参数："+JSON.toJSONString(payParam));
            }
            JSONObject resJson = JSON.parseObject(resultStr);
            //只取正确的值，其他情况抛出异常
            if(null !=resJson && resJson.containsKey("retCode") && "10000".equalsIgnoreCase(resJson.getString("retCode")) && resJson.containsKey("payurl")){
                result.put(JUMPURL, HandlerUtil.UrlDecode(resJson.getString("payurl")));
            }else {
            	String unicodeToString = UnicodeUtil.unicodeToString(resultStr);
            	log.error("[通达]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(unicodeToString) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            	throw new PayException(unicodeToString);
            }
        } catch (Exception e) {
        	log.error("[通达]-[请求支付]-3.3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		payResultList.add(result);
        log.debug("[通达]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[通达]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }

}