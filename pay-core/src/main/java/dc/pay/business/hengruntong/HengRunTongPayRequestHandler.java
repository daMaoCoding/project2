package dc.pay.business.hengruntong;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.jsoup.Jsoup;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import com.alibaba.fastjson.JSON;
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
import dc.pay.utils.ValidateUtil;
import dc.pay.utils.qr.QRCodeUtil;

/**
 * 
 * @author tree
 * Aug 14, 2018
 */
@RequestPayHandler("HENGRUNTONG")
public final class HengRunTongPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(HengRunTongPayRequestHandler.class);

    private static final String      inputCharset  = "inputCharset" ;       //是    参数字符集编码
    private static final String      notifyUrl	   = "notifyUrl" ;          //是	    异步通知地址
    private static final String      payType	   = "payType" ;            //是	    支付方式
    private static final String      merchantId	   = "merchantId" ;         //是	    商户号
    private static final String      orderId	   = "orderId" ;            //是    商户订单号
    private static final String      transAmt	   = "transAmt" ;           //是    订单金额
    private static final String      orderTime	   = "orderTime" ;          //是    商户订单时间
    private static final String      isPhone	   = "isPhone" ;            //否    传1时，支付宝wap支付，不传为扫码支付
    private static final String      sign	       = "sign" ;               //是    签名

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
            	put(inputCharset, "UTF-8");
                put(notifyUrl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(payType, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(merchantId, channelWrapper.getAPI_MEMBERID());
                put(orderId, channelWrapper.getAPI_ORDER_ID());
                put(transAmt, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(orderTime, DateUtil.formatDateTimeStrByParam("yyyy-MM-dd HH:mm:ss"));
                //put(isPhone, "1");
            }
        };
        log.debug("[恒润通]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }
    
    /**
     * 生成签名
     */
    protected String buildPaySign(Map<String,String> params) throws PayException {
        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if(StringUtils.isBlank(params.get(paramKeys.get(i))) || sign.equalsIgnoreCase(paramKeys.get(i).toString()))
                continue;
            sb.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i))).append("&");
        }
        sb.append("key=" + channelWrapper.getAPI_KEY());
        String signStr = sb.toString(); //.replaceFirst("&key=","")
        pay_md5sign = HandlerUtil.getMD5UpperCase(signStr).toLowerCase();
        log.debug("[恒润通]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }    

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        try {
            String resultStr = RestTemplateUtil.sendByRestTemplateRedirectWithSendSimpleForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, HttpMethod.POST);
            String imgSrc = "";
            if (StringUtils.isBlank(resultStr)) {
            	log.error("[恒润通]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//            	throw new PayException("返回空,参数："+JSON.toJSONString(payParam));
            	throw new PayException(resultStr);
            }
            try {
    			imgSrc = Jsoup.parse(resultStr).select("[id=content] img").first().attr("src");            	
            } catch (Exception e) {
            	log.error("[恒润通]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            	throw new PayException(resultStr);
			}
        	String qrUrl = imgSrc.split("url=")[1];
        	if (StringUtils.isBlank(qrUrl)) {
        		log.error("[恒润通]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
        		throw new PayException(resultStr);
        	}

            if (handlerUtil.isWapOrApp(channelWrapper)) {
                result.put(JUMPURL, QRCodeUtil.decodeByUrl(imgSrc));            	
            } else {
        		result.put(QRCONTEXT, QRCodeUtil.decodeByUrl(imgSrc));
            }
        } catch (Exception e) {
            log.error("[恒润通]-[请求支付]-3.4.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		payResultList.add(result);
        log.debug("[恒润通]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
        return payResultList;
    }

    /**
     * 生成返回给前端使用的值
     */
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
        log.debug("[恒润通]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
    
}
