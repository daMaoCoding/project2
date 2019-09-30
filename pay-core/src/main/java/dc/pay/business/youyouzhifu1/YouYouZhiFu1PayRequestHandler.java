package dc.pay.business.youyouzhifu1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

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
import dc.pay.utils.UnicodeUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Feb 18, 2019
 */
@RequestPayHandler("YOUYOUZHIFU1")
public final class YouYouZhiFu1PayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(YouYouZhiFu1PayRequestHandler.class);

    //参数名 是否必传 说明
    //src_code 是 商户唯一标识
    private static final String src_code                ="src_code";
    //out_trade_no 是 接入方交易订单号
    private static final String out_trade_no                ="out_trade_no";
    //total_fee 是 订单总金额，单位分
    private static final String total_fee                ="total_fee";
    //time_start 是    发 起 交 易 的 时 间 ， 时 间 格 式 为 ：    YYYYMMDDHHmmSS，如 20170101120000
    private static final String time_start                ="time_start";
    //goods_name 是 商品名称
    private static final String goods_name                ="goods_name";
    //trade_type 是交易类型，银联二维码：30104；银联 H5：30107；QQ 钱包扫码：40104；QQ 钱包 wap：40107；微信刷卡：50101； 微信 APP：50102；微信公众号：50103； 微信扫码：50104； 微信 wap：50107； 
    private static final String trade_type                ="trade_type";
    //finish_url 是支付完成页面的 url，有效性根据实际通道而定
    private static final String finish_url                ="finish_url";
    //out_mchid 否接入方商户号,注意:out_mchid 与 mchid 两个 参数必须传一个
//    private static final String out_mchid                ="out_mchid";
    //mchid 否平台商户号,注意:out_mchid 与 mchid 两个参 数必须传
    private static final String mchid                ="mchid";
    //openid 否 微信 openid，是否必传，根据通道而定
//    private static final String openid                ="openid";
    //time_expire 否 订单有效期（默认有效期为半个小时）
//    private static final String time_expire                ="time_expire";
    //goods_detail 否 商品详情
//    private static final String goods_detail                ="goods_detail";
    //auth_code 否 授权码（微信刷卡和支付宝反扫必传
//    private static final String auth_code                ="auth_code";
    //sub_appid 否微信开放平台审核通过的移动应用 AppID（微 信 APP 必传）
//    private static final String sub_appid                ="sub_appid";
    //appid 否商户 app 对应的微信开发平台移动应用 APPID（微信 APP 必传）
//    private static final String appid                ="appid";
    //extend 否扩展域，此字段是一个 json 格式，具体参数 如下表 （网关支付必
//    private static final String extend                ="extend";
    //sign 是 签名
//    private static final String sign              ="sign";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="signature";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
        if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 2) {
            log.error("[优优支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号mchid&唯一标识src_code" );
            throw new PayException("[优优支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号mchid&唯一标识src_code" );
        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(src_code, channelWrapper.getAPI_MEMBERID().split("&")[1]);
                put(out_trade_no,channelWrapper.getAPI_ORDER_ID());
                put(total_fee,  channelWrapper.getAPI_AMOUNT());
                put(time_start,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
                put(goods_name,"name");
                put(trade_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(finish_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
//                put(out_mchid, channelWrapper.getAPI_MEMBERID().split("&")[0]);
                put(mchid, channelWrapper.getAPI_MEMBERID().split("&")[0]);
            }
        };
        log.debug("[优优支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
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
        //signSrc.deleteCharAt(paramsStr.length()-1);
        signSrc.append(key +"="+ channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[优优支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

     protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
         payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
         String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
         if (StringUtils.isBlank(resultStr)) {
             log.error("[优优支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + resultStr + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
             throw new PayException(resultStr);
         }
         resultStr = UnicodeUtil.unicodeToString(resultStr);
         JSONObject resJson = null;
         try {
             resJson = JSONObject.parseObject(resultStr);
         } catch (Exception e) {
             e.printStackTrace();
             log.error("[优优支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
             throw new PayException(resultStr);
         }
         HashMap<String, String> result = Maps.newHashMap();
         //只取正确的值，其他情况抛出异常
         if (null != resJson && resJson.containsKey("respcd") && "0000".equalsIgnoreCase(resJson.getString("respcd")) && null != resJson.getJSONObject("data") && StringUtils.isNotBlank(resJson.getJSONObject("data").getString("pay_params"))) {
             result.put(handlerUtil.isWEBWAPAPP_SM(channelWrapper) ?  QRCONTEXT : JUMPURL , resJson.getJSONObject("data").getString("pay_params"));
         }else {
             log.error("[优优支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
             throw new PayException(resultStr);
         }
         ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
         payResultList.add(result);
         log.debug("[优优支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
         return payResultList;
     }
     
//    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
//        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
//        
//        HashMap<String, String> result = Maps.newHashMap();
//        System.out.println("请求地址=========>"+channelWrapper.getAPI_CHANNEL_BANK_URL());
//        System.out.println("请求参数=========>"+JSON.toJSONString(payParam));
////      if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
//        if (false) {
////      if (true) {
//          result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
//          //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
//      }else{
////          String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
////        String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(payParam),MediaType.APPLICATION_JSON_VALUE).trim();
////          String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
////          String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST,defaultHeaders);
////          String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
////          String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.GET);
//        String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(payParam),MediaType.APPLICATION_FORM_URLENCODED_VALUE).trim();
////          String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
//          //if (StringUtils.isBlank(resultStr)) {
//          //    log.error("[优优支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//          //    throw new PayException(resultStr);
//          //    //log.error("[优优支付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
//          //    //throw new PayException("返回空,参数："+JSON.toJSONString(map));
//          //}
//          System.out.println("请求返回=========>"+resultStr);
//          //if (!resultStr.contains("{") || !resultStr.contains("}")) {
//          //   log.error("[优优支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//          //   throw new PayException(resultStr);
//          //}
//          //JSONObject jsonObject = JSONObject.parseObject(resultStr);
//          JSONObject jsonObject;
//          try {
//              jsonObject = JSONObject.parseObject(resultStr);
//          } catch (Exception e) {
//              e.printStackTrace();
//              log.error("[优优支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//              throw new PayException(resultStr);
//          }
//          //只取正确的值，其他情况抛出异常
//          //if (null != jsonObject && jsonObject.containsKey("result_code") && "SUCCESS".equalsIgnoreCase(jsonObject.getString("result_code"))  && 
//          //(jsonObject.containsKey("qrcode") && StringUtils.isNotBlank(jsonObject.getString("qrcode")) || 
//          // jsonObject.containsKey("pay_url") && StringUtils.isNotBlank(jsonObject.getString("pay_url")))
//          //){
//          if (null != jsonObject && jsonObject.containsKey("respcd") && "0000".equalsIgnoreCase(jsonObject.getString("respcd"))  && jsonObject.containsKey("data") && StringUtils.isNotBlank(jsonObject.getString("data"))) {
//              String code_url = jsonObject.getString("codeimg");
//              result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
//              //if (handlerUtil.isWapOrApp(channelWrapper)) {
//              //    result.put(JUMPURL, code_url);
//              //}else{
//              //    result.put(QRCONTEXT, code_url);
//              //}
//          }else {
//              log.error("[优优支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//              throw new PayException(resultStr);
//          }
//      }
//        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
//        payResultList.add(result);
//        log.debug("[优优支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
//        return payResultList;
//    }

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
        log.debug("[优优支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}