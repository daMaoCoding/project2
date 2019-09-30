package dc.pay.business.kmazhifu;

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

@RequestPayHandler("KMAZHIFU")
public final class KmaZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(KmaZhiFuPayRequestHandler.class);

    private static final String      pay_amount ="pay_amount";      //  =100.21
    private static final String      pay_applydate ="pay_applydate";      //  =2018-05-24+13:10:15
    private static final String      pay_bankcode ="pay_bankcode";      //  =902
    private static final String      pay_callbackurl ="pay_callbackurl";      //  =http://www.stfuu.com/demo/page.php
    private static final String      pay_memberid ="pay_memberid";      //  =11800
    private static final String      pay_notifyurl ="pay_notifyurl";      //  =http://www.stfuu.com/demo/server.php
    private static final String      pay_orderid ="pay_orderid";      //  =Y20180524131009239470
    private static final String      pay_md5sign ="pay_md5sign";      //  =3C4B0B076D0397A4BFBF12835E227569
    private static final String      pay_attach ="pay_attach";      //  =1234|456
    private static final String      pay_productname ="pay_productname";      //  =测试商品
    private static final String      location ="window.location.href";


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(pay_memberid ,channelWrapper.getAPI_MEMBERID());
            payParam.put(pay_applydate ,DateUtil.curDateTimeStr());
            payParam.put(pay_bankcode ,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(pay_notifyurl ,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(pay_callbackurl ,channelWrapper.getAPI_WEB_URL());
            payParam.put(pay_amount ,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(pay_attach ,pay_attach);
            payParam.put(pay_productname ,pay_productname);
            payParam.put(pay_orderid ,channelWrapper.getAPI_ORDER_ID());
        }
        log.debug("[KMA支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> payParam) throws PayException {
        String paramsStr = String.format("pay_amount=%s&pay_applydate=%s&pay_bankcode=%s&pay_callbackurl=%s&pay_memberid=%s&pay_notifyurl=%s&pay_orderid=%s&key=%s",
                payParam.get(pay_amount),
                payParam.get(pay_applydate),
                payParam.get(pay_bankcode),
                payParam.get(pay_callbackurl),
                payParam.get(pay_memberid),
                payParam.get(pay_notifyurl),
                payParam.get(pay_orderid),
                channelWrapper.getAPI_KEY());
        String  signMd5 = HandlerUtil.md5(paramsStr);
        log.debug("[KMA支付]]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr = null;
        String qrContent = null;
        try {
                //qq扫码302直接到1张图，第三方要修改。
                if ( HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)  ||HandlerUtil.isWapOrApp(channelWrapper) ||channelWrapper.getAPI_CHANNEL_BANK_NAME().endsWith("WEBWAPAPP_QQ_SM")) {
                    result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());//.replace("method='post'","method='get'"));
                    payResultList.add(result);
                }else{

                         resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST,defaultHeaders).trim();
                        // resultStr = HttpUtil.doPost(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
                       //<script>window.location.href='http://api.unpay.com/PayForm.aspx?Data=MzAwNDI5MjAxODA1MjUwMzI1NTQ1MDUwNDg='</script>

                    if(StringUtils.isNotBlank(resultStr) && resultStr.contains("<form") && !resultStr.contains("{")){
                        result.put(HTMLCONTEXT,resultStr);
                        payResultList.add(result);
                    }else if(StringUtils.isNotBlank(resultStr) ){
                        JSONObject jsonResultStr = JSON.parseObject(resultStr);
                        if(null!=jsonResultStr && jsonResultStr.containsKey("status") && "0000".equalsIgnoreCase(jsonResultStr.getString("status"))
                                && jsonResultStr.containsKey("data") && null!=jsonResultStr.getJSONObject("data") && jsonResultStr.getJSONObject("data").containsKey("code_url")   &&StringUtils.isNotBlank( jsonResultStr.getJSONObject("data").getString("code_url")    ) ){
                            if(HandlerUtil.isWapOrApp(channelWrapper)){
                                result.put(JUMPURL, jsonResultStr.getJSONObject("data").getString("code_url")  );
                            }else{
                                result.put(QRCONTEXT,jsonResultStr.getJSONObject("data").getString("code_url")  );
                            }
                            payResultList.add(result);
                        }else {throw new PayException(resultStr); }
                    }else{ throw new PayException(EMPTYRESPONSE);}


                   }
        }catch (Exception e) {
            log.error("[KMA支付]3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG(),e);
            throw new PayException("无法解析出二维码,第三方首次返回："+resultStr);
        }
        log.debug("[KMA支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[KMA支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}