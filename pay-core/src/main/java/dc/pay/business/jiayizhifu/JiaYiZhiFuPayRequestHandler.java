package dc.pay.business.jiayizhifu;

/**
 * ************************
 * @author tony 3556239829
 */

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
import dc.pay.utils.*;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequestPayHandler("JIAYIZHIFU")
public final class JiaYiZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(JiaYiZhiFuPayRequestHandler.class);

     private static final String  amount = "amount";       //	金额（纯数字，单位：分）	14	必填
     private static final String  callBackUrl = "callBackUrl";       //	支付结果通知地址	128	必填
     private static final String  callBackViewUrl = "callBackViewUrl";       //	支付结果回显地址	128	必填
     private static final String  charset = "charset";       //	编码格式，固定值：UTF-8	10	必填
     private static final String  goodsName = "goodsName";       //	商品名称（英文或拼音）	20	必填
     private static final String  merNo = "merNo";       //	商户号	16	必填
     private static final String  netway = "netway";       //	支付网关代码，详见附录 [支付网关字典]	16	必填
     private static final String  orderNum = "orderNum";       //	订单号	32	必填
     private static final String  random = "random";       //	随机数	4	必填
     private static final String  version = "version";       //	版本号，固定值：V3.1.0.0	8	必填
     private static final String  sign = "sign";       //	签名	32	必填


     private static final String data = "data";  //
     private static final String merchNo = "merchNo";  //
    //private static final String version = "version";  //=版本号



    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        if(!channelWrapper.getAPI_MEMBERID().contains("&")){
            throw new PayException("商户号格式错误，正确格式请使用&符号链接【商户号】和【MD5秘钥】,如：商户号&MD5秘钥");
        }

        Map<String, String> payParam = Maps.newTreeMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(amount,channelWrapper.getAPI_AMOUNT());
            payParam.put(callBackUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(callBackViewUrl,channelWrapper.getAPI_WEB_URL());
            payParam.put(charset,"UTF-8");
            payParam.put(goodsName,channelWrapper.getAPI_ORDER_ID());
            payParam.put(merNo,channelWrapper.getAPI_MEMBERID().split("&")[0]);
            payParam.put(netway,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(orderNum,channelWrapper.getAPI_ORDER_ID());
            payParam.put(random,HandlerUtil.getRandomStr(3));
            payParam.put(version,"V3.1.0.0");
        }
        log.debug("[嘉亿支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String metaSignJsonStr = HandlerUtil.mapToJson(params);
        String pay_md5sign = HandlerUtil.getMD5UpperCase(metaSignJsonStr+channelWrapper.getAPI_MEMBERID().split("&")[1]);
        log.debug("[嘉亿支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            String ciphertext = RsaUtil.encryptToBase64(JSON.toJSONString(payParam), channelWrapper.getAPI_PUBLIC_KEY());
            Map<String, String> reqMap = new HashMap<String, String>();
            reqMap.put(data, ciphertext);
            reqMap.put(merchNo, channelWrapper.getAPI_MEMBERID().split("&")[0]);
            reqMap.put(version, "V3.1.0.0");

            if (1==2 && HandlerUtil.isWY(channelWrapper) &&  HandlerUtil.isYLKJ(channelWrapper)  &&  HandlerUtil.isWapOrApp(channelWrapper)    ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),reqMap).toString().replace("method='post'","method='post'"));
                payResultList.add(result);
            }else{
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), reqMap, String.class, HttpMethod.POST).trim();
                resultStr = new String(resultStr.getBytes("ISO-8859-1"), "UTF-8");
				if(StringUtils.isNotBlank(resultStr) && resultStr.contains("<form") && !resultStr.contains("{")){
                    result.put(HTMLCONTEXT,resultStr);
                    payResultList.add(result);
                }else{
				
                   JSONObject jsonResultStr = JSON.parseObject(resultStr);
                    if(null!=jsonResultStr && jsonResultStr.containsKey("stateCode") && "00".equalsIgnoreCase(jsonResultStr.getString("stateCode")) && jsonResultStr.containsKey("qrcodeUrl")){
                            if(StringUtils.isNotBlank(jsonResultStr.getString("qrcodeUrl"))){
                                if(HandlerUtil.isWY(channelWrapper)){
                                    result.put(JUMPURL,jsonResultStr.getString("qrcodeUrl"));
                                }else{
                                    result.put(QRCONTEXT,jsonResultStr.getString("qrcodeUrl"));
                                }
                                payResultList.add(result);
                            }
                    }else {
                        throw new PayException(resultStr);
                    }
				}
                 
            }
        } catch (Exception e) { 
             log.error("[嘉亿支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[嘉亿支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[嘉亿支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}