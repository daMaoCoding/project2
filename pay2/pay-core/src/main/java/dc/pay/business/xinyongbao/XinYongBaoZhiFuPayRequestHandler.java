package dc.pay.business.xinyongbao;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestPayHandler("XINYONGBAOZHIFU")
public final class XinYongBaoZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XinYongBaoZhiFuPayRequestHandler.class);

     private static final String   version = "version";   //  版本号  String(5)  是  固定填写：1.0.0
     private static final String   charset = "charset";   //  编码方式  String(1-20)  是  固定填写：UTF-8
     private static final String   merCode = "merCode";   //  商户号  String(32)  是  Pay+平台提供的商户号
     private static final String   signMethod = "signMethod";   //  签名方式  String(1-20)  是  MD5
     private static final String   productCode = "productCode";   //  产品编码  String(8-16)  是  商户在Pay+平台开通的产品编码
     private static final String   orderNo = "orderNo";   //  订单号  String  是  订单号，不能含有“-”和“_”等特殊符  号
     private static final String   orderAmt = "orderAmt";   //  订单金额  String  是  订单金额:以分为单位.
     private static final String   orderTime = "orderTime";   //  订单时间  String(14)  是  yyyyMMddHHmmss 如：  20170606100011
     private static final String   backNotifyUrl = "backNotifyUrl";   //   后台通知  地址 String  是  接收pay+支付平台后台通知的地  址
     private static final String   signature = "signature";   //  签名  String  是  填写对报文摘要的签名


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(version,"1.0.0");
            payParam.put(charset,"UTF-8");
            payParam.put(merCode,channelWrapper.getAPI_MEMBERID());
            payParam.put(signMethod,"MD5");
            payParam.put(productCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(orderNo,channelWrapper.getAPI_ORDER_ID() );
            payParam.put(orderAmt, channelWrapper.getAPI_AMOUNT());
            payParam.put(orderTime,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
            payParam.put(backNotifyUrl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
        }
        log.debug("[信用宝支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
              if(StringUtils.isBlank(params.get(paramKeys.get(i))) || signature.equalsIgnoreCase(paramKeys.get(i).toString())    )  //
                continue;
            sb.append(paramKeys.get(i)).append("=").append(    params.get(paramKeys.get(i))   ).append("&");
        }
        sb.append("signkey=" + channelWrapper.getAPI_KEY());
        String signStr = sb.toString(); //.replaceFirst("&signkey=","")
        pay_md5sign = HandlerUtil.getMD5UpperCase(signStr);
        log.debug("[信用宝支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
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
				
                resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
                if(StringUtils.isNotBlank(resultStr)) resultStr = new String(resultStr.getBytes("ISO-8859-1"), "UTF-8");
				
				if(StringUtils.isNotBlank(resultStr) && resultStr.contains("<form") && !resultStr.contains("{")){
                    result.put(HTMLCONTEXT,resultStr);
                    payResultList.add(result);
                }else if(StringUtils.isNotBlank(resultStr) ){
                    JSONObject jsonResultStr = JSON.parseObject(resultStr);
                    if(null!=jsonResultStr && jsonResultStr.containsKey("respCode") && "000000".equalsIgnoreCase(jsonResultStr.getString("respCode"))
                            && jsonResultStr.containsKey("payUrl") && StringUtils.isNotBlank(jsonResultStr.getString("payUrl"))){
                        if(HandlerUtil.isWapOrApp(channelWrapper)){
                            result.put(JUMPURL, jsonResultStr.getString("payUrl"));
                        }else{
                            result.put(QRCONTEXT, jsonResultStr.getString("payUrl"));
                        }
                        payResultList.add(result);
                    }else {throw new PayException(resultStr); }
				}else{ throw new PayException(EMPTYRESPONSE);}
                 
            }
        } catch (Exception e) { 
             log.error("[信用宝支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[信用宝支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[信用宝支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}