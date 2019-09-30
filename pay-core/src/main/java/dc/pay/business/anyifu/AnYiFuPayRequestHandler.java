package dc.pay.business.anyifu;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayRequestHandler;
import dc.pay.business.RequestPayResult;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Nov 7, 2018
 */
@RequestPayHandler("ANYIFU")
public final class AnYiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(AnYiFuPayRequestHandler.class);

    //报文头参数说明：
    //输入参数               head                 
    //字段名        变量名                类型                 说明           可空
    //head          服务名                serviceName         String           具体业务服务名，例如（fund_gpay_query）        N
    //              商户交易流水号        traceNo             String           商户交易流水号，通常和商户请求订单号相同        N
    //              合作商户签约号        senderId            String           商户号        N
    //              请求发送时间          sendTime            String           时间格式201708091107        N
    //              报文编码格式          charset             String           默认utf-8        N
    //              接口版本号            version             String           默认3.0        N
    private static final String serviceName                   ="serviceName";
    private static final String traceNo                       ="traceNo";
    private static final String senderId                      ="senderId";
    private static final String sendTime                      ="sendTime";
    private static final String charset                       ="charset";
    private static final String version                       ="version";
    
    //输入参数                      body
    //字段名                        变量名                 类型             说明        可空
    //交易时间                      transTime              String           交易发起时间，时间格式为：yyyyMMddHHmmss，如:20110707112233        N
    //交易金额                      transAmount            String           订单交易总金额（单位：分）        N
    //交易货币类型                  transCurrency          String           默认：人民币（CNY）        N
    //交易类型                      transType              String           1-收银台 （暂只支持收银台）        N
    //订单号                        orderNo                String           商户唯一订单号(最长度20位 只能英文字母组合 )        N
    //订单描述                      orderDesc              String           订单描述( 请使用英文或数字填写 )        Y
    //产品名称                      productName            String           商户产品名称( 请使用英文或数字填写 )        N
    //付款方账户类型                payerAccType           String           必填（DEBIT-借记卡账户）        N
    //付款方账户所在机构标识        payerInstId            String           非必填；账号对应的银行：见  HYPERLINK  \l "_银行编码" 7、银行编码   Y
    //消息通知地址                  notifyUrl              String           服务器通知地址        N
    //返回页面地址                  pageReturnUrl          String           http://或https开头        N
    //交易方式                      payMode                String           网银：Bank 微信扫码：Wechat   微信WAP ：WechatWap       支付宝扫码：Alipay   支付宝WAP：AlipayWap   QQ扫码：QQ    QQ WAP：QQWAP    京东扫码：JD    网银快捷：BankEX  银联扫码：BankQRCode          N
    private static final String transTime                   ="transTime";
    private static final String transAmount                 ="transAmount";
    private static final String transCurrency               ="transCurrency";
    private static final String transType                   ="transType";
    private static final String orderNo                     ="orderNo";
    private static final String orderDesc                   ="orderDesc";
    private static final String productName                 ="productName";
    private static final String payerAccType                ="payerAccType";
    private static final String payerInstId                 ="payerInstId";
    private static final String notifyUrl                   ="notifyUrl";
    private static final String pageReturnUrl               ="pageReturnUrl";
    private static final String payMode                     ="payMode";
    
    private static final String partner_id             ="partner_id";
    private static final String service_name           ="service_name";
    private static final String rsamsg                 ="rsamsg";
    private static final String md5msg                 ="md5msg";
//    private static final String version                ="version ";

    private static final String head                 ="head";
    private static final String body                 ="body";
    
    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="md5msg";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                //head          服务名                         String           具体业务服务名，例如（fund_gpay_query）        N
                put(serviceName,"fund_gpay_payment");
                put(traceNo,channelWrapper.getAPI_ORDER_ID());
                put(senderId, channelWrapper.getAPI_MEMBERID());
                put(sendTime, DateUtil.formatDateTimeStrByParam("yyyyMMddHHmm"));
                put(charset,"utf-8");
                put(version,"3.0");
                //输入参数                      body
                put(transTime, DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
                put(transAmount, channelWrapper.getAPI_AMOUNT());
                put(transCurrency,"CNY");
//                put(transType,handlerUtil.isWY(channelWrapper) || handlerUtil.isWapOrApp(channelWrapper) ? "1" : "2");
//                技术支持
//                transType 全部都传值1
//                技术支持 2019/2/7 10:26:38
//                1-收银台
                put(transType,"1");
                put(orderNo,channelWrapper.getAPI_ORDER_ID());
                put(orderDesc,"name");
                put(productName,"name");
                put(payerAccType,"DEBIT");
//                put(payerInstId,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(payerInstId,handlerUtil.isWY(channelWrapper) ? channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG() : "1");
                put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(pageReturnUrl,channelWrapper.getAPI_WEB_URL());
                put(payMode,handlerUtil.isWY(channelWrapper) ? "Bank" : channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                
                put(partner_id,channelWrapper.getAPI_MEMBERID());
                put(service_name,"fund_gpay_payment");
            }
        };
        log.debug("[安逸付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }
    
    protected String buildPaySign(Map api_response_params) throws PayException {
        JSONObject headData = new JSONObject(true);
        // 报文编码格式
        headData.put(charset, api_response_params.get(charset));
        // 请求发送时间
        headData.put(sendTime, api_response_params.get(sendTime));
        // 合作商户签约号
        headData.put(senderId, api_response_params.get(senderId));
        // 服务名
        headData.put(serviceName, api_response_params.get(serviceName));
        // 商户交易流水号
        headData.put(traceNo, api_response_params.get(traceNo));
        // 接口版本号
        headData.put(version, api_response_params.get(version));
        JSONObject bodyData = new JSONObject(true);
        // 消息通知地址
        bodyData.put(notifyUrl, api_response_params.get(notifyUrl));
        // 订单描述
        bodyData.put(orderDesc, api_response_params.get(orderDesc));
        // 订单号
        bodyData.put(orderNo, api_response_params.get(orderNo));
        // 返回页面地址
        bodyData.put(pageReturnUrl, api_response_params.get(pageReturnUrl));
        // 交易方式
        bodyData.put(payMode, api_response_params.get(payMode));
        // 付款方账户类型
        bodyData.put(payerAccType, api_response_params.get(payerAccType));
        // 付款方账户所在机构标识
        bodyData.put(payerInstId, api_response_params.get(payerInstId));
        // 产品名称
        bodyData.put(productName, api_response_params.get(productName));
        // 交易金额
        bodyData.put(transAmount, api_response_params.get(transAmount));
        // 交易货币类型
        bodyData.put(transCurrency, api_response_params.get(transCurrency));
        // 交易时间
        bodyData.put(transTime, api_response_params.get(transTime));
        // 交易类型
        bodyData.put(transType, api_response_params.get(transType));;
        JSONObject params = new JSONObject(true);
        params.put(head, headData);
        params.put(body, bodyData);
        String signMd5 = null;
        try {
            signMd5 = HandlerUtil.getMD5UpperCase(URLEncoder.encode(params.toString(), "utf-8")+channelWrapper.getAPI_KEY()).toLowerCase();
        } catch (Exception e1) {
            e1.printStackTrace();
            log.error("[安逸付]-[请求支付]-2.1.生成加密URL签名完成结果：" + JSON.toJSONString(params) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(JSON.toJSONString(params));
        }
        //String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[安逸付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        JSONObject headData = new JSONObject(true);
        // 报文编码格式
        headData.put(charset, payParam.get(charset));
        // 请求发送时间
        headData.put(sendTime, payParam.get(sendTime));
        // 合作商户签约号
        headData.put(senderId, payParam.get(senderId));
        // 服务名
        headData.put(serviceName, payParam.get(serviceName));
        // 商户交易流水号
        headData.put(traceNo, payParam.get(traceNo));
        // 接口版本号
        headData.put(version, payParam.get(version));
        JSONObject bodyData = new JSONObject(true);
        // 消息通知地址
        bodyData.put(notifyUrl, payParam.get(notifyUrl));
        // 订单描述
        bodyData.put(orderDesc, payParam.get(orderDesc));
        // 订单号
        bodyData.put(orderNo, payParam.get(orderNo));
        // 返回页面地址
        bodyData.put(pageReturnUrl, payParam.get(pageReturnUrl));
        // 交易方式
        bodyData.put(payMode, payParam.get(payMode));
        // 付款方账户类型
        bodyData.put(payerAccType, payParam.get(payerAccType));
        // 付款方账户所在机构标识
        bodyData.put(payerInstId, payParam.get(payerInstId));
        // 产品名称
        bodyData.put(productName, payParam.get(productName));
        // 交易金额
        bodyData.put(transAmount, payParam.get(transAmount));
        // 交易货币类型
        bodyData.put(transCurrency, payParam.get(transCurrency));
        // 交易时间
        bodyData.put(transTime, payParam.get(transTime));
        // 交易类型
        bodyData.put(transType, payParam.get(transType));;
        JSONObject params = new JSONObject(true);
        params.put(head, headData);
        params.put(body, bodyData);
        String rsamsgData =  null;
        try {
            rsamsgData = Base64.encode(RSAUtils.encryptByPublicKey(URLEncoder.encode(params.toString(), "utf-8").getBytes("utf-8"), channelWrapper.getAPI_PUBLIC_KEY(), "utf-8"));
        } catch (Exception e1) {
            e1.printStackTrace();
            log.error("[安逸付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(params) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(JSON.toJSONString(params));
        }
        Map<String, String> map = new HashMap<String, String>();
        map.put(partner_id, payParam.get(partner_id));
        map.put(service_name, payParam.get(service_name));
        map.put(rsamsg, rsamsgData);
        map.put(md5msg, pay_md5sign);
        map.put(version, payParam.get(version));
        Map<String,String> result = Maps.newHashMap();
//        if(HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)){
        if(true){
//        if(true){
            StringBuffer htmlContent = HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), map);
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
            //保存第三方返回值
            result.put(HTMLCONTEXT,htmlContent.toString());
        }
        
//        else{
////            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), map,"UTF-8");
////            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), map, String.class, HttpMethod.POST);
//            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), map, String.class, HttpMethod.POST,defaultHeaders);
//            if (StringUtils.isBlank(resultStr)) {
//                log.error("[安逸付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//                //log.error("[安逸付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
//                //throw new PayException("返回空,参数："+JSON.toJSONString(map));
//            }
//            if (resultStr.contains("form")) {
//                log.error("[安逸付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException("请阅读《特殊通道配置说明》"+resultStr);
//            }
//            String qr = QRCodeUtil.decodeByUrl(resultStr);
//            if (StringUtils.isBlank(qr)) {
//                log.error("[安逸付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException("请阅读《特殊通道配置说明》"+resultStr);
//            }
//            result.put(QRCONTEXT, qr);
//        }
        List<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[安逸付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[安逸付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
    
}