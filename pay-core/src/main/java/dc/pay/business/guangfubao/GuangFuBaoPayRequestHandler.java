package dc.pay.business.guangfubao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
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
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;
import dc.pay.utils.qr.QRCodeUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Aug 16, 2018
 */
@RequestPayHandler("GUANGFUBAO")
public final class GuangFuBaoPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(GuangFuBaoPayRequestHandler.class);

    //参数名称                 参数含义               是否必填        参与签名        参数说明
    //pay_memberid             商户号                 是               是             平台分配商户号
    //pay_orderid              订单号                 是               是             上送订单号唯一, 字符长度20
    //pay_applydate            提交时间               是               是             时间格式：2016-12-26 18:18:18
    //pay_bankcode             银行编码               是               是             参考后续说明
    //pay_notifyurl            服务端通知             是               是             服务端返回地址.（POST返回数据）
    //pay_callbackurl          页面跳转通知           是               是             页面跳转返回地址（POST返回数据）
    //pay_amount               订单金额               是               是             商品金额
    //pay_attach               附加字段               否               否             此字段在返回时按原样返回 (中文需要url编码)
    //pay_productname          商品名称               是               否        
    //pay_productnum           商户品数量             否               否        
    //pay_productdesc          商品描述               否               否        
    //pay_producturl           商户链接地址           否               否        
    //pay_md5sign              MD5签名                是               否             请看MD5签名字段格式
    private static final String pay_memberid                ="pay_memberid";
    private static final String pay_orderid                 ="pay_orderid";
    private static final String pay_applydate               ="pay_applydate";
    private static final String pay_bankcode                ="pay_bankcode";
    private static final String pay_notifyurl               ="pay_notifyurl";
    private static final String pay_callbackurl             ="pay_callbackurl";
    private static final String pay_amount                  ="pay_amount";
    private static final String pay_attach                  ="pay_attach";
    private static final String pay_productname             ="pay_productname";
//    private static final String pay_productnum              ="pay_productnum";
//    private static final String pay_productdesc             ="pay_productdesc";
//    private static final String pay_producturl              ="pay_producturl";
    private static final String key                         ="key";

    //signature    数据签名    32    是    　
//    private static final String signature  ="pay_md5sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(pay_memberid, channelWrapper.getAPI_MEMBERID());
                put(pay_orderid,channelWrapper.getAPI_ORDER_ID());
                put(pay_applydate,DateUtil.formatDateTimeStrByParam("yyyy-MM-dd HH:mm:ss"));
                put(pay_bankcode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(pay_notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(pay_callbackurl,channelWrapper.getAPI_WEB_URL());
                put(pay_amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(pay_attach,channelWrapper.getAPI_MEMBERID());
                put(pay_productname,"name");
            }
        };
        log.debug("[光付宝]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!pay_productname.equals(paramKeys.get(i)) && !pay_attach.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        signSrc.append(key +"="+ channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[光付宝]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        Map<String, String> param = new TreeMap<String, String>() {
            {
                put(pay_amount, payParam.get(pay_amount));
                put(pay_applydate, payParam.get(pay_applydate));
                put(pay_bankcode, payParam.get(pay_bankcode));
                put(pay_callbackurl, payParam.get(pay_callbackurl));
                put(pay_memberid, payParam.get(pay_memberid));
                put(pay_notifyurl, payParam.get(pay_notifyurl));
                put(pay_orderid, payParam.get(pay_orderid));
                put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
                put(pay_attach, payParam.get(pay_attach));
                put(pay_productname, payParam.get(pay_productname));
            }
        };
        HashMap<String, String> result = Maps.newHashMap();
        if (HandlerUtil.isWapOrApp(channelWrapper)) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),param).toString());
        }else{
            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), param,"UTF-8");
            if (StringUtils.isBlank(resultStr)) {
                log.error("[光付宝]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(param));
                throw new PayException("返回空,参数："+JSON.toJSONString(param));
            }
            if (!resultStr.contains("form")) {
                log.error("[光付宝]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            Document document = Jsoup.parse(resultStr);  //Jsoup.parseBodyFragment(html)
            Element formEl = document.getElementsByTag("form").first();
            if (formEl == null) {
                log.error("[光付宝]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            Map<String, String> secondPayParam = HandlerUtil.parseFormElement(formEl);
            String resultStr2 = RestTemplateUtil.postForm(secondPayParam.get("action"), secondPayParam,"UTF-8");
            Document document2 = Jsoup.parse(resultStr2);  //Jsoup.parseBodyFragment(html)
            Element img = document2.getElementsByTag("img").first();
            if (null == img || !img.hasAttr("src")) {
                log.error("[光付宝]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr2) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr2);
            }
            String jumpToUrl = img.attr("src");
            if (StringUtils.isBlank(jumpToUrl)) {
                log.error("[光付宝]-[请求支付]-3.5.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr2) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr2);
            }
            String qr = QRCodeUtil.decodeByUrl(jumpToUrl);
            if (StringUtils.isBlank(qr)) {
                log.error("[光付宝]-[请求支付]-3.6.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr2) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr2);
            }
            result.put(QRCONTEXT, qr);
        }
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		payResultList.add(result);
        log.debug("[光付宝]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[光付宝]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}