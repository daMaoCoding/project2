package dc.pay.business.baomi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
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
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.UnicodeUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Dec 3, 2018
 */
@RequestPayHandler("BAOMI")
public final class BaoMiPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(BaoMiPayRequestHandler.class);

    //参数名             参数                  可空          加入签名          说明
    //商户ID             partner                N               Y              商户id,由分配
    //支付类型           banktype               N               Y              填写： ALIPAY
    //金额               paymoney               N               Y              单位元（人民币）
    //商户订单号         ordernumber            N               Y              商户系统订单号，该订单号将作为接口的返回数据。该值需在商户系统内唯一
    //订单标题           subject                N               Y              订单标题，下行中会原样返回。如果没有商品名称 会替代
    //异步地址           callbackurl            N               Y              下行异步通知的地址，需要以http://开头且没有任何参数
    //指定通道           chname                 N               Y              没有填写：‘false’  有按具体情况填写
    //MD5签名            sign                   N               N              32位小写MD5签名值，utf8编码
    private static final String partner                 ="partner";
    private static final String banktype                ="banktype";
    private static final String paymoney                ="paymoney";
    private static final String ordernumber             ="ordernumber";
    private static final String subject                 ="subject";
    private static final String callbackurl             ="callbackurl";
    private static final String chname                  ="chname";
//    private static final String sign                    ="sign";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(partner, channelWrapper.getAPI_MEMBERID());
                put(banktype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(paymoney,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(ordernumber,channelWrapper.getAPI_ORDER_ID());
                put(subject, channelWrapper.getAPI_MEMBERID());
                put(callbackurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(chname, "false");
            }
        };
        log.debug("[苞米]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        
        TreeMap<String, String> map = new TreeMap<>(api_response_params);
        map.put(key, channelWrapper.getAPI_KEY());
        List paramKeys = MapUtils.sortMapByKeyAsc(map);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (StringUtils.isNotBlank(map.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(map.get(paramKeys.get(i))).append("&");
            }
        }
        //最后一个&转换成#
        //signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "#" );
        //删除最后一个字符
        signSrc.deleteCharAt(signSrc.length()-1);
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[苞米]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
//      String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
        String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
        if (StringUtils.isBlank(resultStr)) {
            log.error("[苞米]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        resultStr = UnicodeUtil.unicodeToString(resultStr);
        if (!resultStr.contains("{") || !resultStr.contains("}")) {
            log.error("[苞米]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        //JSONObject resJson = JSONObject.parseObject(resultStr);
        JSONObject resJson;
        try {
            resJson = JSONObject.parseObject(resultStr);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[苞米]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        //只取正确的值，其他情况抛出异常
        if (null != resJson && resJson.containsKey("status") && "10000".equalsIgnoreCase(resJson.getString("status"))  && resJson.containsKey("data") && StringUtils.isNotBlank(resJson.getString("data"))) {
            JSONObject resJson2 = resJson.getJSONObject("data");
            String code_url = resJson2.getString("url");
            if (handlerUtil.isWapOrApp(channelWrapper)) {
                result.put(JUMPURL, code_url);
            }else {
                result.put(QRCONTEXT, code_url);
            }
        }else {
            log.error("[苞米]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[苞米]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[苞米]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}