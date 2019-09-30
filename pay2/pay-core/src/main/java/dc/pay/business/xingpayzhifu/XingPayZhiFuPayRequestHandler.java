package dc.pay.business.xingpayzhifu;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

import dc.pay.base.processor.ChannelWrapper;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayRequestHandler;
import dc.pay.business.RequestPayResult;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 * 
 * @author andrew
 * Aug 16, 2019
 */
@RequestPayHandler("XINGPAYZHIFU")
public final class XingPayZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XingPayZhiFuPayRequestHandler.class);

    //1.7.【预下单】预下单接口
    //接口地址: http://47.90.45.242/pay/perpare  请求方式 post。
//    private static final String perpare_url                ="http://47.90.45.242/pay/perpare";
    //公共请求参数
    //字段名 是否必传    类型  描述
    //sign    是   String  签名
    private static final String sign                ="sign";
    //data    是   String  业务参数
    private static final String data                ="data";
    //业务参数组成
    //orderId 是   String  订单号码（唯一）
    private static final String orderId                ="orderId";
    //amount  是   String  金额（分）
    private static final String amount                ="amount";
    //returnUrl   否   String  付款成功后页面（url）
    private static final String returnUrl                ="returnUrl";
    //notifyUrl   是   String  回调通知地址（url）
    private static final String notifyUrl                ="notifyUrl";
    //body    是   String  商品描述
    private static final String body                ="body";
    //version 是   String  版本号: 1
    private static final String version                ="version";
    //merchantCode    是   String  商家编号
    private static final String merchantCode                ="merchantCode";
    //terminalIp  否   String  终端IP
    private static final String terminalIp                ="terminalIp";
    
    //1.8.【支付】获取支付链接
    //接口地址: http://47.90.45.242/pay/post请求方式 post。
    //公共请求参数
    //字段名 是否必传    类型  描述
    //sign    是   String  签名
//    private static final String sign                ="sign";
    //data    是   String  业务参数
//    private static final String data                ="data";
    //业务参数组成
    //merchantCode    是   String  商家编号
//    private static final String merchantCode                ="merchantCode";
    //tranId  是   String  交易编号（唯一）
    private static final String tranId                ="tranId";
    //version 是   String  版本号: 1
//    private static final String version                ="version";
    //way 是   String  支付方式    ali_jyes 支付宝JYES    wx_jyes 微信JYES    ali_nxys 支付宝XNYS    wx_nxys 微信NXYS    ali_gpay 普通红包    ali_bank 支付宝转卡    wx_fix_v2 微信固码V2    ali_gm 支付宝个码    union_qr 云闪付    pdd_alipay 拼多多
    private static final String way                ="way";
    
//    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
        if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 2) {
            log.error("[新Gpay支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&通道类型编码值（向第三方获取当前使用通道编码值）" );
            throw new PayException("[新Gpay支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&通道类型编码值（向第三方获取当前使用通道编码值）" );
        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merchantCode, channelWrapper.getAPI_MEMBERID().split("&")[0].trim());
//                put(tranId,channelWrapper.getAPI_ORDER_ID());
                put(tranId,perpare(channelWrapper).getString("tranId"));
                put(version,"1");
//                put(way,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(way,channelWrapper.getAPI_MEMBERID().split("&")[1].trim());
            }
        };
        log.debug("[新Gpay支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
        //2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
        String paramsStr = JSON.toJSONString(api_response_params)+channelWrapper.getAPI_KEY();

        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[新Gpay支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        Map<String,String> result = Maps.newHashMap();
        try {
            Map<String,String> map = new LinkedHashMap<>();
            map.put(data, URLEncoder.encode(toBase64(JSON.toJSONString(payParam)), "utf-8"));
            map.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
            
//            String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0], JSON.toJSONString(map),MediaType.APPLICATION_JSON_VALUE);
            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0], map,"UTF-8");
            //if (StringUtils.isBlank(resultStr)) {
            //    log.error("[新Gpay支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            //    throw new PayException(resultStr);
            //    //log.error("[新Gpay支付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
            //    //throw new PayException("返回空,参数："+JSON.toJSONString(map));
            //}
            resultStr = new String(resultStr.getBytes("ISO-8859-1"), "UTF-8");
            //if (!resultStr.contains("{") || !resultStr.contains("}")) {
            //    log.error("[新Gpay支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            //    throw new PayException(resultStr);
            //}
            
//            JSONObject jsonObject = JSONObject.parseObject(new String(resultStr.getBytes("ISO-8859-1"), "GBK"));
            JSONObject jsonObject = JSONObject.parseObject(new String(resultStr.getBytes("ISO-8859-1"), "GBK"));
            if (null == jsonObject || !jsonObject.containsKey("success") || !"true".equalsIgnoreCase(jsonObject.getString("success"))){
                log.error("[新Gpay支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }        
            String resultStr2 = parseBase64(URLDecoder.decode(jsonObject.getString(data), "utf-8"));
            JSONObject jsonObject2 = JSONObject.parseObject(resultStr2);
            //只取正确的值，其他情况抛出异常
//            if (null != jsonObject && jsonObject.containsKey("success") && "true".equalsIgnoreCase(jsonObject.getString("success"))  && 
//                    (jsonObject.containsKey("payParams") && StringUtils.isNotBlank(jsonObject.getString("payParams")) || 
//                            jsonObject.getJSONObject("payParams").containsKey("codeUrl") && StringUtils.isNotBlank(jsonObject.getJSONObject("payParams").getString("codeUrl")))
//                    ){
            if (jsonObject2.containsKey("payParams") && StringUtils.isNotBlank(jsonObject2.getString("payParams")) &&
                    jsonObject2.getJSONObject("payParams").containsKey("codeUrl") && StringUtils.isNotBlank(jsonObject2.getJSONObject("payParams").getString("codeUrl"))){
//            if (null != jsonObject && jsonObject.containsKey("success") && "true".equalsIgnoreCase(jsonObject.getString("success"))  && jsonObject.containsKey("codeimg") && StringUtils.isNotBlank(jsonObject.getString("codeimg"))) {
                    String code_url = jsonObject2.getJSONObject("payParams").getString("codeUrl");
                    result.put( JUMPURL, code_url);
                    //if (handlerUtil.isWapOrApp(channelWrapper)) {
                    //    result.put(JUMPURL, code_url);
                    //}else{
                    //    result.put(QRCONTEXT, code_url);
                    //}
                //按不同的请求接口，向不同的属性设置值
                //if(channelWrapper.getAPI_CHANNEL_BANK_NAME().contains(PayEumeration.CHANNEL_TYPE._WAP_.name())||channelWrapper.getAPI_CHANNEL_BANK_NAME().contains(PayEumeration.CHANNEL_TYPE._WAPAPP_.name())) {
                //    result.put(JUMPURL, jsonObject.getString("barCode"));
                //}else{
                //    result.put(QRCONTEXT, jsonObject.getString("barCode"));
                //}
            }else {
                log.error("[新Gpay支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
        } catch (Exception e) {
            log.error("[新Gpay支付]-[请求支付]-3.2.发送支付请求，及获取支付请求出错，订单号：" + channelWrapper.getAPI_ORDER_ID() + ",通道名称：" + channelWrapper.getAPI_CHANNEL_BANK_NAME() + ",postUrl:" + channelWrapper.getAPI_CHANNEL_BANK_URL() + ",payForManagerCGIResultJsonObj" + e.getMessage(), e);
            //log.error("[盛盈付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + resultStr + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(e.getMessage(),e);
        }
        List<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[新Gpay支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[新Gpay支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
    
    private JSONObject perpare(ChannelWrapper channelWrapper) throws PayException {

        
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(orderId,channelWrapper.getAPI_ORDER_ID());
                put(amount,channelWrapper.getAPI_AMOUNT());
                put(returnUrl,channelWrapper.getAPI_WEB_URL());
                put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(body,"name");
                put(version,"1");
                put(merchantCode, channelWrapper.getAPI_MEMBERID().split("&")[0].trim());
                put(terminalIp,channelWrapper.getAPI_Client_IP());
            }
        };

        Map<String, String> requestMap = new HashMap<String, String>();
        try {
            requestMap.put(data, URLEncoder.encode(toBase64(JSON.toJSONString(payParam)), "utf-8"));
            requestMap.put(sign, HandlerUtil.getMD5UpperCase(JSON.toJSONString(payParam) + channelWrapper.getAPI_KEY()));        

            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[1], requestMap,"UTF-8");
//            String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[1], requestMap);
//            String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[1], JSON.toJSONString(requestMap),"application/x-www-form-urlencoded");
//            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[1], requestMap, String.class, HttpMethod.POST);
//            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[1], requestMap, String.class, HttpMethod.GET, defaultHeaders);
            resultStr = new String(resultStr.getBytes("ISO-8859-1"), "UTF-8");
            JSONObject jsonObject = JSONObject.parseObject(resultStr);
            if (null != jsonObject && jsonObject.containsKey("success") && "true".equalsIgnoreCase(jsonObject.getString("success"))  && jsonObject.containsKey("data") && StringUtils.isNotBlank(jsonObject.getString("data"))) {
                String parseBase64 = parseBase64(URLDecoder.decode(jsonObject.getString(data), "utf-8"));
                JSONObject jsonObject2 = JSONObject.parseObject(parseBase64);
                if (null != jsonObject2 && jsonObject2.containsKey("tranId") && StringUtils.isNotBlank(jsonObject2.getString("tranId"))){
                    return jsonObject2;
                }
            }else {
                log.error("[新Gpay支付]-[请求支付]-3.1.发送支付请求perpare()，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
        } catch (UnsupportedEncodingException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
            throw new PayException(JSON.toJSONString(payParam));
        }
//        requestMap.put(sign, HandlerUtil.getMD5UpperCase(JSON.toJSONString(payParam) + channelWrapper.getAPI_KEY()));        
//        String resultStr = RestTemplateUtil.postForm(perpare_url, requestMap,"UTF-8");
//        JSONObject jsonObject;
//        try {
//            jsonObject = JSONObject.parseObject(resultStr);
//        } catch (Exception e) {
//            e.printStackTrace();
//            log.error("[新Gpay支付]-[请求支付]-3.1.发送支付请求perpare()，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//            throw new PayException(resultStr);
//        }
//        if (null != jsonObject && jsonObject.containsKey("success") && "true".equalsIgnoreCase(jsonObject.getString("success"))  && jsonObject.containsKey("data") && StringUtils.isNotBlank(jsonObject.getString("data"))) {
//            parseBase64(URLDecoder.decode(jsonObject.getString(data), "utf-8"));
//            return jsonObject;
//        }else {
//            log.error("[新Gpay支付]-[请求支付]-3.1.发送支付请求perpare()，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//            throw new PayException(resultStr);
//        }
//        return null;
        return null;
    }
    
    public static String toBase64(String str) {
        byte[] b = null;
        String s = null;
        try {
            b = str.getBytes("utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (b != null) {
            s = new BASE64Encoder().encode(b);
        }
        return s;
    }

    // 解密
    public static String parseBase64(String s) {
        byte[] b = null;
        String result = null;
        if (s != null) {
            BASE64Decoder decoder = new BASE64Decoder();
            try {
                b = decoder.decodeBuffer(s);
                result = new String(b, "utf-8");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }
}