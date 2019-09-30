package dc.pay.business.tejiefu;

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
import dc.pay.utils.DateUtil;
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
@RequestPayHandler("TEJIEFU")
public final class TeJieFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(TeJieFuPayRequestHandler.class);

//    参数值					参数名			类型				是否必填			说明
//    version				版本号			String			Y				固定值 1.0
//    sign_type				签名方式			String			Y				固定值MD5，不参与签名
//    sign					签名				String			Y				不参与签名
//    mer_no				商户代码			String			Y				平台分配唯一
//    back_url				后台通知地址		String			N	
//    mer_order_no			商家订单号		String			Y				保证每笔订单唯一
//    gateway_type			支付类型			String			Y				001、微信wap; 
//    currency				交易币种			String			Y				固定值156
//    trade_amount			交易金额			String			Y				整数，以元为单位，不允许有小数点
//    order_date			订单时间			String			Y				时间格式：yyyy-MM-dd HH:mm:ss
//    client_ip				客户端ip			String			Y				交易请求IP地址
//    goods_name			商品名称			String			Y				不超过50字节

    private static final String version               	="version";
    private static final String sign_type           	="sign_type";
    private static final String sign           			="sign";
    private static final String mer_no           		="mer_no";
    private static final String back_url          		="back_url";
    private static final String mer_return_msg          ="mer_return_msg";
    private static final String mer_order_no            ="mer_order_no";
    private static final String gateway_type            ="gateway_type";
    private static final String currency            	="currency";
    private static final String trade_msg               ="trade_msg";
    private static final String trade_amount            ="trade_amount";
    private static final String order_date              ="order_date";
    private static final String client_ip               ="client_ip";
    private static final String goods_name              ="goods_name";
    
    private static final String key              		="key";
    
    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(mer_no, channelWrapper.getAPI_MEMBERID());
                put(mer_order_no,channelWrapper.getAPI_ORDER_ID());
                put(trade_amount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(back_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(gateway_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(goods_name,channelWrapper.getAPI_ORDER_ID());
                put(client_ip,channelWrapper.getAPI_Client_IP());
                put(order_date,DateUtil.formatDateTimeStrByParam("yyyy-MM-dd HH:mm:ss"));
                put(version,"1.0");
                put(sign_type,"MD5");
                put(currency,"156");
            }
        };
        log.debug("[特捷付支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        paramKeys.remove(sign_type);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
//            if (!pay_productname.equals(paramKeys.get(i)) && !pay_attach.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
            if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
            	signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        //最后一个&转换成#
        //signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "#" );
        signSrc.append(key+"="+channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[特捷付支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
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
	            log.error("[特捷付支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(EMPTYRESPONSE);
	        }
	        resultStr = UnicodeUtil.unicodeToString(resultStr);
	        resultStr=resultStr.replaceAll("\\\\", "");
	        if (!resultStr.contains("{") || !resultStr.contains("}")) {
	            log.error("[特捷付支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        JSONObject resJson;
	        try {
	            resJson = JSONObject.parseObject(resultStr);
	        } catch (Exception e) {
	            e.printStackTrace();
	            log.error("[特捷付支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        if (null != resJson && resJson.containsKey("tradeResult") &&"1".equals(resJson.getString("tradeResult"))) {
	        	if(HandlerUtil.isYLSM(channelWrapper)||HandlerUtil.isJDSM(channelWrapper)|| HandlerUtil.isWxSM(channelWrapper)){
	        		String code_url = resJson.getString("payInfo");
		            result.put(QRCONTEXT,code_url);
	        	}else{
	        		String code_url = resJson.getString("payInfo");
		            result.put(JUMPURL,code_url);
	        	}
	            
	        }else {
	            log.error("[特捷付支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        payResultList.add(result);
        }
        log.debug("[特捷付支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[特捷付支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}