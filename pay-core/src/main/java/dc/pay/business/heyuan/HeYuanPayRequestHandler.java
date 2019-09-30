package dc.pay.business.heyuan;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.codec.digest.DigestUtils;
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
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.UnicodeUtil;
import dc.pay.utils.ValidateUtil;


/**
 * 
 * 
 * @author sunny
 * Dec 25, 2018
 */
@RequestPayHandler("HEYUAN")
public final class HeYuanPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(HeYuanPayRequestHandler.class);

//    参数名称			变量名				类型长度			是否可空		说明
//    版本号			version					varchar(5)		默认1.0.0
//    商户终端号		merchantNum				Varchar(32)		商户进件时返回的商户终端号
//    机构订单号		orgOrderNo				varchar(32)		
//    通道序号		aisleNo					varchar(2)		具体见附录（通道序号）
//    订单支付金额(单位分)	payAmount			int		
//    商品名称		productName				Varchar(40)		
//    支付者id		userId					varchar(20)		
//    异步通知地址	notifyUrl				varchar(50)		



    private static final String version                       ="version";
    private static final String merchantNum               	  ="merchantNum";
    private static final String orgOrderNo                    ="orgOrderNo";
    private static final String aisleNo           		      ="aisleNo";
    private static final String payAmount                     ="payAmount";
    private static final String productName                   ="productName";
    private static final String userId                        ="userId";
    private static final String notifyUrl                     ="notifyUrl";
    
    
    private static final String encryptData                   ="encryptData";
    private static final String partnerNo                     ="partnerNo";
    private static final String sign                          ="sign";
    private static final String requestId                     ="requestId";
    

    

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
    	if(!channelWrapper.getAPI_MEMBERID().contains("&")){
            throw new PayException("商户号格式错误，正确格式请使用&符号链接商户账户和appId,如：商户号&merchantNum&partnerNo");
        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(version, "1.0.0");
                put(merchantNum,channelWrapper.getAPI_MEMBERID().split("&")[1]);
                put(orgOrderNo,channelWrapper.getAPI_ORDER_ID());
                put(aisleNo,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(payAmount,channelWrapper.getAPI_AMOUNT());
                put(productName,channelWrapper.getAPI_ORDER_ID());
                put(userId,"");
                put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                //put(app_secret,channelWrapper.getAPI_KEY());
            }
        };
        log.debug("[合源]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        String jsonStr=HandlerUtil.simpleMapToJsonStr(api_response_params);
        String apiKey=channelWrapper.getAPI_KEY().substring(16);
        String paramsStr=jsonStr+apiKey;
        String signSha1 = DigestUtils.sha1Hex(paramsStr);
        log.debug("[合源]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signSha1));
        return signSha1;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
    	Map<String, String> postParam = new TreeMap<String, String>();
    	//encryptData 加密
    	String dataKey = channelWrapper.getAPI_KEY().substring(0,16);
    	String encryptData1 = Base64.encode(AES.encode(HandlerUtil.simpleMapToJsonStr(payParam), dataKey));
    	postParam.put(encryptData, encryptData1);
    	postParam.put(partnerNo, channelWrapper.getAPI_MEMBERID().split("&")[2]);
    	postParam.put(requestId, new SimpleDateFormat("yyyyMMddHHmmssS").format(new Date()));
    	postParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(),HandlerUtil.simpleMapToJsonStr(postParam));
        //返回结果解密
        Map<String, String> resultMap = HandlerUtil.jsonToMap(resultStr);
        String decodeBody ="";
        try {		
        	decodeBody = AesSignUtil.decrypt(channelWrapper.getAPI_KEY(),resultMap.get("encryptData"),resultMap.get("sign"));
		} catch (Exception e1) {
			log.error(e1.getMessage());
			log.error("[合源]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
		}
        if (StringUtils.isBlank(resultStr)) {
            log.error("[合源]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        resultStr = UnicodeUtil.unicodeToString(decodeBody);
        if (!resultStr.contains("{") || !resultStr.contains("}")) {
            log.error("[合源]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        JSONObject resJson;
        try {
            resJson = JSONObject.parseObject(resultStr);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[合源]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        if (null != resJson && resJson.containsKey("respCode") && resJson.getString("respCode").equals("000000")) {
        	handlerUtil.saveStrInRedis(postParam.get(partnerNo), channelWrapper.getAPI_KEY(), 6000);
        	if(HandlerUtil.isWapOrApp(channelWrapper)){
        		String payInfo = resJson.getString("payInfo");
                if(StringUtils.isNotBlank(payInfo) && payInfo.contains("<form")){
                	result.put(HTMLCONTEXT, HandlerUtil.UrlDecode(payInfo));
                }
        	}else{
        		String content = resJson.getString("content");
        		result.put(JUMPURL, HandlerUtil.UrlDecode(content));
        	}
            
            
        }else {
            log.error("[合源]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[合源]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[合源]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}