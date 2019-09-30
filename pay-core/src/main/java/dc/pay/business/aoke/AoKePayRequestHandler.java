package dc.pay.business.aoke;

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
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;
import dc.pay.utils.qr.QRCodeUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.crypto.hash.Sha256Hash;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestPayHandler("AOKE")
public final class AoKePayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(AoKePayRequestHandler.class);

    private static final String        merchantid = "merchantid";
    private static final String        siteid = "siteid";
    private static final String        Amount = "Amount";
    private static final String        order_id = "order_id";
    private static final String        type = "type";
    private static final String        version = "version";
    private static final String        bankcode = "bankcode";
    private static final String        return_url = "return_url";
    private static final String        notify_url = "notify_url";
    private static final String        security_code = "security_code";



    @Override
    protected Map<String, String> buildPayParam() throws PayException {

        if(!channelWrapper.getAPI_MEMBERID().contains("&")){
            throw new PayException("商户号格式错误，正确格式请使用&符号链接商户账户和SiteID,如：商户号&网站ID");
        }

        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(merchantid,channelWrapper.getAPI_MEMBERID().split("&")[0]);
            payParam.put(siteid,channelWrapper.getAPI_MEMBERID().split("&")[1]);
            payParam.put(Amount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(order_id,channelWrapper.getAPI_ORDER_ID());
            payParam.put(type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            if(HandlerUtil.isWY(channelWrapper)) {
                payParam.put(type,"3");
                payParam.put(bankcode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            }
            payParam.put(version,"2.0");
            payParam.put(return_url,channelWrapper.getAPI_WEB_URL());
            payParam.put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());

        }
        log.debug("[澳科支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
       // $securityCode = md5(hash('sha256', hash('sha256', md5($orderId . $amount . $merchantId . $siteId . $password))));
        String pay_md5sign = null;
        String pay_sign_str=String.format("%s%s%s%s%s",params.get(order_id),params.get(Amount),params.get(merchantid),params.get(siteid),channelWrapper.getAPI_KEY());
        String md51 = HandlerUtil.getMD5UpperCase(pay_sign_str).toLowerCase();

        String sha2561 = new Sha256Hash(md51).toString();
        String sha2562  = new Sha256Hash(sha2561).toString();
        pay_md5sign = HandlerUtil.getMD5UpperCase(sha2562).toLowerCase();
        log.debug("[澳科支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr="";
        try {
            if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)  ||  HandlerUtil.isWapOrApp(channelWrapper)   ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());  //.replace("method='post'","method='get'"));
                payResultList.add(result);
            }else{
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
                Document document = Jsoup.parse(resultStr);
                Element  bodyEl = document.getElementsByTag("body").first();

                String  imgSrc="";
                if(HandlerUtil.isYLSM(channelWrapper)){//解析银联扫码
                       imgSrc =QRCodeUtil.decodeByUrl(bodyEl.select("div.qr-image img").first().attr("src"));
                }else{
                      imgSrc = bodyEl.select("a.sure-pay").first().attr("href");
                }




//                String imgSrc = bodyEl.select("div.qr-image img ").first().attr("src");
//                if(StringUtils.isBlank(imgSrc) || !imgSrc.toLowerCase().startsWith("data:image")){
//                    log.error("[付呗支付]-[请求支付]-3.发送支付请求，及获取支付请求结果失败：二维码出错。"+HandlerUtil.UrlDecode(imgSrc));
//                    throw new PayException("[付呗支付]-[请求支付]-3.发送支付请求，及获取支付请求结果失败：二维码出错。"+HandlerUtil.UrlDecode(imgSrc));
//                }
//                String rqContent = QRCodeUtil.decodeByBase64(imgSrc);

            if(1==1 ) {  //扫码（银联扫码&微信&支付宝&QQ）
                    if(StringUtils.isNotBlank(imgSrc)){
                        result.put(QRCONTEXT, imgSrc);
                        payResultList.add(result);
                    }else {
                        throw new PayException(resultStr);
                    }

                } else if(1==2){//H5

                }else {
                    log.error("[澳科支付]3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(JSON.toJSONString(resultStr));
                }
            }
        } catch (Exception e) {
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());//.replace("method='post'","method='get'"));
            //payResultList.add(result);
            log.error("[澳科支付]3.发送支付请求，及获取支付请求结果出错：{},Error{}:", resultStr,e.getMessage());
            throw new PayException(resultStr+"Error: "+e.getMessage());
        }
        log.debug("[澳科支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[澳科支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}