package dc.pay.business.wenfuzhifu;

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
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.util.*;

@RequestPayHandler("WENFUZHIFU")
public final class WenFuZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(WenFuZhiFuPayRequestHandler.class);

    private static final String    amount = "amount";  //	订单总金额，单位:分，必须为整数
    private static final String    mechno = "mechno";  //	商户编号
    private static final String    notifyurl = "notifyurl";  //	异步通知地址
    private static final String    orderno = "orderno";  //	订单号	是
    private static final String    paytype = "paytype";  //	支付类型	是
    private static final String    payway = "payway";  //	支付方式	是
    private static final String    timestamp = "timestamp";  //	时间戳	是
    private static final String    sign = "sign";  //	签名数据


    private static final String    body = "body";
    private static final String    returl = "returl";
    private static final String    orderip = "orderip";
    private static final String    status = "status";
    private static final String    toPayData = "toPayData";




    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(amount,channelWrapper.getAPI_AMOUNT());
            payParam.put(mechno,channelWrapper.getAPI_MEMBERID());
            payParam.put(notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(orderno,channelWrapper.getAPI_ORDER_ID());
            payParam.put(paytype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
            payParam.put(payway,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
            payParam.put(timestamp,System.currentTimeMillis()+"");

            payParam.put(body,channelWrapper.getAPI_ORDER_ID());
            payParam.put(returl,channelWrapper.getAPI_WEB_URL());
            payParam.put(orderip,channelWrapper.getAPI_Client_IP());
        }
        log.debug("[稳付支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        SortedMap<String, String> smap = new TreeMap<String, String>(params);
        StringBuffer stringBuffer = new StringBuffer();
        for (Map.Entry<String, String> m : smap.entrySet()) {
            Object value = m.getValue();
            if (!"null".equals(value)&&value != null && !StringUtils.isBlank(String.valueOf(value))&&!sign.equals(m.getKey())){
                stringBuffer.append(m.getKey()).append("=").append(value).append("&");
            }
        }
        String argPreSign = stringBuffer.append("key=").append(channelWrapper.getAPI_KEY()).toString();
        String  pay_md5sign = HandlerUtil.getMD5UpperCase(argPreSign);
        log.debug("[稳付支付]-[请求支付]-2.生成加密URL签名完成：" + pay_md5sign);
        return pay_md5sign;
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
				
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.GET).trim();
				
				if(StringUtils.isNotBlank(resultStr) && resultStr.contains("<form") && !resultStr.contains("{")){
                    result.put(HTMLCONTEXT,resultStr);
                    payResultList.add(result);
                }else{
				
                   JSONObject jsonResultStr = JSON.parseObject(resultStr);
                    if(null!=jsonResultStr && jsonResultStr.containsKey(status) && "true".equalsIgnoreCase(jsonResultStr.getString(status)) && jsonResultStr.containsKey(toPayData)){
                            if(StringUtils.isNotBlank(jsonResultStr.getString(toPayData))){
                                if(HandlerUtil.isWapOrApp(channelWrapper) ||  HandlerUtil.isYLKJ(channelWrapper) ){
                                    result.put(JUMPURL,  jsonResultStr.getString(toPayData));
                                }else {
                                    result.put(QRCONTEXT, jsonResultStr.getString(toPayData));
                                }
                                payResultList.add(result);
                            }
                    }else {
                        throw new PayException(resultStr);
                    }
				}
                 
            }
        } catch (Exception e) { 
             log.error("[稳付支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[稳付支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[稳付支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}