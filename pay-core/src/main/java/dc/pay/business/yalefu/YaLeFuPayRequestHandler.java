package dc.pay.business.yalefu;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
import dc.pay.utils.ValidateUtil;


/**
 * 
 * 
 * @author sunny
 * Dec 18, 2018
 */
@RequestPayHandler("YALEFU")
public final class YaLeFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(YaLeFuPayRequestHandler.class);

//    参数			参数名称			类型（长度）			使用			签名顺序
//    charset		编码方式			String(10)			必填			1
//    merchantCode	商户号			String(15)			必填			2
//    orderNo		订单号			String(100)			必填			3
//    amount		订单金额			Number(9)			必填			4
//    channel		支付通道			String(10)			必填			5
//    bankCode		到收银台的支付方式	String(10)			必填			6
//    remark		订单描述			String(255)			必填			7
//    notifyUrl		异步通知地址		String(500)			必填			8
//    returnUrl		页面通知地址		String(500)			必填			9
//    extraReturnParam	公用回传参数	String(100)			必填			10
//    signType		签名方式			String(10)			必填	
//    sign			数据签名			String				必填	

    private static final String charset               	="charset";
    private static final String merchantCode           	="merchantCode";
    private static final String orderNo           		="orderNo";
    private static final String amount           		="amount";
    private static final String channel          		="channel";
    private static final String bankCode              	="bankCode";
    private static final String remark            		="remark";
    private static final String notifyUrl           	="notifyUrl";
    private static final String returnUrl           	="returnUrl";
    private static final String extraReturnParam        ="extraReturnParam";
    private static final String signType           		="signType";
    
    private static final String sign                ="sign";
    private static final String key                 ="key";
    


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merchantCode, channelWrapper.getAPI_MEMBERID());
                put(orderNo,channelWrapper.getAPI_ORDER_ID());
                put(amount,channelWrapper.getAPI_AMOUNT());
                put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(bankCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(returnUrl,channelWrapper.getAPI_WEB_URL());
                put(charset,"UTF-8");
                put(channel,"BANK");
                put(remark,channelWrapper.getAPI_ORDER_ID());
                put(extraReturnParam,channelWrapper.getAPI_ORDER_ID());
                put(signType,"RSA");
            }
        };
        log.debug("[亚乐付支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        String signSrc=String.format("%s%s%s%s%s%s%s%s%s%s", 
        		charset+"="+api_response_params.get(charset)+"&",
        		merchantCode+"="+api_response_params.get(merchantCode)+"&",
        		orderNo+"="+api_response_params.get(orderNo)+"&",
        		amount+"="+api_response_params.get(amount)+"&",
        		channel+"="+api_response_params.get(channel)+"&",
        		bankCode+"="+api_response_params.get(bankCode)+"&",
        		remark+"="+api_response_params.get(remark)+"&",
        		notifyUrl+"="+api_response_params.get(notifyUrl)+"&",
        		returnUrl+"="+api_response_params.get(returnUrl)+"&",
        		extraReturnParam+"="+api_response_params.get(extraReturnParam)
        		);
        //String privateKeyStr = "MIICeAIBADANBgkqhkiG9w0BAQEFAASCAmIwggJeAgEAAoGBAJfoAFH/4yJeUJH65+W0S0QnoX1qEi/7pbbd/J3QALfkhawq97yCme10O10uBt3KCkKxKqCyr1ZZxYR1uAcRj4cYXjp0P2D9aUeHxzY26E/GBA1qhG3JSYkSaAPLWhm57ZTo6rqzgWX/miHH4QR9YX1y5LmkgbCsEAd62/nTxs6hAgMBAAECgYBYMoC1G3AiDUaUa2xnBxZIPQZbTdG/gEmH8j1w10sDejBOdSgfkGiU4M5y3H1qGpt4e2r3oKeC6F5w+PRfTkfZUD8V7sfqEIZqc8pz+LrvfCw4rfL2Tqj5HLYqCOI9o6RmcPiyM/fk10PzAJB+Vs2t0RyAYERKYE4BEhAX+JgMEQJBAMePf6ZQNf9ZYLZQ1y+ZRrk6apJLUlK5HTZmkaZZX4FvemY4X23SOcL5AAGpa83FAe0VfZbJe37+42rWpmKGqKUCQQDC3kGcSm6HrSF6A9F+i9+eje2CPMCE5HrrGdsonSOJ5GLMYrD65bI/P3nWgHq+xo+Q9ksBHSd7HtfIPZxQPLFNAkEApSSRRDZ4mPmD2RwJF3DGYB6BdtMyKxbZn909huXK7TgtmE3qWt1vw3N/l+B2P/BaabIxHglaU3vnAtDjHAHHqQJBAIk+Wb1GmtQJtpMlz00uHB+iXA2m1fyCoqOkQTs4eQgsBv1l4LnEPqbXAOt273wfgouZOzgcnWrUTStlYQsNw90CQQDGjZ0uNPNIYSG/tBUM7abdF/+V5QhA4QE2gs6YBHbxedAFGmn5BpObNX4TrAnR/cMUIFqEa7oCtISYb+hQ9+bH";
		String paramsStr = signSrc.toString();
        String sign = Signaturer(paramsStr, channelWrapper.getAPI_KEY());
        log.debug("[亚乐付支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(sign));
        return sign;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());  //.replace("method='post'","method='get'"));
        payResultList.add(result);
        log.debug("[亚乐付支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[亚乐付支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
    
    /** 
     * RSA Signature
     * @param content: Signature data to be signed 
     * @param privateKey: Merchant private key 
     * @param encode: Character set coding
     * @return Signature value
     */  
    public static final String SIGN_ALGORITHMS = "SHA1WithRSA";  
    
     public static String Signaturer(String content, String privateKey)  
     {  
         try   
         {  
             PKCS8EncodedKeySpec priPKCS8    = new PKCS8EncodedKeySpec( Base64Utils.decode(privateKey) );   
             KeyFactory keyf                 = KeyFactory.getInstance("RSA");  
             PrivateKey priKey               = keyf.generatePrivate(priPKCS8);  
             java.security.Signature signature = java.security.Signature.getInstance(SIGN_ALGORITHMS);  
             signature.initSign(priKey);  
             signature.update( content.getBytes());  
   
             byte[] signed = signature.sign();  
             return Base64Utils.encode(signed);  
         }  
         catch (Exception e)   
         {  
             e.printStackTrace();  
         }  
           
         return null;  
     }  
}