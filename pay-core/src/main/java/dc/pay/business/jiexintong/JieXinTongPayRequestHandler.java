package dc.pay.business.jiexintong;

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
import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequestPayHandler("JIEXINTONG")
public final class JieXinTongPayRequestHandler extends PayRequestHandler {
    private static final Logger log =  LoggerFactory.getLogger(JieXinTongPayRequestHandler.class);
    private static  final String  MerCode 	     = "MerCode";
    private static  final String  MerOrderNo 	 = "MerOrderNo";
    private static  final String  Amount	     = "Amount";
    private static  final String  OrderDate	     = "OrderDate";
    private static  final String  Currency	     = "Currency";
    private static  final String  GatewayType	 = "GatewayType";
    private static  final String  OrderEncodeType= "OrderEncodeType";
    private static  final String  RetEncodeType	 = "RetEncodeType";
    private static  final String  Rettype	     = "Rettype";
    private static  final String  ServerUrl	     = "ServerUrl";
    private static  final String  DoCredit	     = "DoCredit";
    private static  final String  BankCode	     = "BankCode";
    private static  final String  Language	     = "Language";
    private static  final String  ReturnUrl	     = "ReturnUrl";
    private static  final String  SignMD5	     = "SignMD5";
    private static  final String QRCONTEXT = "QrContext";
    private static  final String HTMLCONTEXT = "HtmlContext";
    private static  final String PARSEHTML = "parseHtml";
    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newTreeMap();
        if(channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("WEB_SM")){
            payParam.put(MerCode, channelWrapper.getAPI_MEMBERID());
            payParam.put(MerOrderNo, channelWrapper.getAPI_ORDER_ID());
            payParam.put(Amount, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(OrderDate, HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(),"yyyyMMdd"));
            payParam.put(Currency, "RMB");
            payParam.put(GatewayType, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
            payParam.put(OrderEncodeType, "2");
            payParam.put(RetEncodeType, "12");
            payParam.put(Rettype, "1");
            payParam.put(ServerUrl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(DoCredit, "1");
            payParam.put(Language, "GB");
            payParam.put(ReturnUrl, "http://www.baidu.com");
            payParam.put("GoodsInfo", "GoodsInfoABC");
            payParam.put(BankCode, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
        }
        log.debug("[捷信通]-[请求支付]-1.组装请求参数完成："+JSON.toJSONString(payParam));
        return payParam;
    }
    protected String buildPaySign(Map<String,String> payParam) throws PayException {
        String paramsStr = payParam.get(MerOrderNo)+payParam.get(Amount)+payParam.get(OrderDate)+payParam.get(Currency)+channelWrapper.getAPI_KEY();
        String pay_md5sign = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[捷信通]-[请求支付]-2.生成加密URL签名完成："+ JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }
    protected List<Map<String,String>> sendRequestGetResult(Map<String, String> payParam,String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String,String>> payResultList = Lists.newArrayList();
        try {
            String channel_flag = channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG();
             String firstPayresult1 = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam,null);
            Result firstPayresult = HttpUtil.get(channelWrapper.getAPI_CHANNEL_BANK_URL(), null, payParam);
            if(firstPayresult.getBody().length()<10){
                log.error("[捷信通]3.发送支付请求，及获取支付请求结果："+firstPayresult.getBody()+"订单号："+channelWrapper.getAPI_ORDER_ID()+" ,通道："+channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(firstPayresult.getBody());
            }
            if(200 ==firstPayresult.getStatusCode() &&  0<firstPayresult.getBody().length()){
                 if(channel_flag.equalsIgnoreCase("TENPAY")  || channel_flag.equalsIgnoreCase("WEIXINWAP")){
                    if(firstPayresult.getBody().contains("data=")  && firstPayresult.getBody().contains("<script>")){
                        String body = firstPayresult.getBody();
                        String subString = body.substring(body.indexOf("data=")+5);
                        subString = subString.substring(0,subString.indexOf("<script>"));
                        String tenPayQrContext = HandlerUtil.UrlDecode(subString);
                        HashMap<String, String> result = Maps.newHashMap();
                        result.put(QRCONTEXT,tenPayQrContext);
                        result.put(PARSEHTML,body);
                        payResultList.add(result);
                    }
                }else if(channel_flag.equalsIgnoreCase("WEIXIN")|| channel_flag.equalsIgnoreCase("ALIPAY")){
                    Document document = Jsoup.parse(firstPayresult.getBody());
                    String imgSrc = document.select("div#divQRCode img ").first().attr("src");
                    String wxQRContext = HandlerUtil.UrlDecode(imgSrc.substring(imgSrc.indexOf("data=") + 5));
                    HashMap<String, String> result = Maps.newHashMap();
                    result.put(QRCONTEXT,wxQRContext);
                    result.put(PARSEHTML,document.toString());
                    payResultList.add(result);
                    if(StringUtils.isBlank(wxQRContext)){
                        throw new PayException(SERVER_MSG.REQUEST_PAY_PARSE_RESULT_ERROR);
                    }
                }else{
                    String body = firstPayresult.getBody();
                    HashMap<String, String> result = Maps.newHashMap();
                    result.put(HTMLCONTEXT,body);
                    payResultList.add(result);
                }
            }else{
                throw new PayException(SERVER_MSG.NOT200);
            }
        } catch (Exception e) {
            log.error("[捷信通]3.发送支付请求，及获取支付请求结果出错：",e);
            throw new PayException(e.getMessage(),e);
        }
        log.debug("[捷信通]-[请求支付]-3.发送支付请求，及获取支付请求结果成功："+ JSON.toJSONString(payResultList));
        return payResultList;
    }
    protected RequestPayResult buildResult(List<Map<String,String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if(null!=resultListMap && !resultListMap.isEmpty()){
            if(resultListMap.size()==1){
                Map<String, String> resultMap = resultListMap.get(0);
                if(null!=resultMap && resultMap.containsKey(QRCONTEXT)){
                    requestPayResult.setRequestPayamount(channelWrapper.getAPI_AMOUNT());
                    requestPayResult.setRequestPayOrderId(channelWrapper.getAPI_ORDER_ID());
                    requestPayResult.setRequestPayOrderCreateTime(HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(),"yyyy-MM-dd HH:mm:ss"));
                    requestPayResult.setRequestPayQRcodeURL(null);
                    requestPayResult.setRequestPayChannelBankName(channelWrapper.getAPI_CHANNEL_BANK_NAME());
                    requestPayResult.setRequestPayQRcodeContent(resultMap.get(QRCONTEXT));
                }
                if(null!=resultMap && resultMap.containsKey(HTMLCONTEXT)){
                    requestPayResult.setRequestPayamount(channelWrapper.getAPI_AMOUNT());
                    requestPayResult.setRequestPayOrderId(channelWrapper.getAPI_ORDER_ID());
                    requestPayResult.setRequestPayOrderCreateTime(HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(),"yyyy-MM-dd HH:mm:ss"));
                    requestPayResult.setRequestPayQRcodeURL(null);
                    requestPayResult.setRequestPayChannelBankName(channelWrapper.getAPI_CHANNEL_BANK_NAME());
                    requestPayResult.setRequestPayHtmlContent(resultMap.get(HTMLCONTEXT));
                }
            }
            if(ValidateUtil.requestesultValdata(requestPayResult)){
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            }else{
                throw  new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        }else{
            throw  new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[捷信通]-[请求支付]-4.处理请求响应成功："+ JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}