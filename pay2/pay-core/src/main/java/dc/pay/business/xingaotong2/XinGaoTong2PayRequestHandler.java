package dc.pay.business.xingaotong2;

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
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.Sha1Util;
import dc.pay.utils.UnicodeUtil;
import dc.pay.utils.ValidateUtil;
import org.springframework.http.HttpMethod;

/**
 * @author sunny
 * Dec 18, 2018
 */
@RequestPayHandler("XINGAOTONG2")
public final class XinGaoTong2PayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XinGaoTong2PayRequestHandler.class);

//    参数名			参数			可空			加入签名			说明
//    商户ID			partner		N			Y				商户id,由高通支付分配
//    银行类型		banktype	N			Y				银行类型，具体参考附录1
//    金额			paymoney	N			Y				单位元（人民币）
//    商户订单号		ordernumber	N			Y				商户系统订单号，该订单号将作为高通支付接口的返回数据。该值需在商户系统内唯一，高通支付系统暂时不检查该值是否唯一
//    下行异步通知地址	callbackurl	N			Y				下行异步通知的地址，需要以http://开头且没有任何参数
//    下行同步通知地址	hrefbackurl	Y			N				下行同步通知过程的返回地址(在支付完成后高通支付接口将会跳转到的商户系统连接地址)。 注：若提交值无该参数，或者该参数值为空，则在支付完成后，高通支付接口将不会跳转到商户系统，用户将停留在高通支付接口系统提示支付成功的页面。
//    备注信息		attach		Y			N				备注信息，下行中会原样返回。若该值包含中文，请注意编码
//    MD5签名		sign		N			N				32位小写MD5签名值

    private static final String partner               	="partner";
    private static final String banktype           		="banktype";
    private static final String paymoney           		="paymoney";
    private static final String ordernumber           	="ordernumber";
    private static final String callbackurl          	="callbackurl";
    private static final String hrefbackurl             ="hrefbackurl";
    private static final String attach           		="attach";
    
    private static final String sign                ="sign";
    private static final String key                 ="key";
    


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(partner, channelWrapper.getAPI_MEMBERID());
                put(ordernumber,channelWrapper.getAPI_ORDER_ID());
                put(paymoney,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(callbackurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(banktype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(hrefbackurl,channelWrapper.getAPI_WEB_URL());
            }
        };
        log.debug("[新高通2]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
    	String  signSrc=String.format("%s%s%s%s%s%s", 
    			partner+"="+api_response_params.get(partner)+"&",
    			banktype+"="+api_response_params.get(banktype)+"&",
    			paymoney+"="+api_response_params.get(paymoney)+"&",
    			ordernumber+"="+api_response_params.get(ordernumber)+"&",
    			callbackurl+"="+api_response_params.get(callbackurl),
    			channelWrapper.getAPI_KEY()
    			);
        String paramsStr = signSrc.toString();
        String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[新高通2]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
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
	        String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,String.class,HttpMethod.GET);
	        if (StringUtils.isBlank(resultStr)) {
	            log.error("[新高通2]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(EMPTYRESPONSE);
	        }
	        resultStr = UnicodeUtil.unicodeToString(resultStr);
	        if(StringUtils.isNotBlank(resultStr) && resultStr.contains("<form") && !resultStr.contains("{")){
                result.put(HTMLCONTEXT,resultStr);
                //payResultList.add(result);
            }else{
            	log.error("[新高通2]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
            }
	        payResultList.add(result);
        }
        log.debug("[新高通2]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[新高通2]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}