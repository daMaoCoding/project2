package dc.pay.business.ruyifu;

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
import dc.pay.utils.qr.QRCodeUtil;
import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.util.*;

@RequestPayHandler("RUYIFU")
public final class RuYiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(RuYiFuPayRequestHandler.class);

    private static final String P_UserID = "P_UserID";
    private static final String P_OrderID = "P_OrderID";
    private static final String P_FaceValue = "P_FaceValue";
    private static final String P_Price = "P_Price";
    private static final String P_ChannelID = "P_ChannelID";
    private static final String P_Result_URL = "P_Result_URL";
    private static final String P_Notify_URL = "P_Notify_URL";
    private static final String QRCONTEXT = "QRCONTEXT";
    private static final String PARSEHTML = "PARSEHTML";
    private static final String HTMLCONTEXT = "HTMLCONTEXT";
    private static final String JUMPURL = "JUMPURL";
    private static final String P_Description = "P_Description";





    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        //String p_custormId =  NM + "_" + RuYiFuUtil.Md5(channelWrapper.getAPI_MEMBERID() + "|" + channelWrapper.getAPI_KEY() + "|" + NM);

        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(P_UserID, channelWrapper.getAPI_MEMBERID());
                put(P_OrderID, channelWrapper.getAPI_ORDER_ID());
                put(P_FaceValue, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(P_Price, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                if(HandlerUtil.isWY(channelWrapper)){
                    put(P_ChannelID,"1");
                    put(P_Description,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                }else{
                    put(P_ChannelID,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                }
                put(P_Result_URL,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(P_Notify_URL, channelWrapper.getAPI_WEB_URL());
            }
        };
        log.debug("[如一付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map payParam) throws PayException {
        String signStr = payParam.get(P_UserID) + "|" + payParam.get(P_OrderID) + "|" + "|" + "|" + payParam.get(P_FaceValue) + "|" + payParam.get(P_ChannelID);
        String pay_md5sign = MD5Util.MD5(signStr + "|" + channelWrapper.getAPI_KEY()).toLowerCase();
        // pay_md5sign = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[如一付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        try {

          //  String urlWithParam = HttpUtil.getURLWithParam(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
            //System.out.println(urlWithParam);

            if(channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("_WY_") ||channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("_WAP_")||channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("_WAPAPP_") ){
                StringBuffer sbHtml = new StringBuffer();
                sbHtml.append("<form id='mobaopaysubmit' name='mobaopaysubmit' action='" + channelWrapper.getAPI_CHANNEL_BANK_URL() + "' method='post'>");
                for (Map.Entry<String, String> entry : payParam.entrySet()) {
                    sbHtml.append("<input type='hidden' name='" + entry.getKey() + "' value='" + entry.getValue() + "'/>");
                }
                sbHtml.append("</form>");
                sbHtml.append("<script>document.forms['mobaopaysubmit'].submit();</script>");
                Map result = Maps.newHashMap();
                result.put(HTMLCONTEXT, sbHtml.toString());
                payResultList.add(result);
            } else {
                String firstPayresult = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.GET);
                if (firstPayresult.length() < 10 || firstPayresult.contains("errcode")) {
                    log.error("[如一付]3.发送支付请求，及获取支付请求结果：" + firstPayresult + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(firstPayresult);
                }

                Document document = Jsoup.parse(firstPayresult);
                Element bodyEl = document.getElementsByTag("body").first();
                Element formEl = bodyEl.getElementsByTag("form").first();
                Map<String, String> secondPayParam = HandlerUtil.parseFormElement(formEl);


                if (null != secondPayParam && StringUtils.isNotBlank(secondPayParam.get("action"))) {
                    Result result2 = HandlerUtil.sendToThreadPayServ(channelWrapper.getAPI_CHANNEL_BANK_URL(), secondPayParam, channelWrapper.getAPI_CHANNEL_BANK_URL());
                    document = Jsoup.parse(result2.getBody());
                    bodyEl = document.getElementsByTag("body").first();
                    Element qrImg = bodyEl.getElementsByTag("div").select("div[class='qr-image'] img").first();
                    if(null==qrImg || StringUtils.isBlank(qrImg.attr("src"))){
                        throw new PayException(bodyEl.toString() );
                    }
                    String qrUrl = HandlerUtil.getDomain(channelWrapper.getAPI_CHANNEL_BANK_URL()).concat(qrImg.attr("src"));
                    HashMap<String, String> resultMap = Maps.newHashMap();
                    resultMap.put(QRCONTEXT, QRCodeUtil.decodeByUrl(qrUrl));
                   // resultMap.put(HTMLCONTEXT, bodyEl.toString());
                    payResultList.add(resultMap);


                }

            }
        } catch (Exception e) {
            log.error("[如一付]3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(e.getMessage(), e);
        }
        log.debug("[如一付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
        return payResultList;
    }


    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (null != resultListMap && !resultListMap.isEmpty()) {
            if (resultListMap.size() == 1) {
                Map<String, String> resultMap = resultListMap.get(0);
                if (null != resultMap && resultMap.containsKey(QRCONTEXT)) {
                    requestPayResult.setRequestPayamount(channelWrapper.getAPI_AMOUNT());
                    requestPayResult.setRequestPayOrderId(channelWrapper.getAPI_ORDER_ID());
                    requestPayResult.setRequestPayOrderCreateTime(HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "yyyy-MM-dd HH:mm:ss"));
                    requestPayResult.setRequestPayQRcodeURL(null);
                    requestPayResult.setRequestPayChannelBankName(channelWrapper.getAPI_CHANNEL_BANK_NAME());
                    requestPayResult.setRequestPayQRcodeContent(resultMap.get(QRCONTEXT));
                }
                if (null != resultMap && resultMap.containsKey(HTMLCONTEXT)) {
                    requestPayResult.setRequestPayamount(channelWrapper.getAPI_AMOUNT());
                    requestPayResult.setRequestPayOrderId(channelWrapper.getAPI_ORDER_ID());
                    requestPayResult.setRequestPayOrderCreateTime(HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "yyyy-MM-dd HH:mm:ss"));
                    requestPayResult.setRequestPayQRcodeURL(null);
                    requestPayResult.setRequestPayChannelBankName(channelWrapper.getAPI_CHANNEL_BANK_NAME());
                    requestPayResult.setRequestPayHtmlContent(resultMap.get(HTMLCONTEXT));
                }
                if (null != resultMap && resultMap.containsKey(JUMPURL)) {
                    requestPayResult.setRequestPayamount(channelWrapper.getAPI_AMOUNT());
                    requestPayResult.setRequestPayOrderId(channelWrapper.getAPI_ORDER_ID());
                    requestPayResult.setRequestPayOrderCreateTime(HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "yyyy-MM-dd HH:mm:ss"));
                    requestPayResult.setRequestPayQRcodeURL(null);
                    requestPayResult.setRequestPayChannelBankName(channelWrapper.getAPI_CHANNEL_BANK_NAME());
                    requestPayResult.setRequestPayJumpToUrl(resultMap.get(JUMPURL));
                }

            }
            if (ValidateUtil.requestesultValdata(requestPayResult)) {
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            } else {
                throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[如一付]-[请求支付]-4.处理请求支付成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}