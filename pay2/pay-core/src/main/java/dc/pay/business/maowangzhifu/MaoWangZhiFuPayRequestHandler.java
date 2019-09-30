package dc.pay.business.maowangzhifu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.UnicodeUtil;
import dc.pay.utils.ValidateUtil;


/**
 * 
 * 
 * @author sunny
 * Dec 18, 2018
 */
@RequestPayHandler("MAOWANGZHIFU")
public final class MaoWangZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(MaoWangZhiFuPayRequestHandler.class);

//    NO			参数名称					参数含义			长度				是否必填			参数说明
//    1				inputCharset			字符集			String(1)		是				固定填1；1代表UTF-8
//    2				partnerId				商户号			String(32)		是				平台提供的商户号
//    3				signType				签名类型			String(1)		是				1代表RSA
//    4				notifyUrl				支付结果异步通知地址String(190)		是				服务器主动通知商户网站里指定的页面http路径。必须保证服务器异步通知页面（notifyUrl）上无任何字符，如空格、HTML标签、开发系统自带抛出的异常提示信息等
//    5				returnUrl				页面跳转同步通知页面路径	String(200)	是			目前都使用异步通知，该字段填写和notifyUrl一样的即可（杉德快捷支付请传支付成功跳转网站）
//    6				orderNo					商户订单号		String(50)		是				字符串，只允许使用字母、数字、- 、_,并以字母或数字开头；每商户提交的订单号，必须是商户的唯一订单号
//    7				orderAmount				商户金额			String(10)		是				整型数字，单位是分，
//    8				orderCurrency			币种类型			String(3)		是				固定填156;人民币
//    9				orderDatetime			商户订单提交时间	String(14)		是				日期格式：yyyyMMDDhhmmss，例如：20180116020101必须使用24小时制
//    10			signMsg					签名信息			String(1024)	是				请参见本文档“3.3节 签名与验证”
//    11			payMode					支付方式			String(1)		是				请参见本文档“5 支付方式编码”
//    12			subject					交易名称			String(256)		是	
//    13			body					订单描述			String(1000)	是				对一笔交易的具体描述信息。
//    14			cardNo					支付卡号			String(30)		是				网银需要，部分通道必输
//    15			bnkCd					银行编码			String(10)		是				网银必需，请参见本文档“6 银行编码”
//    16			accTyp					卡类型			String(1)		是				网银需要 0-借记 1-贷记
//    18			ip						客户的真实ip		String(20)		是				商户获取客户的ip，然后提交给平台，非商户的服务器ip

    private static final String inputCharset               	="inputCharset";
    private static final String partnerId           		="partnerId";
    private static final String signType           			="signType";
    private static final String notifyUrl           		="notifyUrl";
    private static final String returnUrl          			="returnUrl";
    private static final String orderNo              		="orderNo";
    private static final String orderAmount            		="orderAmount";
    private static final String orderCurrency           	="orderCurrency";
    private static final String orderDatetime            	="orderDatetime";
    private static final String signMsg                		="signMsg";
    private static final String payMode                 	="payMode";
    private static final String subject                 	="subject";
    private static final String body                 		="body";
    private static final String cardNo                 		="cardNo";
    private static final String bnkCd                 		="bnkCd";
    private static final String accTyp                 		="accTyp";
    private static final String ip                 			="ip";
    
    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(partnerId, channelWrapper.getAPI_MEMBERID());
                put(orderNo,channelWrapper.getAPI_ORDER_ID());
                put(orderAmount,channelWrapper.getAPI_AMOUNT());
                put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(payMode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(returnUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(ip,channelWrapper.getAPI_Client_IP());
                put(orderDatetime,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
                put(inputCharset,"1");
                put(signType,"1");
                put(orderCurrency,"156");
                put(subject,channelWrapper.getAPI_ORDER_ID());
                put(body,channelWrapper.getAPI_ORDER_ID());
            }
        };
        log.debug("[猫王付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        paramKeys.remove(signType);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
//            if (!pay_productname.equals(paramKeys.get(i)) && !pay_attach.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
            if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
            	signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        //最后一个&转换成#
        signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "");
        String paramsStr = signSrc.toString();
        String signMD5 = RSASignature.sign(paramsStr, channelWrapper.getAPI_KEY());
        log.debug("[猫王付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
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
        	String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,null);
	        if (StringUtils.isBlank(resultStr)) {
	            log.error("[猫王付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(EMPTYRESPONSE);
	        }
	        if (!resultStr.contains("{") || !resultStr.contains("}")) {
	            log.error("[猫王付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        JSONObject resJson;
	        try {
	            resJson = JSONObject.parseObject(resultStr);
	        } catch (Exception e) {
	            e.printStackTrace();
	            log.error("[猫王付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        if (null != resJson && resJson.containsKey("errCode") && resJson.getString("errCode").equals("0000")) {
	        	if(HandlerUtil.isZfbSM(channelWrapper)){
	        		String code_url = resJson.getString("qrCode");
		            result.put(QRCONTEXT, code_url);
	        	}else{
	        		String code_url = resJson.getString("retHtml");
		            result.put(HTMLCONTEXT, code_url);
	        	}
	            
	        }else {
	            log.error("[猫王付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        payResultList.add(result);
        }
        log.debug("[猫王付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[猫王付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}