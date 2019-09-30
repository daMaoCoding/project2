package dc.pay.business.yuanbao;

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
import org.springframework.http.HttpMethod;

import java.util.*;

@RequestPayHandler("YUANBAO")
public final class YuanBaoPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(YuanBaoPayRequestHandler.class);

    private static final String QRCONTEXT = "QrContext";
    private static final String HTMLCONTEXT = "HtmlContext";
    private static final String PARSEHTML = "parseHtml";


    private static final  String  version	     = "version";        //是	固定值3.0
     private static final String  method	         = "method";     //是	Boh.online.interface
     private static final String  partner	 = "partner";            //是	商户id,由元宝分配
     private static final String  banktype	     = "banktype";       //是	银行类型，default为跳转到元宝接口进行选择支付
     private static final String  paymoney	 = "paymoney";           //是	单位元（人民币）
     private static final String  ordernumber	 = "ordernumber";    //是	商户订单号
     private static final String  callbackurl	 = "callbackurl";    //是	下行异步通知地址
     private static final String  isshow	 = "isshow";             //否	固定值:0
     private static final String JUMPURL = "JUMPURL";


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(version, "3.0");
                put(method, "Boh.online.interface");
                put(partner, channelWrapper.getAPI_MEMBERID());
                put(banktype, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(paymoney, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(ordernumber, channelWrapper.getAPI_ORDER_ID());
                put(callbackurl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(isshow,"0");
            }
        };
        log.debug("[元宝]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
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
        log.debug("[元宝]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        try {
            String channel_flag = channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG();

            if(channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("_WAP_")||channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("_WAPAPP_")||HandlerUtil.isWebYlKjzf(channelWrapper)){
                String getURLForwapAPP = HttpUtil.getURLWithParam(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
                HashMap<String, String> result = Maps.newHashMap();
                result.put(JUMPURL, getURLForwapAPP);
                payResultList.add(result);
            }else {
                String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
                // JSONObject resJson = JSONObject.parseObject(new String(resultStr.getBytes("ISO-8859-1"), "GBK"));
                String api_channel_bank_name = channelWrapper.getAPI_CHANNEL_BANK_NAME();
                if (resultStr.toLowerCase().contains("</form>") && api_channel_bank_name.contains("_WY_")) {
                    //String body = HandlerUtil.replaceBlank("");
                    HashMap<String, String> result = Maps.newHashMap();
                    result.put(HTMLCONTEXT, resultStr);
                    payResultList.add(result);
                } else {
                    JSONObject resJson = JSONObject.parseObject(resultStr);
                    String status = resJson.getString("status");
                    String qrurl = resJson.getString("qrurl");
                    String message = resJson.getString("message");
                    if (status.equalsIgnoreCase("1") && StringUtils.isBlank(message) && StringUtils.isNotBlank(qrurl) && !ValidateUtil.isHaveChinese(qrurl) && !qrurl.contains("空")) {
                        HashMap<String, String> result = Maps.newHashMap();
                        result.put(QRCONTEXT, qrurl);
                        result.put(PARSEHTML, resultStr);
                        payResultList.add(result);
                    } else {
                        log.error("[元宝]3.发送支付请求，及获取支付请求结果：" + resultStr + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                        throw new PayException(resJson.toJSONString());
                    }
                }
            }
        } catch (Exception e) {
            log.error("[元宝]3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(e.getMessage(), e);
        }
        log.debug("[元宝]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
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
        log.debug("[元宝]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}