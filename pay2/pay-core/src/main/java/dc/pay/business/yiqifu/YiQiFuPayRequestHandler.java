package dc.pay.business.yiqifu;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.util.*;

@RequestPayHandler("YIQIFU")
public final class YiQiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(YiQiFuPayRequestHandler.class);

    //扫码
    private static final String  version	 = "version";           //版本号	String	Y	固定值 1.0
    private static final String  sign_type	 = "sign_type";         //签名方式	String	Y	固定值MD5，不参与签名
    private static final String  mer_no	 = "mer_no";           //商户代码	String	Y	平台分配唯一
    private static final String  currency	 = "currency";         //交易币种	String	Y	固定值156
    private static final String  back_url	 = "back_url";         //后台通知地址	String	N
    private static final String  mer_order_no	 = "mer_order_no"; //商家订单号	String	Y	保证每笔订单唯一
    private static final String  gateway_type	 = "gateway_type"; //支付类型	String	Y
    private static final String  trade_amount	 = "trade_amount"; //交易金额	String	Y	整数或者浮点类型，以元为单位，最多2位小数
    private static final String  order_date	 = "order_date";   //订单时间	String	Y	时间格式：yyyy-MM-dd HH:mm:ss
    private static final String  client_ip	 = "client_ip";        //客户端ip	String	Y	交易请求IP地址
    private static final String  sign	 = "sign";                  //签名	String	Y	不参与签名
    private static final String   bank_code	  ="bank_code";             //银行代码	String	Y	银行代码见附录必填


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                if(channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("WEB_WY")){
                    put(version ,"2.0");
                    put(gateway_type ,"000");
                    put(bank_code ,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                }else if (channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("_WAP_YL_")) {
                	put(version ,"2.0");
                	put(gateway_type ,"008");
                }else{
                    put(version ,"1.0");
                    put(gateway_type, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                }
                put(sign_type,"MD5"); //不参与签名
                put(currency,"156");
                put(trade_amount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(order_date, DateUtil.formatDateTimeStrByParam("yyyy-MM-dd HH:mm:ss"));
                put(client_ip,channelWrapper.getAPI_Client_IP());
                put(mer_no,channelWrapper.getAPI_MEMBERID());
                put(mer_order_no,channelWrapper.getAPI_ORDER_ID());
               // put(returnURL,channelWrapper.getAPI_WEB_URL());
                put(back_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            }
        };
        log.debug("[易起付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if(sign_type.equalsIgnoreCase(paramKeys.get(i).toString()))continue;
            sb.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i))).append("&");
        }
        sb.append("key=" + channelWrapper.getAPI_KEY());
        String pay_md5sign = HandlerUtil.getMD5UpperCase(sb.toString()).toLowerCase();
        log.debug("[易起付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();

        HashMap<String, String> result = Maps.newHashMap();
        if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLWAP(channelWrapper) ){
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            payResultList.add(result);
        }else{
	        String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
            if (null == resultStr || StringUtils.isBlank(resultStr)) {
            	log.error("[易起付]3.1.发送支付请求，获取支付请求返回值异常:返回空");
            	throw new PayException("第三方返回异常:返回空");
            }
            JSONObject resJson ;
            try {
            	resJson = JSONObject.parseObject(resultStr);
			} catch (Exception e) {
			 	log.error("[易起付]3.2.发送支付请求，获取支付请求返回值异常:"+resultStr);
            	throw new PayException(resultStr);
			}
            if (resJson.containsKey("errorCode") && StringUtils.isNotBlank(resJson.getString("errorCode"))) {
            	log.error("[易起付]3.3.发送支付请求，获取支付请求返回值异常:"+resultStr);
            	throw new PayException(resultStr);
            }
            if (!resJson.containsKey("auth") || !"SUCCESS".equals(resJson.getString("auth"))) {
            	log.error("[易起付]3.3.发送支付请求，获取支付请求返回值异常:"+resultStr);
            	throw new PayException(resultStr);
            }
            if (!resJson.containsKey("tradeResult") || !"1".equals(resJson.getString("tradeResult"))) {
            	log.error("[易起付]3.4.发送支付请求，获取支付请求返回值异常:"+resultStr);
            	throw new PayException(resultStr);
            }
	        if (!resJson.containsKey("payInfo") || StringUtils.isBlank(resJson.getString("payInfo"))) {
			 	log.error("[易起付]3.5.发送支付请求，获取支付请求返回值异常:"+resultStr);
            	throw new PayException(resultStr);
	        }
            String code_url = resJson.getString("payInfo");
            if(HandlerUtil.isWapOrApp(channelWrapper) && !HandlerUtil.isWxGZH(channelWrapper)){
                result.put(JUMPURL, code_url);
            }else{
              result.put(QRCONTEXT, code_url);
            }
            payResultList.add(result);
        }
        log.debug("[易起付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
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
        log.debug("[易起付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}