package dc.pay.business.xinbofu;

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
import org.jsoup.Jsoup;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RequestPayHandler("XINBOFU")
public final class XinBoFuPayRequestHandler extends PayRequestHandler {
    //private static final Logger log = LoggerFactory.getLogger(GeFuPayRequestHandler.class);

     private static final String    merchantNo = "merchantNo";   //	String	20	商户号(公共字段)	Y
     private static final String    nonceStr = "nonceStr";   //	String	20	随机字符串(公共字段)	Y
     private static final String    sign = "sign";   //	String	200	签名(公共字段)	Y
     private static final String    paymentType = "paymentType";   //	String	20	支付类型，参考附录	Y
     private static final String    mchOrderNo = "mchOrderNo";   //	String	30	商户平台订单号(商户平台唯一)	Y
     private static final String    orderTime = "orderTime";   //	String	15	下单日期,格式 yyyyMMddHHmmss 如：20180901123511	Y
     private static final String    goodsName = "goodsName";   //	String	50	商品名称	Y
     private static final String    amount = "amount";   //	String	8	金额(单位分)	Y
     private static final String    clientIp = "clientIp";   // String	20	客户端ip地址	Y
     private static final String    notifyUrl = "notifyUrl";   //	String	500	回调通知地址	Y
     private static final String    buyerId = "buyerId";   //	String	50	买家id(商户平台唯一)	Y
     private static final String    buyerName = "buyerName";   //	String	30	买家姓名	  Y
     private static final String    goodsInfo = "goodsInfo";   //	String	100	商品信息	N
     private static final String    goodsNum = "goodsNum";   //	String	8	商品数量	N
     private static final String    remark = "remark";   //	String	200	付款摘要	N
     private static final String    buyerContact = "buyerContact";   //	String	50	买家联系信息	N
     private static final String    frontReturnUrl = "frontReturnUrl";




    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(merchantNo,channelWrapper.getAPI_MEMBERID());
            payParam.put(nonceStr,channelWrapper.getAPI_ORDER_ID());
            payParam.put(paymentType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(mchOrderNo,channelWrapper.getAPI_ORDER_ID());
            payParam.put(orderTime,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
            payParam.put(goodsName,channelWrapper.getAPI_ORDER_ID() );
            payParam.put(amount, channelWrapper.getAPI_AMOUNT());
            payParam.put(clientIp, channelWrapper.getAPI_Client_IP());
            payParam.put(notifyUrl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(buyerId,channelWrapper.getAPI_ORDER_ID() );
            payParam.put(buyerName, channelWrapper.getAPI_ORDER_ID());
            payParam.put(goodsInfo, channelWrapper.getAPI_ORDER_ID());
            payParam.put(goodsNum, "1");
            payParam.put(remark,channelWrapper.getAPI_ORDER_ID() );
            payParam.put(buyerContact, channelWrapper.getAPI_ORDER_ID());
            payParam.put(frontReturnUrl, channelWrapper.getAPI_WEB_URL());
            if(HandlerUtil.isWebWyKjzf(channelWrapper)){
                if(HandlerUtil.isOrderFromWapOrApp(channelWrapper.getAPI_ORDER_FROM())){
                    payParam.put(paymentType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
                }else{
                    payParam.put(paymentType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
                }
            }
        }
        log.debug("[新博付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
              if(StringUtils.isBlank(params.get(paramKeys.get(i))) || sign.equalsIgnoreCase(paramKeys.get(i).toString())    )  //
                continue;
            sb.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i))).append("&");
        }
        sb.append("appkey=" + channelWrapper.getAPI_KEY());
        String signStr = sb.toString(); //.replaceFirst("&key=","")
        pay_md5sign = HandlerUtil.getMD5UpperCase(signStr).toLowerCase();
        log.debug("[新博付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (1==2  && HandlerUtil.isWY(channelWrapper) &&  HandlerUtil.isYLKJ(channelWrapper)  &&  HandlerUtil.isWapOrApp(channelWrapper)    ) {
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
				
				if(StringUtils.isNotBlank(resultStr) && resultStr.contains("<form") && !resultStr.contains("{")){
                    result.put(HTMLCONTEXT,resultStr);
                    payResultList.add(result);
                }else if(StringUtils.isNotBlank(resultStr) ){
                    JSONObject jsonResultStr = JSON.parseObject(resultStr);
                    if(null!=jsonResultStr && jsonResultStr.containsKey("returnCode") && "SUCCESS".equalsIgnoreCase(jsonResultStr.getString("returnCode"))
                            && jsonResultStr.containsKey("payUrl") && StringUtils.isNotBlank(jsonResultStr.getString("payUrl"))){

                        String payUrl = jsonResultStr.getString("payUrl");

//                        if(HandlerUtil.isYLSM(channelWrapper) || HandlerUtil.isJDSM(channelWrapper)){  //京东和银联的二维码都有隐藏域hidUnionBankCardPay名称都一样。
//                            String hidUnionBankCardPay = null;
//                            String htmlSources = handlerUtil.getEndHtml(payUrl).asXml();
//                            if(StringUtils.isBlank(htmlSources)) throw new PayException("第三方返回页面无法解析："+payUrl);
//                             hidUnionBankCardPay = Jsoup.parse(htmlSources).select("#hidUnionBankCardPay").eq(0).attr("value");
//                            if(StringUtils.isBlank(hidUnionBankCardPay)) throw new PayException(hidUnionBankCardPay);
//                            payUrl = hidUnionBankCardPay;
//                        }
//
//                        if(HandlerUtil.isWapOrApp(channelWrapper)|| HandlerUtil.isWebYlKjzf(channelWrapper) || HandlerUtil.isWY(channelWrapper)){
//                            if(jsonResultStr.getString("urlType").equalsIgnoreCase("HTML")){
//                                result.put(HTMLCONTEXT, payUrl);
//                            }else{
//                               result.put(JUMPURL, payUrl);
//                            }
//                        }else{
//                            result.put(QRCONTEXT, payUrl);
//                        }


                        if(StringUtils.isBlank(payUrl)) throw new PayException(resultStr);
                        if(HandlerUtil.isQQSM(channelWrapper)){
                        	result.put(QRCONTEXT, payUrl);
                            payResultList.add(result);
                        }else{
                        	result.put(JUMPURL, payUrl);
                            payResultList.add(result);
                        }
                    }else {throw new PayException(resultStr); }
				}else{ throw new PayException(EMPTYRESPONSE);}
                 
            }
        } catch (Exception e) { 
             log.error("[新博付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[新博付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[新博付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}