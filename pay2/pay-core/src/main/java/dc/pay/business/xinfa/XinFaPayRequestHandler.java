package dc.pay.business.xinfa;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

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
 * Aug 17, 2018
 */
@RequestPayHandler("XINFA")
public final class XinFaPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XinFaPayRequestHandler.class);

    private static final String      version	  	  = "version";                         
    private static final String      merchNo	  	  = "merchNo";                       
    private static final String      payType	      = "payType";                       
    private static final String      randomNum	      = "randomNum";                         
    private static final String      orderNo	      = "orderNo";                         
    private static final String      amount	          = "amount";                         
    private static final String      goodsName	  	  = "goodsName";                        
    private static final String      notifyUrl	      = "notifyUrl";                       
    private static final String      notifyViewUrl	  = "notifyViewUrl";                         
    private static final String      charsetCode	  = "charsetCode";                          
    
    @Override
    protected Map<String, String> buildPayParam() throws PayException {
    	if(!channelWrapper.getAPI_MEMBERID().contains("&")){
            throw new PayException("商户号格式错误：商户号&MD5秘钥");
        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
            	put(version,"V3.3.0.0");
            	put(merchNo,channelWrapper.getAPI_MEMBERID().split("&")[0]);
            	put(payType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            	put(randomNum,HandlerUtil.getRandomStr(4));
                put(orderNo,channelWrapper.getAPI_ORDER_ID());
                put(amount,channelWrapper.getAPI_AMOUNT());
                put(goodsName,"GOODS");
                put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(notifyViewUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(charsetCode,"UTF-8");
            }
        };
        log.debug("[鑫发]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
    	String metaSignJsonStr = HandlerUtil.mapToJson(api_response_params);
        String pay_md5sign = XinFaUtil.MD5(metaSignJsonStr + channelWrapper.getAPI_MEMBERID().split("&")[1], XinFaUtil.CHARSET);// 32位
        log.debug("[鑫发]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
    	payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        Map<String,String> result = Maps.newHashMap();
        try {
            byte[] dataStr = XinFaUtil.encryptByPublicKey(HandlerUtil.mapToJson(payParam).getBytes(XinFaUtil.CHARSET),channelWrapper.getAPI_PUBLIC_KEY());
            String param = java.util.Base64.getEncoder().encodeToString(dataStr);
            String reqParam = "data=" + URLEncoder.encode(param, XinFaUtil.CHARSET) + "&merchNo=" + channelWrapper.getAPI_MEMBERID().split("&")[0];
            String contents = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), reqParam,MediaType.APPLICATION_FORM_URLENCODED_VALUE).trim();
            contents = new String(contents.getBytes("ISO-8859-1"), "UTF-8");
            // 检查状态
            net.sf.json.JSONObject resultJsonObj = net.sf.json.JSONObject.fromObject(contents);
            String stateCode = resultJsonObj.getString("stateCode");
            if (stateCode.equals("00")) {
                String resultSign = resultJsonObj.getString("sign");
                resultJsonObj.remove("sign");
                String targetString = XinFaUtil.MD5(resultJsonObj.toString() + channelWrapper.getAPI_MEMBERID().split("&")[1], XinFaUtil.CHARSET);
                
                if (targetString.equals(resultSign) && StringUtils.isNotBlank(resultJsonObj.getString("qrcodeUrl"))) {
                    result.put(handlerUtil.isWEBWAPAPP_SM(channelWrapper) ? QRCONTEXT : JUMPURL, resultJsonObj.getString("qrcodeUrl"));
                }else {
                    log.error("[鑫发]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(contents) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(contents);
                }
            
//                if (targetString.equals(resultSign)) {
//                    if(HandlerUtil.isWapOrApp(channelWrapper)){
//                        result.put(JUMPURL, resultJsonObj.getString("qrcodeUrl"));
//
//                    }else{
//                        result.put(QRCONTEXT, resultJsonObj.getString("qrcodeUrl"));
//                    }
//                }
            }else {
            	log.error("[鑫发]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(contents) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(contents);
            }
        } catch (Exception e) {
        	log.error("[鑫发]-[请求支付]-3.2.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
    	
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		payResultList.add(result);
        log.debug("[鑫发]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[鑫发]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }

}


