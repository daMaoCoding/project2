package dc.pay.business.lefuerweizhifu;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
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
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Jun 10, 2019
 */
@RequestPayHandler("LEFUERWEIZHIFU")
public final class LeFuErWeiZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(LeFuErWeiZhiFuPayRequestHandler.class);

    //请求参数列表
    //字段名 字段说明    最大长度    是否必填    备注
    //merchno 商户号 15  是   商户签约时，本系统分配给商家的唯一标识。
    private static final String merchno                ="merchno";
    //amount  交易金额    12  是   以元为单位
    private static final String amount                ="amount";
    //ip  客户端IP   15  否   微信WAP支付必填,且必须上送终端用户手机上的公网IP
    private static final String ip                ="ip";
    //traceno 商户流水号   32  是   商户网站唯一订单号，由商户系统生成，保证其唯一性。
    private static final String traceno                ="traceno";
    //payType 支付方式    1   是   1-支付宝2-微信
    private static final String payType                ="payType";
    //goodsName   商品名称    30  否   默认取商户名称
    private static final String goodsName                ="goodsName";
    //cust1   自定义域1   100 否   商户网站自定义，系统原样返回给商户网站。
//    private static final String cust1                ="cust1";
    //cust2   自定义域2   100 否   商户网站自定义，系统原样返回给商户网站。
//    private static final String cust2                ="cust2";
    //cust3   自定义域3   100 否   商户网站自定义，系统原样返回给商户网站。
//    private static final String cust3                ="cust3";
    //notifyUrl   通知地址    50  否   交易成功，则给商户发送异步通知。
    private static final String notifyUrl                ="notifyUrl";
    //returnUrl   返回地址    50  否   交易成功，则返回到商家的地址。
    private static final String returnUrl                ="returnUrl";
    //settleType  结算方式    1   否   默认为T+1    1-T+1结算
//    private static final String settleType                ="settleType";
    //signature   数据签名    32  是   对签名数据进行MD5加密的结果。
//    private static final String signature                ="signature";

//    private static final String key        ="token";
    //signature    数据签名    32    是    　
//    private static final String signature  ="signature";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
//        String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
//        if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 2) {
//            log.error("[乐付二维支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号merId&应用appId" );
//            throw new PayException("[乐付二维支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号merId&应用appId" );
//        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merchno, channelWrapper.getAPI_MEMBERID());
                put(amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(ip, channelWrapper.getAPI_Client_IP());
                put(traceno,channelWrapper.getAPI_ORDER_ID());
                put(payType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(goodsName,"name");
                put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(returnUrl,channelWrapper.getAPI_WEB_URL());
            }
        };
        log.debug("[乐付二维支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
        }
        //最后一个&转换成#
        //signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "#" );
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[乐付二维支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
//        if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
        if (false) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
//            result.put(HTMLCONTEXT, getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
        }else{
//            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
        String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
//        String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
//            if (StringUtils.isBlank(resultStr)) {
//                log.error("[乐付二维支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//            }
//            if (!resultStr.contains("{") || !resultStr.contains("}")) {
//                log.error("[乐付二维支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//            }
            JSONObject jsonObject = null;
            try {
                resultStr = new String(resultStr.getBytes("ISO-8859-1"), "GBK");
                jsonObject = JSONObject.parseObject(resultStr);
            } catch (Exception e) {
                log.error("[乐付二维支付]-[请求支付]-3.1.发送支付请求，及获取支付请求出错，订单号：" + channelWrapper.getAPI_ORDER_ID() + ",通道名称：" + channelWrapper.getAPI_CHANNEL_BANK_NAME() + ",postUrl:" + channelWrapper.getAPI_CHANNEL_BANK_URL() + ",payForManagerCGIResultJsonObj" + e.getMessage(), e);
                //log.error("[盛盈付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + resultStr + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(e.getMessage(),e);
            }          
            //只取正确的值，其他情况抛出异常
            if (null != jsonObject && jsonObject.containsKey("respCode") && "00".equalsIgnoreCase(jsonObject.getString("respCode"))  && jsonObject.containsKey("barCode") && StringUtils.isNotBlank(jsonObject.getString("barCode"))) {
                result.put(JUMPURL, jsonObject.getString("barCode"));
                
//                JSONObject jsonObject2 = jsonObject.getJSONObject("data");
//                if (null != jsonObject2 && jsonObject2.containsKey("url") && StringUtils.isNotBlank(jsonObject2.getString("url"))) {
//                    if (handlerUtil.isWapOrApp(channelWrapper) || handlerUtil.isWebWyKjzf(channelWrapper)) {
//                        result.put(JUMPURL, jsonObject2.getString("url"));
//                    }else {
//                        String qr = QRCodeUtil.decodeByUrl(jsonObject2.getString("url"));
//                        if (StringUtils.isBlank(qr)) {
//                            log.error("[乐付二维支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                            throw new PayException(resultStr);
//                        }
//                        result.put(QRCONTEXT, qr);
//                    }
//                }else {
//                    log.error("[乐付二维支付]-[请求支付]-3.5.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                    throw new PayException(resultStr);
//                }
            }else {
                log.error("[乐付二维支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[乐付二维支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[乐付二维支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
    
//    public static void main(String[] args) {  
//        
//        Date date = new Date(System.currentTimeMillis());  // 对应的北京时间是2017-08-24 11:17:10
//         
//        SimpleDateFormat bjSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");     // 北京
//        bjSdf.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));  // 设置北京时区
//         
//        SimpleDateFormat tokyoSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");  // 东京
//        tokyoSdf.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));  // 设置东京时区
//         
//        SimpleDateFormat londonSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // 伦敦
//        londonSdf.setTimeZone(TimeZone.getTimeZone("Europe/London"));  // 设置伦敦时区
//         
//        System.out.println("毫秒数:" + date.getTime() + ", 北京时间:" + bjSdf.format(date));
//        System.out.println("毫秒数:" + date.getTime() + ", 东京时间:" + tokyoSdf.format(date));
//        
//        String timeStr =  DateUtil.formatDateTimeStrByParam("yyyy-MM-dd HH:mm:ss"); // 字面时间
//        SimpleDateFormat bjSdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        bjSdf2.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
//        Date bjDate = null;
//        try {
//            bjDate = bjSdf2.parse(timeStr);
//        } catch (ParseException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }  // 解析
//        System.out.println("字面时间: " + timeStr +",按北京时间来解释:" + bjSdf2.format(bjDate) + ", " + bjDate.getTime());
//         
//        SimpleDateFormat tokyoSdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");  // 东京
//        tokyoSdf2.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));  // 设置东京时区
//        Date tokyoDate = null;
//        try {
//            tokyoDate = tokyoSdf2.parse(timeStr);
//        } catch (ParseException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        } // 解析
//        System.out.println("字面时间: " + timeStr +",按东京时间来解释:"  + tokyoSdf2.format(tokyoDate) + ", " + tokyoDate.getTime());
//        System.out.println(tokyoDate.getTime());
//        System.out.println(bjDate.getSeconds());
//
//    }  
    
    /**
     * String(yyyy-MM-dd HH:mm:ss)转10位时间戳
     * @param time
     * @return
     */
    public static Integer StringToTimestamp(String time){
        int times = 0;
        try {  
            times = (int) ((Timestamp.valueOf(time).getTime())/1000);  
        } catch (Exception e) {  
            e.printStackTrace();  
        }
        if(times==0){
            System.out.println("String转10位时间戳失败");
        }
        return times; 
        
    }
    
    public static void main(String[] args) {    
        Date date = new Date(1391174450000L); // 2014-1-31 21:20:50    
        System.out.println(date);    
        Calendar calendar = Calendar.getInstance();    
        calendar.setTimeZone(TimeZone.getTimeZone("GMT"));    
        // 或者可以 Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));    
        calendar.setTime(date);    
        System.out.println(calendar.get(Calendar.HOUR_OF_DAY) + ":" + calendar.get(Calendar.MINUTE));    
        
        System.out.println(StringToTimestamp(DateUtil.formatDateTimeStrByParam("yyyy-MM-dd HH:mm:ss")).toString());    
    }    
}