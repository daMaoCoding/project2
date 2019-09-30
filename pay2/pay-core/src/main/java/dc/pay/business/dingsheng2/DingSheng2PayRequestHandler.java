package dc.pay.business.dingsheng2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
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
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Dec 3, 2018
 */
@RequestPayHandler("DINGSHENG2")
public final class DingSheng2PayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(DingSheng2PayRequestHandler.class);

    //参数                 参数名称               类型（长度）          使用              说明
    //基本参数
    //merchant_code        商家号                 String(16)            必填              商户签约时，NATIVEPAYPAL支付平台分配的唯一商家号。
    //service_type         业务类型               String                必选              固定值：pay.weixin.scan 微信扫码  pay.weixin.wapscan微信移动扫码  pay.weixin.h5 微信H5  pay.alipay.scan 支付宝扫码   pay.alipay.wapscan 支付宝移动扫码  
    //notify_url           异步通知地址           String(200)           必选              支付成功后，NATIVEPAYPAL支付平台会主动通知商家系统，商家系统必须指定接收通知的地址。
    //return_url           同步返回地址           String(200)           必选              支付完成后，同步回调到商家的地址
    //client_ip            客户端IP               String                必选              消费者创建交易时所使用机器的IP或者终端ip，最大长度为15个字符。举例：192.168.1.25
    //sign                 签名                   String                必选              签名数据，具体请见附录的签名规则定义。
    //order_no             商户唯一订单号         String(32)            必选              商户系统订单号，由商户系统生成,保证其唯一性,由数字字母组成，必须16-32位
    //order_time           商户订单时间           String(14)            必选              商户订单时间，格式：yyyyMMddHHmmss，举例：20131101123458
    //amount               商户订单金额           Number(13,2)          必选              该笔订单的总金额，以元为单位，精确到小数点后两位。举例：12.01。
    //coin_type            币种                   String                必选              仅支持CNY
    //product_name         商品名称               String(50)            必选              商品名称，不超过50个字符。举例：onlinepay。
    //extends              公用业务扩展参数       String                可选              格式：参数名1^参数值|参数名2^参数值2说明：多条数据间用"|"间隔举例：name^张三|sex^男
    //device_info          设备类型               String                可选              IOS_WAP 苹果网站    AND_WAP 安卓网站
    //app_name             应用名称               String                可选              网站名称或者APP名称
    //app_id               应用ID                 String                可选              应用市场请提供应用包名,网站提供域名      
    private static final String merchant_code                    ="merchant_code";
    private static final String service_type                     ="service_type";
    private static final String notify_url                       ="notify_url";
    private static final String return_url                       ="return_url";
    private static final String client_ip                        ="client_ip";
//    private static final String sign                             ="sign";
    private static final String order_no                         ="order_no";
    private static final String order_time                       ="order_time";
    private static final String amount                           ="amount";
    private static final String coin_type                        ="coin_type";
    private static final String product_name                     ="product_name";
//    private static final String extends                          ="extends";
    private static final String device_info                      ="device_info";
    private static final String app_name                         ="app_name";
    private static final String app_id                           ="app_id";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merchant_code, channelWrapper.getAPI_MEMBERID());
                put(service_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(return_url,channelWrapper.getAPI_WEB_URL());
                put(client_ip,channelWrapper.getAPI_Client_IP());
                put(order_no,channelWrapper.getAPI_ORDER_ID());
                put(order_time,  DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
                put(amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(coin_type,"CNY");
                put(product_name,"name");
                if (handlerUtil.isWapOrApp(channelWrapper)) {
                    //6 (备注：3 APP-Android，4 APP-IOS，5 APP-Other，6 WEB，7 Windows，8 Mac,9 WAP)
                    //device_info   设备类型    String  可选  IOS_WAP 苹果网站                        AND_WAP 安卓网站
                    if (channelWrapper.getAPI_ORDER_FROM() == "3") {
                        put(device_info,"AND_WAP");
                    }else if (channelWrapper.getAPI_ORDER_FROM() == "4") {
                        put(device_info,"IOS_WAP");
                    }
                    put(app_name,"name");
                    put(app_id,channelWrapper.getAPI_WEB_URL());
                }
            }
        };
        log.debug("[鼎盛2]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
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
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[鼎盛2]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
//        if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
        if (true) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
        }
        
//        else{
//            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
//            //String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
//            if (StringUtils.isBlank(resultStr)) {
//                log.error("[鼎盛2]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//                //log.error("[鼎盛2]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
//                //throw new PayException("返回空,参数："+JSON.toJSONString(map));
//            }
//            if (!resultStr.contains("{") || !resultStr.contains("}")) {
//               log.error("[鼎盛2]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//               throw new PayException(resultStr);
//            }
//            //JSONObject resJson = JSONObject.parseObject(resultStr);
//            JSONObject resJson;
//            try {
//                resJson = JSONObject.parseObject(resultStr);
//            } catch (Exception e) {
//                e.printStackTrace();
//                log.error("[鼎盛2]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//            }
//            //只取正确的值，其他情况抛出异常
//            if (null != resJson && resJson.containsKey("status") && "1".equalsIgnoreCase(resJson.getString("status"))  && resJson.containsKey("codeimg") && StringUtils.isNotBlank(resJson.getString("codeimg"))) {
//                String code_url = resJson.getString("codeimg");
//                result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
//                //if (handlerUtil.isWapOrApp(channelWrapper)) {
//                //    result.put(JUMPURL, code_url);
//                //}else{
//                //    result.put(QRCONTEXT, code_url);
//                //}
//            }else {
//                log.error("[鼎盛2]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//            }
//        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[鼎盛2]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[鼎盛2]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}