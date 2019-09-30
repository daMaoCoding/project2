package dc.pay.business.helibao;

/**
 * ************************
 * @author tony 3556239829
 */

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
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
@RequestPayHandler("HELIBAO")
public final class HelibaoPayRequestHandler extends PayRequestHandler {
    private final Logger log =  LoggerFactory.getLogger(getClass());
    static final String HTML_CONTENT_KEY = "HTML_CONTENT_KEY";
    static final String P10_PERIOD_UNIT = "Day";
    static final String GOODS_NAME  = "Pay";
    @Override
    protected Map<String, String> buildPayParam() throws PayException, UnsupportedEncodingException {
        Map<String, String> bankFlags = HelibaoPayUtil.parseBankFlag(channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
        if(null!=bankFlags && !bankFlags.isEmpty() && bankFlags.size()==3){
            String P1_bizType =bankFlags.get("P1_bizType");
            Map<String, String> paramsMap = new HashMap<String, String>();
            if("AppPay".equalsIgnoreCase(P1_bizType)){
                paramsMap.put("P1_bizType",bankFlags.get("P1_bizType"));
                paramsMap.put("P2_orderId",channelWrapper.getAPI_ORDER_ID());
                paramsMap.put("P3_customerNumber",channelWrapper.getAPI_MEMBERID());
                paramsMap.put("P4_payType",bankFlags.get("P4_payType"));
                paramsMap.put("P5_orderAmount",HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                paramsMap.put("P6_currency","CNY");
                paramsMap.put("P7_authcode","1");
                paramsMap.put("P8_appType",bankFlags.get("P8_appType"));
                paramsMap.put("P9_notifyUrl",channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                paramsMap.put("P10_successToUrl","");
                paramsMap.put("P11_orderIp",HandlerUtil.getRandomIp(channelWrapper));
                paramsMap.put("P12_goodsName",URLDecoder.decode((GOODS_NAME), "UTF-8"));
                paramsMap.put("P13_goodsDetail",URLDecoder.decode(("会员充值"), "UTF-8"));
                paramsMap.put("P14_desc",URLDecoder.decode(("thisIsAppP14_desc@3556239829"), "UTF-8"));
            }if("OnlinePay".equalsIgnoreCase(P1_bizType)){
                paramsMap.put("P1_bizType",bankFlags.get("P1_bizType"));
                paramsMap.put("P2_orderId",channelWrapper.getAPI_ORDER_ID());
                paramsMap.put("P3_customerNumber",channelWrapper.getAPI_MEMBERID());
                paramsMap.put("P4_orderAmount",HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                paramsMap.put("P5_bankId",bankFlags.get("P5_bankId"));
                paramsMap.put("P6_business",bankFlags.get("P6_business"));
                paramsMap.put("P7_timestamp",HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(),"yyyyMMddHHmmss"));
                paramsMap.put("P8_goodsName",URLDecoder.decode((GOODS_NAME), "UTF-8"));
                paramsMap.put("P9_period","1");
                paramsMap.put("P10_periodUnit",P10_PERIOD_UNIT);
                paramsMap.put("P11_callbackUrl","");
                paramsMap.put("P12_serverCallbackUrl",channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL() );
                paramsMap.put("P13_orderIp",HandlerUtil.getRandomIp(channelWrapper));
                paramsMap.put("P14_onlineCardType","");
                paramsMap.put("P15_desc",URLDecoder.decode(("thisIsOnlineP15_desc@3556239829"), "UTF-8"));
            }
            log.debug("[合利宝支付]-[请求支付]-1.组装请求参数完成："+JSON.toJSONString(paramsMap));
            return paramsMap;
        }else{
            log.debug("[合利宝支付]-[请求支付]-1.组装请求参数出错：参数接口名，支付方式，银行代码解析为空或个数不对,订单号："+channelWrapper.getAPI_ORDER_ID()+",Flag:"+channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG()+",解析结果："+JSON.toJSONString(bankFlags));
            throw new PayException(SERVER_MSG.REQUEST_PAY_INFO__ERROR);
        }
    }
    protected String buildPaySign(Map<String,String> payParam) throws PayException {
        if(null!=payParam && !payParam.isEmpty()) {
            String paramsStr = HelibaoPayUtil.generatePayRequest(payParam);
            String signMsg   = HelibaoPayUtil.signData(paramsStr,channelWrapper.getAPI_KEY());
            log.debug("[合利宝支付]-[请求支付]-2.生成加密URL签名完成："+ JSON.toJSONString(signMsg));
            return signMsg;
        }
        return null;
    }
    protected List<Map<String,String>> sendRequestGetResult(Map<String, String> payParam,String pay_md5sign) throws PayException {
        if(null==payParam || !payParam.containsKey("P1_bizType")){
            log.error("[合利宝支付]发送请求前检查参数错误，参数中不包含支付方式P1_bizType，"+JSON.toJSONString(payParam));
            throw new PayException(SERVER_MSG.REQUEST_PAY_INFO__ERROR);
        }
        LinkedList<Map<String,String>> payResultList = Lists.newLinkedList();
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        String epayUrl = channelWrapper.getAPI_CHANNEL_BANK_URL();
        String P1_bizType = payParam.get("P1_bizType");
        if(StringUtils.isNotBlank(P1_bizType) && "AppPay".equalsIgnoreCase(P1_bizType)){
            try {
                String result = RestTemplateUtil.postForm(epayUrl, payParam,"UTF-8").trim();
                if(StringUtils.isBlank(result) || result.startsWith("[")||result.endsWith("]")){
                    throw new PayException(result);
                }else{
                    Map map = JSONObject.fromObject(result);
                    if(null!=map.get("rt2_retCode") && "0000".equalsIgnoreCase((String)map.get("rt2_retCode"))){
                        payResultList.add(map);
                    }else{
                         log.error("[合利宝支付]3.发送支付请求，及获取支付请求结果-获取微信||支付宝并解析出错:{}",result);
                       //  throw  new PayException(HelibaoPayUtil.ServerErrorMsg.getMsgByCode((String)map.get("rt2_retCode")));
                         throw  new PayException(result);
                    }
                }
            } catch (Exception e) {
                log.error("[合利宝支付]3.发送支付请求，及获取支付请求结果-获取微信||支付宝并解析出错，订单号："+channelWrapper.getAPI_ORDER_ID()+",通道名称："+channelWrapper.getAPI_CHANNEL_BANK_NAME()+",postUrl:"+epayUrl+",payForManagerCGIResultJsonObj"+e.getMessage(),e);
                throw new PayException(e.getMessage(),e);
            }
        }else{
            try {
                StringBuffer sbHtml = new StringBuffer();
                sbHtml.append("<form id='postForm' name='mobaopaysubmit' action='"+ epayUrl + "' method='post'>");
                for (Map.Entry<String, String> entry : payParam.entrySet()) {
                    sbHtml.append("<input type='hidden' name='"+ entry.getKey() + "' value='" + entry.getValue()+ "'/>");
                }
                sbHtml.append("</form>");
                sbHtml.append("<script>document.forms['postForm'].submit();</script>");
                Map result = Maps.newHashMap();
                result.put(HTML_CONTENT_KEY, sbHtml.toString());
                payResultList.add(result);
            } catch (Exception e) {
                log.error("[合利宝支付]3.发送支付请求，及获取支付请求结果-获取微信||支付宝并解析出错，订单号："+channelWrapper.getAPI_ORDER_ID()+",通道名称："+channelWrapper.getAPI_CHANNEL_BANK_NAME()+",postUrl:"+epayUrl+",payForManagerCGIResultJsonObj"+e.getMessage(),e);
                throw new PayException(e.getMessage(),e);
            }
        }
        log.debug("[合利宝支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功："+ JSON.toJSONString(payResultList));
        return payResultList;
    }
    protected RequestPayResult buildResult(List<Map<String,String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.ERROR.getCodeValue());
        requestPayResult.setRequestPayamount(channelWrapper.getAPI_AMOUNT());
        requestPayResult.setRequestPayOrderId(channelWrapper.getAPI_ORDER_ID());
        requestPayResult.setRequestPayChannelBankName(channelWrapper.getAPI_CHANNEL_BANK_NAME());
        requestPayResult.setRequestPayOrderCreateTime(channelWrapper.getAPI_OrDER_TIME());
        requestPayResult.setDetail(resultListMap);
        if(null!=resultListMap && !resultListMap.isEmpty() && resultListMap.size()==1){
            Map<String, String> result = resultListMap.get(0);
            if(result.containsKey(HTML_CONTENT_KEY)){
                requestPayResult.setRequestPayHtmlContent(result.get(HTML_CONTENT_KEY));
            }else{
                requestPayResult.setRequestPayQRcodeContent(result.get("rt8_qrcode"));
            }
        }else{
            throw  new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
        }
        if(ValidateUtil.requestesultValdata(requestPayResult)){
            requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
        }else{
            throw  new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
        }
        log.debug("[合利宝支付]-[请求支付]-4.处理请求响应成功："+ JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}