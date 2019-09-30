package dc.pay.business.laobanzhifu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import dc.pay.utils.*;
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

/**
 * @author Cobby
 * Apr 05, 2019
 */
@RequestPayHandler("LAOBANZHIFU")
public final class LaoBanZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(LaoBanZhiFuPayRequestHandler.class);

    private static final String shopAccountId          ="shopAccountId";  //    是  商户ID (商户后台获取)
    private static final String shopUserId             ="shopUserId";     //    是  商户自己平台用户ID,记录作用 没有可为空
    private static final String amountInString         ="amountInString"; //    是  订单⾦金金额，单位元，如:0.01表示⼀一分 钱;
    private static final String payChannel             ="payChannel";     //    是  ⽀支付宝: alipay(AA收款) 支付宝原生(淘宝现金红包):wxpay 微信:wechat 支付宝转银行:bank 支付宝转银行:bank
    private static final String shopNo                 ="shopNo";         //    是  商户订单号，⻓度不超过40;
    private static final String shopCallbackUrl        ="shopCallbackUrl";//    是  订单支付成功回调地址(具体参数详见接口2，如果为空平台会调用商家在WEB端设置的订单回调地址;否则，平台会调用该地址，WEB端设置的地址不会被调用);
    private static final String returnUrl              ="returnUrl";      //    是  ⼆维码扫码支付模式下:支付成功⻚页面‘返回商家端’按钮点击后的跳转地址; 如果商家采用自有界⾯面，则忽略该参数;
    private static final String target                 ="target";         //    是  跳转方式 1，手机跳转(PC和H5自适应) 3、返回json


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(shopAccountId, channelWrapper.getAPI_MEMBERID());
                put(shopUserId,channelWrapper.getAPI_ORDER_ID());
                put(amountInString,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(payChannel,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(shopNo,channelWrapper.getAPI_ORDER_ID());
                put(shopCallbackUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(returnUrl,channelWrapper.getAPI_WEB_URL());
                put(target,"3");
            }
        };
        log.debug("[老板支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //1.MD5(shopAccountId + shopUserId +amountInString + shopNo + payChannel + KEY);
        //2.字符串相加再计算MD5⼀一次，MD5为32位小写; shopAccountId 和KEY登陆商家后台可以查看;
        String paramsStr = String.format("%s%s%s%s%s%s",
                api_response_params.get(shopAccountId),
                api_response_params.get(shopUserId),
                api_response_params.get(amountInString),
                api_response_params.get(shopNo),
                api_response_params.get(payChannel),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[老板支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        HashMap<String, String> result = Maps.newHashMap();
        try {
                String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
                resultStr =UnicodeUtil.unicodeToString(resultStr);
                JSONObject jsonObject;
                try {
                    jsonObject = JSONObject.parseObject(resultStr);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("[老板支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
                if (null != jsonObject && jsonObject.containsKey("code") && "0".equalsIgnoreCase(jsonObject.getString("code"))
                        && jsonObject.containsKey("url") && StringUtils.isNotBlank(jsonObject.getString("url"))) {
                    String code_url = jsonObject.getString("url");
                    result.put(  QRCONTEXT, code_url);
//                    if (handlerUtil.isWapOrApp(channelWrapper)) {
//                        result.put(JUMPURL, code_url);
//                    }else{
//                        result.put(QRCONTEXT, code_url);
//                    }
                }else {
                    log.error("[老板支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
        } catch (Exception e) {
            log.error("[老板支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[老板支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[老板支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}