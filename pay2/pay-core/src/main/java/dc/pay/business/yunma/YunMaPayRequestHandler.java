package dc.pay.business.yunma;

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
import dc.pay.utils.UnicodeUtil;
import dc.pay.utils.ValidateUtil;


/**
 * @author sunny
 * 01 05, 2019
 */
@RequestPayHandler("YUNMA")
public final class YunMaPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(YunMaPayRequestHandler.class);

//    字段名称 				含义 					⻓度 					是否必须 				备注
//    merchant_no 			商⼾号 				20 					是 例如： MER0000001
//    nonce_str 			随机串 				32 					是 例如：5bced2873dc
//    sign 					签名 					32 					是
//    request_no 			请求单号 				32 					是 商⼾订单号(商⼾应保证唯⼀性)
//    amount 				总⾦额 				16 					是 订单总⾦额,以元为单位
//    pay_channel 			通道 					20 					是
//    channel_id 			通道ID 				10					 否 指定通道ID
//    request_time 			时间戳 				10 					是 下单请求时间戳
//    notify_url 			回调地址 				100 				否 回调地址，如果不传此参数，不会触发回调
//    goods_name 			商品名称 				100 				是 商品名称，将显⽰在⽀付⻚⾯上
//    return_url 			同步回调地址 			100 				否 扫码类⽀付⽆须此地址

    private static final String merchant_no                  ="merchant_no";
    private static final String nonce_str               	 ="nonce_str";
    private static final String sign                 		 ="sign";
    private static final String request_no           		 ="request_no";
    private static final String amount                 		 ="amount";
    private static final String pay_channel             	 ="pay_channel";
    private static final String request_time             	 ="request_time";
    private static final String notify_url             		 ="notify_url";
    private static final String goods_name             		 ="goods_name";
    private static final String ip_addr             		 ="ip_addr";
    
    private static final String key        ="key";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merchant_no, channelWrapper.getAPI_MEMBERID());
                put(nonce_str, UUID.randomUUID().toString().replace("-", "").toLowerCase());
                put(request_no, channelWrapper.getAPI_ORDER_ID());
                put(amount, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(pay_channel, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(request_time,(System.currentTimeMillis()+"").substring(0,10));
                put(notify_url, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(goods_name, channelWrapper.getAPI_ORDER_ID());
                put(ip_addr, channelWrapper.getAPI_Client_IP());
                //put(app_secret,channelWrapper.getAPI_KEY());
            }
        };
        log.debug("[云码支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
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
        signSrc.append(key+"="+channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[云码支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return signMD5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        //Map<String,String> headersMap = new HashMap<>();
        //headersMap.put("Charset", "UTF-8");
        //headersMap.put("Content-Type", "application/json");
        //String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(),handlerUtil.simpleMapToJsonStr(payParam),headersMap);
        //String resultStr=RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
        String resultStr=RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
        //String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
        if (StringUtils.isBlank(resultStr)) {
            log.error("[云码支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(EMPTYRESPONSE);
        }
        resultStr = UnicodeUtil.unicodeToString(resultStr);
        resultStr=resultStr.replaceAll("\\\\", "");
        if (!resultStr.contains("{") || !resultStr.contains("}")) {
            log.error("[云码支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        JSONObject resJson;
        try {
            resJson = JSONObject.parseObject(resultStr);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[云码支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        if (null != resJson && resJson.containsKey("success") && resJson.getString("success").equals("true")) {
            String code_url = resJson.getString("data");
            JSONObject retJson = JSONObject.parseObject(code_url);
            result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, retJson.getString("bank_url"));
        }else {
            log.error("[云码支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[云码支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[云码支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}