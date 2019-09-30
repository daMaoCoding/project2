package dc.pay.business.zhifutong;

import java.io.UnsupportedEncodingException;
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
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.HmacSha256Util;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Jul 11, 2018
 */
@RequestPayHandler("ZHIFUTONG")
public final class ZhiFuTongPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(ZhiFuTongPayRequestHandler.class);

    //2. 公共请求参数
    //参数名         必选      参数值      最大长度       备注
    //appid          是                    （分配）       商户分配的appid
    //pay_type       否                    1              支付方式。1：h5支付，2：PC支付。默认为h5支付
    //timestamp      是                    11             发送请求时间戳。11位
    //nonce_str      是                    32             随机字符串
    //sign           是                                   签名
    private static final String appid                  ="appid";
    private static final String pay_type               ="pay_type";
    private static final String timestamp              ="timestamp";
    private static final String nonce_str              ="nonce_str";
//    private static final String sign                   ="sign";
    //参数名            是否必选       参数值        备注
    //biz_content       是                           商户订单数据。除公共参数外所有请求参数都必须放在这个参数中传递，字段值为json字符串。
    //biz_content业务参数：
    //out_trade_no      商户内部订单号
    //amount            订单金额
    //params            额外参数，会在异步通知时将该参数原样返回。本参数必须进行UrlEncode之后才可以发送
    //return_url        支付成功后直接跳转页面（get方式）
    //notify_url        异步通知。支付完成后 主动通知商户服务器里指定的页面（post方式）
    private static final String biz_content                   ="biz_content";
    private static final String out_trade_no                  ="out_trade_no";
    private static final String amount                        ="amount";
    private static final String params                        ="params";
    private static final String return_url                    ="return_url";
    private static final String notify_url                    ="notify_url";

    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
    	Map<String, String> tMap = new TreeMap<String, String>() {
    		{
    			put(out_trade_no,channelWrapper.getAPI_ORDER_ID());
    			put(amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
    			put(params,channelWrapper.getAPI_MEMBERID());
    			put(return_url,channelWrapper.getAPI_WEB_URL());
    			put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
    		}
    	};
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(appid, channelWrapper.getAPI_MEMBERID());
                put(pay_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(timestamp,System.currentTimeMillis()+"");
                put(nonce_str,handlerUtil.getRandomStr(6));
                put(biz_content,JSON.toJSONString(tMap));
            }
        };
        log.debug("[支付通]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        signSrc.append("key=" + channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HmacSha256Util.digest(paramsStr, channelWrapper.getAPI_KEY()).toLowerCase();
        log.debug("[支付通]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
        if (StringUtils.isBlank(resultStr)) {
            log.error("[支付通]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(payParam));
            throw new PayException("返回空,参数："+JSON.toJSONString(payParam));
        }
        try {
			resultStr = new String(resultStr.getBytes("ISO-8859-1"), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			log.error("[支付通]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
			throw new PayException(resultStr);
		}
        JSONObject resJson = JSONObject.parseObject(resultStr);
        if (!resJson.containsKey("code") || !"10000".equals(resJson.getString("code"))) {
            log.error("[支付通]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        String data = resJson.getString("data");
        if (StringUtils.isBlank(data)) {
            log.error("[支付通]-[请求支付]-3.4.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(payParam));
            throw new PayException(resultStr);
        }
        JSONObject resJson2 = JSONObject.parseObject(data);
        if (handlerUtil.isWapOrApp(channelWrapper)) {
        	result.put(JUMPURL, resJson2.getString("user_pay"));
		}else {
			result.put(QRCONTEXT, resJson2.getString("qrcode"));
		}
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[支付通]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[支付通]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}