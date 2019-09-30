package dc.pay.business.tiantianfu;

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
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Nov 6, 2018
 */
@RequestPayHandler("TIANTIANFU")
public final class TianTianFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(TianTianFuPayRequestHandler.class);

    //变量名                可空          长度             备注
    //merchantNo             N             32             商户平台订单号
    //orderTime              N             20             商户订单提交时间，格式yyyyMMddHHmmss
    //customerOrderNo        N             20             商户订单号，唯一
    //amount                 N             15             单位：分
    //subject                N             40             商品名称
    //body                   N             80             商品描述
    //payerIp                N             20             付款用户的IP地址
    //payerAccountNo         N/Y           40             付款用户的银行卡卡号(银联扫码此字段不需要)
    //notifyUrl              N             80             商户接收付款成功通知的地址
    //pageUrl                Y             80             付款成功后商户页面跳转地址
    //channel                N             20             银行编码，请参考银行编码，银联云闪付H5填：UNIONPAY
    //payType                N             20             固定值，请参考支付方式编码，银联云闪付H5填：UnionpayH5，扫码：UnionpayScan（信息传递中暂不支持中文）
    //signType               N             20             固定值：MD5
    //sign                   N             256            签名字符串
    private static final String merchantNo                          ="merchantNo";
    private static final String orderTime                           ="orderTime";
    private static final String customerOrderNo                     ="customerOrderNo";
    private static final String amount                              ="amount";
    private static final String subject                             ="subject";
    private static final String body                                ="body";
    private static final String payerIp                             ="payerIp";
//    private static final String payerAccountNo                      ="payerAccountNo";
    private static final String notifyUrl                           ="notifyUrl";
//    private static final String pageUrl                             ="pageUrl";
    private static final String channel                             ="channel";
    private static final String payType                             ="payType";
    private static final String signType                            ="signType";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merchantNo, channelWrapper.getAPI_MEMBERID());
                put(orderTime,  DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
                put(customerOrderNo,channelWrapper.getAPI_ORDER_ID());
                put(amount,  channelWrapper.getAPI_AMOUNT());
                put(subject,"name");
                put(body,"name");
                put(payerIp,channelWrapper.getAPI_Client_IP());
//                put(payerAccountNo,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(channel,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
                put(payType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
                put(signType,"MD5");
            }
        };
        log.debug("[天天付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
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
        signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "" );
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[天天付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
        if (StringUtils.isBlank(resultStr)) {
            log.error("[天天付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
            //log.error("[天天付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
            //throw new PayException("返回空,参数："+JSON.toJSONString(map));
        }
        //JSONObject resJson = JSONObject.parseObject(resultStr);
        JSONObject resJson;
        try {
            resultStr = new String(resultStr.getBytes("ISO-8859-1"), "UTF-8");
            if (!resultStr.contains("{") || !resultStr.contains("}")) {
                log.error("[天天付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            resJson = JSONObject.parseObject(resultStr);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[天天付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        //只取正确的值，其他情况抛出异常
        if (null != resJson && resJson.containsKey("code") && "SUCCESS".equalsIgnoreCase(resJson.getString("code"))  && 
//                (resJson.containsKey("qrCode") && StringUtils.isNotBlank(resJson.getString("qrCode")) ||
//                        resJson.containsKey("payUrl") && StringUtils.isNotBlank(resJson.getString("payUrl")))
                resJson.containsKey("qrCode") && StringUtils.isNotBlank(resJson.getString("qrCode"))
                ) {
            if (handlerUtil.isWapOrApp(channelWrapper)) {
//                result.put(JUMPURL, resJson.getString("payUrl"));
                result.put(JUMPURL, resJson.getString("qrCode"));
            }else{
                result.put(QRCONTEXT, resJson.getString("qrCode"));
            }
        }else {
            log.error("[天天付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[天天付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[天天付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}