package dc.pay.business.diandizhifu;

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
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestPayHandler("DIANDIZHIFU")
public final class DianDiZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(DianDiZhiFuPayRequestHandler.class);


     private static final String   	mchntId="mchntId";      //	  商户号  M	商户号（系统分配）
     private static final String   	mchntOrderId="mchntOrderId";      //	  商户订单号  M	商户订单号（10位以上，不重复）
     private static final String   	orderSubject="orderSubject";      //	  订单标题  M	订单标题, 长度3字节以上
     private static final String   	bizType="bizType";      //	  业务类型  M	业务类型：
     private static final String   	txnAmt="txnAmt";      //	  交易金额  M	交易单位为分
     private static final String   	currency="currency";      //	  交易币种  M	三位 ISO 货币代码，当前仅人民币 CNY
     private static final String   	notifyUrl="notifyUrl";      //	  后台通知地址  M	支付平台通过改地址通知支付结果
     private static final String   	returnUrl="returnUrl";      //	  成功跳转地址  M	支付成功后的跳转地址
     private static final String   	sendIp="sendIp";      //	  商户终端IP  M	客户端的IP地址
     private static final String   	txnTime="txnTime";      //	  订单发送时间  M	yyyyMMddHHmmss
     private static final String   	signature="signature";      //	  签名信息  M	MD5加密结果
     private static final String   	signMethod="signMethod";      //	  签名算法  M	签名算法，当前仅支持MD5
     private static final String   	CNY="CNY";
     private static final String   	MD5="MD5";
     private static final String   	orderDesc="orderDesc";
     private static final String   	remarks="remarks";



    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(mchntId,channelWrapper.getAPI_MEMBERID());
            payParam.put(mchntOrderId,channelWrapper.getAPI_ORDER_ID());
            payParam.put(orderSubject,channelWrapper.getAPI_ORDER_ID());
            payParam.put(bizType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(txnAmt,channelWrapper.getAPI_AMOUNT());
            payParam.put(currency,CNY);
            payParam.put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(returnUrl,channelWrapper.getAPI_WEB_URL());
            payParam.put(sendIp,channelWrapper.getAPI_Client_IP());
            payParam.put(txnTime,DateFormatUtils.format(System.currentTimeMillis(), "yyyyMMddHHmmss"));
            payParam.put(signMethod,MD5);
            payParam.put(orderDesc,channelWrapper.getAPI_ORDER_ID());
            payParam.put(remarks,channelWrapper.getAPI_ORDER_ID());
        }
        log.debug("[点滴支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if(StringUtils.isBlank(params.get(paramKeys.get(i))) || signMethod.equalsIgnoreCase(paramKeys.get(i).toString())  ||  signature.equalsIgnoreCase(paramKeys.get(i).toString())    )  //
                continue;
            sb.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i))).append("&");
        }
        sb.append("key=" + channelWrapper.getAPI_KEY());
        String signStr = sb.toString().replaceFirst("&key=","");
        pay_md5sign = HandlerUtil.getMD5UpperCase(signStr);
        log.debug("[点滴支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if ( 1==2 && HandlerUtil.isWY(channelWrapper)  &&  HandlerUtil.isYLKJ(channelWrapper)   &&  HandlerUtil.isWapOrApp(channelWrapper)    ) {
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
				
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
				
				if(StringUtils.isNotBlank(resultStr) && resultStr.contains("<form") && !resultStr.contains("{")){
                    result.put(HTMLCONTEXT,resultStr);
                    payResultList.add(result);
                }else{
                   JSONObject jsonResultStr = JSON.parseObject(resultStr);
                    if(null!=jsonResultStr && jsonResultStr.containsKey("code") && "0000".equalsIgnoreCase(jsonResultStr.getString("code")) && jsonResultStr.containsKey("imgUrl")){
                            if(StringUtils.isNotBlank(jsonResultStr.getString("imgUrl"))){
                                if(HandlerUtil.isWapOrApp(channelWrapper)){
                                    result.put(JUMPURL, jsonResultStr.getString("imgUrl"));
                                }else{
                                    result.put(QRCONTEXT, jsonResultStr.getString("imgUrl"));
                                }
                                payResultList.add(result);
                            }
                    }else {
                        throw new PayException(resultStr);
                    }
				}
            }
        } catch (Exception e) { 
             log.error("[点滴支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[点滴支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[点滴支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}