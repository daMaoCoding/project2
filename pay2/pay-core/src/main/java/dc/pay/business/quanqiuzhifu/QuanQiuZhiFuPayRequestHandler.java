package dc.pay.business.quanqiuzhifu;

/**
 * ************************
 * @author tony 3556239829
 */

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
import dc.pay.utils.*;
import org.apache.commons.lang.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RequestPayHandler("QUANQIUZHIFU")
public final class QuanQiuZhiFuPayRequestHandler extends PayRequestHandler {
    //private static final Logger log = LoggerFactory.getLogger(GeFuPayRequestHandler.class);


     private static final String    uid = "uid";      //	商户ID	string(50)	您的商户唯一标识，注册后在基本资料里获得
     private static final String    price = "price";      //	金额	float	单位：元。精确小数点后2位
     private static final String    paytype = "paytype";      //	支付渠道	int	1：支付宝；2：微信支付
     private static final String    notify_url = "notify_url";      //	异步回调地址	string(255)	用户支付成功后，我们服务器会主动发送一个post消息到这个网址。由您自定义。不要urlencode并且不带任何参数。例：http://www.xxx.com/notify_url
     private static final String    return_url = "return_url";      //	同步跳转地址	string(255)	用户支付成功后，我们会让用户浏览器自动跳转到这个网址。由您自定义。不要urlencode并且不带任何参数。例：http://www.xxx.com/return_url
     private static final String    user_order_no = "user_order_no";      //	商户自定义订单号	string(50)	我们会据此判别是同一笔订单还是新订单。我们回调时，会带上这个参数。例：201810110922
     private static final String    tm = "tm";      //	日期时间	string(50)	请求时间yyyy-mm-dd hh:mi:ss
     private static final String    sign = "sign";      //	签名	string(32)	将参数1至6按顺序连Token一起，做md5-32位加密，取字符串小写。网址类型的参数值不要urlencode（例：uid + price + paytype + notify_url + return_url + user_order_no + token）


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(uid,channelWrapper.getAPI_MEMBERID());
            payParam.put(price,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(paytype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(return_url,channelWrapper.getAPI_WEB_URL());
            payParam.put(user_order_no,channelWrapper.getAPI_ORDER_ID());
            payParam.put(tm, DateUtil.formatDateTimeStrByParam("yyyy-MM-dd HH:mm:ss"));
        }

        log.debug("[全球支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String paramsStr = String.format("%s%s%s%s%s%s%s",
                params.get(uid),
                params.get(price),
                params.get(paytype),
                params.get(notify_url),
                params.get(return_url),
                params.get(user_order_no),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[全球支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {


	        resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
	        JSONObject jsonObject = null;
	        try {
		        jsonObject = JSON.parseObject(resultStr);
	        } catch (Exception e) {
		        log.error("[全球支付]-[请求支付]-3.1.发送支付请求，及获取支付请求出错，订单号：" + channelWrapper.getAPI_ORDER_ID() + ",通道名称：" + channelWrapper.getAPI_CHANNEL_BANK_NAME() + ",postUrl:" + channelWrapper.getAPI_CHANNEL_BANK_URL() + ",payForManagerCGIResultJsonObj" + e.getMessage(), e);
		        throw new PayException(e.getMessage(),e);
	        }

	        if (null!=jsonObject && jsonObject.containsKey("Code") && "1".equalsIgnoreCase(jsonObject.getString("Code"))
			        && jsonObject.containsKey("QRCodeLink") && StringUtils.isNotBlank(jsonObject.getString("QRCodeLink"))){
		        if( handlerUtil.isWapOrApp(channelWrapper)) {
		            result.put(JUMPURL, jsonObject.getString("callurl"));
		        }else{
			        String qrCodeLink = jsonObject.getString("QRCodeLink").split("text=")[1];
		            result.put(QRCONTEXT, qrCodeLink);
		        }
		        result.put("第三方返回",jsonObject.toString());
	        }else {
		        log.error("[全球支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
		        throw new PayException(resultStr);
	        }
	        payResultList.add(result);
            /*if (HandlerUtil.isWY(channelWrapper) ||  HandlerUtil.isYLKJ(channelWrapper)) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='post'"));
                payResultList.add(result);
            }else{

            	if(HandlerUtil.isWxSM(channelWrapper)){
            		resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"");
            		if (StringUtils.isBlank(resultStr)) {
    					log.error("[全球支付]-[请求支付]-3.1第一次发送支付请求，获取支付请求返回值异常:返回空");
    					throw new PayException(EMPTYRESPONSE);
    				}
            		if(!resultStr.contains("<!DOCTYPE html>")){
            			log.error("[全球支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
         	            throw new PayException(resultStr);
            		}
            		// 正则表达式规则
            	    String regEx = "wxp:(.+?)[)];";
            	    Pattern pattern = Pattern.compile(regEx);
            	    Matcher matcher = pattern.matcher(resultStr);
            	    String url="";
            	    while (matcher.find()) {
            	    	url=matcher.group().replace("\\", "");
                    }
            	    if(StringUtils.isEmpty(url)){
            	    	log.error("[全球支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
         	            throw new PayException(resultStr);
            	    }
            	    result.put(QRCONTEXT, url.substring(0,url.length()-3));
            	    payResultList.add(result);
            	}else{
            		resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
    				
    				if(StringUtils.isNotBlank(resultStr) && resultStr.contains("<form") && !resultStr.contains("{")){
                        result.put(HTMLCONTEXT,resultStr);
                        payResultList.add(result);
                    }else if(StringUtils.isNotBlank(resultStr) ){
                        JSONObject jsonResultStr = JSON.parseObject(resultStr);
                        if(null!=jsonResultStr && jsonResultStr.containsKey("Code") && "1".equalsIgnoreCase(jsonResultStr.getString("Code"))
                                && jsonResultStr.containsKey("QRCodeLink") && StringUtils.isNotBlank(jsonResultStr.getString("QRCodeLink"))){
                            if(HandlerUtil.isWapOrApp(channelWrapper)){
                                result.put(JUMPURL, jsonResultStr.getString("QRCodeLink"));
                            }else{
                                String replace = jsonResultStr.getString("QRCodeLink").replace("http://mobile.qq.com/qrcode?url=", "");
                                result.put(QRCONTEXT, HandlerUtil.UrlDecode(replace));
                            }
                            payResultList.add(result);
                        }else {throw new PayException(resultStr); }
    				}else{ throw new PayException(EMPTYRESPONSE);}
            	}
            }*/
        } catch (Exception e) { 
             log.error("[全球支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[全球支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[全球支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}