package dc.pay.business.lezhifut;

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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestPayHandler("LEZHIFUT")
public final class LeZhiFuTPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(LeZhiFuTPayRequestHandler.class);


      private static final String    account = "account";  //    商户编号
      private static final String    order = "order";  //    商户订单号
      private static final String    money = "money";  //     订单金额    12 是0.01单位：元，个别测
      private static final String    paytype  = "paytype";  //    支付类型
      private static final String    type = "type";  //    网银类型
      private static final String    notify = "notify";  //    异步通知URL
      private static final String    callback = "callback";  //    同步跳转URL
      private static final String    ip = "ip";  //    客户IP
      private static final String    sign = "sign";  //      签名    MD5后小写的字符串

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(account,channelWrapper.getAPI_MEMBERID());
            payParam.put(order,channelWrapper.getAPI_ORDER_ID());
            payParam.put(money,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(paytype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
           // payParam.put(type,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(notify,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(callback,channelWrapper.getAPI_WEB_URL());
            payParam.put(ip,channelWrapper.getAPI_Client_IP());
        }
        log.debug("[乐智付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        //account={value}&callback={value}&money={value}&notify={value}&order={value}&paytype={value}&{商户key}
        String paramsStr = String.format("account=%s&callback=%s&money=%s&notify=%s&order=%s&paytype=%s&%s",
                params.get(account),
                params.get(callback),
                params.get(money),
                params.get(notify),
                params.get(order),
                params.get(paytype),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[乐智付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (1==2 && HandlerUtil.isWY(channelWrapper)  &&  HandlerUtil.isYLKJ(channelWrapper)   &&  HandlerUtil.isWapOrApp(channelWrapper)    ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='post'"));
                payResultList.add(result);
            }else{


                HttpHeaders headers = new HttpHeaders();
                headers.add("api-key",channelWrapper.getAPI_KEY());
                //headers.add("Content-Type","application/json");

                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST,headers).trim();
                JSONObject jsonResultStr = JSON.parseObject(resultStr);
          
                    if(null!=jsonResultStr && jsonResultStr.containsKey("code") && "1".equalsIgnoreCase(jsonResultStr.getString("code")) && jsonResultStr.containsKey("payurl")){
                            if(StringUtils.isNotBlank(jsonResultStr.getString("payurl"))){
                                if(HandlerUtil.isYLKJ(channelWrapper)){
                                    result.put(JUMPURL, jsonResultStr.getString("payurl"));
                                    payResultList.add(result);
                                }else{
                                    String payurl = jsonResultStr.getString("payurl");
                                    if(StringUtils.isNotBlank(payurl) && payurl.contains("text=")){
                                        result.put(QRCONTEXT,  payurl.substring(payurl.indexOf("text=")+5) );
                                        payResultList.add(result);
                                    }else if(payurl.startsWith("H")||payurl.startsWith("h")){
                                        result.put(JUMPURL,  payurl);
                                        payResultList.add(result);
                                    }else{
                                        throw new PayException(resultStr);
                                    }
                                }
                            }
                    }else {
                        throw new PayException(resultStr);
                    }

                 
            }
        } catch (Exception e) { 
             log.error("[乐智付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[乐智付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[乐智付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}