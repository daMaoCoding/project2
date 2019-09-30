package dc.pay.business.dayinzhifu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.UnicodeUtil;
import dc.pay.utils.ValidateUtil;


/**
 * 
 * @author andrew
 * Aug 16, 2019
 */
@RequestPayHandler("DAYINZHIFU")
public final class DaYinZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(DaYinZhiFuPayRequestHandler.class);
//    参数项               类型          可否为空            默认值         说明
//    merchantNo        String      否               商户号
//    key               String      否               商户密钥
//    nonce             String      否               随机字符（与获  取签名时的保持    一致）
//    timestamp         Long        否               时间戳（与获取  签名时的保持一 致）
//    sign              String      否               签名（大写,详情 见 1.5）
//    token             String      可以                  令牌

//    参数项               类型          可 否 为空          默认值 说明
//    accessToken       String      否               商户 accessToken
//    outTradeNo        String      否               商户订单号
//    money             long        否               金额(分)
//    type              String      否               T0/T1 付款类型
//    body              String      否               商品描述
//    detail            String      否               商品详情
//    notifyUrl         String      否               后台通知地址
//    productId         String      否               商品 ID
//    successUrl        String      可空              前台通知地址（只是一个跳转地址，无返回）
    private static final String merchantNo               ="merchantNo";
    private static final String key                      ="key";
    private static final String nonce                    ="nonce";
    private static final String timestamp                ="timestamp";
//    private static final String sign                     ="sign";
//    private static final String token                    ="token";
 
//    private static final String accessToken               ="accessToken";
    private static final String outTradeNo                ="outTradeNo";
    private static final String money                     ="money";
    private static final String type                      ="type";
    private static final String body                      ="body";
    private static final String detail                    ="detail";
    private static final String notifyUrl                 ="notifyUrl";
    private static final String productId                 ="productId";
    private static final String successUrl                ="successUrl";
    //merchantIp    String  否       商户Ip（付款客户端的ip）
    private static final String merchantIp                ="merchantIp";
    


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam =new LinkedHashMap<>();
        payParam.put(merchantNo, channelWrapper.getAPI_MEMBERID());
        payParam.put(timestamp, System.currentTimeMillis()+"");
        payParam.put(nonce,UUID.randomUUID().toString().replaceAll("-", "").substring(0, 8));
        payParam.put(key,channelWrapper.getAPI_KEY());
        payParam.put(outTradeNo,channelWrapper.getAPI_ORDER_ID());
        payParam.put(money,channelWrapper.getAPI_AMOUNT());
        payParam.put(type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
        payParam.put(body,channelWrapper.getAPI_ORDER_ID());
        payParam.put(detail,channelWrapper.getAPI_ORDER_ID());
        payParam.put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
        payParam.put(productId,channelWrapper.getAPI_ORDER_ID());
        payParam.put(successUrl,channelWrapper.getAPI_WEB_URL());
        if (channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("DAYINZHIFU_BANK_WAP_WX_SM")) {
            payParam.put(merchantIp,channelWrapper.getAPI_Client_IP());
        }
        log.debug("[大银支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        String signSrc=String.format("%s%s%s%s", 
                merchantNo+"="+api_response_params.get(merchantNo)+"&",
                //no+"="+api_response_params.get(no)+"&",
                nonce+"="+api_response_params.get(nonce)+"&",
                timestamp+"="+api_response_params.get(timestamp)+"&",
                key+"="+api_response_params.get(key)
                );
        String paramsStr = signSrc.toString();
        String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[大银支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return signMD5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();


        Map<String, String> tokenParam =new LinkedHashMap<>();
        tokenParam.put(merchantNo, payParam.get(merchantNo));
        tokenParam.put(timestamp, payParam.get(timestamp));
        tokenParam.put(nonce, payParam.get(nonce));
        tokenParam.put(key, payParam.get(key));
        tokenParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0], tokenParam);
        if (StringUtils.isBlank(resultStr)) {
            log.error("[大银支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(EMPTYRESPONSE);
        }
        resultStr = UnicodeUtil.unicodeToString(resultStr);
        if (!resultStr.contains("{") || !resultStr.contains("}")) {
            log.error("[大银支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
//        System.out.println("resultStr===>"+resultStr);
        JSONObject resJson;
        try {
            resJson = JSONObject.parseObject(resultStr);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[大银支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        if (null != resJson && resJson.containsKey("success") && resJson.getString("success").equals("true")) {
            JSONObject valueJson = resJson.getJSONObject("value");
            String accessToken=valueJson.getString("accessToken");
            Map<String, Object> postParam =new LinkedHashMap<>();
            postParam.put("accessToken", accessToken);
            postParam.put("param", payParam);
            /*JSONObject json = new JSONObject(postParam);
            System.out.println(json.toJSONString());*/
            String resultStr1 = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[1], postParam);
//            System.out.println("resultStr1===>"+resultStr1);
            JSONObject resJson2 = JSONObject.parseObject(resultStr1);
            if(null != resJson2 && resJson2.containsKey("success") && resJson2.getString("success").equals("true")){
                result.put(JUMPURL ,resJson2.getString("value"));
            }else {
                log.error("[大银支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr1) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr1);
            }
        }else {
            log.error("[大银支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
    
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[大银支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[大银支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}