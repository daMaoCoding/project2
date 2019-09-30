package dc.pay.business.baishengzhifu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
 * @author sunny
 *  01 12, 2019
 */
@RequestPayHandler("BAISHENGZHIFU")
public final class BaiShengZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(BaiShengZhiFuPayRequestHandler.class);

//    变量名				域名						说明
//    transactionId		商户订单号				字符串，只允许使用字母、数字、- 、_,并以字母或数字开头，每商户提交的订单号，必须在自身账户交易中唯一
//    orderAmount		商户订单金额				浮点数DECIMAL(10,2)；以元为单位，例如10元，金额格式为10.00
//    payType			支付方式					0301：银联扫码 0215：支付宝 0103：微信
//    mch_create_ip		用户请求Ip				获取用户的实际请求地址，不参与签名
//    bgUrl				支付结果后台通知地址		在线支付平台支持后台通知时必填
//    pageUrl			页面回跳地址				成功支付后页面回调地址
//    bank_code			银行代码					银行代码，不参与签名	
//    signData			签名						签名信息
    private static final String transactionId             ="transactionId";
    private static final String orderAmount               ="orderAmount";
    private static final String payType                   ="payType";
    private static final String mch_create_ip             ="mch_create_ip";
    private static final String bgUrl                     ="bgUrl";
    private static final String pageUrl                   ="pageUrl";
    
    private static final String version                   ="version";
    private static final String merId                     ="merId";
    private static final String orderTime                 ="orderTime";
    private static final String transCode                 ="transCode";
    private static final String signType                  ="signType";
    private static final String charse                    ="charse";
    
    private static final String signData                  ="signData";
    private static final String key                  ="key";
    

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(transactionId,channelWrapper.getAPI_ORDER_ID());
                put(orderAmount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(bgUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(payType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(mch_create_ip,channelWrapper.getAPI_Client_IP());
                put(pageUrl, channelWrapper.getAPI_WEB_URL());
                put(version, "1.0");
                put(merId, channelWrapper.getAPI_MEMBERID());
                put(orderTime, HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "yyyyMMddHHmmss"));
                put(transCode, "T02002");
                put(signType, "MD5");
                put(charse, "UTF-8");
            }
        };
        log.debug("[百胜支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
            	signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        signSrc.append(key+"="+channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 =HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[百胜支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        if (1==2&&(HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)  ||  HandlerUtil.isWapOrApp(channelWrapper))) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());  //.replace("method='post'","method='get'"));
            payResultList.add(result);
        }else{
	        String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, "UTF-8");
	        if (StringUtils.isBlank(resultStr)) {
	            log.error("[百胜支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        resultStr = UnicodeUtil.unicodeToString(resultStr);
	        if (!resultStr.contains("{") || !resultStr.contains("}")) {
	            log.error("[百胜支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        JSONObject resJson;
	        try {
	            resJson = JSONObject.parseObject(resultStr);
	        } catch (Exception e) {
	            e.printStackTrace();
	            log.error("[百胜支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        if (null != resJson && resJson.containsKey("retCode") && resJson.getString("retCode").equals("RC0002")) {
	            String code_url = resJson.getString("qrCodeVal");
	            result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
	        }else {
	            log.error("[百胜支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        payResultList.add(result);
        }
        log.debug("[百胜支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[百胜支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}