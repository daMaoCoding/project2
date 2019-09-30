package dc.pay.business.zhaocaizhifu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
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
 * May 15, 2019
 */
@RequestPayHandler("ZHAOCAIZHIFU")
public final class ZhaoCaiZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(ZhaoCaiZhiFuPayRequestHandler.class);

    //名称  说明  类型  可否为空    最大长度
    //payKey  商户唯一标识  String  否   32
    private static final String payKey               ="payKey";
    //orderPrice  订单金额，单位：元    保留小数点后两位    String  否   12
    private static final String orderPrice               ="orderPrice";
    //outTradeNo  商户支付订单号 String  否   30
    private static final String outTradeNo               ="outTradeNo";
    //productCode 产品编码，请查阅本文档2.4  String  否   8
    private static final String productCode               ="productCode";
    //orderTime   下单时间，格式：yyyyMMddHHmmss  String  否   14
    private static final String orderTime               ="orderTime";
    //goods   商品名称    String  否   200
    private static final String goods               ="goods";
    //orderIp 下单IP    String  否   15
    private static final String orderIp               ="orderIp";
    //returnUrl   页面通知地址  String  否   300
    private static final String returnUrl               ="returnUrl";
    //notifyUrl   后台异步通知地址    String  否   300
    private static final String notifyUrl               ="notifyUrl";
    //bankCode    产品为B2C支付时，填银行编码（请查阅本文档2.5），    产品为H5，扫码时，填 OTHER   String  否   10
    private static final String bankCode               ="bankCode";
    //format        format=html时：直接跳转。    format=json时：显示JSON数据，数据结构为： 响应JSON数据表。     String  否   
    private static final String format               ="format";
    //remark  备注  String  是   200
//    private static final String remark               ="remark";
    //sign    签名  String  否   50
//    private static final String sign               ="sign";

    private static final String key        ="paySecret";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        System.out.println(JSON.toJSONString(channelWrapper));
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(payKey,  channelWrapper.getAPI_MEMBERID());
                put(orderPrice, handlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(outTradeNo,channelWrapper.getAPI_ORDER_ID());
                put(productCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(orderTime,  DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
                put(goods,"name");
                put(orderIp,channelWrapper.getAPI_Client_IP());
                put(returnUrl,channelWrapper.getAPI_WEB_URL());
                put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(bankCode,"OTHER");
                put(format,"html");
            }
        };
        log.debug("[招财支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
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
         signSrc.append(key + "="+channelWrapper.getAPI_KEY());
         String paramsStr = signSrc.toString();
         String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
         log.debug("[招财支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
//        if(HandlerUtil.isWapOrApp(channelWrapper)){
//        if(false){
        if(true){
            StringBuffer htmlContent = HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
            //保存第三方返回值
            result.put(HTMLCONTEXT,htmlContent.toString());
        }
        
//        else{
////            String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(payParam),MediaType.APPLICATION_JSON_VALUE).trim();
////            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
//            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
//            //if (StringUtils.isBlank(resultStr)) {
//            //    log.error("[招财支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//            //    throw new PayException(resultStr);
//            //    //log.error("[招财支付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
//            //    //throw new PayException("返回空,参数："+JSON.toJSONString(map));
//            //}
//            System.out.println("请求返回=========>"+resultStr);
//            //if (!resultStr.contains("{") || !resultStr.contains("}")) {
//            //   log.error("[招财支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//            //   throw new PayException(resultStr);
//            //}
//            //JSONObject jsonObject = JSONObject.parseObject(resultStr);
//            JSONObject jsonObject;
//            try {
//                jsonObject = JSONObject.parseObject(resultStr);
//            } catch (Exception e) {
//                e.printStackTrace();
//                log.error("[招财支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//            }
//            //只取正确的值，其他情况抛出异常
//            //if (null != jsonObject && jsonObject.containsKey("result_code") && "SUCCESS".equalsIgnoreCase(jsonObject.getString("result_code"))  && 
//            //(jsonObject.containsKey("qrcode") && StringUtils.isNotBlank(jsonObject.getString("qrcode")) || 
//            // jsonObject.containsKey("pay_url") && StringUtils.isNotBlank(jsonObject.getString("pay_url")))
//            //){
//            if (null != jsonObject && jsonObject.containsKey("resultCode") && "0000".equalsIgnoreCase(jsonObject.getString("resultCode"))  && jsonObject.containsKey("payMessage") && StringUtils.isNotBlank(jsonObject.getString("payMessage"))) {
//                String respType = jsonObject.getString("respType");
//                if ("URL".equalsIgnoreCase(respType)) {
//                    result.put( JUMPURL, jsonObject.getString("payMessage"));
//                }else if ("HTML".equalsIgnoreCase(respType)) {
//                    result.put( HTMLCONTEXT, jsonObject.getString("payMessage"));
//                }
//                //if (handlerUtil.isWapOrApp(channelWrapper)) {
//                //    result.put(JUMPURL, code_url);
//                //}else{
//                //    result.put(QRCONTEXT, code_url);
//                //}
//            }else {
//                log.error("[招财支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//            }
//        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[招财支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[招财支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}