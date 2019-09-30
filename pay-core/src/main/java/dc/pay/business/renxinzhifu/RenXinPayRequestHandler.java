package dc.pay.business.renxinzhifu;

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

@RequestPayHandler("RENXIN")
public final class RenXinPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(RenXinPayRequestHandler.class);

    private static final String QRCONTEXT = "QrContext";
    private static final String HTMLCONTEXT = "HtmlContext";
    private static final String PARSEHTML = "parseHtml";
    private static final String JUMPURL = "JUMPURL";

    private static final  String  version	     = "version";        //是	固定值3.0
     private static final String  method	         = "method";     //是	Boh.online.interface
     private static final String  partner	 = "partner";            //是	商户id,由仁信分配
     private static final String  banktype	     = "banktype";       //是	银行类型，default为跳转到仁信接口进行选择支付
     private static final String  paymoney	 = "paymoney";           //是	单位元（人民币）
     private static final String  ordernumber	 = "ordernumber";    //是	商户订单号
     private static final String  callbackurl	 = "callbackurl";    //是	下行异步通知地址
     private static final String  isshow	 = "isshow";             //否	固定值:0
     private static final String  callBackUrl	 = "/respPayWeb/RENXIN_BANK_NULL_FOR_CALLBACK/";



    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(version, "3.0");
                put(method, "Rx.online.pay");
                put(partner, channelWrapper.getAPI_MEMBERID());
                put(banktype, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(paymoney, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(ordernumber, channelWrapper.getAPI_ORDER_ID());
                put(callbackurl, HandlerUtil.getDomain(channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL()).concat(callBackUrl));
                put(isshow,"0");
            }
        };

        String fsAuthCode = HandlerUtil.getFsAuthCode(channelWrapper); //反扫授权码

//        if(StringUtils.isNotBlank(fsAuthCode)){ 反扫无需再我们页面获取授权码
//            payParam.put(auth_code,fsAuthCode);
//        }

        log.debug("[仁信]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map api_response_params) throws PayException {
       // version={0}&method={1}&partner={2}&banktype={3}&paymoney={4}&ordernumber={5}&callbackurl={6}key
        String paramsStr = String.format("version=%s&method=%s&partner=%s&banktype=%s&paymoney=%s&ordernumber=%s&callbackurl=%s%s",
                api_response_params.get(version),
                api_response_params.get(method),
                api_response_params.get(partner),
                api_response_params.get(banktype),
                api_response_params.get(paymoney),
                api_response_params.get(ordernumber),
                api_response_params.get(callbackurl),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[仁信]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        try {

            if (HandlerUtil.isFS(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
                Map result = Maps.newHashMap();
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam).toString());
                payResultList.add(result);
            }else{
                String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
                // JSONObject resJson = JSONObject.parseObject(new String(resultStr.getBytes("ISO-8859-1"), "GBK"));
                String api_channel_bank_name = channelWrapper.getAPI_CHANNEL_BANK_NAME();
                if(resultStr.toLowerCase().contains("</form>") && ( HandlerUtil.isWY(channelWrapper) ||  HandlerUtil.isWapOrApp(channelWrapper)) ){
                    //String body = HandlerUtil.replaceBlank("");
                    HashMap<String, String> result = Maps.newHashMap();
                    result.put(HTMLCONTEXT, resultStr);
                    payResultList.add(result);
                }else{

                    if( !resultStr.contains("qrurl") &&resultStr.contains("html") ) {  //HandlerUtil.isWapOrApp(channelWrapper) 微信扫码转到微信反扫
                        Document document = Jsoup.parse(resultStr);
                        Element aEl = document.getElementsByTag("meta").select(" meta[http-equiv='refresh']").first();
                        if(null!=aEl && StringUtils.isNotBlank(aEl.attr("content"))){
                            HashMap<String, String> result = Maps.newHashMap();
                            result.put(JUMPURL, aEl.attr("content").substring(aEl.attr("content").indexOf("url=")+4));
                            payResultList.add(result);
                        }else{
                            log.error("发送支付请求，及获取支付请求结果错误："+resultStr);
                            throw new PayException(resultStr );
                        }
                    }else{
                        JSONObject resJson = JSONObject.parseObject(resultStr);
                        String status = resJson.getString("status");
                        String qrurl = HandlerUtil.UrlDecode(resJson.getString("qrurl").contains("?data=")?resJson.getString("qrurl").substring(resJson.getString("qrurl").indexOf("?data=")+6):"").toString();
                        String message = resJson.getString("message");
                        if (status.equalsIgnoreCase("1") && StringUtils.isBlank(message) && StringUtils.isNotBlank(qrurl) && !qrurl.contains("空")) {
                            HashMap<String, String> result = Maps.newHashMap();
                            result.put(QRCONTEXT, qrurl);
                            result.put(PARSEHTML, resultStr);
                            payResultList.add(result);
                        }else{
                            log.error("[仁信]3.发送支付请求，及获取支付请求结果：" +resultStr + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                            throw new PayException(resJson.toJSONString());
                        }
                    }

                }


            }



        } catch (Exception e) {
            log.error("[仁信]3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(e.getMessage(), e);
        }
        log.debug("[仁信]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
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
        log.debug("[仁信]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}