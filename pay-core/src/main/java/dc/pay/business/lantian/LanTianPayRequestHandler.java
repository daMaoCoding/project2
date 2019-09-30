package dc.pay.business.lantian;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
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
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Nov 22, 2018
 */
@RequestPayHandler("LANTIAN")
public final class LanTianPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(LanTianPayRequestHandler.class);

    //参数              必填          类型                 含义                   说明
    //id                 是           String(32)           商户号                 您的商户唯一标识，注册后跟上级获取。 
    //data_type          否           String(32)           支付模式               使用接口模式可不传，使用收银台模式必传，收银台模式值为redirect 
    //money              是           float                价格                   单位：元。精确小数点后2位 
    //name               否           string(50)           商品名称               非必填。
    //notify_url         是           string(50)           通知回调网址           用户支付成功后，我们服务器会主动发送一个post消息到这个网址。由您自定义。不要urlencode。例：http://www .aaa.com/qpay_notify
    //return_url         否           string(50)           跳转返回网址           非必填
    //out_trade_no       是           string(50)           商户自定义订单号       我们会据此判别是同一笔订单还是新订单。我们回调时，会带上这个参数，用于告知业务平台数据属于哪一笔订单。例：201710192541
    //user_name          是           string(50)           支付用户标识           我们会据此判别是否是同一个用户在进行支付，同一用户支付会根据以往的记录进行调度，以此增加成功率， 请务必提供能够识别业务平台中用户唯一标识的字段 ，如无法提供，请提供用户请求ip作为标识
    //sitename           否           string(50)           站点名称               非必填
    //type               是           string(50)           支付方式               获取的码类型：alipay:支付宝,wechat:微信。后续可扩展
    //sign_type          是           string(50)           加密方式               默认使用MD5，后续可扩展
    //sign               是           string(50)           签名字符串             不参与加密签名，为加密后的结果值，具体规则请参照 签名算法 
    private static final String id                       ="id";
    private static final String data_type                ="data_type";
    private static final String money                    ="money";
//    private static final String name                     ="name";
    private static final String notify_url               ="notify_url";
//    private static final String return_url               ="return_url";
    private static final String out_trade_no             ="out_trade_no";
    private static final String user_name                ="user_name";
//    private static final String sitename                 ="sitename";
    private static final String type                     ="type";
    private static final String sign_type                ="sign_type";
//    private static final String sign                     ="sign";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(id, channelWrapper.getAPI_MEMBERID());
                put(data_type,"redirect");
                put(money,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(out_trade_no,channelWrapper.getAPI_ORDER_ID());
                put(user_name,channelWrapper.getAPI_Client_IP());
                put(type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(sign_type,"MD5");
            }
        };
        log.debug("[蓝天]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
       //这里将map.entrySet()转换成list
         List<Map.Entry<String,String>> list = new ArrayList<Map.Entry<String,String>>(api_response_params.entrySet());
         //然后通过比较器来实现排序
         Collections.sort(list,new Comparator<Map.Entry<String,String>>() {
             //升序排序
             public int compare(Entry<String, String> o1,
                     Entry<String, String> o2) {
                 return o1.getValue().compareTo(o2.getValue());
             }
         });
         StringBuilder signSrc = new StringBuilder();
         for(Map.Entry<String,String> mapping:list){ 
             if (!sign_type.equals(mapping.getKey()) && StringUtils.isNotBlank(api_response_params.get(mapping.getKey()))) {
//                 signSrc.append(mapping.getKey()).append("=").append(mapping.getValue()).append("&");
                 signSrc.append(mapping.getValue());
             }
         }
        //删除最后一个字符
//        signSrc.deleteCharAt(signSrc.length()-1);
//        signSrc.append(key + channelWrapper.getAPI_KEY());
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[蓝天]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
        }
//        else{
////            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
//            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
//            if (StringUtils.isBlank(resultStr)) {
//                log.error("[蓝天]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//                //log.error("[蓝天]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
//                //throw new PayException("返回空,参数："+JSON.toJSONString(map));
//            }
//            if (!resultStr.contains("{") || !resultStr.contains("}")) {
//               log.error("[蓝天]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//               throw new PayException(resultStr);
//            }
//            //JSONObject resJson = JSONObject.parseObject(resultStr);
//            JSONObject resJson;
//            try {
//                resJson = JSONObject.parseObject(resultStr);
//            } catch (Exception e) {
//                e.printStackTrace();
//                log.error("[蓝天]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//            }
//            //只取正确的值，其他情况抛出异常
//            if (null != resJson && resJson.containsKey("code") && "1".equalsIgnoreCase(resJson.getString("code"))  && resJson.containsKey("data") && StringUtils.isNotBlank(resJson.getString("data"))) {
//                JSONObject resJson2 = resJson.getJSONObject("data");
////                result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, resJson2.getString("pay_url"));
//                if (handlerUtil.isWapOrApp(channelWrapper)) {
//                    result.put(JUMPURL, resJson2.getString("pay_url"));
//                }else{
//                    result.put(QRCONTEXT, resJson2.getString("qrcode_url"));
//                }
//            }else {
//                log.error("[蓝天]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//            }
//        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[蓝天]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[蓝天]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}