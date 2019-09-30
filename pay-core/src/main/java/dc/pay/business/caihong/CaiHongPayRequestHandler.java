package dc.pay.business.caihong;

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
import dc.pay.utils.HttpUtil;
import dc.pay.utils.Result;
import dc.pay.utils.ValidateUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * ************************
 * @author tony 3556239829
 */
@RequestPayHandler("CAIHONG")
public class CaiHongPayRequestHandler extends PayRequestHandler {
    private static final Logger log =  LoggerFactory.getLogger(CaiHongPayRequestHandler.class);

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put("parter",channelWrapper.getAPI_MEMBERID());
                put("type", channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put("value", HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put("orderid",channelWrapper.getAPI_ORDER_ID());
                put("callbackurl",channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
             }
        };
        log.debug("[彩虹支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }
    @Override
    protected String buildPaySign(Map<String, String> payParam) throws PayException {
        StringBuilder sb = new StringBuilder();
        sb.append("parter=").append(payParam.get("parter")).append("&")
          .append("type=").append(payParam.get("type")).append("&")
          .append("value=").append(payParam.get("value")).append("&")
          .append("orderid=").append(payParam.get("orderid")).append("&")
          .append("callbackurl=").append(payParam.get("callbackurl"))
          .append(channelWrapper.getAPI_KEY());
        String pay_md5sign = HandlerUtil.getMD5UpperCase(sb.toString()).toLowerCase();
        log.debug("[彩虹支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return  pay_md5sign;
    }


    @Override
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String,String>> payResultList = Lists.newArrayList();
        try{
            Result firstPayresult = HttpUtil.get(channelWrapper.getAPI_CHANNEL_BANK_URL(), null, payParam);
            Document document = Jsoup.parse(firstPayresult.getBody());
            Element bodyEl = document.getElementsByTag("body").first();
            if(bodyEl.html().contains("error:")){
                log.error("发送支付请求，及获取支付请求结果错误："+bodyEl.html());
                throw new PayException(bodyEl.html() );
            }

            if(channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("WX_SM") || channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("ZFB_SM") ||  channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("JD_SM") || channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("QQ_SM") || channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("YL_SM")){
                bodyEl = document.getElementsByTag("body").first();
                String imgSrc = bodyEl.select("div#wxQrcode img ").first().attr("src");
                String qr_imgSrc = imgSrc.substring(imgSrc.indexOf("data=")+5);
                HashMap<String, String> resultMap = Maps.newHashMap();
                resultMap.put("qr_imgContent", qr_imgSrc);
                payResultList.add(resultMap);
            }else if(channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("_WY_")){

            }


        } catch (PayException |IOException   e ) {
            if(e instanceof  PayException){
                throw new PayException(e.getMessage(),e);
            }
            throw new PayException(SERVER_MSG.REQUEST_PAY_PARSE_RESULT_ERROR,e);
        }
        log.debug("[彩虹支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
        return payResultList;
    }

    @Override
    protected RequestPayResult buildResult(List<Map<String, String>> result) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if(!result.isEmpty() && result.size()==1){
            Map<String, String> lastResult = result.get(0);
            requestPayResult.setRequestPayamount(channelWrapper.getAPI_AMOUNT());
            requestPayResult.setRequestPayOrderId(channelWrapper.getAPI_ORDER_ID());
            requestPayResult.setRequestPayOrderCreateTime(HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(),"yyyy-MM-dd HH:mm:ss"));
            requestPayResult.setRequestPayChannelBankName(channelWrapper.getAPI_CHANNEL_BANK_NAME());
            if (lastResult.containsKey("qr_imgContent")) {
                requestPayResult.setRequestPayQRcodeContent(lastResult.get("qr_imgContent"));
            }else if(lastResult.containsKey("requestPayHtmlContent")){
                requestPayResult.setRequestPayHtmlContent(lastResult.get("requestPayHtmlContent"));

            }
            if(ValidateUtil.requestesultValdata(requestPayResult)){
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            }else{
                throw  new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        }else{
            throw  new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[彩虹支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}