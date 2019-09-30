package dc.pay.business.efutong;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import dc.pay.utils.XmlUtil;
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
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.RsaUtil;
import dc.pay.utils.ValidateUtil;

/**
 *
 * 
 * @author kevin
 * Jul 26, 2018
 */
@RequestPayHandler("EFUTONG")
public final class EFuTongPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(EFuTongPayRequestHandler.class);

    private static final String      merchant_code	  	  = "merchant_code";                         
    private static final String      service_type	  	  = "service_type";                       
    private static final String      notify_url	  		  = "notify_url";                       
    private static final String      interface_version	  = "interface_version";                         
    private static final String      input_charset	      = "input_charset";                         
    private static final String      sign_type	  		  = "sign_type";                         
    private static final String      pay_type	  	  	  = "pay_type";                         
    private static final String      client_ip	     	  = "client_ip";                          
    private static final String      order_no  		  	  = "order_no";                    
    private static final String      order_time	      	  = "order_time";
    private static final String      order_amount	      = "order_amount";
    private static final String      bank_code	      	  = "bank_code";
    private static final String      redo_flag	      	  = "redo_flag";
    private static final String      product_name	      = "product_name";

    @Override
//    protected Map<String, String> buildPayParam() throws PayException {
//        
//        Map<String, String> payParam = new TreeMap<String, String>() {
//            {
//                put(merchant_code,channelWrapper.getAPI_MEMBERID());
//                put(service_type,"direct_pay");
//                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
//                put(interface_version,"V3.0");
//                put(input_charset,"UTF-8");
//                put(sign_type,"RSA-S");
//                put(order_no,channelWrapper.getAPI_ORDER_ID());
//                put(order_time,HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "yyyy-MM-dd HH:mm:ss"));
//                put(order_amount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
//                put(redo_flag,"1");
//                put(product_name,"GOODS");
//                put(client_ip,channelWrapper.getAPI_Client_IP());
//                put(pay_type,"b2c");
//                put(bank_code,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                
//            }
//        };
//        log.debug("[易富通]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
//        return payParam;
//    }
    protected Map<String, String> buildPayParam() throws PayException {
        
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
            	put(merchant_code,channelWrapper.getAPI_MEMBERID());
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(input_charset,"UTF-8");
                put(sign_type,"RSA-S");
                put(order_no,channelWrapper.getAPI_ORDER_ID());
                put(order_time,HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "yyyy-MM-dd HH:mm:ss"));
                put(order_amount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(product_name,"GOODS");
                put(client_ip,channelWrapper.getAPI_Client_IP());
            	if (handlerUtil.isWY(channelWrapper)) {
            	    put(interface_version,"V3.0");
                    put(service_type,"direct_pay");
                    put(bank_code,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    put(redo_flag,"1");
                    put(pay_type,"b2c");
                }else {
                    put(interface_version,"V3.1");
                    put(service_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                }
            }
        };
        log.debug("[易富通]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String, String> params) throws PayException {
        StringBuffer signSrc= new StringBuffer();
        if (handlerUtil.isWY(channelWrapper)) {
            signSrc.append("bank_code=").append(params.get(bank_code)).append("&");
        }
        signSrc.append("client_ip=").append(params.get(client_ip)).append("&");
        signSrc.append("input_charset=").append(params.get(input_charset)).append("&");
        signSrc.append("interface_version=").append(params.get(interface_version)).append("&");
        signSrc.append("merchant_code=").append(params.get(merchant_code)).append("&");
        signSrc.append("notify_url=").append(params.get(notify_url)).append("&");
        signSrc.append("order_amount=").append(params.get(order_amount)).append("&");
        signSrc.append("order_no=").append(params.get(order_no)).append("&");
        signSrc.append("order_time=").append(params.get(order_time)).append("&");
        if (handlerUtil.isWY(channelWrapper)) {
            signSrc.append("pay_type=").append(params.get(pay_type)).append("&");
        }
        signSrc.append("product_name=").append(params.get(product_name)).append("&");
        if (handlerUtil.isWY(channelWrapper)) {
            signSrc.append("redo_flag=").append(params.get(redo_flag)).append("&");
        }
        signSrc.append("service_type=").append(params.get(service_type));
        String signInfo = signSrc.toString();
        String pay_md5sign="";
        try {
        	pay_md5sign = RsaUtil.signByPrivateKey(signInfo,channelWrapper.getAPI_KEY());	// 签名
        } catch (Exception e) {
            log.error("[易富通]-[请求支付]-2.1生成加密URL签名出错，签名出错：{}",e.getMessage(),e);
        }
        log.debug("[易富通]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        Map<String, String> result = Maps.newHashMap();
        if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) ) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
        }else{
            String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
            if (StringUtils.isBlank(resultStr)) {
                log.error("[易富通]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            if (!resultStr.contains("<resp_code>SUCCESS</resp_code>")) {
                log.error("[易富通]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            resultStr = resultStr.replaceAll("<dinpay>", "").replaceAll("</dinpay>", "");
            Map<String, String> mapBodys = null ;
            try {
                mapBodys = XmlUtil.toMap(resultStr.getBytes(), "utf-8");
                if (null == mapBodys || !mapBodys.containsKey("result_code") || !"0".equals(mapBodys.get("result_code"))) {
                    log.error("[易富通]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
            } catch (Exception e) {
                e.printStackTrace();
                log.error("[易富通]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            result.put(QRCONTEXT, mapBodys.get("qrcode"));
//            if (handlerUtil.isWapOrApp(channelWrapper)) {
//                result.put(JUMPURL, HandlerUtil.UrlDecode(mapBodys.get("payurl")));
//            }else {
//                result.put(QRCONTEXT, mapBodys.get("qrcode"));
//            }
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		payResultList.add(result);
        log.debug("[易富通]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[易富通]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }

}