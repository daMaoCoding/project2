package dc.pay.business.chuizizhifu;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

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
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Jan 29, 2019
 */
@RequestPayHandler("CHUIZIZHIFU")
public final class ChuiZiZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(ChuiZiZhiFuPayRequestHandler.class);

    //参数名 请求参数    请求可空    加入签名    说明
    //订单号 outTradeNo  N   Y   
    private static final String outTradeNo                ="outTradeNo";
    //金额  totalFee        N   Y   单位分
    private static final String totalFee                ="totalFee";
    //类型  type    N   Y   wechat:微信 alipay:支付宝
    private static final String type                ="type";
    //回调地址    notifyUrl        N   Y   
    private static final String notifyUrl                ="notifyUrl";
    //MD5签名   sign    N   N   MD5签名值
//    private static final String sign                ="sign";
    //用户唯一标识  uid N   Y   
    private static final String uid                ="uid";
    //场景  scene        N   Y   如qrcode  jsapi  unionPay
    private static final String scene                ="scene";
    //订单描述    body    Y   Y    支付成功显示页面链接  payTurnyurl N   Y   场景为jsapi/unionPay时必填
    private static final String body                ="body";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="signature";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(outTradeNo,channelWrapper.getAPI_ORDER_ID());
                put(totalFee,  channelWrapper.getAPI_AMOUNT());
                put(type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
                put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(uid, channelWrapper.getAPI_MEMBERID());
                put(scene,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
                put(body,channelWrapper.getAPI_WEB_URL());
            }
        };
        log.debug("[锤子支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
        }
        //最后一个&转换成#
        //signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "#" );
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        signSrc.append(key +"="+ channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[锤子支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(payParam),MediaType.APPLICATION_JSON_VALUE).trim();
//        String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
        //String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
        //if (StringUtils.isBlank(resultStr)) {
        //    log.error("[锤子支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
        //    throw new PayException(resultStr);
        //    //log.error("[锤子支付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
        //    //throw new PayException("返回空,参数："+JSON.toJSONString(map));
        //}
        //if (!resultStr.contains("{") || !resultStr.contains("}")) {
        //   log.error("[锤子支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
        //   throw new PayException(resultStr);
        //}
        //JSONObject jsonObject = JSONObject.parseObject(resultStr);
        JSONObject jsonObject;
        try {
            jsonObject = JSONObject.parseObject(resultStr);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[锤子支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        //只取正确的值，其他情况抛出异常
        if (null != jsonObject && jsonObject.containsKey("resultCode") && "true".equalsIgnoreCase(jsonObject.getString("resultCode"))  && jsonObject.containsKey("data") && StringUtils.isNotBlank(jsonObject.getString("data"))){
            JSONObject jsonObject2 = jsonObject.getJSONObject("data");
            if (null != jsonObject2 && jsonObject2.containsKey("status") && "true".equalsIgnoreCase(jsonObject2.getString("status"))  && 
                    (jsonObject2.containsKey("qrcodeUrl") && StringUtils.isNotBlank(jsonObject2.getString("qrcodeUrl")) || jsonObject2.containsKey("payUrl") && StringUtils.isNotBlank(jsonObject2.getString("payUrl")))){
                if (handlerUtil.isWEBWAPAPP_SM(channelWrapper)) {
//                    result.put(QRCONTEXT, jsonObject2.getString("qrcodeUrl"));
                    result.put(QRCONTEXT, URLDecoder.decode(jsonObject2.getString("qrcodeUrl")));
                }else {
                    result.put(JUMPURL, jsonObject2.getString("payUrl"));
                }
            }
        }else {
            log.error("[锤子支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[锤子支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[锤子支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}