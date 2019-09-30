package dc.pay.business.huili;

/**
 * ************************
 * @author tony 3556239829
 */

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
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
import dc.pay.utils.*;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestPayHandler("HUILIZHIFU")
public final class HuiLiZhiFuPayRequestHandler extends PayRequestHandler {
     private static final Logger      log = LoggerFactory.getLogger(HuiLiZhiFuPayRequestHandler.class);
     private static final String      mch_id = "mch_id";  //	String(16)	是  我方分配，商户号
     private static final String      pay_type = "pay_type";  //	String(4)	是	支付类型
     private static final String      out_trade_no = "out_trade_no";  //	String(64)	是	商户订单号，必须唯一
     private static final String      total_fee = "total_fee";  //	String(8)	是	下单价格，单位分
     private static final String      notify_url = "notify_url";  //	String(255)	是	支付成功后异步通知地址
     private static final String      ip = "ip";  //	String(20)	是	客户端真实ip
     private static final String      nonce_str = "nonce_str";  //	String(32)	是	随机字符串 ,不长于32位
     private static final String      sign = "sign";  //	String(32)	是	Md5签名值统一大写，所有传输的参数除sign都参与签名，按ASCII码升序排序组成key1=value1&key2=value2的字符串，在字符串最后加上&key=密钥，所有参与签名的参数都是没有编码前的原始参数
     private static final String      bank_code = "bank_code";
     private static final String      card_type = "card_type";


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(mch_id,channelWrapper.getAPI_MEMBERID());

            if(HandlerUtil.isWY(channelWrapper)){
                payParam.put(pay_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
                payParam.put(bank_code,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
                payParam.put(card_type,"1");
            }else{
                payParam.put(pay_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            }
            payParam.put(out_trade_no,channelWrapper.getAPI_ORDER_ID());
            payParam.put(total_fee,channelWrapper.getAPI_AMOUNT());
            payParam.put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(ip,channelWrapper.getAPI_Client_IP());
            payParam.put(nonce_str,HandlerUtil.getRandomStr(8));
        }
        log.debug("[汇利支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if(StringUtils.isBlank(params.get(paramKeys.get(i))) || sign.equalsIgnoreCase(paramKeys.get(i).toString()    ))  //
                continue;
            sb.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i))).append("&");
        }
        sb.append("key=" + channelWrapper.getAPI_KEY());
        String signStr = sb.toString(); //.replaceFirst("&key=","")
        pay_md5sign = HandlerUtil.getMD5UpperCase(signStr);
        log.debug("[汇利支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }
    


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();

        String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.GET).trim();
        resultStr = HandlerUtil.UrlDecode(resultStr);
        JSONObject jsonResultStr = JSON.parseObject(resultStr);
            if(null!=jsonResultStr && jsonResultStr.containsKey("status") && "200".equalsIgnoreCase(jsonResultStr.getString("status")) && jsonResultStr.containsKey("pay_url")){
                    if(StringUtils.isNotBlank(jsonResultStr.getString("pay_url"))){
                        if(HandlerUtil.isYLSM(channelWrapper)){
                            result.put(QRCONTEXT, jsonResultStr.getString("pay_url"));
                        }else{
                            result.put(JUMPURL, jsonResultStr.getString("pay_url"));
                        }
                        payResultList.add(result);
                        
//                        if(HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper)||HandlerUtil.isYLKJ(channelWrapper)){
//                            result.put(JUMPURL, jsonResultStr.getString("pay_url"));
//                            payResultList.add(result);
//                        }else if(HandlerUtil.isYLSM(channelWrapper)){ //银联扫码特殊，直接返回
//                            result.put(QRCONTEXT, jsonResultStr.getString("pay_url"));
//                            payResultList.add(result);
//                        }else if(HandlerUtil.isWxSM(channelWrapper)){//微信能扫码，跳转第三方页面
//                            result.put(QRCONTEXT, jsonResultStr.getString("pay_url"));
//                            payResultList.add(result);
//                        }else{
//                            //扫码返回的是二维码页面
//                            HtmlPage endHtml = HttpUtil.sendByHtmlUnit(jsonResultStr.getString("pay_url"));
//                            String qrContent=null;
//                            if(null!=endHtml && endHtml.getByXPath("//img[@id='img1']").size()==1){
//                                HtmlImage payUrlInput = (HtmlImage) endHtml.getByXPath("//img[@id='img1']").get(0);
//                                if(payUrlInput!=null ){
//                                    String qrContentSrc = payUrlInput.getSrcAttribute();
//                                    if(StringUtils.isNotBlank(qrContentSrc) && qrContentSrc.contains("url=")){
//                                        qrContent = qrContentSrc.substring(qrContentSrc.indexOf("url=")+4);
//                                    }
//                                }
//                            }
//                           if(StringUtils.isNotBlank(qrContent)){
//                                 result.put(QRCONTEXT, qrContent);
//                                 payResultList.add(result);
//                            }else {  throw new PayException(endHtml.asXml()); }
//                        }

                    
                    }
            }else {
                throw new PayException(resultStr);
            }
    
        log.debug("[汇利支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[汇利支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}