package dc.pay.business.bbfu;

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
import dc.pay.utils.ValidateUtil;

/**
 *
 * 
 * @author kevin
 * Jul 25, 2018
 */
@RequestPayHandler("BBFU")
public final class BbFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(BbFuPayRequestHandler.class);

    private static final String      pay_memberid	  	  = "pay_memberid";                         
    private static final String      pay_orderid	  	  = "pay_orderid";                       
    private static final String      pay_amount	  		  = "pay_amount";                       
    private static final String      pay_applydate	      = "pay_applydate";                         
    private static final String      pay_notifyurl	      = "pay_notifyurl";                         
    private static final String      pay_callbackurl	  = "pay_callbackurl";                         
    private static final String      pay_bankcode	  	  = "pay_bankcode";                        
    private static final String      pay_productname	  = "pay_productname";                       
    private static final String      pay_attach	  	  	  = "pay_attach";                         
    private static final String      pay_bankname	      = "pay_bankname";                          
    private static final String      return_type	  	  = "return_type";                       
    private static final String      client_ip  		  = "client_ip";                    
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
                put(pay_productname,"PAY-D");
                put(return_type,"code");
                put(client_ip,channelWrapper.getAPI_Client_IP());
                if(HandlerUtil.isWY(channelWrapper)) {
                	put(pay_bankcode,"907");
                	put(pay_bankname,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                }else {
                	put(pay_bankcode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());	
                }
            }
        };
        log.debug("[BB付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
    	//log.debug("api_response_params========>"+api_response_params);
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if(StringUtils.isBlank(api_response_params.get(paramKeys.get(i))) || pay_productname.equalsIgnoreCase(paramKeys.get(i).toString()) || pay_bankname.equalsIgnoreCase(paramKeys.get(i).toString()) || return_type.equalsIgnoreCase(paramKeys.get(i).toString()) || client_ip.equalsIgnoreCase(paramKeys.get(i).toString()) )  
                continue;
            sb.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            
        }
        sb.append("key=" + channelWrapper.getAPI_KEY());
        String signStr = sb.toString();
        //log.debug("signStr========>"+signStr);
        String pay_md5sign = HandlerUtil.getMD5UpperCase(signStr);
        log.debug("[BB付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        Map result = Maps.newHashMap();
        try {
            if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            }else{
            	String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
                if (StringUtils.isBlank(resultStr)) {
                    log.error("[BB付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(payParam));
                    throw new PayException("返回空,参数："+JSON.toJSONString(payParam));
                }
                //System.out.println("请求返回=========>"+resultStr);
                JSONObject resJson = JSON.parseObject(resultStr);
                //只取正确的值，其他情况抛出异常
                if(null !=resJson && resJson.containsKey("returnCode") && "00".equalsIgnoreCase(resJson.getString("returnCode")) && resJson.containsKey("code_url")){
                    result.put(QRCONTEXT, HandlerUtil.UrlDecode(resJson.getString("code_url")));
                }else {
                	log.error("[BB付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                	throw new PayException(resultStr);
                }
            }
        } catch (Exception e) {
        	log.error("[BB付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		payResultList.add(result);
        log.debug("[BB付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[BB付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }

}