package dc.pay.business.sdkzhifu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayRequestHandler;
import dc.pay.business.RequestPayResult;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Jan 24, 2019
 */
@RequestPayHandler("SDKZHIFU")
public final class SDKZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(SDKZhiFuPayRequestHandler.class);

    //参数名 类型 是否必填 说明
    //service String Y 默认传wappay
    private static final String service                ="service";
    //appid String Y 商户号，接⼊后由我⽅提供
    private static final String appid                ="appid";
    //extra String Y 透传参数，回调时原样返回给商户
    private static final String extra                ="extra";
    //money String Y ⾦额（单位：分）
    private static final String money                ="money";
    //sign String Y 签名，详⻅签名算法
//    private static final String sign                ="sign";
    //notifyurl String Y 异步回调地址
    private static final String notifyurl                ="notifyurl";
    //callbackurl String N 同步回调地址
    private static final String callbackurl                ="callbackurl";
    //paymethod String Y ⽀付⽅式，具体⽀付⽅式代码⻅附1
    private static final String paymethod                ="paymethod";
    //sign String Y md5(appid + money + extra + key)
//    private static final String sign                ="sign";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="pay_md5sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(service, "wappay");
                put(appid, channelWrapper.getAPI_MEMBERID());
                put(extra, channelWrapper.getAPI_ORDER_ID());
                put(money,  channelWrapper.getAPI_AMOUNT());
                put(notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(callbackurl,channelWrapper.getAPI_WEB_URL());
                put(paymethod,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            }
        };
        log.debug("[sdk支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
         //1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
         //2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
         StringBuffer signSrc= new StringBuffer();
         signSrc.append(api_response_params.get(appid));
         signSrc.append(api_response_params.get(money));
         signSrc.append(api_response_params.get(extra));
         signSrc.append(channelWrapper.getAPI_KEY());
         //删除最后一个字符
         //signSrc.deleteCharAt(paramsStr.length()-1);
         String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[sdk支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
//        if (false) {
//            result.put(HTMLCONTEXT, getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
        }
        
//        else{
//            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
////        String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
////        String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
//            if (StringUtils.isBlank(resultStr)) {
//                log.error("[sdk支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//            }
//            if (!resultStr.contains("{") || !resultStr.contains("}")) {
//                log.error("[sdk支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//            }
//            JSONObject jsonObject = null;
//            try {
//                jsonObject = JSONObject.parseObject(resultStr);
//            } catch (Exception e) {
//                log.error("[sdk支付]-[请求支付]-3.3.发送支付请求，及获取支付请求出错，订单号：" + channelWrapper.getAPI_ORDER_ID() + ",通道名称：" + channelWrapper.getAPI_CHANNEL_BANK_NAME() + ",postUrl:" + channelWrapper.getAPI_CHANNEL_BANK_URL() + ",payForManagerCGIResultJsonObj" + e.getMessage(), e);
//                //log.error("[盛盈付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + resultStr + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(e.getMessage(),e);
//            }          
//            //只取正确的值，其他情况抛出异常
//            //只取正确的值，其他情况抛出异常
//            if (null != jsonObject && jsonObject.containsKey("code") && "0".equalsIgnoreCase(jsonObject.getString("code"))  && 
//              jsonObject.containsKey("data") && StringUtils.isNotBlank(jsonObject.getString("data")) && 
//              jsonObject.containsKey(jsonObject.getJSONObject("data").containsKey("codeurl")) && StringUtils.isNotBlank(jsonObject.getJSONObject("data").getString("codeurl"))
//              
//            ){
//                String code_url = jsonObject.getJSONObject("data").getString("codeurl");
//                result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
//            }else {
//                log.error("[sdk支付]-[请求支付]-3.6.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//            }
//        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[sdk支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[sdk支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
    
}