package dc.pay.business.xinzhonglianzhifu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
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
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Apr 22, 2019
 */
@RequestPayHandler("XINZHONGLIANZHIFU")
public final class XinZhongLianZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XinZhongLianZhiFuPayRequestHandler.class);

  //字段名 填写类型    说明
    //merchantNo  必填  商户号（由我方提供的资料）
    private static final String merchantNo                       ="merchantNo";
    //merchantName    必填  商户名称（填写您的商户名称）
    private static final String merchantName                       ="merchantName";
    //payKey  必填  登陆商户后台-账户信息，获取支付key（非密钥，我方会根据该参数和商户号参数判断通道配置是否正确）
    private static final String payKey                       ="payKey";
    //payWayCode  必填  支付通道：    WEIXIN：微信    ALIPAY：支付宝    UNIONPAY 银联    
    private static final String payWayCode                       ="payWayCode";
    //orderNo 必填  你方生成的订单号
    private static final String orderNo                       ="orderNo";
    //payGateWay  必填  产品类型编号（10002）我方会定期提供该参数调整的
    private static final String payGateWay                       ="payGateWay";
    //productName 必填  产品名称：由我方提供的产品名称（zfcs+orderNo）
    private static final String productName                       ="productName";
    //orderPrice  必填  订单金额（单位：元）
    private static final String orderPrice                       ="orderPrice";
    //returnUrl   必填  支付成功跳转地址，注意：同步返回不带数据，只完成跳转操作。
    private static final String returnUrl                       ="returnUrl";
    //notifyUrl   必填  异步通知地址，订单成功后会通过该地址通知商户平台
    private static final String notifyUrl                       ="notifyUrl";
    //orderPeriod 必填  订单有效期（单位：分钟）如传入10，则订单有效期为10分钟
    private static final String orderPeriod                       ="orderPeriod";
    //ismobile    必填  H5-1，扫码-0
    private static final String ismobile                       ="ismobile";
    //orderDate   必填  下单日期（yyyy-MM-dd HH:mm:ss）
    private static final String orderDate                       ="orderDate";
    //orderTime   必填  与orderDate值相同加入验签
    private static final String orderTime                       ="orderTime";
    //sign    必填  MD5签名结果
//    private static final String sign                       ="sign";

    private static final String key        ="paySecret";
    //signature    数据签名    32    是    　
//    private static final String signature  ="pay_md5sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
        if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 2) {
            log.error("[新众联支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号merchantNo&支付payKey" );
            throw new PayException("[新众联支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号merchantNo&支付payKey" );
        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merchantNo, channelWrapper.getAPI_MEMBERID().split("&")[0]);
                put(merchantName,"name");
                put(payKey, channelWrapper.getAPI_MEMBERID().split("&")[1]);
//                put(payWayCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
//                put(payGateWay,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
                put(payWayCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(orderNo,channelWrapper.getAPI_ORDER_ID());
                put(payGateWay,"10002");
                put(productName,"name");
                put(orderPrice,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(returnUrl,channelWrapper.getAPI_WEB_URL());
                put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(orderPeriod,"10");
                //ismobile    必填  H5-1，扫码-0
                put(ismobile,  HandlerUtil.isWapOrApp(channelWrapper) ? "1" : "0");
                put(orderDate, DateUtil.formatDateTimeStrByParam("yyyy-MM-dd HH:mm:ss"));
                put(orderTime, DateUtil.formatDateTimeStrByParam("yyyy-MM-dd HH:mm:ss"));
            }
        };
        log.debug("[新众联支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
        }
        //最后一个&转换成#
        //signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "#" );
        signSrc.append(key +"="+ channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[新众联支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
//        if(HandlerUtil.isWapOrApp(channelWrapper)){
        if(true){
            StringBuffer htmlContent = HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
            //保存第三方返回值
            result.put(HTMLCONTEXT,htmlContent.toString());
        }
        
//        else{
//            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
//            if (StringUtils.isBlank(resultStr)) {
//                log.error("[新众联支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//            }
//            if (!resultStr.contains("<form") || !resultStr.contains("</form>")) {
//                log.error("[新众联支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//            }
//            Document document = Jsoup.parse(resultStr);  //Jsoup.parseBodyFragment(html)
//            Element formEl = document.getElementsByTag("form").first();
//            Map<String, String> secondPayParam = HandlerUtil.parseFormElement(formEl);
//            String resultStr2 = RestTemplateUtil.postForm(secondPayParam.get("action"), secondPayParam,"UTF-8");
//            if (StringUtils.isBlank(resultStr2)) {
//                log.error("[新众联支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr2) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr2);
//            }
//            Elements select = Jsoup.parse(resultStr2).select("[id=show_qrcode]");
//            if (null == select || select.size() < 1) {
//                log.error("[新众联支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr2) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr2);
//            }
//            String src = select.first().attr("src");
//            if (StringUtils.isBlank(src) || !src.contains("://")) {
//                log.error("[新众联支付]-[请求支付]-3.5.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr2) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr2);
//            }
//            String qr = QRCodeUtil.decodeByUrl(src);
//            if (StringUtils.isBlank(qr)) {
//                log.error("[新众联支付]-[请求支付]-3.6.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr2) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr2);
//            }
//            result.put(QRCONTEXT, qr);
//        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[新众联支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[新众联支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}