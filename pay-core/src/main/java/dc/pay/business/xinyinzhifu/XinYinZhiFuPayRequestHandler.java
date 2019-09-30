package dc.pay.business.xinyinzhifu;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import dc.pay.utils.RsaUtil;
import dc.pay.utils.ValidateUtil;
import java.util.TreeMap;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;

import org.json.JSONObject;
import com.google.common.collect.Maps;



/**
 * 
 * 
 * @author sunny
 * Dec 18, 2018
 */
@RequestPayHandler("XINYINZHIFU")
public final class XinYinZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XinYinZhiFuPayRequestHandler.class);

//    字段 			说明 			类型 			必填 			备注
//    appId 		商户标 识		String 		是 			您的商户唯一标识，登陆后台系统 在个人中心获得。
//    payMethod 	支付方 式		String 		是			支付方式，固定枚举值。 2001：支付宝 1001:微信
//    notifyUrl 	支付成功回调地址String 	是			用户支付成功后，会做异步通知， 我们服务器会主动发送 post 消 息到这个地址。由调用者自定义。 
//    returnUrl		支付成功跳转地址String 	是			用户支付成功后，可以让用户浏览器自动跳转到这个网址。由调用者 自定义。
//    outTradeNo	商户支付单标识String 		是			系统会据此判别是同一笔订单还是新订单。回调时，会返回这个参数。
//    signType 		签名方 式		String 		是		       使用 MD5 签名。默认值MD5sign 签名后 数据String 是把使用到的所有参数，按参数名字 母升序排序。
//    amount 		订单金 额		String 		是 			单位：元。精确小数点后 2 位。
//    nonceStr 		随机字 符串	String 		是			 随机字符串。
//    timestamp 	时间戳		 String 	是 			当前时间戳。

    private static final String bank_code               ="bank_code";
    private static final String service_type           	="service_type";
    private static final String amount           		="amount";
    private static final String merchant_user           ="merchant_user";
    private static final String risk_level          	="risk_level";
    private static final String merchant_order_no       ="merchant_order_no";
    private static final String platform            	="platform";
    private static final String callback_url           	="callback_url";
    private static final String note           			="note";
    
    private static final String merchant_code            	="merchant_code";
    private static final String data                		="data";
    private static final String sign                 		="sign";
    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
            	put(merchant_user, "phoenixtest");
                put(merchant_order_no,channelWrapper.getAPI_ORDER_ID());
                put(amount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(callback_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(service_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(platform,"PC");
                put(bank_code,"");
                put(risk_level,"1");
                put(note,"-");
            }
        };
        log.debug("[新银支付]-[请求支付]-1.组装请求参数完成：{}" );
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
    	JSONObject voJson = new JSONObject(api_response_params);
  		String json = voJson.toString();
     	String data="";
 		try {
 			data = RsaUtil.encryptToBase64(json,channelWrapper.getAPI_PUBLIC_KEY() );
 			System.out.println("data =====> " + data);
 			api_response_params.put("data", data);
 		} catch (Exception e) {
 			e.printStackTrace();
 		}
     	String signRAS=sign(data,channelWrapper.getAPI_KEY());
     	return signRAS;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        HashMap<String, String> result = Maps.newHashMap();
        ArrayList<Map<String, String>> payResultList =Lists.newArrayList();
        StringBuilder sb = new StringBuilder();
    	try {
			sb.append("merchant_code=").append(URLEncoder.encode(channelWrapper.getAPI_MEMBERID(), "UTF-8")).append("&");
			sb.append("data=").append(URLEncoder.encode(payParam.get(data), "UTF-8")).append("&");
			sb.append("sign=").append(URLEncoder.encode(pay_md5sign, "UTF-8"));
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
        String json ="";
        try {
        	json=sendData(sb.toString(),channelWrapper.getAPI_CHANNEL_BANK_URL(),"POST");
        	com.alibaba.fastjson.JSONObject resJson=com.alibaba.fastjson.JSONObject.parseObject(json);
        	if (null != resJson && resJson.containsKey("status") && resJson.getString("status").equals("1")) {
        		String signStr=RSAUtil.decryptByPrivateKey(resJson.getString("data"), channelWrapper.getAPI_KEY());
        		com.alibaba.fastjson.JSONObject signJson=com.alibaba.fastjson.JSONObject.parseObject(signStr);
        		if(HandlerUtil.isZfbSM(channelWrapper)){
        			result.put(QRCONTEXT, signJson.getString("qr_image_url"));
        		}else{
        			result.put(JUMPURL, signJson.getString("transaction_url"));
        		}
        		handlerUtil.saveStrInRedis(channelWrapper.getAPI_MEMBERID(), channelWrapper.getAPI_KEY(), 6000);
        		payResultList.add(result);
        	}else{
        		log.error("[新银支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(json) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(json);
        	}
		} catch (Exception e1) {
			e1.printStackTrace();
		}
        log.debug("[新银支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[新银支付]-[请求支付]-4.处理请求响应成功：{}");
        return requestPayResult;
    }
    

 	public static String sign(String content, String privateKey) {
 		try {
 			PKCS8EncodedKeySpec priPKCS8 = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKey));
 			KeyFactory keyf = KeyFactory.getInstance("RSA");
 			PrivateKey priKey = keyf.generatePrivate(priPKCS8);
 			java.security.Signature signature = java.security.Signature.getInstance("SHA1WithRSA");
 			signature.initSign(priKey);
 			signature.update(content.getBytes());
 			byte[] signed = signature.sign();
 			return Base64.getEncoder().encodeToString(signed);
 		} catch (Exception e) {
 			e.printStackTrace();
 		}
 		return null;
 	}

 	public static String sendData(String params, String url, String method) throws IOException {
		String response = null;
		DataOutputStream outputStream = null;
		InputStreamReader isr = null;
		BufferedReader br = null;
		try{
			URL endpoint = new URL(url); 
			HttpURLConnection conn = (HttpURLConnection) endpoint.openConnection();
			conn.setRequestMethod(method);
			conn.setDoInput(true);
			conn.setDoOutput(true);
			conn.setConnectTimeout(30000);
			conn.setRequestProperty("Content-Type","application/x-www-form-urlencoded; charset=UTF-8");
			
			outputStream = new DataOutputStream(conn.getOutputStream());
	        outputStream.write(params.getBytes("UTF-8"));
	        outputStream.flush();
	        
	        isr = new InputStreamReader(conn.getInputStream());
	        br = new BufferedReader(isr);
	        while( (response = br.readLine()) != null ) { 
	        	System.out.println("result====" + response);
	    		System.out.println();
	    		return response;
	        }
		}catch(Exception e){
			e.printStackTrace();
		}finally {
			br.close();
			isr.close();
			outputStream.close();
		}
		return response;
	}
 	
}