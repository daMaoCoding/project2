package dc.pay.business.haolian;

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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@RequestPayHandler("HAOLIAN")
public final class HaoLianPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(HaoLianPayRequestHandler.class);

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
                put(get_code, "");
                if(channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("_WY_")){
                    put(paytype, "bank");
                    put(bankcode, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                }
            }
        };
        log.debug("[豪联]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
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
        log.debug("[豪联]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        try {
            String channel_flag = channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG();
            String api_channel_bank_name = channelWrapper.getAPI_CHANNEL_BANK_NAME();

            //String resultStr = HttpUtil.doPostRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);


            Result firstPayresult = HttpUtil.get(channelWrapper.getAPI_CHANNEL_BANK_URL(), null, payParam);
            Document document = Jsoup.parse(firstPayresult.getBody());
            Element bodyEl = document.getElementsByTag("body").first();
            if(bodyEl.html().contains("error:") || bodyEl.html().length()<20){
                log.error("发送支付请求，及获取支付请求结果错误："+bodyEl.html());
                throw new PayException(bodyEl.html() );
            }

                if (api_channel_bank_name.endsWith("WX_SM") || api_channel_bank_name.endsWith("ZFB_SM")  ||api_channel_bank_name.endsWith("QQ_SM") ) {
                        String hidUrl = bodyEl.select("input#hidUrl").first().attr("value");
                        HashMap<String, String> result = Maps.newHashMap();
                        result.put(QRCONTEXT, hidUrl);
                        result.put(PARSEHTML, firstPayresult.getBody());
                        payResultList.add(result);
                } else if (api_channel_bank_name.contains("_WY_")) {
                    StringBuffer sbHtml = new StringBuffer();
                    sbHtml.append("<form id='webFormPay' name='webFormPay' action='" + channelWrapper.getAPI_CHANNEL_BANK_URL() + "' method='post'>");
                    for (Map.Entry<String, String> entry : payParam.entrySet()) {
                        sbHtml.append("<input type='hidden' name='" + entry.getKey() + "' value='" + entry.getValue() + "'/>");
                    }
                    sbHtml.append("</form>");
                    sbHtml.append("<script>document.forms['webFormPay'].submit();</script>");
                    Map result = Maps.newHashMap();
                    result.put(HTMLCONTEXT, sbHtml.toString());
                    payResultList.add(result);
                }



        } catch (Exception e) {
            log.error("[豪联]3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(e.getMessage(), e);
        }
        log.debug("[豪联]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
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
                    requestPayResult.setRequestPayHtmlContent(resultMap.get(HTMLCONTEXT));
                    requestPayResult.setRequestPayJumpToUrl(resultMap.get(null));
                    requestPayResult.setRequestPayChannelBankName(channelWrapper.getAPI_CHANNEL_BANK_NAME());
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
        log.debug("[豪联]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}