package dc.pay.business.jinyang;

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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.util.*;

@RequestPayHandler("JINYANG")
public final class JinYangPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(JinYangPayRequestHandler.class);

         private static final String QRCONTEXT = "QrContext";
         private static final String HTMLCONTEXT = "HtmlContext";
         private static final String PARSEHTML = "parseHtml";

         private static final String   p1_mchtid      ="p1_mchtid";          //商户ID
    	 private static final String   p2_paytype     ="p2_paytype";         //支付方式
    	 private static final String   p3_paymoney    ="p3_paymoney";        //支付金额
    	 private static final String   p4_orderno     ="p4_orderno";         //商户平台唯一订单号
    	 private static final String   p5_callbackurl ="p5_callbackurl";     //商户异步回调通知地址
    private static final String p7_version   ="p7_version";         //版本号
    private static final String p8_signtype  ="p8_signtype";        //签名加密方式
    private static final String p11_isshow   ="p11_isshow";  	     //是否显示收银台
    private static final String p12_orderip  ="p12_orderip";
    private static final String p13_memberid = "p13_memberid";  //商户系统用户唯一标识
    private static final String JUMPURL      = "JUMPURL";
    private static final String FASTPAY      = "FASTPAY";
    private static final String UNIONFASTPAY = "UNIONFASTPAY";






    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(p1_mchtid, channelWrapper.getAPI_MEMBERID());
                put(p2_paytype, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(p3_paymoney, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()) );
                put(p4_orderno, channelWrapper.getAPI_ORDER_ID());
                //put(p4_orderno, HandlerUtil.getRandomStr(8));  //// TODO: 2017/11/21 开发
                put(p5_callbackurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(p7_version,"v2.8");
                put(p8_signtype,"1");
                put(p11_isshow,"0");
                put(p12_orderip,channelWrapper.getAPI_Client_IP());
                if (channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().equalsIgnoreCase(FASTPAY) || channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().equalsIgnoreCase(UNIONFASTPAY))
                    put(p13_memberid, HandlerUtil.getRandomStr(8));
            }
        };
        log.debug("[金阳]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map api_response_params) throws PayException {
       //p1_mchtid=22222&p2_signtype=1&p3_orderno=20171014161133666&p4_version=v2.8e465b1d4cad5ca6201b05cc3ddf041aa
        String paramsStr ="";
        if (channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().equalsIgnoreCase(FASTPAY) || channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().equalsIgnoreCase(UNIONFASTPAY)) { //银联快捷
            paramsStr = String.format("p1_mchtid=%s&p2_paytype=%s&p3_paymoney=%s&p4_orderno=%s&p5_callbackurl=%s&p6_notifyurl=&p7_version=%s&p8_signtype=%s&p9_attach=&p10_appname=&p11_isshow=%s&p12_orderip=%s&p13_memberid=%s%s",
                    api_response_params.get(p1_mchtid),
                    api_response_params.get(p2_paytype),
                    api_response_params.get(p3_paymoney),
                    api_response_params.get(p4_orderno),
                    api_response_params.get(p5_callbackurl),
                    api_response_params.get(p7_version),
                    api_response_params.get(p8_signtype),
                    api_response_params.get(p11_isshow),
                    api_response_params.get(p12_orderip),
                    api_response_params.get(p13_memberid),
                    channelWrapper.getAPI_KEY());
        }else{
            paramsStr = String.format("p1_mchtid=%s&p2_paytype=%s&p3_paymoney=%s&p4_orderno=%s&p5_callbackurl=%s&p6_notifyurl=&p7_version=%s&p8_signtype=%s&p9_attach=&p10_appname=&p11_isshow=%s&p12_orderip=%s%s",
                    api_response_params.get(p1_mchtid),
                    api_response_params.get(p2_paytype),
                    api_response_params.get(p3_paymoney),
                    api_response_params.get(p4_orderno),
                    api_response_params.get(p5_callbackurl),
                    api_response_params.get(p7_version),
                    api_response_params.get(p8_signtype),
                    api_response_params.get(p11_isshow),
                    api_response_params.get(p12_orderip),
                    channelWrapper.getAPI_KEY());
        }


        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[金阳]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        try {
            if(HandlerUtil.isWY(channelWrapper)|| HandlerUtil.isWebYlKjzf(channelWrapper) ||HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isFS(channelWrapper)){
                StringBuffer htmlContent = HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
                result.put(HTMLCONTEXT,htmlContent.toString());
                payResultList.add(result);
            }else{
                String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
                if(resultStr.contains("html") && HandlerUtil.isWapOrApp(channelWrapper)) {
                    // String getURLForwapAPP = HttpUtil.getURLWithParam(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
                    Document document = Jsoup.parse(resultStr);
                    Element aEl = document.getElementsByTag("a").first();
                    if(null!=aEl && StringUtils.isNotBlank(aEl.attr("href"))){
                        result.put(JUMPURL, aEl.attr("href"));
                        payResultList.add(result);
                    }else{
                        log.error("发送支付请求，及获取支付请求结果错误："+resultStr);
                        throw new PayException(resultStr );
                    }
                }else{
                    JSONObject resJson = JSONObject.parseObject(resultStr);
                    String rspCode = resJson.getString("rspCode");
                    if (null!=resJson &&  rspCode.equalsIgnoreCase("1") && resJson.containsKey("data")) {
                        JSONObject data = resJson.getJSONObject("data");
                        String r6_qrcode = data.getString("r6_qrcode");
                        //String qrContent = QRCodeUtil.decodeByUrl(r6_qrcode);
                        result.put(QRCONTEXT, r6_qrcode);
                        result.put(PARSEHTML, resultStr);
                        payResultList.add(result);
                    }else{
                        log.error("[金阳]3.发送支付请求，及获取支付请求结果：" +resultStr + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                        throw new PayException(resJson.toJSONString());
                    }
                }
           }
        } catch (Exception e) {
            log.error("[金阳]3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(e.getMessage(), e);
        }
        log.debug("[金阳]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
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
        log.debug("[金阳]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}