package dc.pay.business.yunlian;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

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
import dc.pay.utils.HttpUtil;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;
import dc.pay.utils.qr.QRCodeUtil;

/**
 * ************************
 *
 * @author beck Aug 06, 2018 3556239829
 */

@RequestPayHandler("YUNLIAN")
public final class YunLianPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(YunLianPayRequestHandler.class);
    private static final String parter = "parter";
    private static final String type = "type";
    private static final String value = "value";
    private static final String orderid = "orderid";
    private static final String callbackurl = "callbackurl";
    private static final String QRCONTEXT = "QrContext";
    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newTreeMap();
        payParam.put(parter, channelWrapper.getAPI_MEMBERID());
        payParam.put(type, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
        payParam.put(value, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
        payParam.put(orderid, channelWrapper.getAPI_ORDER_ID());
        payParam.put(callbackurl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
        log.debug("[云联]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        
        return payParam;
    }
    
    @Override
    protected String buildPaySign(Map payParam) throws PayException {
        String pay_md5sign = null;
        String paramsStr = String.format("parter=%s&type=%s&value=%s&orderid=%s&callbackurl=%s%s",
                payParam.get(parter),
                payParam.get(type),
                payParam.get(value),
                payParam.get(orderid),
                payParam.get(callbackurl),
                channelWrapper.getAPI_KEY());
        pay_md5sign = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[云联]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }
    

    @Override
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        
        try {
            
            //处理微信扫码
            if(channelWrapper.getAPI_CHANNEL_BANK_NAME().equalsIgnoreCase("YUNLIAN_BANK_WEBWAPAPP_WX_SM")){
                String html = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.GET);
                String wechatQrCode = this.handleWechatScan(html);
                result.put(QRCONTEXT, wechatQrCode);
            }else if(channelWrapper.getAPI_CHANNEL_BANK_NAME().equalsIgnoreCase("YUNLIAN_BANK_WEBWAPAPP_ZFB_SM")){
                String html = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.GET);
                String alipayQrCode = this.handleAliPayScan(html);
                result.put(QRCONTEXT, alipayQrCode);
            }else{
                String payUrl = HttpUtil.getURLWithParam(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
                result.put(JUMPURL, payUrl);
            }
            payResultList.add(result);
            
        } catch (Exception e) {
            log.error("[云联]3.2.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(e.getMessage(), e);
        }
        
        log.debug("[云联]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));
        return payResultList;
    }
    

    @Override
    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (null != resultListMap && !resultListMap.isEmpty()) {
            if (resultListMap.size() == 1) {
                Map<String, String> resultMap = resultListMap.get(0);
                requestPayResult = buildResult(resultMap, channelWrapper, requestPayResult);
            }
            if (ValidateUtil.requestesultValdata(requestPayResult)) {
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            } else {
                throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[云联]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
    
    /**
     * 处理微信扫码表单跳转
     * */
    private String handleWechatScan(String html) throws PayException{
        
        Document document = Jsoup.parse(html);
        
        Element formEle = document.getElementsByTag("form").first();
        String action = formEle.attr("action");
        Elements inputEles = document.getElementsByTag("input");
        
        if(StringUtils.isBlank(action)||inputEles.size()==0){
            log.error("[云联]-[请求支付]-5.第三方返回表单错误："+html);
            throw new PayException(html);
        }
        
        Map<String, String> paramas = Maps.newHashMap();
        
        for(Element input :inputEles){
            String type = input.attr("type");
            String name = input.attr("name");
            String value = input.attr("value");
            
            if(type.equalsIgnoreCase("submit") ||name.equalsIgnoreCase("__VIEWSTATE")||name.equalsIgnoreCase("__VIEWSTATEGENERATOR")) continue;
            
            paramas.put(name, value);
        }
                
        String html2 = RestTemplateUtil.sendByRestTemplate(action, paramas, String.class, HttpMethod.GET);
        document = Jsoup.parse(html2);
        Elements images = document.getElementsByTag("img");
        
        if(images == null || images.size()==0){
           log.error("[云联]-[请求支付]-6.第三方返回表单错误："+html2);
           throw new PayException(html2);
        }
        
        String qrCodeUrl = "";
        for(Element img:images){
            String src = img.attr("src");
            if(src.indexOf("qrcode")>=0){
                qrCodeUrl = "https://vip.dddyn.com"+src;
                break;
            }
        }
        
        String qrcode = QRCodeUtil.decodeByUrl(qrCodeUrl);

        return qrcode;
    }
    
    
    /**
     * 处理支付宝扫码表单跳转
     * */
    private String handleAliPayScan(String html) throws PayException{
        
        String regEx = "location.href=\'(.*?)\'";
        // 编译正则表达式
        Pattern pattern = Pattern.compile(regEx,Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(html);
        
        if(matcher.groupCount() == 0){
            log.error("[云联]-[请求支付]-7.第三方返回表单错误："+html);
            throw new PayException(html);
        }
        
        String skipUrl = "";
        while(matcher.find()){
            skipUrl = matcher.group(0);
            skipUrl = skipUrl.replaceAll("location.href=","").replace("'", "");
            break;
        }
                
        String html2 = RestTemplateUtil.sendByRestTemplate(skipUrl, Maps.newHashMap(), String.class, HttpMethod.GET);
        Document document = Jsoup.parse(html2);
        Element hidUrlNode = document.getElementById("hidUrl");
        if(hidUrlNode == null){
            log.error("[云联]-[请求支付]-8.第三方返回表单错误："+html2);
            throw new PayException(html2);
        }
        
        String hidUrl= hidUrlNode.attr("value");
        if(StringUtils.isBlank(hidUrl)){
            log.error("[云联]-[请求支付]-9.第三方返回表单错误："+html2);
            throw new PayException(html2);
        }
        
        String qrCode = HandlerUtil.decodeQRContext(hidUrl);
        
        return qrCode;
    }
    
    
    
}