package dc.pay.business.zhifu;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import dc.pay.utils.*;
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

/**
 * 
 * @author andrew
 * Dec 6, 2017
 */
@RequestPayHandler("ZHIFU")
public final class ZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(ZhiFuPayRequestHandler.class);

    private static final String QRCONTEXT = "QrContext";
    private static final String HTMLCONTEXT = "HtmlContext";
    private static final String PARSEHTML = "parseHtml";
    private static final String JUMPURL = "JUMPURL";
    
    //基本参数
    //merchant_code		商家号		String(10)	必填	商户签约时，智付支付平台分配的唯一商家号。举例：1111110166。
    private static final String merchant_code = "merchant_code";
    //service_type		业务类型	String(10)	必选	固定值：alipay_scan 或 weixin_scan或 zhb_scan或 qq_scan
    private static final String service_type = "service_type";
    //notify_url	服务器异步通知地址	String(200)	必选	支付成功后，智付支付平台会主动通知商家系统，商家系统必须指定接收通知的地址。举例：http://www.dinpay.com/Notify_Url.jsp
    private static final String notify_url = "notify_url";
    //interface_version	接口版本	String(10)	必选	接口版本，固定值：V3.1(必须大写)
    private static final String interface_version = "interface_version";
    //client_ip		客户端IP	String(15)	必选	消费者创建交易时所使用机器的IP或者终端ip，最大长度为15个字符。举例：192.168.1.25
    private static final String client_ip = "client_ip";
    //sign_type		签名方式	String(10)	必选	RSA或RSA-S，不参与签名sign	签名	String	必选	签名数据，具体请见附录的签名规则定义。
    private static final String sign_type = "sign_type";
    
    //业务参数
    //order_no	商户网站唯一订单号	String(64)	必选	商户系统订单号，由商户系统生成,保证其唯一性，最长100位,由字母、数字组成.举例：1000201666。
    private static final String order_no = "order_no";
    //order_time	商户订单时间		Date		必选	商户订单时间，格式：yyyy-MM-dd HH:mm:ss，举例：2013-11-01 12:34:58
    private static final String order_time = "order_time";
    //order_amount	商户订单总金额		Number(13,2)	必选	该笔订单的总金额，以元为单位，精确到小数点后两位。举例：12.01。
    private static final String order_amount = "order_amount";
    //product_name	商品名称		String(100)	必选	商品名称，不超过100个字符。举例：华硕G750Y47JX-BL。
    private static final String product_name = "product_name";
    //input_charset		String(5)	√	参数名称：参数编码字符集	取值：UTF-8、GBK(必须大写)
    private static final String input_charset = "input_charset";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
              put(merchant_code, channelWrapper.getAPI_MEMBERID());
              put(notify_url, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
              put(sign_type, "RSA-S");
//              put(order_no, new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
	          put(order_no, channelWrapper.getAPI_ORDER_ID());
              put(order_time, DateUtil.formatDateTimeStrByParam("yyyy-MM-dd HH:mm:ss")); //yyyy-MM-dd HH:mm:ss
              put(order_amount, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
              put(product_name, "PAY");
              //如果是网银支付：
              if(channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("WEB_WY")){
            	  put(input_charset, "UTF-8");
            	  put(interface_version, "V3.0");
            	  put(service_type, "direct_pay");
              }else {
            	  put(service_type, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            	  put(client_ip, HandlerUtil.getRandomIp(channelWrapper));
            	  put(interface_version, "V3.1");
              }
            }
        };
        log.debug("[智付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map params) throws PayException {
        StringBuffer signSrc= new StringBuffer();
        //如果是网银支付：
        if(channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("WEB_WY")){
      	  signSrc.append(input_charset+"=").append(params.get(input_charset)).append("&");
        }else {
        	signSrc.append(client_ip+"=").append(params.get(client_ip)).append("&");
		}
    	signSrc.append(interface_version+"=").append(params.get(interface_version)).append("&");
    	signSrc.append(merchant_code+"=").append(params.get(merchant_code)).append("&");
    	signSrc.append(notify_url+"=").append(params.get(notify_url)).append("&");
    	signSrc.append(order_amount+"=").append(params.get(order_amount)).append("&");
    	signSrc.append(order_no+"=").append(params.get(order_no)).append("&");
    	signSrc.append(order_time+"=").append(params.get(order_time)).append("&");
    	signSrc.append(product_name+"=").append(params.get(product_name)).append("&");
    	signSrc.append(service_type+"=").append(params.get(service_type));
        String signInfo = signSrc.toString();
        String sign="";
        try {
        	sign = RsaUtil.signByPrivateKey(signInfo,channelWrapper.getAPI_KEY());	// 签名
        } catch (Exception e) {
            log.error("[智付]-[请求支付]-2.生成加密URL签名出错，签名出错：{}",e.getMessage(),e);
        }
        log.debug("[智付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(sign));
        return sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        if (channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("WEB_WY")) {
            StringBuffer sbHtml = new StringBuffer();
            sbHtml.append("<form id='mobaopaysubmit' name='mobaopaysubmit' action='" + channelWrapper.getAPI_CHANNEL_BANK_URL() + "' method='post'>");
            for (Map.Entry<String, String> entry : payParam.entrySet()) {
                sbHtml.append("<input type='hidden' name='" + entry.getKey() + "' value='" + entry.getValue() + "'/>");
            }
            sbHtml.append("</form>");
            sbHtml.append("<script>document.forms['mobaopaysubmit'].submit();</script>");
            result.put(HTMLCONTEXT, sbHtml.toString());
            payResultList.add(result);
        }else{
            try {
            	String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
	            if (null == resultStr || StringUtils.isBlank(resultStr)) {
	            	log.error("[通扫]3.1.发送支付请求，获取支付请求返回值异常:返回空");
	            	throw new PayException("返回空");
	            }
	            resultStr = resultStr.replaceAll("<dinpay>", "").replaceAll("</dinpay>", "");
	            Map<String, String> mapBodys = XmlUtil.toMap(resultStr.getBytes(), "UTF-8");
	            //取值为“SUCCESS”表示校验成功，           取值为“FAIL”表示通讯失败（参数异常或者验签失败
	            String resp_code = mapBodys.get("resp_code");
	            String order_amount = mapBodys.get("order_amount");
	            //0：获取二维码成功
	            String result_code = mapBodys.get("result_code");
	            if ("SUCCESS".equalsIgnoreCase(resp_code) && "0".equalsIgnoreCase(result_code) && StringUtils.isNotBlank(order_amount)) {
                    result.put(QRCONTEXT, HandlerUtil.UrlDecode(mapBodys.get("qrcode")));
                    payResultList.add(result);
	            } else {
	                log.error("[智付]3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(mapBodys) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	                throw new PayException(JSON.toJSONString(mapBodys));
	            }
            } catch (Exception e) {
            	log.error("[智付]3.发送支付请求，及获取支付请求结果出错：", e);
            	throw new PayException(e.getMessage(), e);
            }
        }	

        log.debug("[智付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
        return payResultList;
    }


    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (null != resultListMap && !resultListMap.isEmpty()) {
            if (resultListMap.size() == 1) {
                Map<String, String> resultMap = resultListMap.get(0);
                if (null != resultMap && resultMap.containsKey(QRCONTEXT)) {
                    requestPayResult.setRequestPayQRcodeContent(resultMap.get(QRCONTEXT));
                }
                if (null != resultMap && resultMap.containsKey(HTMLCONTEXT)) {
                    requestPayResult.setRequestPayHtmlContent(resultMap.get(HTMLCONTEXT));
                }
                if (null != resultMap && resultMap.containsKey(JUMPURL)) {
                    requestPayResult.setRequestPayJumpToUrl(resultMap.get(JUMPURL));
                }
                requestPayResult.setRequestPayamount(channelWrapper.getAPI_AMOUNT());
                requestPayResult.setRequestPayOrderId(channelWrapper.getAPI_ORDER_ID());
                requestPayResult.setRequestPayOrderCreateTime(HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "yyyy-MM-dd HH:mm:ss"));
                requestPayResult.setRequestPayQRcodeURL(null);
                requestPayResult.setRequestPayChannelBankName(channelWrapper.getAPI_CHANNEL_BANK_NAME());
            }
            if (ValidateUtil.requestesultValdata(requestPayResult)) {
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            } else {
                throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[智付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}