package dc.pay.business.zhinengyun;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestPayHandler("ZHINENGYUN")
public final class ZhiNengYunPayRequestHandler extends PayRequestHandler {
     private static final Logger log = LoggerFactory.getLogger(ZhiNengYunPayRequestHandler.class);


     private static final String     uid = "uid";  // post true string 20 商户号
     private static final String     orderid = "orderid";  // post true string 32 商户订单号，唯一
     private static final String     istype = "istype";  // post true string 10 付款方式编号10001支付宝20001微信
     private static final String     price = "price";  // post true float 20 订单金额，单位元，小数位最末位不能是0；
     private static final String     goodsname = "goodsname";  // post true string 100 订单名称（描述）
     private static final String     orderuid = "orderuid";  // post true string 20 商品详情
     private static final String     notify_url = "notify_url";  // post true string 200 异步通知地址
     private static final String     return_url = "return_url";  // post true string 200 支付结果跳转地址
     private static final String     key = "key";  // post true string 32 签名
     private static final String     token = "token";  // post true string 32 签名


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        if(!channelWrapper.getAPI_MEMBERID().contains("&")){
            throw new PayException("商户号格式错误，正确格式请使用&符号链接商户账户和token,如：商户号&token");
        }

        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(uid,channelWrapper.getAPI_MEMBERID().split("&")[0]);
            payParam.put(orderid,channelWrapper.getAPI_ORDER_ID());
            payParam.put(istype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(price,HandlerUtil.getYuanWithoutZero(channelWrapper.getAPI_AMOUNT()));
            payParam.put(goodsname,channelWrapper.getAPI_ORDER_ID());
            payParam.put(orderuid,channelWrapper.getAPI_ORDER_ID());
            payParam.put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(return_url,channelWrapper.getAPI_WEB_URL());
        }
        log.debug("[智能云支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        //goodsname+istype+notify_url+orderid+orderuid+price+return_url+token+uid

        String paramsStr = String.format("%s%s%s%s%s%s%s%s%s",
                params.get(goodsname),
                params.get(istype),
                params.get(notify_url),
                params.get(orderid),
                params.get(orderuid),
                params.get(price),
                params.get(return_url),
                channelWrapper.getAPI_MEMBERID().split("&")[1],
                params.get(uid));
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[智能云支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (1==2 && HandlerUtil.isWY(channelWrapper) &&  HandlerUtil.isYLKJ(channelWrapper) && HandlerUtil.isWapOrApp(channelWrapper)    ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='post'"));
                payResultList.add(result);
            }else{
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
				if(StringUtils.isNotBlank(resultStr) && resultStr.contains("<form") && !resultStr.contains("{")){
                    result.put(HTMLCONTEXT,resultStr);
                    payResultList.add(result);
                }else{
                   JSONObject jsonResultStr = JSON.parseObject(resultStr);
                    if(null!=jsonResultStr && jsonResultStr.containsKey("code") && "200".equalsIgnoreCase(jsonResultStr.getString("code")) && jsonResultStr.containsKey("data") && null!=jsonResultStr.getJSONObject("data")){
                        JSONObject data = jsonResultStr.getJSONObject("data");
                        if(data.containsKey("qrcode") &&  StringUtils.isNotBlank(data.getString("qrcode"))){
                            if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)  || HandlerUtil.isWapOrApp(channelWrapper)    ){
                                result.put(JUMPURL,  data.getString("qrcode"));
                            }else{
                                result.put(QRCONTEXT, data.getString("qrcode"));
                            }
                                payResultList.add(result);
                            }
                    }else {
                        throw new PayException( UnicodeUtil.unicodeToString(resultStr));
                    }
				}
                 
            }
        } catch (Exception e) { 
             log.error("[智能云支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[智能云支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[智能云支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}