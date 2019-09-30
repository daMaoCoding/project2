package dc.pay.business.jinniuzhifu;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

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
import dc.pay.utils.HmacSha256Util;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * @author XXXXXXX
 * June 1, 2019
 */
@RequestPayHandler("JINZUANZHIFU")
public final class JinZuanZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(JinZuanZhiFuPayRequestHandler.class);

    //公共基础参数（放入请求 url）
    //merchantId 接入商户标识 String 否 商户在金钻支付上的接入商户标识
    private static final String merchantId                ="merchantId";
    //timestamp 请求时间 String 否 格式：unix_timestamp，精确到毫秒
    private static final String timestamp                ="timestamp";
    //signatureMethod 签名类型 String 否 签名类型，目前支持 HmacSHA256
    private static final String signatureMethod                ="signatureMethod";
    //signatureVersion 签名算法版本 Int 否 签名算法版本，目前支持 1
    private static final String signatureVersion                ="signatureVersion";
//    //signature 签名 String 否 签名信息，算法参见第 2.3.1 节
//    private static final String signature                ="signature";

    //支付业务参数（放入请求 body）
    //jUserId 订单用户 String 否 接入商户下单的客户 Id jUserIp 客户端 IP String 否 不是商户请求的服务器 IP,指的是商户的客户 IPv4 地址
    private static final String jUserId                ="jUserId";
    //jOrderId 订单 ID String 否 由商户内部生成的唯一订单编号, 最长不超过 32 字符只能由数字或字母组成
    private static final String jOrderId                ="jOrderId";
    //orderType 订单类型 Int 否 订单类型，目前只支持充值，取值如下1 为充值订单
    private static final String orderType                ="orderType";
    //payWay 支付方式 String 否 见附录一“支付方式”
    private static final String payWay                ="payWay";
    //amount 订单金额 Double 否 单位：元，两位小数,不能小于 1.0，金额上限和下限根据实际情况制定
    private static final String amount                ="amount";
    //currency 支付货币类型 String 否 客户打款币种，目前只支持人民币：CNY
    private static final String currency                ="currency";
    //jExtra 扩展字段 String 可空    附加数据，在查询和支付通知中原样返回，该字段主要用于商户携带订单的自定义数据    原样返回
    private static final String jExtra                ="jExtra";
    //notifyUrl 后台通知回调URL   String 否 需要带上 http://或 https://
    private static final String notifyUrl                ="notifyUrl";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="signature";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
//        String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
//        if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 2) {
//            log.error("[金钻支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&机构号" );
//            throw new PayException("[金钻支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&机构号" );
//        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
//                //jUserId 订单用户 String 否 接入商户下单的客户 Id jUserIp 客户端 IP String 否 不是商户请求的服务器 IP,指的是商户的客户 IPv4 地址
//                private static final String                 ="jUserId";
//                //jOrderId 订单 ID String 否 由商户内部生成的唯一订单编号, 最长不超过 32 字符只能由数字或字母组成
//                private static final String                 ="jOrderId";
//                //orderType 订单类型 Int 否 订单类型，目前只支持充值，取值如下1 为充值订单
//                private static final String                 ="orderType";
//                //payWay 支付方式 String 否 见附录一“支付方式”
//                private static final String                 ="payWay";
//                //amount 订单金额 Double 否 单位：元，两位小数,不能小于 1.0，金额上限和下限根据实际情况制定
//                private static final String                 ="amount";
//                //currency 支付货币类型 String 否 客户打款币种，目前只支持人民币：CNY
//                private static final String currency                ="currency";
//                //jExtra 扩展字段 String 可空    附加数据，在查询和支付通知中原样返回，该字段主要用于商户携带订单的自定义数据    原样返回
//                private static final String                 ="jExtra";
//                //notifyUrl 后台通知回调URL   String 否 需要带上 http://或 https://
//                private static final String                 ="notifyUrl";
                put(jUserId,  HandlerUtil.getRandomNumber(8));
                put(jOrderId,channelWrapper.getAPI_ORDER_ID());
                put(orderType,"1");
                put(payWay,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(currency,"CNY");
                put(jExtra, channelWrapper.getAPI_MEMBERID());
                put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            }
        };
        log.debug("[金钻支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        //最后一个&转换成#
        //signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "#" );
        //删除最后一个字符
        signSrc.deleteCharAt(signSrc.length()-1);
//        signSrc.append(key + channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        System.out.println("签名源串=========>"+paramsStr);
//        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        String signMd5 = HmacSha256Util.digest(paramsStr, channelWrapper.getAPI_KEY());
        log.debug("[金钻支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);

        Map<String,String> map = new LinkedHashMap<>();
        map.put(merchantId, channelWrapper.getAPI_MEMBERID());
        map.put(timestamp, System.currentTimeMillis()+"");
        map.put(signatureMethod, "HmacSHA256");
        map.put(signatureVersion, "1");
        map.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);

        HashMap<String, String> result = Maps.newHashMap();
        System.out.println("请求地址=========>"+channelWrapper.getAPI_CHANNEL_BANK_URL());
        System.out.println("请求参数=========>"+JSON.toJSONString(map));

//        if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
        if (false) {
//            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),map).toString());
            try {
//                result.put(JUMPURL, HandlerUtil.getUrlWithEncode(channelWrapper.getAPI_CHANNEL_BANK_URL(),map,"utf-8").toString());
                result.put(JUMPURL, HandlerUtil.getHtmlUrl(channelWrapper.getAPI_CHANNEL_BANK_URL(), map));
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),map).toString().replace("method='post'","method='get'"));
        }else{
//            System.setProperty("https.protocols", "TLSv1.2,TLSv1.1,SSLv3");
//            String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(map),MediaType.APPLICATION_JSON_VALUE);
//            String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(map),MediaType.APPLICATION_FORM_URLENCODED_VALUE);
//            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), map, String.class, HttpMethod.POST);
//            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), map, String.class, HttpMethod.POST,defaultHeaders);

//            String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), map, String.class, HttpMethod.POST);
            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), map,"UTF-8");
            //if (StringUtils.isBlank(resultStr)) {
            //    log.error("[金钻支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            //    throw new PayException(resultStr);
            //    //log.error("[金钻支付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
            //    //throw new PayException("返回空,参数："+JSON.toJSONString(map));
            //}
            System.out.println("请求返回=========>"+resultStr);
            //if (!resultStr.contains("{") || !resultStr.contains("}")) {
            //   log.error("[金钻支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            //   throw new PayException(resultStr);
            //}
            //JSONObject jsonObject = JSONObject.parseObject(resultStr);
            JSONObject jsonObject;
            try {
                jsonObject = JSONObject.parseObject(resultStr);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("[金钻支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            //只取正确的值，其他情况抛出异常
            //if (null != jsonObject && jsonObject.containsKey("result_code") && "SUCCESS".equalsIgnoreCase(jsonObject.getString("result_code"))  && 
            //(jsonObject.containsKey("qrcode") && StringUtils.isNotBlank(jsonObject.getString("qrcode")) || 
            // jsonObject.containsKey("pay_url") && StringUtils.isNotBlank(jsonObject.getString("pay_url")))
            //){
            if (null != jsonObject && jsonObject.containsKey("code") && "0".equalsIgnoreCase(jsonObject.getString("code"))  && jsonObject.containsKey("data") && StringUtils.isNotBlank(jsonObject.getString("data"))) {
                String code_url = jsonObject.getString("data");
                result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
                //if (handlerUtil.isWapOrApp(channelWrapper)) {
                //    result.put(JUMPURL, code_url);
                //}else{
                //    result.put(QRCONTEXT, code_url);
                //}
            }else {
                log.error("[金钻支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[金钻支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[金钻支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}