package dc.pay.business.fubei;

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
import dc.pay.utils.qr.QRCodeUtil;
import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * ************************
 * @author tony 3556239829
 */
@RequestPayHandler("FUBEI")
public class FuBeiPayRequestHandler extends PayRequestHandler {
    private static final Logger log =  LoggerFactory.getLogger(FuBeiPayRequestHandler.class);

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
        log.debug("[付呗支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
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
        log.debug("[付呗支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return  pay_md5sign;
    }


    @Override
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        String bank_name = channelWrapper.getAPI_CHANNEL_BANK_NAME();
        ArrayList<Map<String,String>> payResultList = Lists.newArrayList();
        try{
            Result firstPayresult = HttpUtil.get(channelWrapper.getAPI_CHANNEL_BANK_URL(), null, payParam);
            Document document = Jsoup.parse(firstPayresult.getBody());
            Element bodyEl = document.getElementsByTag("body").first();
            if(bodyEl.html().contains("error:")){
                log.error("发送支付请求，及获取支付请求结果错误："+bodyEl.html());
                throw new PayException(bodyEl.html() );
            }
            Element formEl = bodyEl.getElementsByTag("form").first();
            Map<String, String> secondPayParam = HandlerUtil.parseFormElement(formEl);
            payResultList.add(secondPayParam);


            if(bank_name.contains("_ZFB_SM") || bank_name.contains("_QQ_SM") ){//支付宝,QQ扫码
                bodyEl = document.getElementsByTag("body").first();
                String imgSrc = bodyEl.select("div.erweima img ").first().attr("src");
                if(StringUtils.isBlank(imgSrc) || !imgSrc.toLowerCase().startsWith("data:image")){
                    log.error("[付呗支付]-[请求支付]-3.发送支付请求，及获取支付请求结果失败：二维码出错。"+HandlerUtil.UrlDecode(imgSrc));
                    throw new PayException("[付呗支付]-[请求支付]-3.发送支付请求，及获取支付请求结果失败：二维码出错。"+HandlerUtil.UrlDecode(imgSrc));
                }
                String rqContent = QRCodeUtil.decodeByBase64(imgSrc);
                HashMap<String, String> resultMap = Maps.newHashMap();
                resultMap.put("qr_imgSrc", imgSrc);
                resultMap.put("qr_imgContent", rqContent);
                payResultList.add(resultMap);
            }





            if(null!=secondPayParam && ("/code/QQCode.aspx".equalsIgnoreCase(secondPayParam.get("action")) || "/code/wxcode.aspx".equalsIgnoreCase(secondPayParam.get("action")))){
                Result result = HandlerUtil.sendToThreadPayServ(secondPayParam, channelWrapper.getAPI_CHANNEL_BANK_URL());
                document = Jsoup.parse(result.getBody());
                bodyEl = document.getElementsByTag("body").first();
                String imgSrc = bodyEl.select("div.divCode img ").first().attr("src");
                if(StringUtils.isBlank(imgSrc)){
                    log.error("[付呗支付]-[请求支付]-3.发送支付请求，及获取支付请求结果失败：二维码为空。"+bodyEl.select("div.divCode img ").first().toString());
                    throw new PayException("[付呗支付]-[请求支付]-3.发送支付请求，及获取支付请求结果失败：二维码为空。"+bodyEl.select("div.divCode img ").first().toString());
                }
                String rqContent =    QRCodeUtil.decodeByUrl(imgSrc);
                HashMap<String, String> resultMap = Maps.newHashMap();
                resultMap.put("qr_imgSrc", imgSrc);
                resultMap.put("qr_imgContent", rqContent);
                payResultList.add(resultMap);
            }else
            if(null!=secondPayParam && ("http://zwpay.chinagpay.com/bas/FrontTrans".equalsIgnoreCase(secondPayParam.get("action")) || "http://gpay.chinagpay.com/bas/FrontTrans".equalsIgnoreCase(secondPayParam.get("action"))  || channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("_WY_")  ||  "http://pay1.zhuhaixin.com/gotopay.aspx".equalsIgnoreCase(secondPayParam.get("action"))   )){
                HashMap<String, String> resultMap = Maps.newHashMap();
                String requestPayHtmlContent =  firstPayresult.getBody();
                resultMap.put("requestPayHtmlContent", requestPayHtmlContent);
                payResultList.add(resultMap);
             }else
                if(null!=secondPayParam && "/Cashier/Index.aspx".equalsIgnoreCase(secondPayParam.get("action"))){
                HashMap<String, String> resultMap = Maps.newHashMap();
                String actionUrl = HandlerUtil.getActionUrl("/Cashier/Index.aspx",channelWrapper.getAPI_CHANNEL_BANK_URL());
                bodyEl.getElementsByTag("form").first().attr("action",actionUrl); //todo
                resultMap.put("requestPayHtmlContent", bodyEl.html());
                payResultList.add(resultMap);
            }
        } catch (Exception e) {
            if(e instanceof  PayException){
                throw new PayException(e.getMessage(),e);
            }
            throw new PayException(SERVER_MSG.REQUEST_PAY_PARSE_RESULT_ERROR,e);
        }
        log.debug("[付呗支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
        return payResultList;
    }

    @Override
    protected RequestPayResult buildResult(List<Map<String, String>> result) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if(!result.isEmpty() && result.size()==2){
            Map<String, String> lastResult = result.get(1);
            requestPayResult.setRequestPayamount(channelWrapper.getAPI_AMOUNT());
            requestPayResult.setRequestPayOrderId(channelWrapper.getAPI_ORDER_ID());
            requestPayResult.setRequestPayOrderCreateTime(HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(),"yyyy-MM-dd HH:mm:ss"));
            requestPayResult.setRequestPayChannelBankName(channelWrapper.getAPI_CHANNEL_BANK_NAME());
            if (lastResult.containsKey("qr_imgSrc") && lastResult.containsKey("qr_imgContent")) {
                requestPayResult.setRequestPayQRcodeURL(lastResult.get("qr_imgSrc"));
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
        log.debug("[付呗支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}