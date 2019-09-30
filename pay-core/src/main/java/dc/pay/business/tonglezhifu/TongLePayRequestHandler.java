package dc.pay.business.tonglezhifu;

/**
 * ************************
 * @author tony 3556239829
 */

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayRequestHandler;
import dc.pay.business.RequestPayResult;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.XmlUtil;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;
import dc.pay.utils.qr.QRCodeUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.codec.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestPayHandler("TONGLEZHIFU")
public final class TongLePayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(TongLePayRequestHandler.class);

    private static final String   biz_content    ="biz_content";        //= "JTdCJTIybWNoX2FwcF9pZCUyMiUzQSUyMmh0dHBzJTNBJTJGJTJGd3d3LmJhaWR1LmNvbSUyMiUyQyUyMmRldmljZV9pbmZvJTIyJTNBJTIyQU5EX1dBUCUyMiUyQyUyMnVhJTIyJTNBJTIyTW96aWxsYSUyRjUuMCslMjhXaW5kb3dzK05UKzEwLjAlM0IrV09XNjQlMjkrQXBwbGVXZWJLaXQlMkY1MzcuMzYrJTI4S0hUTUwlMkMrbGlrZStHZWNrbyUyOStDaHJvbWUlMkY2MS4wLjMxNjMuMTAwK1NhZmFyaSUyRjUzNy4zNiUyMiUyQyUyMm1jaF9hcHBfbmFtZSUyMiUzQSUyMiVFNiU5NCVBRiVFNCVCQiU5OCVFNiVCNSU4QiVFOCVBRiU5NSUyMiUyQyUyMmNhc2hpZXJfZGVzayUyMiUzQSUyMjElMjIlN0Q="
    private static final String   body    ="body";        //= "支付测试"
    private static final String   charset    ="charset";        //= "utf-8"
    private static final String   merchant_id    ="merchant_id";        //= "1000"
    private static final String   nonce_str    ="nonce_str";        //= "152TL8BHL17v0BNz9B14"
    private static final String   notify_url    ="notify_url";        //= "http://localhost:8090/paydemo/pay_notify_url.php"
    private static final String   out_trade_no    ="out_trade_no";        //= "20180605115514"
    private static final String   return_url    ="return_url";        //= "http://localhost:8090/paydemo/pay_return_url.php"
    private static final String   subject    ="subject";        //= "支付测试"
    private static final String   total_fee    ="total_fee";        //= "0.01"
    private static final String   trade_type    ="trade_type";        //= "010007"
    private static final String   user_id    ="user_id";        //= "abc123"
    private static final String   user_ip    ="user_ip";        //= "127.0.0.1"
    private static final String   version    ="version";        //= "1.0"
    private static final String   sign    ="sign";         //= "1.0"


    private static final String    bizStr = "{\"mch_app_id\":\"https://www.baidu.com\",\"device_info\":\"AND_WAP\",\"ua\":\"Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36\",\"mch_app_name\":\"支付测试\",\"cashier_desk\":\"1\"}";
    private static final String    biz_content_str = Base64.encodeToString( HandlerUtil.UrlEncode(bizStr).getBytes());


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
            payParam.put(biz_content ,  biz_content_str);
            payParam.put(body ,  body);
            payParam.put(charset ,  "utf-8");
            payParam.put(merchant_id ,  channelWrapper.getAPI_MEMBERID());
            payParam.put(nonce_str ,  HandlerUtil.randomString(10));
            payParam.put(notify_url ,  channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(out_trade_no ,  channelWrapper.getAPI_ORDER_ID());
            payParam.put(return_url ,  channelWrapper.getAPI_WEB_URL());
            payParam.put(subject ,  subject);
            payParam.put(total_fee ,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(trade_type ,  channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(user_id ,  HandlerUtil.getRandomStr(10));
            payParam.put(user_ip ,  channelWrapper.getAPI_Client_IP());
            payParam.put(version , "1.0");
        log.debug("[同乐]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if(StringUtils.isBlank(params.get(paramKeys.get(i))) || sign.equalsIgnoreCase(paramKeys.get(i).toString()    ))  //
                continue;
            sb.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i))).append("&");
        }
        sb.append("key=" + channelWrapper.getAPI_KEY());
        String signStr = sb.toString(); //.replaceFirst("&key=","")
        pay_md5sign = HandlerUtil.getMD5UpperCase(signStr);
        log.debug("[同乐]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if ( 1==2 && HandlerUtil.isWY(channelWrapper) && HandlerUtil.isYLKJ(channelWrapper)  && HandlerUtil.isWapOrApp(channelWrapper)    ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
                payResultList.add(result);
            }else{
                resultStr = RestTemplateUtil.postXml(channelWrapper.getAPI_CHANNEL_BANK_URL(), XmlUtil.map2Xml(payParam, false, "xml", true));
                Map<String, String> resultMap = XmlUtil.xml2Map(resultStr);
                    if(null!=resultMap && resultMap.containsKey("status") && "0".equalsIgnoreCase(resultMap.get("status")) && resultMap.containsKey("pay_info")){
                            if(StringUtils.isNotBlank(resultMap.get("pay_info"))){

                                if(HandlerUtil.isWapOrApp(channelWrapper)){
                                    result.put(QRCONTEXT,  resultMap.get("pay_info"));
                                }else{
                                    try{
                                       result.put(JUMPURL,  QRCodeUtil.decodeByUrl(resultMap.get("pay_info")));
                                    }catch (Exception e){
                                        result.put(JUMPURL,  resultMap.get("pay_info"));
                                    }
                                }
                                payResultList.add(result);
                            }
                    }else {
                        throw new PayException(resultStr);
                    }
            }
        } catch (Exception e) { 
             log.error("[同乐]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[同乐]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[同乐]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}