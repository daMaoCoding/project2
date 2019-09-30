package dc.pay.business.eefutong;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 *      会与易富通EFUTONG重名，现在命名为：eefutong
 * 
 * @author andrew
 * Dec 14, 2018
 */
@RequestPayHandler("EEFUTONG")
public final class EEFutongPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(EEFutongPayRequestHandler.class);

    //参数名 参数  可空  加入签名    说明
    //商户号 merchant    N   Y   下发的商户号
    private static final String merchant                ="merchant";
    //金额  amount  N   Y   单位元（人民币），2位小数
    private static final String amount                ="amount";
    //支付方式    pay_code    N   Y   填写相应的支付方式编码
    private static final String pay_code                ="pay_code";
    //商户订单号   order_no    N   Y   订单号，max(50),该值需在商户系统内唯一
    private static final String order_no                ="order_no";
    //异步通知地址  notify_url  N   Y   异步通知地址，需要以http://开头且没有任何参数
    private static final String notify_url                ="notify_url";
    //同步通知地址  return_url  N   Y   同步跳转地址，支付成功后跳回
    private static final String return_url                ="return_url";
    //请求返回方式  json    Y   N   固定值：json; 注意：只适用于扫码付款
    private static final String json                ="json";
    //备注消息    attach  Y   有值加入    回调时原样返回
//    private static final String attach                ="attach";
    //请求时间    order_time  Y   Y   格式YYYY-MM-DD hh:ii:ss，回调时原样返回
    private static final String order_time                ="order_time";
    //商户的用户id cuid    Y   有值加入    商户名下的能表示用户的标识，方便对账，回调时原样返回
//    private static final String cuid                ="cuid";
    //MD5签名   sign    N   N   32位小写MD5签名值
//    private static final String sign                 ="sign";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merchant, channelWrapper.getAPI_MEMBERID());
                put(amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(pay_code,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(order_no,channelWrapper.getAPI_ORDER_ID());
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(return_url,channelWrapper.getAPI_WEB_URL());
                if (handlerUtil.isWEBWAPAPP_SM(channelWrapper)) {
                    put(json,"json");
                }
                put(order_time,DateUtil.formatDateTimeStrByParam("yyyy-MM-dd HH:mm:ss") );
            }
        };
        log.debug("[e付通]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
//            if (!pay_productname.equals(paramKeys.get(i)) && !pay_attach.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
            if (!json.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        //最后一个&转换成#
        //signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "#" );
        signSrc.append(key +"="+ channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[e付通]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
//        if (handlerUtil.isWEBWAPAPP_SM(channelWrapper) && !HandlerUtil.isOrderFromWapOrApp(channelWrapper.getAPI_ORDER_FROM())) {
//            StringBuffer sbHtml = new StringBuffer();
//            sbHtml.append("<header>");
//            sbHtml.append("<meta http-equiv='expires' content='0'>");
//            sbHtml.append("<meta http-equiv='pragma' content='no-cache'>");
//            sbHtml.append("<meta http-equiv='cache-control' content='no-cache'>");
//            sbHtml.append("<meta name='viewport' content='width=device-width, user-scalable=no, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0'>");
//            sbHtml.append("<meta name='format-detection' content='telephone=no'>");
//            sbHtml.append("<meta name='apple-mobile-web-app-capable' content='yes'>");
//            sbHtml.append("</header>");
//            sbHtml.append("<input type='hidden' id='timestamp' value='"+System.currentTimeMillis()+"'/>");
//            sbHtml.append("<div id='msg' style='padding:8px;border:1px solid #96c2f1;background:#eff7ff;text-align: center;'> 请在APP或者WAP应用上使用通道...... <div>");
//            sbHtml.append("");
//            System.out.println("========>来自电脑扫码");
//            //保存第三方返回值
//            result.put(HTMLCONTEXT,sbHtml.toString());
//        }else {
//            System.out.println("========>来自手机跳转");
//            StringBuffer htmlContent = HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
//            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
//            //保存第三方返回值
//            result.put(HTMLCONTEXT,htmlContent.toString());
//        }
        //支付宝扫码，不允许电脑上直接扫码
//        if ((handlerUtil.isWEBWAPAPP_SM(channelWrapper) && HandlerUtil.isOrderFromWapOrApp(channelWrapper.getAPI_ORDER_FROM())) || 
//                channelWrapper.getAPI_ORDER_ID().startsWith("T") || 
//                handlerUtil.isWapOrApp(channelWrapper)) {
//            StringBuffer htmlContent = HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
//            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
//            //保存第三方返回值
//            result.put(HTMLCONTEXT,htmlContent.toString());
//        }else {
//            throw new PayException("请在APP或者WAP应用上使用通道......");
//        }
        if (handlerUtil.isWapOrApp(channelWrapper)) {
            StringBuffer htmlContent = HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
            //保存第三方返回值
            result.put(HTMLCONTEXT,htmlContent.toString());
        }else{
            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
            if (StringUtils.isBlank(resultStr)) {
                log.error("[e付通]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            
            JSONObject jsonObject = null;
            try {
                jsonObject = JSONObject.parseObject(new String(resultStr.getBytes("ISO-8859-1"), "GBK"));
            } catch (Exception e) {
                log.error("[通扫]-[请求支付]-3.3.发送支付请求，及获取支付请求出错，订单号：" + channelWrapper.getAPI_ORDER_ID() + ",通道名称：" + channelWrapper.getAPI_CHANNEL_BANK_NAME() + ",postUrl:" + channelWrapper.getAPI_CHANNEL_BANK_URL() + ",payForManagerCGIResultJsonObj" + e.getMessage(), e);
                //log.error("[盛盈付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + resultStr + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(e.getMessage(),e);
            }          
            //只取正确的值，其他情况抛出异常
            //if (null != jsonObject && jsonObject.containsKey("result_code") && "SUCCESS".equalsIgnoreCase(jsonObject.getString("result_code"))  && 
            //(jsonObject.containsKey("qrcode") && StringUtils.isNotBlank(jsonObject.getString("qrcode")) || 
            // jsonObject.containsKey("pay_url") && StringUtils.isNotBlank(jsonObject.getString("pay_url")))
            //){
            if (null != jsonObject && jsonObject.containsKey("QRCodeLink") && StringUtils.isNotBlank(jsonObject.getString("QRCodeLink"))) {
                    result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, handlerUtil.UrlDecode(jsonObject.getString("QRCodeLink")));
                    //if (handlerUtil.isWapOrApp(channelWrapper)) {
                    //    result.put(JUMPURL, code_url);
                    //}else{
                    //    result.put(QRCONTEXT, code_url);
                    //}
                //按不同的请求接口，向不同的属性设置值
                //if(channelWrapper.getAPI_CHANNEL_BANK_NAME().contains(PayEumeration.CHANNEL_TYPE._WAP_.name())||channelWrapper.getAPI_CHANNEL_BANK_NAME().contains(PayEumeration.CHANNEL_TYPE._WAPAPP_.name())) {
                //    result.put(JUMPURL, jsonObject.getString("barCode"));
                //}else{
                //    result.put(QRCONTEXT, jsonObject.getString("barCode"));
                //}
                result.put("第三方返回",jsonObject.toString()); //保存全部第三方信息，上面的拆开没必要
            }else {
                log.error("[通扫]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            
//            if (!resultStr.contains("<form") || !resultStr.contains("</form>")) {
//                log.error("[e付通]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//            }
//            Document document = Jsoup.parse(resultStr);  //Jsoup.parseBodyFragment(html)
//            Element formEl = document.getElementsByTag("form").first();
//            Map<String, String> secondPayParam = HandlerUtil.parseFormElement(formEl);
//            String resultStr2 = RestTemplateUtil.postForm(secondPayParam.get("action"), secondPayParam,"UTF-8");
//            if (StringUtils.isBlank(resultStr2)) {
//                log.error("[e付通]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr2) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr2);
//            }
//            Elements select = Jsoup.parse(resultStr2).select("[id=  ]");
//            if (null == select || select.size() < 1) {
//                log.error("[e付通]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr2) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr2);
//            }
//            String href = select.first().attr("href");
//            if (StringUtils.isBlank(href) || !href.contains("://")) {
//                log.error("[e付通]-[请求支付]-3.5.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr2) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr2);
//            }
//            if (handlerUtil.isWapOrApp(channelWrapper)) {
//                result.put(JUMPURL, href);
//            }else {
//                result.put(QRCONTEXT, href);
//            }
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[e付通]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[e付通]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}