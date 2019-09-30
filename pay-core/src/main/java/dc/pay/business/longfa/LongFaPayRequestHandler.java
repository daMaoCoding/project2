package dc.pay.business.longfa;


import java.net.URLEncoder;

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
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;
import dc.pay.business.ruijietong.RuiJieTongUtil;

/**
 * 
 * @author andrew
 * Oct 16, 2018
 */
@RequestPayHandler("LONGFA")
public final class LongFaPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(LongFaPayRequestHandler.class);

//     private static final String       merchNo = "merchNo";  //	商户号
     private static final String       version = "version";  //	参数列表

     //参数名          参数含义                 类型/长度      是否必填
     //merchNo        商户号                     String/14      是
     //netwayType     支付类型，参考附录8.1      String/16      是
     //randomNo       随机号                     String/8       是
     //orderNo        商户订单号，唯一           String/32      是
     //amount         金额（单位：分）           String/16      是
     //goodsName      商品名称                   String/20      是
     //notifyUrl      支付结果通知地址           String/128     是
     //notifyViewUrl  回显地址                   String/128     是
     //sign           签名（字母大写）           String/32      是
     private static final String merchNo                   ="merchNo";
     private static final String netwayType                ="netwayType";
     private static final String randomNo                  ="randomNo";
     private static final String orderNo                   ="orderNo";
     private static final String amount                    ="amount";
     private static final String goodsName                 ="goodsName";
     private static final String notifyUrl                 ="notifyUrl";
     private static final String notifyViewUrl             ="notifyViewUrl";

     //signature    数据签名    32    是    　
//     private static final String signature  ="sign";
     
     @Override
     protected Map<String, String> buildPayParam() throws PayException {
         String api_KEY = channelWrapper.getAPI_KEY();
         if (null == api_KEY || !api_KEY.contains("-") || api_KEY.split("-").length != 2) {
             log.error("[隆发]-[请求支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
             throw new PayException("[隆发]-[请求支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
         }
         Map<String, String> payParam = new TreeMap<String, String>() {
             {
                 put(merchNo, channelWrapper.getAPI_MEMBERID());
                 put(netwayType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                 put(randomNo,  HandlerUtil.getRandomStr(8));
                 put(orderNo,channelWrapper.getAPI_ORDER_ID());
                 put(amount, channelWrapper.getAPI_AMOUNT());
                 put(goodsName,"name");
                 put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                 put(notifyViewUrl,channelWrapper.getAPI_WEB_URL());
             }
         };
         log.debug("[隆发]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
         return payParam;
     }

    protected String buildPaySign(Map<String,String> params) throws PayException {
        String content = JSON.toJSONString(params);
        String pay_md5sign = HandlerUtil.getMD5UpperCase(content+channelWrapper.getAPI_KEY().split("-")[0]);
        log.debug("[隆发]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        String resultStr = null;
        try {
            byte[] dataStr = RuiJieTongUtil.encryptByPublicKey(JSON.toJSONString(new TreeMap<>(payParam)).getBytes(RuiJieTongUtil.CHARSET),channelWrapper.getAPI_PUBLIC_KEY());
            String reqParam = "data=" + URLEncoder.encode(java.util.Base64.getEncoder().encodeToString(dataStr), RuiJieTongUtil.CHARSET) + "&merchNo=" + payParam.get(merchNo)+ "&version=" + payParam.get(version);
            resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), reqParam,MediaType.APPLICATION_FORM_URLENCODED_VALUE).trim();
            if (StringUtils.isBlank(resultStr)) {
                log.error("[隆发]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            resultStr = new String(resultStr.getBytes("ISO-8859-1"), "UTF-8");
        } catch (Exception e) {
            log.error("[隆发]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        if (!resultStr.contains("{") || !resultStr.contains("}")) {
           log.error("[隆发]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
           throw new PayException(resultStr);
        }
        JSONObject resJson;
        try {
            resJson = JSONObject.parseObject(resultStr);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[隆发]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        //只取正确的值，其他情况抛出异常
        if (null != resJson && resJson.containsKey("stateCode") && "00".equalsIgnoreCase(resJson.getString("stateCode"))  && resJson.containsKey("qrcodeUrl") && StringUtils.isNotBlank(resJson.getString("qrcodeUrl"))) {
            String code_url = resJson.getString("qrcodeUrl");
            if (handlerUtil.isWapOrApp(channelWrapper)) {
                result.put(JUMPURL, code_url);
            }else if(handlerUtil.isFS(channelWrapper)) {
                result.put(HTMLCONTEXT, code_url);
            }else {
                result.put(QRCONTEXT, code_url);
            }
        }else {
            log.error("[隆发]-[请求支付]-3.5.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[隆发]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
        return payResultList;
    }
    
    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (null != resultListMap && !resultListMap.isEmpty()) {
            if (resultListMap.size() == 1) {
                Map<String, String> resultMap = resultListMap.get(0);
                requestPayResult = buildResult(resultMap,channelWrapper,requestPayResult);
            }
            if (ValidateUtil.requestesultValdata(requestPayResult)) {
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            } else {
                throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[隆发]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}