package dc.pay.business.caoliuzhifu;

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
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.util.*;

@RequestPayHandler("CAOLIUZHIFU")
public final class CaoLiuZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(CaoLiuZhiFuPayRequestHandler.class);


    private static final  String appid ="appid";  //	商户号	Y
    private static final  String paytype ="paytype";  //	支付类型	Y
    private static final  String paymoney ="paymoney";  //	金额（元）	Y
    private static final  String ordernumber ="ordernumber";  //	订单号	Y
    private static final  String callbackurl ="callbackurl";  //	异步通知地址	Y
    private static final  String qrurl ="qrurl";  //	是否获取二维码	N
    private static final  String sign ="sign";  //	MD5 签名	N


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(appid, channelWrapper.getAPI_MEMBERID());
                put(paytype, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(paymoney, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(ordernumber, channelWrapper.getAPI_ORDER_ID());
                put(callbackurl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(qrurl, "1");
            }
        };
        log.debug("[草榴支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map api_response_params) throws PayException {
       // appid={}&paytype={}& paymoney={}& ordernumber={}&callbackurl={}appkey
        String paramsStr = String.format("appid=%s&paytype=%s&paymoney=%s&ordernumber=%s&callbackurl=%s%s",
                api_response_params.get(appid),
                api_response_params.get(paytype),
                api_response_params.get(paymoney),
                api_response_params.get(ordernumber),
                api_response_params.get(callbackurl),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[草榴支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;

        try {
            if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)  || HandlerUtil.isZFB(channelWrapper) ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
                payResultList.add(result);
            }else{
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.GET).trim();
                if(StringUtils.isNotBlank(resultStr) && resultStr.contains("<form") && !resultStr.contains("{")){
                    result.put(HTMLCONTEXT,resultStr);
                    payResultList.add(result);
                }else{
                    JSONObject jsonResultStr = JSON.parseObject(resultStr);
                    if(null!=jsonResultStr && jsonResultStr.containsKey("code") && "1".equalsIgnoreCase(jsonResultStr.getString("code")) && jsonResultStr.containsKey("message") && StringUtils.isNotBlank(jsonResultStr.getString("message"))){
                            if(HandlerUtil.isWapOrApp(channelWrapper)){
                                result.put(JUMPURL,  jsonResultStr.getString("message"));
                            }else{
                                result.put(QRCONTEXT,  jsonResultStr.getString("message"));
                            }
                            payResultList.add(result);
                    }else {
                        throw new PayException(resultStr);
                    }
                }

            }
    } catch (Exception e) {
            log.error("[草榴支付]3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(e.getMessage(), e);
        }
        log.debug("[草榴支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
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
        log.debug("[草榴支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}