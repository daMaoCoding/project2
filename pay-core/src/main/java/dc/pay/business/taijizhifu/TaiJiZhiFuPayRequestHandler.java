package dc.pay.business.taijizhifu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 
 * @author andrew
 * Aug 1, 2019
 */
@RequestPayHandler("TAIJIZHIFU")
public final class TaiJiZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(TaiJiZhiFuPayRequestHandler.class);

    //1）请求报文
    //参数名称    参数含义    格式  出现要求    备注
    //txnType 报文类型    N2  M   01
    private static final String txnType                ="txnType";
    //txnSubType  报文子类    N2  M   11-收银台模式
    private static final String txnSubType                ="txnSubType";
    //secpVer 安全协议版本  AN3..16 M   icp3-1.1   （注意，旧版本的secpver = icp3-1.0）
    private static final String secpVer                ="secpVer";
    //secpMode    安全协议类型  AN4..8  M   固定值 perm
    private static final String secpMode                ="secpMode";
    //macKeyId    密钥识别    ANS1..16    M   密钥编号，由平台提供，现与商户号相同
    private static final String macKeyId                ="macKeyId";
    //orderDate   下单日期    N8  M   YYYYMMdd
    private static final String orderDate                ="orderDate";
    //orderTime   下单时间    N6  M   HHmmss
    private static final String orderTime                ="orderTime";
    //merId   商户代号    AN1..15 M   由平台分配的商户号
    private static final String merId                ="merId";
    //orderId 商户订单号   AN8..32 M   商户系统产生，同一商户同一交易日唯一
    private static final String orderId                ="orderId";
    //payerId 会员编号    AN0..40 M   必填，可带入空字符串
    private static final String payerId                ="payerId";
    //pageReturnUrl   交易结果页面通知地址  ANS1..256   M   
    private static final String pageReturnUrl                ="pageReturnUrl";
    //notifyUrl   交易结果后台通知地址  ANS1..128   M   交易结果以后台通知为准
    private static final String notifyUrl                ="notifyUrl";
    //productTitle    商品名称    ANS0..64    M   用以标注在支付页面主要的商品说明，可带入空字符串
    private static final String productTitle                ="productTitle";
    //txnAmt  交易金额    N1..12  M   单位为分，实际交易金额
    private static final String txnAmt                ="txnAmt";
    //currencyCode    交易币种    NS3 M   默认：156
    private static final String currencyCode                ="currencyCode";
    //timeStamp   时间戳 N14 M   请带入报文(目前)时间，格式：YYYYMMddHHmmss
    private static final String timeStamp                ="timeStamp";
    //mac 签名      M   请参考安全方案
//    private static final String mac                ="mac";

    //扫码
    
    //h5
    //clientIp    客户端ip   N1..15  M   微信H5支付时，必填。为用户真实IP地址
    private static final String clientIp                ="clientIp";
    //sceneBizType    场景业务类型  AN3..11 M   WAP|IOS_APP|ANDROID_APP
    private static final String sceneBizType                ="sceneBizType";
    //wapUrl  WAP网址   ANS1..256   C   当sceneBizType =WAP时必填
    private static final String wapUrl                ="wapUrl";
    //wapName WAP名称   ANS1..48    C   当sceneBizType =WAP时必填
    private static final String wapName                ="wapName";
    //appName 应用名 ANS1..48    C   当sceneBizType =IOS_APP|ANDROID_APP时必填
//    private static final String appName                ="appName";
    //appPackage  应用包名    ANS1..48    C   当sceneBizType =IOS_APP|ANDROID_APP时必填
//    private static final String appPackage                ="appPackage";
    //Mac 签名      M   请参考安全方案
//    private static final String Mac                ="Mac";
    
    
    
    private static final String key        ="k";
    //signature    数据签名    32    是    　
//    private static final String signature  ="signature";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
//        String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
//        if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 3) {
//            log.error("[太极支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&机构号" );
//            throw new PayException("[太极支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&机构号" );
//        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(txnType,"01");
//                put(txnSubType,"11");
                put(txnSubType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(secpVer,"icp3-1.1");
                put(secpMode,"perm");
                //macKeyId    密钥识别    ANS1..16    M   密钥编号，由平台提供，现与商户号相同
                put(macKeyId, channelWrapper.getAPI_MEMBERID());
                put(orderDate, DateUtil.formatDateTimeStrByParam("yyyyMMdd"));
                put(orderTime, DateUtil.formatDateTimeStrByParam("HHmmss"));
                put(merId, channelWrapper.getAPI_MEMBERID());
                put(orderId,channelWrapper.getAPI_ORDER_ID());
                put(payerId,  HandlerUtil.getRandomNumber(6));
                put(pageReturnUrl,channelWrapper.getAPI_WEB_URL());
                put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(productTitle,"1");
                put(txnAmt, channelWrapper.getAPI_AMOUNT());
                put(currencyCode,"156");
                put(timeStamp, DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
                if (handlerUtil.isWapOrApp(channelWrapper)) {
                    put(clientIp, channelWrapper.getAPI_Client_IP());
                    put(sceneBizType, "WAP");
                    put(wapUrl, channelWrapper.getAPI_WEB_URL());
                    put(wapName, "baidu");
                }else if (handlerUtil.isWY(channelWrapper)) {
                    put(txnSubType,"21");
                }
            }
        };
        log.debug("[太极支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        //最后一个&转换成#
        //signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "#" );
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        signSrc.append(key +"="+ channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[太极支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
////        method=pay
//        payParam.put("method", "pay");
        
        HashMap<String, String> result = Maps.newHashMap();

//        if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
        if (handlerUtil.isWebWyKjzf(channelWrapper) || handlerUtil.isYLKJ(channelWrapper) ) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
        }else{
//            String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(payParam),MediaType.APPLICATION_JSON_VALUE);
//            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
            //if (StringUtils.isBlank(resultStr)) {
            //    log.error("[太极支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            //    throw new PayException(resultStr);
            //    //log.error("[太极支付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
            //    //throw new PayException("返回空,参数："+JSON.toJSONString(map));
            //}
//            System.out.println("请求返回=========>"+resultStr);
            //if (!resultStr.contains("{") || !resultStr.contains("}")) {
            //   log.error("[太极支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            //   throw new PayException(resultStr);
            //}
            //JSONObject jsonObject = JSONObject.parseObject(resultStr);
            JSONObject jsonObject;
            try {
                jsonObject = JSONObject.parseObject(resultStr);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("[太极支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            //只取正确的值，其他情况抛出异常
            //if (null != jsonObject && jsonObject.containsKey("result_code") && "SUCCESS".equalsIgnoreCase(jsonObject.getString("result_code"))  && 
            //(jsonObject.containsKey("qrcode") && StringUtils.isNotBlank(jsonObject.getString("qrcode")) || 
            // jsonObject.containsKey("pay_url") && StringUtils.isNotBlank(jsonObject.getString("pay_url")))
            //){
            if (null != jsonObject && jsonObject.containsKey("txnStatus") && "01".equalsIgnoreCase(jsonObject.getString("txnStatus"))  && jsonObject.containsKey("codeImgUrl") && StringUtils.isNotBlank(jsonObject.getString("codeImgUrl"))) {
                String code_url = jsonObject.getString("codeImgUrl");
                result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
                //if (handlerUtil.isWapOrApp(channelWrapper)) {
                //    result.put(JUMPURL, code_url);
                //}else{
                //    result.put(QRCONTEXT, code_url);
                //}
            }else {
                log.error("[太极支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[太极支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[太极支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}