package dc.pay.business.mashanfu2;

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

@RequestPayHandler("MASHANFU2")
public final class MaShanFu2PayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(MaShanFu2PayRequestHandler.class);

     private static final String       merNo = "merNo";  //	商户号
     private static final String       data = "data";  //	参数列表
     private static final String       sign = "sign";  //	签名

     private static final String     amount = "amount";    // 100【非空，这里是积分，一块钱=100分，最低金额10(1000
     private static final String     channelCode = "channelCode";    //支付类型  QQ,ZFB，ZFBWAP,QQH5【非空】
     private static final String     goodsName = "goodsName";    // "商品名称",【非空】
     private static final String     orderNum = "orderNum";    // 订单号,【非空】
     private static final String     organizationCode = "organizationCode";    // 商户号,【非空】
     private static final String     payResultCallBackUrl = "payResultCallBackUrl";    // 回调地址,【非空】
     private static final String     payViewUrl = "payViewUrl";    // 回显地址,【非空】
     private static final String     remark = "remark";    // 备注,可空
    private static final String     version = "version";    // 1.0




    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        if(!channelWrapper.getAPI_MEMBERID().contains("&")){
            throw new PayException("商户号格式错误，正确格式请使用&符号链接【商户号】和【MD5秘钥】,如：商户号&MD5秘钥");
        }

        Map<String, String> payParam = Maps.newLinkedHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(amount,channelWrapper.getAPI_AMOUNT());
            payParam.put(channelCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(goodsName,channelWrapper.getAPI_ORDER_ID());
            payParam.put(orderNum,channelWrapper.getAPI_ORDER_ID());
            payParam.put(organizationCode,channelWrapper.getAPI_MEMBERID().split("&")[0]);
            payParam.put(payResultCallBackUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(payViewUrl,channelWrapper.getAPI_WEB_URL());
        }

        log.debug("[码闪付2]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String content = JSON.toJSONString(params);
        String pay_md5sign = HandlerUtil.getMD5UpperCase(content+channelWrapper.getAPI_MEMBERID().split("&")[1]);
        log.debug("[码闪付2]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            String ciphertext = RsaUtil.encryptToBase64(JSON.toJSONString(payParam), channelWrapper.getAPI_PUBLIC_KEY());
            Map<String, String> reqMap = new HashMap<String, String>();
            reqMap.put(version,"1.0");
            reqMap.put("data", ciphertext);
            reqMap.put("merNo", channelWrapper.getAPI_MEMBERID().split("&")[0]);
            reqMap.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);

            if (  HandlerUtil.isWY(channelWrapper) ||  HandlerUtil.isYLKJ(channelWrapper)  ||  HandlerUtil.isWapOrApp(channelWrapper)    ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),reqMap).toString().replace("method='post'","method='post'"));
                payResultList.add(result);
            }else{
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), reqMap, String.class, HttpMethod.POST).trim();
				
				if(StringUtils.isNotBlank(resultStr) && (resultStr.contains("<form") ||resultStr.contains("<script"))  && !resultStr.contains("{")){
                    result.put(HTMLCONTEXT,resultStr);
                    payResultList.add(result);
                }else{
				
                   JSONObject jsonResultStr = JSON.parseObject(resultStr);
                    if(null!=jsonResultStr && jsonResultStr.containsKey("status") && "200".equalsIgnoreCase(jsonResultStr.getString("status")) && jsonResultStr.containsKey("data")){
                            if(StringUtils.isNotBlank(jsonResultStr.getString("data"))){
                                if(HandlerUtil.isWapOrApp(channelWrapper)){
                                   result.put(JUMPURL, jsonResultStr.getString("data"));
                                }else{
                                   result.put(QRCONTEXT, jsonResultStr.getString("data"));
                                }
                                payResultList.add(result);
                            }
                    }else {
                        throw new PayException(resultStr);
                    }
				}
                 
            }
        } catch (Exception e) { 
             log.error("[码闪付2]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[码闪付2]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[码闪付2]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}