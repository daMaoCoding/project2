package dc.pay.business.esinzhifu;

/**
 * ************************
 * @author tony 3556239829
 */

import com.alibaba.fastjson.JSON;
import com.gargoylesoftware.htmlunit.html.HtmlImage;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayRequestHandler;
import dc.pay.business.RequestPayResult;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.ValidateUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestPayHandler("ESINZHIFU")
public final class ESINPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(ESINPayRequestHandler.class);

     private static final String    P_UserID = "P_UserID";//	必填	商户编号如1000001
     private static final String    P_OrderID = "P_OrderID";//	必填	商户定单号（要保证唯一），长度最长32字符
     private static final String    P_FaceValue = "P_FaceValue";//	必填	申明交易金额
     private static final String    P_ChannelID = "P_ChannelID";//	必填	支付方式，支付方式编码：参照附录6.1
     private static final String    P_Price = "P_Price";//	必填	商品售价
     private static final String    P_Description = "P_Description";//	非必填	支付方式为网银时的银行编码，参照附录6.2
     private static final String    P_Result_URL = "P_Result_URL";//	必填	支付后异步通知地址，UBL参数是以http://或https://开头的 完整URL地址(后台处理）提交的url地址必须外网能访问到， 否则无法通知商户
     private static final String    P_PostKey = "P_PostKey";//	必填	MD5签名结果


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(P_UserID,channelWrapper.getAPI_MEMBERID());
            payParam.put(P_OrderID,channelWrapper.getAPI_ORDER_ID());
            payParam.put(P_FaceValue,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));

            payParam.put(P_Price,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            if(HandlerUtil.isWY(channelWrapper)){
                payParam.put(P_ChannelID,"1");
                payParam.put(P_Description,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            }else{
                payParam.put(P_ChannelID,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            }
            payParam.put(P_Result_URL,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
        }
        log.debug("[Esin支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String paramsStr = String.format("%s|%s|%s|%s|%s|%s|%s",
                params.get(P_UserID),
                params.get(P_OrderID),
                "",
                "",
                params.get(P_FaceValue),
                params.get(P_ChannelID),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[Esin支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)  || HandlerUtil.isWapOrApp(channelWrapper)    ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='post'"));
                payResultList.add(result);
            }else{
				HtmlPage endHtml = handlerUtil.getEndHtml(channelWrapper.getAPI_CHANNEL_BANK_URL(), channelWrapper.getAPI_ORDER_ID(), payParam);
                String qrContent=null;
                if(null!=endHtml  ){
                    HtmlImage payUrlInput=null;
                    if(endHtml.getByXPath("//div[@class='qr-image']/img").size()==1){
                        payUrlInput = (HtmlImage) endHtml.getByXPath("//div[@class='qr-image']/img").get(0);
                    }else if(endHtml.getByXPath("//div[@class='qr']/img").size()==1){
                        payUrlInput = (HtmlImage) endHtml.getByXPath("//div[@class='qr']/img").get(0);
                    }else {  throw new PayException(endHtml.asXml()); }

                    if(payUrlInput!=null ){
                        String qrContentSrc = payUrlInput.getSrcAttribute();
                        if(StringUtils.isNotBlank(qrContentSrc) && qrContentSrc.contains("url=")){
                            qrContentSrc = HandlerUtil.UrlDecode(qrContentSrc);
                            qrContent= qrContentSrc.substring(qrContentSrc.indexOf("url=")+4,qrContentSrc.length());
                        }
                    }
                }
               if(StringUtils.isNotBlank(qrContent)){
                    result.put(QRCONTEXT, qrContent);
                    payResultList.add(result);
                }else {  throw new PayException(endHtml.asXml()); }
            }
        } catch (Exception e) { 
             log.error("[Esin支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[Esin支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
        return payResultList;
    }


    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (null != resultListMap && !resultListMap.isEmpty()) {
            if (resultListMap.size() == 1) {
                Map<String, String> resultMap = resultListMap.get(0);
                requestPayResult = buildResult(resultMap,channelWrapper,requestPayResult);
            }
            if (ValidateUtil.requestesultValdata(requestPayResult)) {
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            } else {
                throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[Esin支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}