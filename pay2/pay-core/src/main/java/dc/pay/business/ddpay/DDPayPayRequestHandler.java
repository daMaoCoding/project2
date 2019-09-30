package dc.pay.business.ddpay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 
 * @author andrew
 * Sep 24, 2019
 */
@RequestPayHandler("DDPAY")
public final class DDPayPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(DDPayPayRequestHandler.class);

    //1.1.4.请求参数 
    //注意：请求参数中的字段名称不能带空格
    // 字段  名称  格式  说明  必填 
    //1   merId   商户号 字符串 商户在支付平台系统经过注册认证后被分配的唯一商户号   Y
    private static final String merId                ="merId";
    //2.  businessOrderId 商户订单号   字符串     商户发起请求的订单号(最大40 个字节)    Y 
    private static final String businessOrderId                ="businessOrderId";
    //3.  tradeMoney  订单金额    字符串     交易金额(元为单位)  Y 
    private static final String tradeMoney                ="tradeMoney";
    //4.   payType    支付方式        微信支付  1 支付宝支付  2QQ支付  3云闪付        4银行原生（小）     5银行原生（大）    51丰收家                 6农信易扫              61农行E管家          62   Y
    private static final String payType                ="payType";
    //5.   asynURL    异步通知地址      异步通知地址  Y 
    private static final String asynURL                ="asynURL";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {

        String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
        if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 2) {
            log.error("[DDpay]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&通道类型编码值（向第三方获取当前使用通道编码值）" );
            throw new PayException("[DDpay]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&通道类型编码值（向第三方获取当前使用通道编码值）" );
        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merId, channelWrapper.getAPI_MEMBERID().split("&")[0]);
                put(businessOrderId,channelWrapper.getAPI_ORDER_ID());
                put(tradeMoney,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(payType, channelWrapper.getAPI_MEMBERID().split("&")[1]);
                put(asynURL,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            }
        };
        log.debug("[DDpay]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
//        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
//        StringBuilder signSrc = new StringBuilder();
//        for (int i = 0; i < paramKeys.size(); i++) {
//            if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
//                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
//            }
//        }
//        //最后一个&转换成#
//        //signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "#" );
//        //删除最后一个字符
//        //signSrc.deleteCharAt(paramsStr.length()-1);
//        signSrc.append(key + channelWrapper.getAPI_KEY());
//        String paramsStr = signSrc.toString();
//        System.out.println("签名源串=========>"+paramsStr);
//        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        String signMd5 = "本第三方只有回调md5验证签名";
        log.debug("[DDpay]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }
     
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
//        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        HashMap<String, String> result = Maps.newHashMap();
//        System.out.println("请求地址=========>"+channelWrapper.getAPI_CHANNEL_BANK_URL());
//        System.out.println("请求参数=========>"+JSON.toJSONString(payParam));

//        if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
        if (false) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
        }else{
            String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(payParam),MediaType.APPLICATION_JSON_VALUE);
//            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
            //String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
            //if (StringUtils.isBlank(resultStr)) {
            //    log.error("[DDpay]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            //    throw new PayException(resultStr);
            //    //log.error("[DDpay]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
            //    //throw new PayException("返回空,参数："+JSON.toJSONString(map));
            //}
//            System.out.println("请求返回=========>"+resultStr);
            //if (!resultStr.contains("{") || !resultStr.contains("}")) {
            //   log.error("[DDpay]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            //   throw new PayException(resultStr);
            //}
            //JSONObject jsonObject = JSONObject.parseObject(resultStr);
            JSONObject jsonObject;
            try {
                jsonObject = JSONObject.parseObject(resultStr);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("[DDpay]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            //只取正确的值，其他情况抛出异常
            if (null != jsonObject && jsonObject.containsKey("code") && "1000".equalsIgnoreCase(jsonObject.getString("code"))  && 
                    jsonObject.containsKey("info") && StringUtils.isNotBlank(jsonObject.getString("info"))
            ){
                //注意：请求参数payType为5（银行原生5）且为云闪付支付时，必须使用响应数据中的codeurl字段，否则无法支付。
                if ("5".equals(channelWrapper.getAPI_MEMBERID().split("&")[1]) && StringUtils.isNotBlank(jsonObject.getJSONObject("info").getString("codeurl"))) {
                    result.put( QRCONTEXT, jsonObject.getJSONObject("info").getString("codeurl"));
                } else if (StringUtils.isNotBlank(jsonObject.getJSONObject("info").getString("pcodeurl"))) {
                    result.put( JUMPURL, jsonObject.getJSONObject("info").getString("pcodeurl"));
                }else {
                    log.error("[DDpay]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
            }else {
                log.error("[DDpay]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[DDpay]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[DDpay]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}