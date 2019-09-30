package dc.pay.business.jinhongzhifu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

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
import dc.pay.utils.Sha1Util;
import dc.pay.utils.UnicodeUtil;
import dc.pay.utils.ValidateUtil;
import dc.pay.utils.qr.QRCodeUtil;


/**
 * @author sunny
 * 02 23, 2019
 */
@RequestPayHandler("JINHONGZHIFU")
public final class JinHongZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(JinHongZhiFuPayRequestHandler.class);

//    参数名称				是否必填				参数含义							参数类型
//    partner_no			是					商户号或客户号						String
//    mch_order_no			是					商户或客户订单号( 32个字符以内 )		String( 32 )
//    body					是					商品名称( 50个字以内 )				String( 50 )
//    money					是					订单金额, 单位`分`					Int
//    callback_url			否					商户支付回调						String( 255 )
//    return_url			否					付款页面订单支付成功后的跳转地址		String( 255 )
//    time_stamp			是					13位的时间戳，单位`毫秒`			Int( 13 )
//    token					是					32位的验签参数，生成规则见1. 1		String( 32 )
//    code_type				是					支付方式，1=微信2=支付宝			Int

    private static final String partner_no               	="partner_no";
    private static final String mch_order_no           		="mch_order_no";
    private static final String body           				="body";
    private static final String money           			="money";
    private static final String callback_url          		="callback_url";
    private static final String return_url              	="return_url";
    private static final String time_stamp            		="time_stamp";
    private static final String token           			="token";
    private static final String code_type            		="code_type";
    
    private static final String key            				="key";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(partner_no, channelWrapper.getAPI_MEMBERID());
                put(mch_order_no,channelWrapper.getAPI_ORDER_ID());
                put(money,channelWrapper.getAPI_AMOUNT());
                put(callback_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(code_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(return_url,channelWrapper.getAPI_WEB_URL());
                put(time_stamp,System.currentTimeMillis()+"");
                put(body,channelWrapper.getAPI_ORDER_ID());
            }
        };
        log.debug("[金虬支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
    	String signSrc=String.format("%s%s%s%s%s",
    			mch_order_no+"="+api_response_params.get(mch_order_no)+"&",
    			money+"="+api_response_params.get(money)+"&",
    			partner_no+"="+api_response_params.get(partner_no)+"&",
    			time_stamp+"="+api_response_params.get(time_stamp)+"&",
    			key+"="+channelWrapper.getAPI_KEY().split("-")[0]
    			);
        String paramsStr = signSrc.toString();
        String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[金虬支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
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
        	payParam.put("action", "pay");
        	payParam.put("m", "pay_it");
	        String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.GET);
	        if (StringUtils.isBlank(resultStr)) {
	            log.error("[金虬支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(EMPTYRESPONSE);
	        }
	        resultStr = UnicodeUtil.unicodeToString(resultStr);
	        resultStr=resultStr.replaceAll("\\\\", "");
	        if (!resultStr.contains("{") || !resultStr.contains("}")) {
	            log.error("[金虬支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        JSONObject resJson;
	        try {
	            resJson = JSONObject.parseObject(resultStr);
	        } catch (Exception e) {
	            e.printStackTrace();
	            log.error("[金虬支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        if (null != resJson && resJson.containsKey("code") && resJson.getString("code").equals("0")) {
	        	/*if(handlerUtil.isWapOrApp(channelWrapper)){
	        		String code_link = resJson.getString("code_link");
		            result.put(JUMPURL, code_link);
	        	}else if(handlerUtil.isZfbSM(channelWrapper)){
	        		String code_img_url = resJson.getString("code_img_url");
		            result.put(QRCONTEXT, QRCodeUtil.decodeByUrl(code_img_url));
	        	}*/
	        	String code_link = resJson.getString("code_link");
	            result.put(JUMPURL, code_link);
	            
	        }else {
	            log.error("[金虬支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        payResultList.add(result);
        }
        log.debug("[金虬支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[金虬支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}