package dc.pay.business.mifengzhifu;

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
import org.apache.shiro.codec.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestPayHandler("MIFENGZHIFU")
public final class MiFengZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(MiFengZhiFuPayRequestHandler.class);

     private static final String      merchant_code = "merchant_code";  //	String(10)	√	参数名称：商家号 商户签约时，蜜蜂支付分配给商家的唯一身份标识
     private static final String      merchant_order_no = "merchant_order_no";  //	String(25)	√	参数名称：商户订单号  由商户网站或APP生成的订单号，支付结果返回时会回传该参数
     private static final String      merchant_goods = "merchant_goods";  //	String(200)	√	参数名称：商品名称 发起支付相关的商品名称或商品代码等
     private static final String      merchant_amount = "merchant_amount";  //	String(10)	√	参数名称：支付金额  发起支付累计金额，如10.01，货币单位：人民币
     private static final String      gateway = "gateway";  //	String(20)	√	参数名称：支付网关/通道
     private static final String      urlcall = "urlcall";  //	String(100)	√	参数名称：服务器异步通知地址  支付成功后，蜜蜂支付会主动发送通知给商户，商户必须指定此通知地址
     private static final String      urlback = "urlback";  //	String(100)	√	参数名称：返回网址 支付成功后，通过页面跳转的方式跳转到商家网站
     private static final String      merchant_sign = "merchant_sign";  //	String(200)	×	参数名称：数据加密印鉴 生成规则：base64_encode(md5('merchant_code='.$merchant_code.'&merchant_order_no='.$merchant_order_no.'&merchant_goods='.$merchant_goods.'&merchant_amount='.$merchant_amount.'&merchant_md5='.$merchant_md5))
     private static final String      merchant_md5 = "merchant_md5";  //


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(merchant_code,channelWrapper.getAPI_MEMBERID());
            payParam.put(merchant_order_no,channelWrapper.getAPI_ORDER_ID());
            payParam.put(merchant_goods,channelWrapper.getAPI_ORDER_ID());
            payParam.put(merchant_amount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(gateway,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(urlcall,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(urlback,channelWrapper.getAPI_WEB_URL());
        }
        log.debug("[蜜蜂支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        // base64_encode(md5('merchant_code='.$merchant_code.'&merchant_order_no='.$merchant_order_no.'&merchant_goods='.$merchant_goods.'&merchant_amount='.$merchant_amount.'&merchant_md5='.$merchant_md5))
        String paramsStr = String.format("merchant_code=%s&merchant_order_no=%s&merchant_goods=%s&merchant_amount=%s&merchant_md5=%s",
                params.get(merchant_code),
                params.get(merchant_order_no),
                params.get(merchant_goods),
                params.get(merchant_amount),
                channelWrapper.getAPI_KEY());

        String signMd5 = Base64.encodeToString(HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase().getBytes());
        log.debug("[蜜蜂支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;

    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            //因为第三方不支持获取二维码内容，暂时直接扫码用户在手机中长安识别。
            if (1==1|| HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)  || HandlerUtil.isWapOrApp(channelWrapper)    ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='post'"));
                payResultList.add(result);
            }else{
                //resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
               // HtmlPage htmlPage =handlerUtil.getEndHtml(channelWrapper.getAPI_CHANNEL_BANK_URL(),channelWrapper.getAPI_ORDER_ID(),payParam);
              //  final HtmlImage htmlImage = (HtmlImage) htmlPage.getByXPath("//img[@id='Image']").get(0);
               // String s = htmlPage.asXml();
              //  System.out.println(s);


                JSONObject jsonResultStr = JSON.parseObject("");
          
                    if(null!=jsonResultStr && jsonResultStr.containsKey("code") && "200".equalsIgnoreCase(jsonResultStr.getString("code")) && jsonResultStr.containsKey("codeimg")){
                            if(StringUtils.isNotBlank(jsonResultStr.getString("codeimg"))){
                                result.put(QRCONTEXT, HandlerUtil.UrlDecode(jsonResultStr.getString("codeimg")));
                                payResultList.add(result);
                            }
                    }else {
                        throw new PayException("不会执行到这");
                    }

                 
            }
        } catch (Exception e) { 
             log.error("[蜜蜂支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[蜜蜂支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[蜜蜂支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}