package dc.pay.business.chengbao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.lang.StringUtils;
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
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;

/**
 * 
 * @author tree
 * Aug 16, 2018
 */
@Slf4j
@RequestPayHandler("CHENGBAO")
public final class ChengBaoPayRequestHandler extends PayRequestHandler {
    //private final Logger log = LoggerFactory.getLogger(getClass());

     private static final String edid = "edid";  //	商务号	是
     private static final String edddh = "edddh";  //	商户订单号	是
     private static final String eddesc = "eddesc";  //	商品名称	是
     private static final String edfee = "edfee";  //	支付金额	是
     private static final String ednotifyurl = "ednotifyurl";  //	异步通知地址	是
     private static final String edbackurl = "edbackurl";  //	同步通知地址	是
     private static final String edpay = "edpay";  //	请求类型 	是
     private static final String edip = "edip";  //	支付用户IP地址
     private static final String edsign = "edsign";  //	签名【md5(商务号+商户订单号+支付金额+异步通知地址+商户秘钥)】

    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(edid, channelWrapper.getAPI_MEMBERID() );
                put(edddh, channelWrapper.getAPI_ORDER_ID());
                put(eddesc, channelWrapper.getAPI_ORDER_ID() );
                put(edfee,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT())  );
                put(ednotifyurl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL() );
                put(edbackurl, channelWrapper.getAPI_WEB_URL() );
                put(edpay,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG()  );
                put(edip, channelWrapper.getAPI_Client_IP() );
            }
        };
        log.debug("[诚宝]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }
    
    protected String buildPaySign(Map payParam) throws PayException {
        // 商务号+商户订单号+支付金额+异步通知地址+商户秘钥
        String paramsStr = String.format("%s%s%s%s%s",
                payParam.get(edid),
                payParam.get(edddh),
                payParam.get(edfee),
                payParam.get(ednotifyurl),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[诚宝]-[请求支付]-2.生成加密URL签名完成：" + signMd5);
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

                if(StringUtils.isNotBlank(resultStr) && resultStr.contains("<form") && !resultStr.contains("{")){
                    result.put(HTMLCONTEXT,resultStr);
                    payResultList.add(result);
                }else if(StringUtils.isNotBlank(resultStr) ){
                    JSONObject jsonResultStr = JSON.parseObject(resultStr);
                    if(null!=jsonResultStr && jsonResultStr.containsKey("status") && "1".equalsIgnoreCase(jsonResultStr.getString("status"))
                            && jsonResultStr.containsKey("payurl") && StringUtils.isNotBlank(jsonResultStr.getString("payurl"))){
                        if(HandlerUtil.isWapOrApp(channelWrapper)){
                            result.put(JUMPURL, jsonResultStr.getString("payurl"));
                        }else{
                            result.put(QRCONTEXT, jsonResultStr.getString("payurl"));
                        }
                        payResultList.add(result);
                    }else {throw new PayException(resultStr); }
                }else{ throw new PayException(EMPTYRESPONSE);}

            }
        } catch (Exception e) {
            log.error("[诚宝]3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(e.getMessage(), e);
        }
        log.debug("[诚宝]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[诚宝]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
    
}