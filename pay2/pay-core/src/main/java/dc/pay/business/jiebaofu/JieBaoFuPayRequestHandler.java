package dc.pay.business.jiebaofu;

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
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.util.*;

@RequestPayHandler("JIEBAOFU")
public final class JieBaoFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(JieBaoFuPayRequestHandler.class);

     private static final String version = "version";           //版本号 1.0
     private static final String customerid = "customerid";     //商户编号
     private static final String sdorderno = "sdorderno";       //商户订单号
     private static final String total_fee = "total_fee";       //订单金额,精确到小数点后两位
     private static final String paytype = "paytype";           //支付编号,微信支付宝等
     private static final String notifyurl = "notifyurl";       //异步通知URL
     private static final String returnurl = "returnurl";       //同步跳转URL
     private static final String get_code = "get_code";         //获取微信二维码  如果只想获取被扫二维码，请设置get_code=1

     private static final String bankcode = "bankcode";         //银行编号 网银直连不可为空，其他支付方式可为空
     private static final String sign = "sign";                 //md5签名串



    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(version, "1.0");
                put(customerid,channelWrapper.getAPI_MEMBERID());
                put(sdorderno, channelWrapper.getAPI_ORDER_ID());
                put(total_fee, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(paytype, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(returnurl,"Success");
                put(get_code, "1");
                if(channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("_WY_")){
                    put(paytype, "bank");
                    put(bankcode, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                }
            }
        };
        log.debug("[捷宝付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



//=%s&=%s&=%s&=%s&=%s&=%s&%s
    protected String buildPaySign(Map payParam) throws PayException {
        String paramsStr = String.format("version=%s&customerid=%s&total_fee=%s&sdorderno=%s&notifyurl=%s&returnurl=%s&%s",
                payParam.get(version),
                payParam.get(customerid),
                payParam.get(total_fee),
                payParam.get(sdorderno),
                payParam.get(notifyurl),
                payParam.get(returnurl),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[捷宝付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        String payResultStr = null;
        String qrHtml = null;
        String qrContent=null;
        HashMap<String, String> result = Maps.newHashMap();
        try {
           if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)  ||  HandlerUtil.isWapOrApp(channelWrapper)   ) {
                    result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());  //.replace("method='post'","method='get'"));
                    payResultList.add(result);
             }else{
                  //获取页面html
                   payResultStr =    RestTemplateUtil.sendByRestTemplateRedirectWithSendSimpleForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,  HttpMethod.POST).trim();
                   if(payResultStr.contains("id=\"qrcode\"")){
                       qrHtml = payResultStr;
                   }else if(payResultStr.contains("<a")){
                       String hrefUrl = Jsoup.parse(payResultStr).getElementsByTag("a").first().attr("HREF");
                       if(StringUtils.isNotBlank(hrefUrl)){
                           qrHtml = RestTemplateUtil.sendByRestTemplateRedirectWithSendSimpleForm(hrefUrl, null, HttpMethod.GET).trim();

                       }
                  }

                //解析页面html
               if(StringUtils.isNotBlank(qrHtml) ){
                   Element qrImgElement = Jsoup.parse(qrHtml).select("div#qrcode img").first();
                   if(qrImgElement!=null &&StringUtils.isNotBlank( qrImgElement.attr("src"))){
                       qrContent =HandlerUtil.UrlDecode(qrImgElement.attr("src").replace("qrcode.php?text=",""));
                       if(qrContent.startsWith("http://pan.baidu.com/share/qrcode") && qrContent.contains("url=")){
                           qrContent =  qrContent.substring(qrContent.indexOf("url=") +4, qrContent.length());
                       }


                   }
               }

                //返回结果
               if(StringUtils.isBlank(qrContent)){
                   log.error("发送支付请求，及获取支付请求结果错误：{}",payResultStr);
                   throw new PayException(payResultStr);
               }

               result.put(QRCONTEXT, qrContent);
               result.put(PARSEHTML, payResultStr);
               payResultList.add(result);
           }


        } catch (Exception e) {
            log.error("[捷宝付]3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(e.getMessage(), e);
        }
        log.debug("[捷宝付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
        return payResultList;
    }


    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (null != resultListMap && !resultListMap.isEmpty()) {
            if (resultListMap.size() == 1) {
                Map<String, String> resultMap = resultListMap.get(0);
                requestPayResult = super.buildResult(resultMap,channelWrapper,requestPayResult);
            }
            if (ValidateUtil.requestesultValdata(requestPayResult)) {
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            } else {
                throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[捷宝付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}