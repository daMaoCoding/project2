package dc.pay.business.jinshanzhifu;

import java.util.*;

import dc.pay.utils.MapUtils;
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
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

@RequestPayHandler("JINSHANZHIFU")
public final class JinShanZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(JinShanZhiFuPayRequestHandler.class);

    private static final String merchant_no      ="merchant_no"; //  商户号   是	例如： MER0000001
    private static final String nonce_str        ="nonce_str";   //  随机串   是	例如：5bced2873dc
    private static final String request_no       ="request_no";  //  请求单号 是	商户订单号(商户应保证唯一性)
    private static final String amount           ="amount";      //  总金额   是	订单总金额,以元为单位
    private static final String pay_channel      ="pay_channel"; //  通道    是	ALH5:支付宝H5; WXH5:微信H5; ALP:支付宝扫码; WXP:微信扫码; KJP:快捷支付 QQP:QQ钱包扫码；WYP:网银支付; YLP:银联扫码; JDP:京东扫码；JDH5:京东H5
//    private static final String channel_id       ="channel_id";  //  通道ID		否	指定通道ID
    private static final String request_time     ="request_time";//  时间戳		是	下单请求时间戳
    private static final String notify_url       ="notify_url";  //  回调地址		否	回调地址，如果不传此参数，不会触发回调
    private static final String goods_name       ="goods_name";  //  商品名称		是	商品名称，将显示在支付页面上
    private static final String ip_addr          ="ip_addr";     //  用户IP地址	是	终端客户IP地址，注意不是商城服务器IP地址或中间平台服务器IP地址，某些情况下，ip_addr不正确时会造成支付不成功
//    private static final String return_url       ="return_url";  //  同步回调地址	否	扫码类支付无须此地址
//    private static final String params           ="params";      //  其他参数		否	其他参数，json格式，例：{“member_id”:”1121”,”accountno”:”*“}
//    private static final String clienttype       ="clienttype";  //  终端类型		否	（1 表示PC端 2表示手机端）
//    private static final String sign             ="sign";        //  签名	    是	例如： AA477964B59E28FE8B87D3BD0D03B5B6
    private static final String key        ="key";


    @Override
    protected Map<String, String> buildPayParam() throws PayException {

        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merchant_no, channelWrapper.getAPI_MEMBERID());
                put(nonce_str, HandlerUtil.getRandomStr(10));
                put(request_no,channelWrapper.getAPI_ORDER_ID());
                put(amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(pay_channel, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(request_time,System.currentTimeMillis()/1000+"");
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(goods_name,channelWrapper.getAPI_ORDER_ID());
                put(ip_addr,channelWrapper.getAPI_Client_IP());
//                put(params,"json");
            }
        };
        log.debug("[金山支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
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
        signSrc.append(key +"="+ channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[金山支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        HashMap<String, String> result = Maps.newHashMap();
        try {

            if (1==2) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString()); //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
            }else{
                String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
                if (StringUtils.isBlank(resultStr)) {
                    log.error("[金山支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
                if (!resultStr.contains("{") || !resultStr.contains("}")) {
                   log.error("[金山支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                   throw new PayException(resultStr);
                }
                JSONObject resJson;
                try {
                    resJson = JSONObject.parseObject(resultStr);
                    resJson = JSONObject.parseObject(resJson.getString("data"));
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("[金山支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
                //只取正确的值，其他情况抛出异常
                if (null != resJson && resJson.containsKey("status") && "2".equalsIgnoreCase(resJson.getString("status"))
                        && resJson.containsKey("sign") && StringUtils.isNotBlank(resJson.getString("sign"))) {
                	String code_url="";
                	if("url".equals(resJson.getString("url_type"))){
                		code_url = resJson.getString("bank_url");
                		result.put( JUMPURL , code_url);
                	}else if("code".equals(resJson.getString("url_type"))){
                		code_url = resJson.getString("code");
                		result.put( JUMPURL , code_url);
                	}else if("html".equals(resJson.getString("url_type"))){
                		result.put(HTMLCONTEXT, resJson.getString("code"));
                	}else{
                		log.error("[金山支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                        throw new PayException(resultStr);
                	}
                }else {
                    log.error("[金山支付]-[请求支付]-3.5.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
            }
            
        } catch (Exception e) {
            log.error("[金山支付]-[请求支付]-3.5.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[金山支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[金山支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}