package dc.pay.business.zixuezhifu;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dc.pay.utils.XmlUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

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
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

@RequestPayHandler("ZIXUEZHIFU")
public final class ZiXueZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(ZiXueZhiFuPayRequestHandler.class);



     private static final String      method = "method";      //=order
     private static final String      username = "username";      //=[用户名]
     private static final String      rid = "rid";      //=[商户订单号] | 每次订单号只能使用一次
     private static final String      subject = "subject";      //=[支付标题] | 可空
     private static final String      amount = "amount";      //=[支付金额] | 单位分
     private static final String      createip = "createip";      //=[您发起订单请求的IP]
     private static final String      notifyurl = "notifyurl";      //=[支付成功后异步通知地址]
     private static final String      typecode = "typecode";      //=[支付方式代码] | 支付方式代码参考后台渠道对接
     private static final String      sign = "sign";      //=签名，请看下面的签名部分

     private static final String      order = "order";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(method,order);
            payParam.put(username,channelWrapper.getAPI_MEMBERID());
            payParam.put(rid,channelWrapper.getAPI_ORDER_ID());
            payParam.put(subject,channelWrapper.getAPI_ORDER_ID());
            payParam.put(amount,channelWrapper.getAPI_AMOUNT());
            payParam.put(createip,channelWrapper.getAPI_Client_IP());
            payParam.put(notifyurl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(typecode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG() );
        }
        log.debug("[紫雪支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
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
        sb.append("key=" + channelWrapper.getAPI_KEY());
        String signStr = sb.toString(); //.replaceFirst("&key=","")
        pay_md5sign = HandlerUtil.getMD5UpperCase(signStr);
        log.debug("[紫雪支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }

//    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
//        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
//        
//        HashMap<String, String> result = Maps.newHashMap();
//        System.out.println("请求地址=========>"+channelWrapper.getAPI_CHANNEL_BANK_URL());
//        System.out.println("请求参数=========>"+JSON.toJSONString(payParam));
//
////        if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
//        if (false) {
//            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
//            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
//        }else{
////            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.GET);
//            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
//            if (StringUtils.isBlank(resultStr)) {
//                log.error("[紫雪支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//                //log.error("[通扫]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
//                //throw new PayException("返回空,参数："+JSON.toJSONString(map));
//            }
////    p        resultStr = UnicodeUtil.unicodeToString(resultStr);
//            try {
//                resultStr = new String(resultStr.getBytes("ISO-8859-1"), "UTF-8");
//            } catch (UnsupportedEncodingException e1) {
//                // TODO Auto-generated catch block
//                e1.printStackTrace();
//                log.error("[紫雪支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//            }
//            System.out.println("请求返回=========>"+resultStr);
//            if (!resultStr.contains("<message>ok</message>")) {
//                log.error("[紫雪支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//            }
//            Map<String, String> xml2Map = XmlUtil.xml2Map(resultStr);
//            //只取正确的值，其他情况抛出异常
//            if (null != xml2Map && xml2Map.containsKey("message") && "ok".equalsIgnoreCase(xml2Map.get("message"))  && xml2Map.containsKey("payUrl") && StringUtils.isNotBlank(xml2Map.get("payUrl"))) {
//                String code_url = xml2Map.get("payUrl");
//                result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
//                //if (handlerUtil.isWapOrApp(channelWrapper)) {
//                //    result.put(JUMPURL, code_url);
//                //}else{
//                //    result.put(QRCONTEXT, code_url);
//                //}
//            }else {
//                log.error("[紫雪支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//            }
//        }
//        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
//        payResultList.add(result);
//        log.debug("[紫雪支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
//        return payResultList;
//    }
    
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
        if(StringUtils.isNotBlank(resultStr) && resultStr.contains("<form") && !resultStr.contains("{")){
            result.put(HTMLCONTEXT,resultStr);
            payResultList.add(result);
        }else if(StringUtils.isNotBlank(resultStr) ){
          try {
              resultStr = new String(resultStr.getBytes("ISO-8859-1"), "UTF-8");
          } catch (UnsupportedEncodingException e1) {
              e1.printStackTrace();
              log.error("[紫雪支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              throw new PayException(resultStr);
          }
            Map<String, String> resultMap = XmlUtil.xml2Map(resultStr);
            if(null!=resultMap && resultMap.containsKey("status") && "unpaid".equalsIgnoreCase(resultMap.get("status"))
                    && resultMap.containsKey("castUrl") && StringUtils.isNotBlank(resultMap.get("castUrl"))){
                if(HandlerUtil.isWapOrApp(channelWrapper)){
                    result.put(JUMPURL, resultMap.get("castUrl"));
                }else{
                    result.put(QRCONTEXT,resultMap.get("payUrl"));
                }
                payResultList.add(result);
            }else {throw new PayException(resultStr); }
        }else{ throw new PayException(EMPTYRESPONSE);}
        log.debug("[紫雪支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[紫雪支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}