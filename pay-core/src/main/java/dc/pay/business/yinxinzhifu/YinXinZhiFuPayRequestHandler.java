package dc.pay.business.yinxinzhifu;

import java.util.*;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayRequestHandler;
import dc.pay.utils.*;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import dc.pay.business.RequestPayResult;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;

/**
 * @author cobby
 * Jan 28, 2019
 */
@RequestPayHandler("YINXINZHIFU")
public final class YinXinZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(YinXinZhiFuPayRequestHandler.class);

    private static final String orderid               ="orderid";     // 必选 订单id,商户自定义生成 QQ777898979797979
    private static final String ordername             ="ordername";   // 必选 订单名称,客户自定义 度极高
    private static final String paymoney              ="paymoney";    // 必选 支付金额 22
    private static final String orderuid              ="orderuid";    // 必选 客户自定义生成 464646471dd
    private static final String paytype               ="paytype";     // 必选 支付方式 11:支付宝
    private static final String notifyurl             ="notifyurl";   // 必选 回调地址,支付结果通知地址 http://baidu.com
    private static final String returnurl             ="returnurl";   // 必选 支付成功前台跳转地址 http://baidu.com
    private static final String orderinfo             ="orderinfo";   // 必选 订单说明 翻开历史
    private static final String isSign                ="isSign";      // 可选 是否验签 Y:验签N:不验签	isSign=Y
    private static final String signType              ="signType";    // 可选 验签类型 MD5
    private static final String payCodeType           ="payCodeType"; // 可选 支付码返回模式,默认是URL,返回我们的一个支付页面(推荐URL)payCode返回的是一个封装好的支付码,如:https://qr.alipay.com/fkx05506sj9bbyscfgeuy6a?t=1540808851366  可以用来生成二维码,也可以浏览器或webview打开直接跳转支付宝	payCodeType=URL
    private static final String sign                  ="sign";        // 可选 验签的签名,appid加orderid	sign=e1508577bacd4e4e9b85951d28d340a0SH-98d6cffa-d742-4f30-9637-0bffb76b0b4b

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="signature";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
	            put(orderid,channelWrapper.getAPI_ORDER_ID());
	            put(ordername,"name");
	            put(paymoney,  HandlerUtil.getYuanWithoutZero(channelWrapper.getAPI_AMOUNT()));
	            put(orderuid,"客户:"+handlerUtil.getRandomStr(8));
	            put(paytype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            put(notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
	            put(returnurl,channelWrapper.getAPI_WEB_URL());
	            put(orderinfo, "订单说明");
	            put(isSign, "Y");
	            put(signType, "MD5");
	            put(payCodeType, "URL");
            }
        };
        log.debug("[银鑫支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {

	     String src = "appid="+channelWrapper.getAPI_MEMBERID() + "&orderid="+api_response_params.get(orderid);
	     String signMd5 = HandlerUtil.getMD5UpperCase(src);
	     String paramsStr = String.format("orderid=%s&ordername=%s&paymoney=%s&orderuid=%s&paytype=%s&" +
					     "notifyurl=%s&returnurl=%s&orderinfo=%s&isSign=%s&signType=%s&payCodeType=%s&sign=%s",
			     api_response_params.get(orderid),
			     api_response_params.get(ordername),
			     api_response_params.get(paymoney),
			     api_response_params.get(orderuid),
			     api_response_params.get(paytype),
			     api_response_params.get(notifyurl),
			     api_response_params.get(returnurl),
			     api_response_params.get(orderinfo),
			     api_response_params.get(isSign),
			     api_response_params.get(signType),
			     api_response_params.get(payCodeType),
			     signMd5
	     );
        System.out.println("签名源串=========>"+paramsStr);

        log.debug("[银鑫支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(paramsStr));
        return paramsStr;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
	    Map<String, String> payParam1 = new HashMap<>();
	    payParam1.put("appid",channelWrapper.getAPI_MEMBERID());
	    payParam1.put("params",pay_md5sign);
	    payParam1.put("isEncryption","N");
        Map<String,String> result = Maps.newHashMap();
	    String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam1,"UTF-8");
            if (StringUtils.isBlank(resultStr)) {
                log.error("[银鑫支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            JSONObject jsonObject;
            try {
                jsonObject = JSONObject.parseObject(resultStr);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("[银鑫支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            if (null != jsonObject && jsonObject.containsKey("status") && "OK".equalsIgnoreCase(jsonObject.getString("status"))
		            && jsonObject.containsKey("data") && StringUtils.isNotBlank(jsonObject.getString("data"))) {
	            jsonObject = JSONObject.parseObject(jsonObject.getString("data"));
                String code_url = jsonObject.getString("payCode");
                result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
            }else {
                log.error("[银鑫支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }

        List<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[银鑫支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[银鑫支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}