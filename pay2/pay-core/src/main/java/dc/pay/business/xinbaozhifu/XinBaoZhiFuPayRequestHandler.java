package dc.pay.business.xinbaozhifu;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayRequestHandler;
import dc.pay.business.RequestPayResult;
import dc.pay.business.yunbao.StringUtil;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.UnicodeUtil;
import dc.pay.utils.ValidateUtil;

import org.apache.commons.codec.binary.Base64;


/**
 * 
 * 
 * @author sunny
 * Dec 25, 2018
 */
@RequestPayHandler("XINBAOZHIFU")
public final class XinBaoZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XinBaoZhiFuPayRequestHandler.class);

//    参数名				说明
//    banktype			支付类型  0-	支付宝/PC 1-	1-支付宝/H5 2-	2-微信/pc 3-	3-微信/H5
//    usernumber		商户单号
//    paymoney			充值金额
//    callbackurl		发送此参数，支付成功通知将采用该地址，如果不发送此参数，支付成功通知将采用商户中心所填写地址
//    a					我的账户-Appid获取 AppID会在修改密码后自动改变
//    p					Base64加密后的参数，详情见p参数说明表
//    s					MD5加密的签名验证值 

    private static final String banktype                  	  ="banktype";
    private static final String usernumber               	  ="usernumber";
    private static final String paymoney                 	  ="paymoney";
    private static final String callbackurl           		  ="callbackurl";
    
    private static final String appid                 		  ="a";
    private static final String param             		  	  ="p";
    private static final String sign             		      ="s";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(banktype, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(usernumber,channelWrapper.getAPI_ORDER_ID());
                put(paymoney,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(callbackurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            }
        };
        log.debug("[新宝支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
    	String base64Str = resolvePayParam(api_response_params);
        String signMD5="";
		try {
			signMD5 = getMD5(base64Str);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        log.debug("[新宝支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return signMD5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
    	Map<String, String> postParam=new HashMap<String,String>();
    	HashMap<String, String> result = Maps.newHashMap();
    	ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
    	String base64Str = resolvePayParam(payParam);
    	try {
			postParam.put(appid, java.net.URLEncoder.encode(channelWrapper.getAPI_MEMBERID(), "UTF-8"));
			if(base64Str.contains("&")||base64Str.contains("@")){
				postParam.put(param, java.net.URLEncoder.encode(base64Str,"UTF-8"));
			}else{
				postParam.put(param, base64Str);
			}
			postParam.put(sign, java.net.URLEncoder.encode(pay_md5sign, "UTF-8"));
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
    	if ((HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)  ||  HandlerUtil.isWapOrApp(channelWrapper)   )) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),postParam).toString());  //.replace("method='post'","method='get'"));
            payResultList.add(result);
        }else{
	        String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), postParam,String.class,HttpMethod.GET);
	        if (StringUtils.isBlank(resultStr)) {
	            log.error("[新宝支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(EMPTYRESPONSE);
	        }
	        resultStr = UnicodeUtil.unicodeToString(resultStr);
	        resultStr=resultStr.replaceAll("\\\\", "");
	        try {
				Document document = Jsoup.parse(resultStr);
				Element  payUrlInputs = document.getElementById("Image1");
				if(null==payUrlInputs){
					log.error("[新宝支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + resultStr + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
					throw new PayException(resultStr);
				}
				String url=payUrlInputs.attr("src")==null?"":payUrlInputs.attr("src").replace("QRCodeHandler.ashx?url=", "");
				if(StringUtil.isBlank(url)){
					log.error("[新宝支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + resultStr + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
					throw new PayException(resultStr);
				}
				result.put(QRCONTEXT, url);
			} catch (Exception e) {
				log.error("[新宝支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + resultStr + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
				 throw new PayException(resultStr);
			}
	        payResultList.add(result);
        }
        log.debug("[新宝支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[新宝支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
    
    private String resolvePayParam(Map<String,String> api_response_params){
    	/*List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
            	signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "" );	*/
    	 String signSrc = new String();
    	 signSrc=createParam(api_response_params);
        byte[] base64Byte= Base64.encodeBase64(signSrc.toString().getBytes(),true);
        
        String base64Str = new String(base64Byte);
        base64Str = base64Str.substring(0, base64Str.length() - 2);//java的Base64加密后需要减去最后2位字符
        return base64Str;
    }
    
    /**
     * 获取MD5加密后的字符串
     *
     * @param str 明文
     * @return 加密后的字符串
     * @throws Exception
     */
    public static String getMD5(String str) throws Exception {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(str.getBytes());
        /**
         * 获取加密后的字节数组
         */
        byte[] md5Bytes = md5.digest();
        String res = "";
        for (int i = 0; i < md5Bytes.length; i++) {
            int temp = md5Bytes[i] & 0xFF;
            if (temp <= 0XF) { // 转化成十六进制不够两位，前面加零
                res += "0";
            }
            res += Integer.toHexString(temp);
        }
        return res;
    }
    
    public static String createParam(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        // 将参数以参数名的字典升序排序
        Map<String, String> sortParams = new TreeMap<>(params);
        // 遍历排序的字典,并拼接"key=value"格式
        for (Map.Entry<String, String> entry : sortParams.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().trim();
            if (StringUtils.isNotEmpty(value)) {
                sb.append("&").append(key).append("=").append(value);
            }
        }
        String signValue = sb.toString().replaceFirst("&", "");
        return signValue;
    }
}