package dc.pay.business.jinfazhifu;

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
 * May 1, 2019
 */
@RequestPayHandler("JINFAZHIFU")
public final class JinFaZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(JinFaZhiFuPayRequestHandler.class);

    private static final String version                ="version";   //    版本号     Y    Y    版本号，固定值是1.0
    private static final String mer_id                 ="mer_id";    //    商户号     Y    Y    商户号，由平台分配
    private static final String order_id               ="order_id";  //    商户订单号  Y    Y    商户系统内部的订单号(32个字符内)可包含字母,确保在商户系统唯一
    private static final String price                  ="price";     //    订单金额    Y    Y    金额（单位:分）
    private static final String notify_url             ="notify_url";//    异步回调地址 Y    Y    用于接收支付结果的通知地址，不可携带任何参数
    private static final String pay_type               ="pay_type";  //    支付类型    Y    Y    支付类型,参考附录
    private static final String randomid               ="randomid";  //    随机字符    Y    Y    随机字符串，不长于32位

    private static final String key        ="key";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(version,"1.0");
                put(mer_id, channelWrapper.getAPI_MEMBERID());
                put(order_id,channelWrapper.getAPI_ORDER_ID());
                put(price,  channelWrapper.getAPI_AMOUNT());
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(pay_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(randomid,HandlerUtil.getRandomStr(10));
            }
        };
        log.debug("[金发支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //version=1.0&mer_id=19202606345&order_id=11&price=10000&notify_url=http://www.b&pay_type=wx&randomid=1555677796&key=密钥
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(version+"=").append(api_response_params.get(version)).append("&");
        signSrc.append(mer_id+"=").append(api_response_params.get(mer_id)).append("&");
        signSrc.append(order_id+"=").append(api_response_params.get(order_id)).append("&");
        signSrc.append(price+"=").append(api_response_params.get(price)).append("&");
        signSrc.append(notify_url+"=").append(api_response_params.get(notify_url)).append("&");
        signSrc.append(pay_type+"=").append(api_response_params.get(pay_type)).append("&");
        signSrc.append(randomid+"=").append(api_response_params.get(randomid)).append("&");
        signSrc.append(key+"=").append(channelWrapper.getAPI_KEY());
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[金发支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        HashMap<String, String> result = Maps.newHashMap();
        try {
//                String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(payParam),MediaType.APPLICATION_JSON_VALUE);
                String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
                resultStr = UnicodeUtil.unicodeToString(resultStr);
                JSONObject jsonObject;
                try {
                    jsonObject = JSONObject.parseObject(resultStr);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("[金发支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
                if (null != jsonObject && jsonObject.containsKey("message") && "success".equalsIgnoreCase(jsonObject.getString("message"))
                        && jsonObject.containsKey("data") && StringUtils.isNotBlank(jsonObject.getString("data"))) {
                    jsonObject = JSONObject.parseObject(jsonObject.getString("data"));

                    String code_url = jsonObject.getString("qrcode_url");
                    if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
                        result.put(JUMPURL, code_url);
                    }else{
                        result.put(QRCONTEXT, code_url);
                    }
//                    result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);

                }else {
                    log.error("[金发支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }

        } catch (Exception e) {
            log.error("[金发支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[金发支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[金发支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}