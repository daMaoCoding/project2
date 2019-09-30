package dc.pay.business.suiyifu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

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

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Jul 3, 2018
 */
@RequestPayHandler("SUIYIFU")
public final class SuiYiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(SuiYiFuPayRequestHandler.class);

    //参数名              参数                    可空           说明
    //商户ID              parter                  否             商户id，由随意付商户系统分配
    //银行类型            type                    否             银行类型，具体请参考附录1	金额 value 否	单位元（人民币） ，2 位小数，最小支付金额	为1.00,微信支付宝至少2 元
    //金额                value                   否             单位元（人民币） ，2 位小数，最小支付金额   为1.00,微信支付宝至少2 元
    //商户订单号          orderid                 否             商户系统订单号，该订单号将作为随意付接口的返回数据。该值需在商户系统内唯一
    //异步通知地址        callbackurl             否             异步通知过程的返回地址，需要以http://开头且没有任何参数
    //同步通知地址        hrefbackurl             是             
    //备注消息            attach                  是             备注信息，会原样返回。若该值包含中文，请注意编码
    //MD5 签名            sign                    否             32 位小写MD5 签名值，GB2312 编码
    private static final String parter                ="parter";
    private static final String type                  ="type";
    private static final String value                  ="value";
    private static final String orderid               ="orderid";
    private static final String callbackurl           ="callbackurl";
    private static final String hrefbackurl           ="hrefbackurl";
    private static final String attach                ="attach";
//    是的 和其它参数一样 onlyqr= QR不签名 传送过来就可以
//    这个参数 一直都有的 只是大部分商户都是直接跳转到扫码收银台 针对部分商户 需要获取qr码自行展示 所以有需要的商户 就告知下
//     只是扫码 
    private static final String onlyqr                ="onlyqr";
    //传qronly = “QR”就可以
//    private static final String onlyqr                  ="onlyqr";
    
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
            	put(parter, channelWrapper.getAPI_MEMBERID());
            	if(HandlerUtil.isOrderFromWapOrApp(channelWrapper.getAPI_ORDER_FROM()) && HandlerUtil.isWY(channelWrapper) ){
                    put(type,"1005"); //第三方新开发手机网银-需求2536
                }else{
                    put(type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                }
            	put(value,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            	put(orderid,channelWrapper.getAPI_ORDER_ID());
            	put(callbackurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            	put(hrefbackurl,channelWrapper.getAPI_WEB_URL());
            	put(attach, channelWrapper.getAPI_MEMBERID());
            	//     只是扫码 
            	if (channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("WEBWAPAPP")) {
            	    put(onlyqr, "QR");
                }
            }
        };
        log.debug("[随意付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map api_response_params) throws PayException {
        //1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
        //2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(parter+"=").append(api_response_params.get(parter)).append("&");
        signSrc.append(type+"=").append(api_response_params.get(type)).append("&");
        signSrc.append(value+"=").append(api_response_params.get(value)).append("&");
        signSrc.append(orderid+"=").append(api_response_params.get(orderid)).append("&");
        signSrc.append(callbackurl+"=").append(api_response_params.get(callbackurl));
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[随意付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
        }else{
            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
            if (StringUtils.isBlank(resultStr) || resultStr.contains("error:")) {
                log.error("[随意付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            if (!channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("SUIYIFU_BANK_WEBWAPAPP_ZFB_SM")) {
                result.put(QRCONTEXT, resultStr);
            }else {
                if (!resultStr.contains("<form")) {
                    log.error("[随意付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
                Document document = Jsoup.parse(resultStr);  //Jsoup.parseBodyFragment(html)
                Element formEl = document.getElementsByTag("form").first();
                Map<String, String> secondPayParam = HandlerUtil.parseFormElement(formEl);
                String resultStr2 = RestTemplateUtil.sendByRestTemplateRedirect(secondPayParam.get("action"), secondPayParam, String.class, HttpMethod.POST);
                if (StringUtils.isBlank(resultStr2)) {
                    log.error("[随意付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr2) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr2);
                }
                Elements elements = Jsoup.parse(resultStr2).select("img");
                if (null == elements || elements.size() < 1) {
                    log.error("[随意付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr2) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr2);
                }
                String src = elements.first().attr("src");
                if (StringUtils.isBlank(src)) {
                    log.error("[随意付]-[请求支付]-3.5.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr2) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr2);
                }
                String qr = QRCodeUtil.decodeByBase64(src);
                if (StringUtils.isBlank(qr)) {
                    log.error("[随意付]-[请求支付]-3.6.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr2) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr2);
                }
                result.put(QRCONTEXT, qr);
            }
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[随意付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
        return payResultList;
    }

    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (null != resultListMap && !resultListMap.isEmpty()) {
            if (resultListMap.size() == 1) {
                Map<String, String> resultMap = resultListMap.get(0);
                requestPayResult=  buildResult(resultMap, channelWrapper,requestPayResult);
            }
            if (ValidateUtil.requestesultValdata(requestPayResult)) {
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            } else {
                throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[随意付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}