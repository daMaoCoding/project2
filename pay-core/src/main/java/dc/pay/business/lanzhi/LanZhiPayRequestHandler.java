package dc.pay.business.lanzhi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

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


/**
 * @author sunny
 * Dec 18, 2018
 */
@RequestPayHandler("LANZHI")
public final class LanZhiPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(LanZhiPayRequestHandler.class);

//    变量名称 			变量命名 			长度定义 			说明 
//    版本号 				v				固定值 1.0		固定 1.0
//    商户编号 			uid 			Int 			必填, 在网站注册后获得
//    订单号 				orderid			String(32) 		必填，要求该订单号在客户系统内不重复。
//    商品名称			title			String(100) 	必填，商品名称/订单标题
//    商品描述			note			String(200) 	选填,商品/订单的具体描述信息
//    充值金额			amount			money(100)		必填,订单金额，单位：元，两位小数
//    接收交易结果的通知地址receiveurl		String(500)		必填，商户用来接收交易结果的URL；使用HTTP协议GET方式向此地址发送交易结果(参见:2)
//    用户IP 			userIP 			MAX(60) 		必填
//    订单签名数据 		sign			String(32)		必填，数字签名,参见:1.3，确保订单交易安全
//    支付返回页			returnurl		MAX(255)		必填.支付结束后用户返回到的页面地址

    private static final String v               	="v";
    private static final String uid           		="uid";
    private static final String orderid           	="orderid";
    private static final String title           	="title";
    private static final String amount              ="amount";
    private static final String receiveurl          ="receiveurl";
    private static final String userpara            ="userpara";
    private static final String returnurl           ="returnurl";
    private static final String userIP              ="userIP";
    private static final String note                ="note";
    
    private static final String sign                ="sign";
    private static final String key                 ="key";
    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(uid, channelWrapper.getAPI_MEMBERID());
                put(orderid,channelWrapper.getAPI_ORDER_ID());
                put(amount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(receiveurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(returnurl,channelWrapper.getAPI_WEB_URL());
                put(v,"1.0");
                put(title,channelWrapper.getAPI_ORDER_ID());
                put(userIP,channelWrapper.getAPI_Client_IP());
                put(note,channelWrapper.getAPI_ORDER_ID());
                put(userpara,channelWrapper.getAPI_ORDER_ID());
            }
        };
        log.debug("[蓝资支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
    	String signSrc=String.format("%s%s%s%s%s", 
    			api_response_params.get(uid),
    			api_response_params.get(orderid),
    			api_response_params.get(amount),
    			api_response_params.get(receiveurl),
    			channelWrapper.getAPI_KEY()
    		   );
        String paramsStr = signSrc.toString();
        String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[蓝资支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
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
	        String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,String.class,HttpMethod.GET);
	        if (StringUtils.isBlank(resultStr)) {
	            log.error("[蓝资支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(EMPTYRESPONSE);
	        }
	        resultStr = UnicodeUtil.unicodeToString(resultStr);
	        if (!resultStr.contains("{") || !resultStr.contains("}")) {
	            log.error("[蓝资支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        JSONObject resJson;
	        try {
	            resJson = JSONObject.parseObject(resultStr);
	        } catch (Exception e) {
	            e.printStackTrace();
	            log.error("[蓝资支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        if (null != resJson && resJson.containsKey("result") && resJson.getString("result").equals("ok")) {
	            String code_url = resJson.getString("data");
	            if(HandlerUtil.isZfbSM(channelWrapper)){
	            	result.put( QRCONTEXT , code_url);
	            }else{
	            	result.put( JUMPURL , code_url);
	            }
	        }else {
	            log.error("[蓝资支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        payResultList.add(result);
        }
        log.debug("[蓝资支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[蓝资支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}