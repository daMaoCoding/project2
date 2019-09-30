package dc.pay.business.wepayzhifu;

/**
 * ************************
 * @author tony 3556239829
 */

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
import dc.pay.utils.*;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpMethod;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RequestPayHandler("WEPAYZHIFU")
public final class WePayZhiFuPayRequestHandler extends PayRequestHandler {
    //private static final Logger log = LoggerFactory.getLogger(GeFuPayRequestHandler.class);


     private static final String  BuCode = "BuCode";   //	String	是	商户号
     private static final String  OrderId = "OrderId";   //	String	是	商户方订单编号
     private static final String  PayChannel = "PayChannel";   //	String	是	用户加值管道 微信支付 => WeChat 支付宝 => AliPay
     private static final String  OrderAccount = "OrderAccount";   //	String	是	商户方UID
     private static final String  Amount = "Amount";   //	Interger	是	加值金额 微信支付支持金额: 100 - 3000  支付宝支持金额: 100 – 3000   ※金额需被100整除(EX:100、500、2000、3000…依此类推) ※金额会因市场需求调整上限与下限，请于对接时询问
     private static final String  Sign = "Sign";   //	String	是	签名MD5 所有参数接上密钥做MD5，参数串接顺序不可随意调换。 范例：  MD5(BuCode=wepay&OrderId=20180212001&PayChannel=AliPay&OrderAccount=Guest001&Amount=1000&Key=21c3031066ec36ab7be4c6f80087694f)
     private static final String  NotifyURL = "NotifyURL";   //	String	否	商户自定义回调地址 ※该栏位不需加入签名中，若无提供此栏位，则会预设为商户提供的预设地址
     private static final String  IP = "IP";   //IP String 否     付款人 IP     ※该栏位不需加入签名中





    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(BuCode,channelWrapper.getAPI_MEMBERID());
            payParam.put(OrderId,channelWrapper.getAPI_ORDER_ID());
            payParam.put(PayChannel,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(OrderAccount,HandlerUtil.getRandomNumber(10));
            payParam.put(Amount,HandlerUtil.getYuanWithoutZero(channelWrapper.getAPI_AMOUNT()));
            payParam.put(NotifyURL,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(IP,channelWrapper.getAPI_Client_IP());
        }
        log.debug("[WePay支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        //BuCode=wepay&OrderId=20180212001&PayChannel=AliPay&OrderAccount=Guest001&Amount=1000&Key=21c3031066ec36ab7be4c6f80087694f
        //=%s&=%s&=%s&=%s&=%s&Key=%s
        String paramsStr = String.format("BuCode=%s&OrderId=%s&PayChannel=%s&OrderAccount=%s&Amount=%s&Key=%s",
                params.get(BuCode),
                params.get(OrderId),
                params.get(PayChannel),
                params.get(OrderAccount),
                params.get(Amount),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[WePay支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (1==2 && HandlerUtil.isWY(channelWrapper) &&  HandlerUtil.isYLKJ(channelWrapper)  &&  HandlerUtil.isWapOrApp(channelWrapper)    ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='post'"));
                payResultList.add(result);
            }else{
/*				
				HtmlPage endHtml = handlerUtil.getEndHtml(channelWrapper.getAPI_CHANNEL_BANK_URL(), channelWrapper.getAPI_ORDER_ID(), payParam);
                String qrContent=null;
                if(null!=endHtml && endHtml.getByXPath("//input[@name='payurl']").size()==1){
                    HtmlInput payUrlInput = (HtmlInput) endHtml.getByXPath("//input[@name='payurl']").get(0);
                    if(payUrlInput!=null ){
                        String qrContentSrc = payUrlInput.getValueAttribute();
                        if(StringUtils.isNotBlank(qrContentSrc))  qrContent = QRCodeUtil.decodeByUrl(qrContentSrc);
                    }
                }
               if(StringUtils.isNotBlank(qrContent)){
                    result.put(QRCONTEXT, qrContent);
                    payResultList.add(result);
                }else {  throw new PayException(endHtml.asXml()); }
				
*/				
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
                resultStr =  UnicodeUtil.unicodeToString(resultStr);

				if(StringUtils.isNotBlank(resultStr) && resultStr.contains("<form") && !resultStr.contains("{")){
                    result.put(HTMLCONTEXT,resultStr);
                    payResultList.add(result);
                }else if(StringUtils.isNotBlank(resultStr) ){
                    JSONObject jsonResultStr = JSON.parseObject(resultStr);
                    if(null!=jsonResultStr && jsonResultStr.containsKey("status") && "true".equalsIgnoreCase(jsonResultStr.getString("status"))
                            && jsonResultStr.containsKey("data") && null!=jsonResultStr.getJSONObject("data")
                            && jsonResultStr.getJSONObject("data").containsKey("redirectURL")
                            && StringUtils.isNotBlank(jsonResultStr.getJSONObject("data").getString("redirectURL"))){
                            result.put(HTMLCONTEXT, jsonResultStr.getJSONObject("data").getString("redirectURL"));
                            payResultList.add(result);
                    }else {throw new PayException(resultStr); }
				}else{ throw new PayException(EMPTYRESPONSE);}
                 
            }
        } catch (Exception e) { 
             log.error("[WePay支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[WePay支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[WePay支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}