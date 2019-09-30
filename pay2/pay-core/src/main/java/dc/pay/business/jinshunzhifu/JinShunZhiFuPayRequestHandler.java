package dc.pay.business.jinshunzhifu;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.*;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayRequestHandler;
import dc.pay.utils.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import dc.pay.business.RequestPayResult;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;

/**
 * @author cobby
 * Jan 15, 2019
 */
@RequestPayHandler("JINSHUNZHIFU")
public final class JinShunZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(JinShunZhiFuPayRequestHandler.class);

    private static final String application                   ="application";          //应用名称   ZFBScanOrder
    private static final String version                       ="version";              //通讯协议版本号  1.0.1
    private static final String timestamp                     ="timestamp";            //时间戳  yyyyMMddHHmmss
    private static final String merchantId                    ="merchantId";           //商户代码
    private static final String merchantOrderId               ="merchantOrderId";      //商户订单号  值唯一
    private static final String merchantOrderAmt              ="merchantOrderAmt";     //订单金额 分
    private static final String merchantOrderDesc             ="merchantOrderDesc";    //订单描述 产品名称
    private static final String userName                      ="userName";             //用户名
    private static final String merchantPayNotifyUrl          ="merchantPayNotifyUrl"; //异步通知地址
    private static final String bankId                        ="bankId";  //银行编码
    private static final String accountType                   ="accountType";  //0借记卡，1贷记卡
    private static final String orderTime                     ="orderTime";  //时间戳  yyyyMMddHHmmss
    private static final String rptType                       ="rptType";  //收款方式 1
    private static final String payMode                       ="payMode";  //付款类型 0
    private static final String encode                        ="utf-8"; //

    @Override
    protected Map<String, String> buildPayParam() throws PayException {

        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(application,      channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(version,"1.0.1");
                put(timestamp, DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
                put(merchantId,       channelWrapper.getAPI_MEMBERID());
                put(merchantOrderId,  channelWrapper.getAPI_ORDER_ID());
                put(merchantOrderAmt, channelWrapper.getAPI_AMOUNT());
                put(merchantOrderDesc,channelWrapper.getAPI_ORDER_ID());
                put(userName,channelWrapper.getAPI_ORDER_ID());
                put(merchantPayNotifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                if(HandlerUtil.isWY(channelWrapper)){
                    put(accountType,"0");
                    put(orderTime, DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
                    put(rptType,"1");
                    put(payMode,"0");
                }else{
                    put(timestamp, DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
                }
                //银行代码      不进行签名，支付系统根据该银行代码直接跳转银行网银，不输或输入的银行代码不存在则展示支付首页让用户选择支付方式。
                put(bankId,HandlerUtil.isYLKJ(channelWrapper) ? "" : channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            }
        };
        log.debug("[金顺支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //签名以报文（xml）BASE64编码作为原串，以报文经过MD5转码后用私钥签名再进行BASE64编码作为签名串，最后把原串的BASE64编码信息和签名串用“|”连接作为最终传输串。

        String srcXml = xmlUtil.orderRequestToXml(api_response_params);
        String xmlBase64 =null;
        String xmlBase64Sign =null;
        try {
            xmlBase64Sign = Base64Local.encodeToString(
                SecurityRSAPay.sign(getMD5Digest(srcXml.getBytes(encode)),Base64Local.decode(channelWrapper.getAPI_KEY())),true
            );
            xmlBase64 = Base64Local.encodeToString(srcXml.getBytes(encode),true);//new String(this.encode(srcXml.getBytes(encode)));
        } catch (Exception e) {
            e.printStackTrace();
        }

        String signMd5 = xmlBase64+"|"+xmlBase64Sign;
        if (!StringUtils.isNotBlank(signMd5)){
            log.error("[金顺支付]-[请求支付]-2.1生成加密URL签名异常：null");
            throw new PayException("私钥或公钥配置错误,请检查!");
        }
        log.debug("[金顺支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {

        HashMap<String, String> result = Maps.newHashMap();
        HashMap<String, String> result1 = Maps.newHashMap();
        result1.put("msg",pay_md5sign);
        String url = channelWrapper.getAPI_CHANNEL_BANK_URL();
        try {

            if (HandlerUtil.isWY(channelWrapper)|| HandlerUtil.isWapOrApp(channelWrapper)){
                StringBuffer htmlContent = HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), result1);
                //保存第三方返回值
                if (htmlContent.toString().contains("<header>")){
                    result.put(HTMLCONTEXT,htmlContent.toString());
                }else {
                    log.error("[金顺支付]-[请求支付]-3.0.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(htmlContent) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(new String(Base64Local.decode(htmlContent.toString().getBytes(encode))));
                }
            }else {
                String resultStr = RestTemplateUtil.postXml(url, pay_md5sign);
                String[] e = xmlUtil.split(resultStr, "|");
                if (e.length != 2) {
                    log.error("[金顺支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }

                String responseSrc = new String(Base64Local.decode(e[0].getBytes(encode)));
//              String responseSrc = new String(this.decode(e[0].getBytes(encode)));
                if (!responseSrc.contains("<message")) {
                    log.error("[金顺支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(responseSrc) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(responseSrc);
                }

                Map<String, String> srcXml = xmlUtil.xmlToPaymentNotifyResponse(responseSrc);
                //只取正确的值，其他情况抛出异常
                if (null != srcXml && srcXml.containsKey("respCode") && "000".equalsIgnoreCase(srcXml.get("respCode"))
                        && srcXml.containsKey("codeUrl") && StringUtils.isNotBlank(srcXml.get("codeUrl"))) {
                    String code_url = srcXml.get("codeUrl");
                    result.put(handlerUtil.isWEBWAPAPP_SM(channelWrapper) ? QRCONTEXT : JUMPURL, code_url);
                } else {
                    log.error("[金顺支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(responseSrc) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(responseSrc);
                }
            }
        } catch (Exception e) {
            log.error("[金顺支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[金顺支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[金顺支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }

    public static byte[] getMD5Digest(byte[] buffer) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(buffer);
            return md5.digest();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static final JinShunZhiFuXmlUtil xmlUtil  = new JinShunZhiFuXmlUtil();
}