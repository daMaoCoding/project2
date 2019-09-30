package dc.pay.business.chengyoukj;

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
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 *
 * 
 * @author beck Aug 06, 2018
 */
@RequestPayHandler("CHENGYOUKJ")
public final class ChengYouKJPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(ChengYouKJPayRequestHandler.class);

    private static final String      pay_memberid	  	  = "pay_memberid";                         
    private static final String      pay_orderid	  	  = "pay_orderid";                       
    private static final String      pay_amount	  		  = "pay_amount";                       
    private static final String      pay_applydate	      = "pay_applydate";                         
    private static final String      pay_notifyurl	      = "pay_notifyurl";                         
    private static final String      pay_callbackurl	  = "pay_callbackurl";                         
    private static final String      pay_bankcode	  	  = "pay_bankcode";                        
    private static final String      pay_productname	  = "pay_productname";                       

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
            	put(pay_memberid,channelWrapper.getAPI_MEMBERID());
                put(pay_orderid,channelWrapper.getAPI_ORDER_ID());
                put(pay_amount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(pay_applydate,DateUtil.getCurDateTime());
                put(pay_notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(pay_callbackurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(pay_productname,"goods");
                put(pay_bankcode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());	
            }
        };
        log.debug("[诚优科技]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        List<String> paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            String keyName = paramKeys.get(i);
            String value = api_response_params.get(keyName);
            
            if(StringUtils.isBlank(value) || pay_productname.equalsIgnoreCase(keyName)) continue;
            sb.append(keyName).append("=").append(value).append("&");
        }
        
        sb.append("key=" + channelWrapper.getAPI_KEY());
        String signStr = sb.toString();
        String pay_md5sign = HandlerUtil.getMD5UpperCase(signStr);
        log.debug("[诚优科技]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        Map<String,String> result = Maps.newHashMap();
        try {
            if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) ||HandlerUtil.isYLKJ(channelWrapper)) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            }else{
            	String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.GET).trim();
                if (StringUtils.isBlank(resultStr)) {
                    log.error("[诚优科技]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(payParam));
                    throw new PayException("返回空,参数："+JSON.toJSONString(payParam));
                }
                
                JSONObject resJson = JSON.parseObject(resultStr);
                if(null !=resJson && resJson.containsKey("pay_code") && resJson.getString("pay_code").equalsIgnoreCase("HL0000") && resJson.containsKey("pay_url")){
                    result.put(QRCONTEXT, HandlerUtil.UrlDecode(resJson.getString("pay_url")));
                }else {
                	log.error("[诚优科技]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr));
                	throw new PayException(resultStr);
                }
            }
        } catch (Exception e) {
        	log.error("[诚优科技]-[请求支付]-3.4.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(e.getMessage());
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		payResultList.add(result);
        log.debug("[诚优科技]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[诚优科技]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }

}