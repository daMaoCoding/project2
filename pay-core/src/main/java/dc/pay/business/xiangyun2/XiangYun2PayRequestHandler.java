package dc.pay.business.xiangyun2;

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
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Mar 19, 2019
 */
@RequestPayHandler("XIANGYUN2")
public final class XiangYun2PayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XiangYun2PayRequestHandler.class);
    
    //公共请求参数和返回结果：
    //请求参数
    //字段名 变量名 必填  类型  示例值 描述
    //商户ID    mchId   是   long    20001222    分配的商户号
    private static final String mchId            ="mchId";
    //应用ID    appId   是   String(32)  0ae8be35ff634e2abe94f5f32f6d5c4f    该商户创建的应用对应的ID
    private static final String appId            ="appId";
    //支付产品ID  productId   否   int 8000    支付产品ID
    private static final String productId            ="productId";
    //商户订单号   mchOrderNo  是   String(30)  20160427210604000490    商户生成的订单号
    private static final String mchOrderNo            ="mchOrderNo";
    //币种  currency    是   String(3)   cny 三位货币代码,人民币:cny
    private static final String currency            ="currency";
    //支付金额    amount  是   int 100 支付金额,单位分
    private static final String amount            ="amount";
    //客户端IP   clientIp    否   String(32)  210.73.10.148   客户端IP地址
    private static final String clientIp            ="clientIp";
    //设备  device  否   String(64)  ios10.3.1   客户端设备
//    private static final String device            ="device";
    //支付结果前端跳转URL returnUrl   否   String(128) http://182.61.170.157/return.htm    支付结果回调URL
    private static final String returnUrl            ="returnUrl";
    //支付结果后台回调URL notifyUrl   是   String(128) http://182.61.170.157/notify.htm    支付结果回调URL
    private static final String notifyUrl            ="notifyUrl";
    //商品主题    subject 是   String(64)  测试商品名称  商品主题
    private static final String subject            ="subject";
    //商品描述信息  body    是   String(256) 测试商品描述  商品描述信息
    private static final String body            ="body";
    //扩展参数1   param1  否   String(64)      支付中心回调时会原样返回
//    private static final String param1            ="param1";
    //扩展参数2   param2  否   String(64)      支付中心回调时会原样返回
//    private static final String param2            ="param2";
    //附加参数    extra   否   String(512) {“openId”:”o2RvowBf7sOVJf8kJksUEMceaDqo”}   特定渠道发起时额外参数,见下面的支付产品说明
//    private static final String extra            ="extra";
    //签名  sign    是   String(32)  C380BEC2BFD727A4B6845133519F3AD6    签名值，详见签名算法
//    private static final String sign            ="sign";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
        if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 2) {
            log.error("[祥云2]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户ID&应用ID" );
            throw new PayException("[祥云2]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户ID&应用ID" );
        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(mchId, channelWrapper.getAPI_MEMBERID().split("&")[0]);
                put(appId, channelWrapper.getAPI_MEMBERID().split("&")[1]);
                put(productId,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(mchOrderNo,channelWrapper.getAPI_ORDER_ID());
                put(currency,"cny");
                put(amount,  channelWrapper.getAPI_AMOUNT());
                put(clientIp,  channelWrapper.getAPI_Client_IP());
                put(returnUrl,channelWrapper.getAPI_WEB_URL());
                put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(subject,"name");
                put(body,"name");
            }
        };
        log.debug("[祥云2]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
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
         signSrc.append(key+"=").append(channelWrapper.getAPI_KEY());
         //删除最后一个字符
         //signSrc.deleteCharAt(paramsStr.length()-1);
         String paramsStr = signSrc.toString();
         String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
//         String signMd5 = Sha1Util.getSha1(paramsStr).toLowerCase();
         log.debug("[祥云2]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
         return signMd5;
     }

    @Override
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        Map<String, String> param = new TreeMap<String, String>() {
            {
                put("params",JSON.toJSONString(payParam));
            }
        };
        HashMap<String, String> result = Maps.newHashMap();
        String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), param,"UTF-8");
//        String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
//        String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST,defaultHeaders);
        if (StringUtils.isBlank(resultStr)) {
            log.error("[祥云2]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        if (!resultStr.contains("{") || !resultStr.contains("}")) {
           log.error("[祥云2]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
           throw new PayException(resultStr);
        }
        //JSONObject resJson = JSONObject.parseObject(resultStr);
        JSONObject resJson;
        try {
            resJson = JSONObject.parseObject(resultStr);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[祥云2]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        //只取正确的值，其他情况抛出异常
        if (null != resJson && resJson.containsKey("retCode") && "SUCCESS".equalsIgnoreCase(resJson.getString("retCode"))  && resJson.containsKey("payParams") && StringUtils.isNotBlank(resJson.getString("payParams")) && 
                (resJson.getJSONObject("payParams").containsKey("payUrl") && StringUtils.isNotBlank(resJson.getJSONObject("payParams").getString("payUrl")))){
//        if (null != resJson && resJson.containsKey("code") && "0000".equalsIgnoreCase(resJson.getString("code"))  && resJson.containsKey("result") && StringUtils.isNotBlank(resJson.getString("result"))) {
            String code_url = resJson.getJSONObject("payParams").getString("payUrl");
            if (handlerUtil.isWapOrApp(channelWrapper)) {
                result.put(JUMPURL, code_url);
            }else {
                result.put(QRCONTEXT, code_url);
            }
        }else {
            log.error("[祥云2]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[祥云2]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[祥云2]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}