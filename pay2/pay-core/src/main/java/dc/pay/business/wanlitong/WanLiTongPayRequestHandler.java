package dc.pay.business.wanlitong;

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
import dc.pay.utils.*;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
@RequestPayHandler("WANLITONG")
public final class WanLiTongPayRequestHandler  extends PayRequestHandler {
    private final Logger log =  LoggerFactory.getLogger(WanLiTongPayRequestHandler.class);
    static final String HTML_CONTENT_KEY = "HTML_CONTENT_KEY";
    static final String P10_PERIOD_UNIT = "Day";
    static final String GOODS_NAME  = "Pay";
    @Override
    protected Map<String, String> buildPayParam() throws PayException, UnsupportedEncodingException {
        Map<String, String> paramsMap = new HashMap<String, String>();
    try{
        paramsMap.put("userid",channelWrapper.getAPI_MEMBERID());
        paramsMap.put("orderid",channelWrapper.getAPI_ORDER_ID());
        paramsMap.put("money",HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
        paramsMap.put("url",channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
        paramsMap.put("bankid",channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
        log.debug("[万里通支付]-[请求支付]-1.组装请求参数完成："+JSON.toJSONString(paramsMap));
        return paramsMap;
        }catch (Exception ex){
            log.debug("[万里通支付]-[请求支付]-1.组装请求参数出错：参数接口名，支付方式,订单号："+channelWrapper.getAPI_ORDER_ID()+",Flag:"+channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG()+",解析结果："+JSON.toJSONString(paramsMap));
            throw new PayException(SERVER_MSG.REQUEST_PAY_INFO__ERROR);
        }
    }
    protected String buildPaySign(Map<String,String> payParam) throws PayException {
        if(null!=payParam && !payParam.isEmpty()) {
            String paramsStr = WanLiTongPayUtil.generatePayRequest(payParam,channelWrapper.getAPI_KEY());
            String signMsg   = WanLiTongPayUtil.disguiseMD5(paramsStr);
            log.debug("[万里通支付]-[请求支付]-2.生成加密URL签名完成："+ JSON.toJSONString(signMsg));
            return signMsg;
        }
        return null;
    }
    protected List<Map<String,String>> sendRequestGetResult(Map<String, String> payParam,String pay_md5sign) throws PayException {
        LinkedList<Map<String,String>> payResultList = Lists.newLinkedList();
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        String epayUrl = channelWrapper.getAPI_CHANNEL_BANK_URL();
        String api_channel_bank_name = channelWrapper.getAPI_CHANNEL_BANK_NAME();
        if(StringUtils.isNotBlank(api_channel_bank_name) && api_channel_bank_name.contains("WX_SM") || api_channel_bank_name.contains("ZFB_SM")){
            try {
                String result = RestTemplateUtil.getRestTemplate().getForObject(epayUrl, String.class,payParam).trim();
                String s = RestTemplateUtil.sendByRestTemplate(epayUrl, payParam, String.class, HttpMethod.GET);
                Result firstPayresult = HttpUtil.get(channelWrapper.getAPI_CHANNEL_BANK_URL(), null, payParam);
                if(StringUtils.isBlank(result) || result.startsWith("[")||result.endsWith("]")){
                    throw new PayException(result);
                }else{
                    Map map = JSONObject.fromObject(result);
                    if(null!=map.get("rt2_retCode") && "0000".equalsIgnoreCase((String)map.get("rt2_retCode"))){
                        payResultList.add(map);
                    }else{
                         log.error("[万里通支付]3.发送支付请求，及获取支付请求结果-获取微信||支付宝并解析出错:{}",result);
                         throw  new PayException(WanLiTongPayUtil.ServerErrorMsg.getMsgByCode((String)map.get("rt2_retCode")));
                    }
                }
            } catch (Exception e) {
                log.error("[万里通支付]3.发送支付请求，及获取支付请求结果-获取微信||支付宝并解析出错，订单号："+channelWrapper.getAPI_ORDER_ID()+",通道名称："+channelWrapper.getAPI_CHANNEL_BANK_NAME()+",postUrl:"+epayUrl+",payForManagerCGIResultJsonObj"+e.getMessage(),e);
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
                log.error("[万里通支付]3.发送支付请求，及获取支付请求结果-获取微信||支付宝并解析出错，订单号："+channelWrapper.getAPI_ORDER_ID()+",通道名称："+channelWrapper.getAPI_CHANNEL_BANK_NAME()+",postUrl:"+epayUrl+",payForManagerCGIResultJsonObj"+e.getMessage(),e);
                throw new PayException(e.getMessage(),e);
            }
        }
        log.debug("[万里通支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功："+ JSON.toJSONString(payResultList));
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
        log.debug("[万里通支付]-[请求支付]-4.处理请求响应成功："+ JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}