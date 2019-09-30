package dc.pay.business.chaofan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
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
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.UnicodeUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 
 * 
 * @author sunny
 * Dec 18, 2018
 */
@RequestPayHandler("CHAOFAN")
public final class ChaoFanPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(ChaoFanPayRequestHandler.class);

    //参数名称               参数含义            是否必填          参数说明
    //service	         接口类型	             是         接口类型，参考3.1接口类型说明
    //version	          版本号	 	    是	        扫码参考下方说明。其他固定值：1.0
    //merchantId	商户ID		    是	        商户在中心的唯一标识
    //orderNo	      商户订单号		    是	        提交的订单号在商户系统中必须唯一
    //tradeDate	    商户交易日期		    是	        商户交易日期，格式：yyyyMMdd
    //tradeTime	    商户交易时间		   是	       商户交易时间，格式：HHmmss
    //amount	   订单金额		   是	      订单金额 （单位：分）
    //clientIp	  客户端IP		            是
    //notifyUrl	商户接收后台返回结果的地址		是	交易成功后，向该网址发送三次成功通知。

    private static final String service                ="service";
    private static final String version                ="version";
    private static final String merchantId             ="merchantId";
    private static final String orderNo                ="orderNo";
    private static final String tradeDate              ="tradeDate";
    private static final String tradeTime              ="tradeTime";
    private static final String amount                 ="amount";
    private static final String clientIp               ="clientIp";
    private static final String notifyUrl              ="notifyUrl";
    private static final String bankCode               ="bankCode";
//    private static final String pay_productdesc             ="pay_productdesc";
//    private static final String pay_producturl              ="pay_producturl";
//    private static final String pay_md5sign                 ="pay_md5sign";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merchantId, channelWrapper.getAPI_MEMBERID());
                put(orderNo,channelWrapper.getAPI_ORDER_ID());
                put(tradeDate, DateUtil.formatDateTimeStrByParam("yyyyMMdd"));
                put(tradeTime, DateUtil.formatDateTimeStrByParam("HHmmss"));
                put(amount,channelWrapper.getAPI_AMOUNT());
                put(clientIp,channelWrapper.getAPI_Client_IP());
                if(handlerUtil.isWxSM(channelWrapper)){
                	put(version,"1.1");
                }else{
                	 put(version,"1.0");
                }
                put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                if(handlerUtil.isWY(channelWrapper)){
                	put(service,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
                	put(bankCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
                }else{
                	 put(service,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                }
            }
        };
        log.debug("[超凡支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
    	api_response_params.put(key, channelWrapper.getAPI_KEY());
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
//            if (!pay_productname.equals(paramKeys.get(i)) && !pay_attach.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
            if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        //最后一个&转换成#
        //signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "#" );
        //signSrc.append(key +"="+ channelWrapper.getAPI_KEY());
        signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "" );
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[超凡支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        HashMap<String, String> retMap = Maps.newHashMap();
//        String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
//        (String url, Map<String,T> params, Class<T> var, HttpMethod method) {
        String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,String.class,HttpMethod.POST);
        
        if (StringUtils.isBlank(resultStr)) {
            log.error("[超凡支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        resultStr = UnicodeUtil.unicodeToString(resultStr);
        
        String[] pairs = resultStr.split("&");
        for (String pair : pairs) {
            final int index = pair.indexOf("=");
            final String key = index > 0 ? pair.substring(0, index) : pair;
            final String value = index > 0 && pair.length() > index + 1 ? pair.substring(index + 1) : null;
            retMap.put(key, value);
        }
        if(retMap.isEmpty()||!retMap.containsKey("repCode")||!retMap.get("repCode").equals("0001")){
        	log.error("[超凡支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
//        result.put(handlerUtil.isWapOrApp(channelWrapper)||handlerUtil.isWY(channelWrapper)||handlerUtil.isYLKJ(channelWrapper)? JUMPURL : QRCONTEXT, retMap.get("resultUrl"));
        result.put(JUMPURL, retMap.get("resultUrl"));
        //result.put(JUMPURL, result.get("resultUrl"));
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[超凡支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[超凡支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}