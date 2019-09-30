package dc.pay.business.xinjuhezhifu3;

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
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 
 * @author andrew
 * Aug 13, 2019
 */
@RequestPayHandler("XINJUHEZHIFU3")
public final class XinJuHeZhiFu3PayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XinJuHeZhiFu3PayRequestHandler.class);

    private static final String params                ="params";
    
    //字段名 变量名 必填 类型 示例值 描述
    //商户 ID mchId 是 long 20001222 分配的商户号
    private static final String mchId                ="mchId";
    //应用 ID appId 是 Strin g(32) 0ae8be35ff634e2 abe94f5f32f6d5c4 f 该商户创建的应用 对应的 ID 
    private static final String appId                ="appId";
    //支付产 品 ID productId 否 int 8000 支付产品 ID 
    private static final String productId                ="productId";
    //商户订 单号 mchOrderNo 是 Strin g(30) 201604272106040 00490 商户生成的订单号
    private static final String mchOrderNo                ="mchOrderNo";
    //币种 currency 是 Strin g(3) cny 三位货币代码,人民 币:cny 
    private static final String currency                ="currency";
    //支付金 额 amount 是 int 100 支付金额,单位分
    private static final String amount                ="amount";
    //客户端 IP clientIp 否 Strin g(32) 210.73.10.148 客户端 IP 地址
    private static final String clientIp                ="clientIp";
    //设备 device 否 Strin g(64) ios10.3.1 客户端设备 
//    private static final String device                ="device";
    //支付结 果前端 跳转URL returnUrl 否 Strin g(12 8) http://domain/ret urn.htm 支付结果回调 URL 
    private static final String returnUrl                ="returnUrl";
    //支付结 果后台 回调URL notifyUrl 是 Strin g(12 8) http://domain/not ify.htm 支付结果回调 URL 
    private static final String notifyUrl                ="notifyUrl";
    //商品主 题 subject 是 String(64) 测试商品名称 
    private static final String subject                ="subject";
    //商品主题 商品描 述信息 body 是 Strin g(25 6) 测试商品描述 商品描述信息
    private static final String body                ="body";
    //扩展参 数 1 param1 否 Strin g(64) 支付中心回调时会 原样返回
//    private static final String param1                ="param1";
    //扩展参 数 2 param2 否 Strin g(64) 支付中心回调时会 原样返回 
//    private static final String param2                ="param2";
    //附加参 数 extra 是 Strin g(51 2) {“openId”:”o2 RvowBf7sOVJf8kJ ksUEMceaDqo”} 特定渠道发起时额 外参数,见下面的支 付产品说明 
//    private static final String extra                ="extra";
    //签名 sign 是 Strin g(32) C380BEC2BFD727 A4B6845133519F 3AD6 签名值，详见签名 算法
//    private static final String sign                ="sign";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
        if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 3) {
            log.error("[新聚合支付3]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号mchId&应用appId&通道类型编码值（向第三方获取当前使用通道编码值）" );
            throw new PayException("[新聚合支付3]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号mchId&应用appId&通道类型编码值（向第三方获取当前使用通道编码值）" );
        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(mchId, channelWrapper.getAPI_MEMBERID().split("&")[0]);
                put(appId, channelWrapper.getAPI_MEMBERID().split("&")[1]);
//                put(merchno,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(productId,channelWrapper.getAPI_MEMBERID().split("&")[2]);
                put(mchOrderNo,channelWrapper.getAPI_ORDER_ID());
                put(currency,"cny");
                put(amount,  channelWrapper.getAPI_AMOUNT());
                put(clientIp,  channelWrapper.getAPI_Client_IP());
                put(returnUrl,channelWrapper.getAPI_WEB_URL());
                put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(subject,"1");
                put(body,"2");
                
            }
        };
        log.debug("[新聚合支付3]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
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
        log.debug("[新聚合支付3]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        Map<String, String> my_params = new TreeMap<String, String>() {
            {
                put(params, JSON.toJSONString(payParam));
                
            }
        };
        HashMap<String, String> result = Maps.newHashMap();

//        if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
        if (false) {
//            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),my_params).toString().replace("method='post'","method='get'"));
        }else{
//            String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), params+"="+JSON.toJSONString(payParam),MediaType.APPLICATION_JSON_VALUE);
//            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), my_params,"UTF-8");
            //if (StringUtils.isBlank(resultStr)) {
            //    log.error("[新聚合支付3]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            //    throw new PayException(resultStr);
            //    //log.error("[新聚合支付3]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
            //    //throw new PayException("返回空,参数："+JSON.toJSONString(map));
            //}
//            System.out.println("请求返回=========>"+resultStr);
            //if (!resultStr.contains("{") || !resultStr.contains("}")) {
            //   log.error("[新聚合支付3]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            //   throw new PayException(resultStr);
            //}
            //JSONObject jsonObject = JSONObject.parseObject(resultStr);
            JSONObject jsonObject;
            try {
                jsonObject = JSONObject.parseObject(resultStr);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("[新聚合支付3]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            //只取正确的值，其他情况抛出异常
            if (null != jsonObject && jsonObject.containsKey("retCode") && "SUCCESS".equalsIgnoreCase(jsonObject.getString("retCode"))  && 
                    jsonObject.containsKey("payParams") && StringUtils.isNotBlank(jsonObject.getString("payParams")) &&
                    jsonObject.getJSONObject("payParams").containsKey("payUrl") && StringUtils.isNotBlank(jsonObject.getJSONObject("payParams").getString("payUrl"))
            ){
//            if (null != jsonObject && jsonObject.containsKey("retCode") && "SUCCESS".equalsIgnoreCase(jsonObject.getString("retCode"))  && jsonObject.containsKey("codeimg") && StringUtils.isNotBlank(jsonObject.getString("codeimg"))) {
                String code_url = jsonObject.getJSONObject("payParams").getString("payUrl");
                result.put( JUMPURL, code_url);
                //if (handlerUtil.isWapOrApp(channelWrapper)) {
                //    result.put(JUMPURL, code_url);
                //}else{
                //    result.put(QRCONTEXT, code_url);
                //}
            }else {
                log.error("[新聚合支付3]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
        }   
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[新聚合支付3]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[新聚合支付3]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}