package dc.pay.business.xpayzhifu;

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
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestPayHandler("XPAY")
public final class XPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XPayRequestHandler.class);



     private static final String pay_memberid = "pay_memberid";           //商户账号
     private static final String pay_orderid = "pay_orderid";           //商户订单号
     private static final String pay_amount = "pay_amount";                 //订单金额   以“元”为单位
     private static final String pay_applydate = "pay_applydate";              //交易日期   格式：YYYYMMDDHHMMSS
     private static final String pay_channelCode = "pay_channelCode";           //交易渠道
     private static final String pay_notifyurl = "pay_notifyurl";           //支付结果通知地址
     private static final String pay_md5sign = "pay_md5sign";            //签名
     private static final String pay_bankcode = "pay_bankcode";



    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(pay_memberid,channelWrapper.getAPI_MEMBERID());
            payParam.put(pay_orderid,channelWrapper.getAPI_ORDER_ID());
            payParam.put(pay_amount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(pay_applydate,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
            payParam.put(pay_channelCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(pay_notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(pay_bankcode,"");
        }
        log.debug("[XPay支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String paramsStr = String.format("pay_memberid^%s&pay_orderid^%s&pay_amount^%s&pay_applydate^%s&pay_channelCode^%s&pay_notifyurl^%s&key=%s",
                params.get(pay_memberid),
                params.get(pay_orderid),
                params.get(pay_amount),
                params.get(pay_applydate),
                params.get(pay_channelCode),
                params.get(pay_notifyurl),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[XPay支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)  || HandlerUtil.isWapOrApp(channelWrapper)    ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='post'"));
                payResultList.add(result);
            }else{
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
                String qrContent =null;
                try{
                      qrContent = getQrContent(resultStr);
                    if(StringUtils.isNotBlank(qrContent)){result.put(QRCONTEXT,qrContent);payResultList.add(result);}
                }catch (Exception e){
                    throw new PayException(resultStr);
                }
                if(StringUtils.isBlank(qrContent)) throw new PayException(resultStr);
            }
        } catch (Exception e) { 
             log.error("[XPay支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[XPay支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[XPay支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }


    public   String getQrContent(String resultStr){
        Document document = Jsoup.parse(resultStr);
        Element bodyEl = document.getElementsByTag("script").eq(2).first();  //获取第三个 <script>标签
        String data = HandlerUtil.trim(bodyEl.data());
        return data.substring(data.indexOf("text:")+6, data.length()-7);
    }

}


