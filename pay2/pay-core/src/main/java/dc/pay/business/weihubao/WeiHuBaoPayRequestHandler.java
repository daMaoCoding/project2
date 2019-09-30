package dc.pay.business.weihubao;

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
import java.util.List;
import java.util.Map;

@RequestPayHandler("WEIHUBAO")
public final class WeiHuBaoPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(WeiHuBaoPayRequestHandler.class);

     private static final String       mchid = "mchid";        //  是  String
     private static final String       paytype = "paytype";        //  是
     private static final String       time = "time";        //  是  String  订单生成的时间，格式：yyyyMMddHHmmss，如：            20160403152206
     private static final String       amount = "amount";        //  是  Int  总金额，单位：分
     private static final String       out_tradeid = "out_tradeid";        //  是  String  商户系统订单号，最大 32 字符
     private static final String       subject = "subject";        //  是  String  商品描述
     private static final String       version = "version";        //  是  String  版本号：1.0
     private static final String       sign = "sign";        //  是  String  MD5 签名结果，查看 1.7 签名算法
     private static final String       notifyurl = "notifyurl";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(mchid,channelWrapper.getAPI_MEMBERID());
            payParam.put(paytype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(time,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
            payParam.put(amount,channelWrapper.getAPI_AMOUNT());
            payParam.put(out_tradeid,channelWrapper.getAPI_ORDER_ID());
            payParam.put(subject,channelWrapper.getAPI_ORDER_ID());
            payParam.put(version,"1");
            payParam.put(notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
        }
        log.debug("[微互宝]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        //paytype+mchid+amount+out_tradeid+time+商户密钥
        String paramsStr = String.format("%s%s%s%s%s%s",
                params.get(paytype),
                params.get(mchid),
                params.get(amount),
                params.get(out_tradeid),
                params.get(time),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[微互宝]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if ( 1==2 && HandlerUtil.isWY(channelWrapper)&&  HandlerUtil.isYLKJ(channelWrapper) &&  HandlerUtil.isWapOrApp(channelWrapper)    ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='post'"));
                payResultList.add(result);
            }else{
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
                JSONObject jsonResultStr = JSON.parseObject(resultStr);
          
                    if(null!=jsonResultStr && jsonResultStr.containsKey("code") && "1".equalsIgnoreCase(jsonResultStr.getString("code")) && jsonResultStr.containsKey("pay_info")){
                            if(StringUtils.isNotBlank(jsonResultStr.getString("pay_info"))){
                                result.put(JUMPURL,jsonResultStr.getString("pay_info"));
                                payResultList.add(result);
                            }
                    }else {
                        throw new PayException(resultStr);
                    }

                 
            }
        } catch (Exception e) { 
             log.error("[微互宝]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[微互宝]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[微互宝]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}