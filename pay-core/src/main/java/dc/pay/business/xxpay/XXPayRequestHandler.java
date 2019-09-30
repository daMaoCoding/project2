package dc.pay.business.xxpay;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

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
 * Nov 21, 2018
 */
@RequestPayHandler("XXPAY")
public final class XXPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XXPayRequestHandler.class);

    //字段名                     变量名              必填                类型          示例值                                   描述          
    //商户ID                      mchId               是                long          20001222                                  分配的商户号          
    //应用ID                      appId               是                String(32)    0ae8be35ff634e2abe94f5f32f6d5c4f          该商户创建的应用对应的ID          
    //支付产品ID                  productId           否                int           8000                                      支付产品ID          
    //商户订单号                  mchOrderNo          是                String(30)    20160427210604000490                      商户生成的订单号          
    //币种                        currency            是                String(3)     cny                                       三位货币代码,人民币:cny          
    //支付金额                    amount              是                int           100                                       支付金额,单位分          
    //客户端IP                    clientIp            否                String(32)    210.73.10.148                             客户端          IP          地址          
    //设备                        device              否                String(64)    ios10.3.1                                 客户端设备          
    //支付结果前端跳转URL         returnUrl           否                String(128)   http://y.wahpal.com/return.htm            支付结果回调          URL          
    //支付结果后台回调URL         notifyUrl           是                String(128)   http://y.wahpal.com/notify.htm            支付结果回调          URL          
    //商品主题                    subject             是                String(64)    测试商品1                                 商品主题          
    //商品描述                    body                是                String(256    测试商品描述                              商品描述信息          
    //扩展参数1                   param1              否                String(64)    支付中心回调时会原样返回          
    //扩展参数2                   param2              否                String(64)    支付中心回调时会原样返回          
    //附加参数                    extra               是                String(512)   {“openId”:”o2RvowBf7sOVJf8kJksUEMceaDqo”} 特定渠道发起时额外参数,见下面说明          
    //签名                        sign                是                String(32)    C380BEC2BFD727A4B6845133519F3AD6          签名值，详见签名算法          
    private static final String mchId                      ="mchId";
    private static final String appId                      ="appId";
    private static final String productId                  ="productId";
    private static final String mchOrderNo                 ="mchOrderNo";
    private static final String currency                   ="currency";
    private static final String amount                     ="amount";
//    private static final String clientIp                   ="clientIp";
//    private static final String device                     ="device";
//    private static final String returnUrl                  ="returnUrl";
    private static final String notifyUrl                  ="notifyUrl";
    private static final String subject                    ="subject";
    private static final String body                       ="body";
//    private static final String param1                     ="param1";
//    private static final String param2                     ="param2";
    private static final String extra                      ="extra";
//    private static final String sign                       ="sign";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
        if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 2) {
            log.error("[XXPAY]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&应用ID" );
            throw new PayException("[XXPAY]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&应用ID" );
        }
        String encode = null;
        try {
            encode = URLEncoder.encode(channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[XXPAY]-[请求支付]-2.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL()) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(mchId, channelWrapper.getAPI_MEMBERID().split("&")[0]);
                put(appId, channelWrapper.getAPI_MEMBERID().split("&")[1]);
                put(productId,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(mchOrderNo,channelWrapper.getAPI_ORDER_ID());
                put(currency,"cny");
                put(amount,  channelWrapper.getAPI_AMOUNT());
//                put(clientIp,channelWrapper.getAPI_Client_IP());
//                put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
//                put(notifyUrl,encode);
                put(subject,"name");
                put(body,"name");
//                put(param1,channelWrapper.getAPI_MEMBERID());
//                put(param1,channelWrapper.getAPI_MEMBERID().split("&")[0]);
//                put(extra,"1");
                
            }
        };
        payParam.put(notifyUrl,encode);
        log.debug("[XXPAY]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!extra.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        //最后一个&转换成#
        //signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "#" );
        //删除最后一个字符
//        signSrc.deleteCharAt(signSrc.length()-1);
        signSrc.append(key +"="+ channelWrapper.getAPI_KEY());
//        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[XXPAY]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
        }else{
            String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
            //String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
            if (StringUtils.isBlank(resultStr)) {
                log.error("[XXPAY]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
                //log.error("[XXPAY]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
                //throw new PayException("返回空,参数："+JSON.toJSONString(map));
            }
            if (!resultStr.contains("{") || !resultStr.contains("}")) {
               log.error("[XXPAY]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
               throw new PayException(resultStr);
            }
            //JSONObject resJson = JSONObject.parseObject(resultStr);
            JSONObject resJson;
            try {
                resJson = JSONObject.parseObject(resultStr);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("[XXPAY]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            //只取正确的值，其他情况抛出异常
            if (null != resJson && resJson.containsKey("retCode") && "SUCCESS".equalsIgnoreCase(resJson.getString("retCode"))  && resJson.containsKey("payParam") && StringUtils.isNotBlank(resJson.getString("payParam"))) {
                String code_url = resJson.getString("codeimg");
                result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
                //if (handlerUtil.isWapOrApp(channelWrapper)) {
                //    result.put(JUMPURL, code_url);
                //}else{
                //    result.put(QRCONTEXT, code_url);
                //}
            }else {
                log.error("[XXPAY]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[XXPAY]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[XXPAY]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}