package dc.pay.business.poppayzhifu;

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
import org.springframework.http.MediaType;

import java.util.*;

/**
 * @author Cobby
 * Apr 12, 2019
 */
@RequestPayHandler("POPPAYZHIFU")
public final class PopPayZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(PopPayZhiFuPayRequestHandler.class);

    private static final String method               ="method";// createOrder
    private static final String params               ="params";// 参数列表
    private static final String merchant             ="merchant";// 是 string(32) 商户名
    private static final String orderNo              ="orderNo";// 是 string(32) 商户订单编号，不可重复
    private static final String amount               ="amount";// 是 decimal(11,2) 订单金额，保留两位小数
    private static final String notify               ="notify";// 是 string(255) 后台通知URL，订单完成后会通到此URL
    private static final String payType              ="payType";// 是 string 支付类型 (h5 | qr | we)
//    private static final String extra                ="extra";// 否 json string 附加信息；所有字段均为可选；

    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {

        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merchant, channelWrapper.getAPI_MEMBERID());
                put(orderNo,channelWrapper.getAPI_ORDER_ID());
                put(amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(notify,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(payType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            }
        };
//        Map<String, String> payParam = new TreeMap<String, String>() {
//            {
//                put(method, "createOrder");
//                put(params,JSON.toJSONString(payParamJson));
//            }
//        };
        log.debug("[PopPay支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
//        String paramsJson = api_response_params.get(params);
//        api_response_params = (Map<String, String>) JSON.parse(paramsJson);
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!signature.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        //删除最后一个字符
        signSrc.deleteCharAt(signSrc.length()-1);
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[PopPay支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        Map<String, Object> payParamJson = new HashMap();
        payParamJson.put(method,"createOrder");
        payParamJson.put(params,payParam);
        payParamJson.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);
//        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        try {
                String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(payParamJson),MediaType.APPLICATION_JSON_VALUE);
                resultStr = UnicodeUtil.unicodeToString(resultStr);
                JSONObject jsonObject;
                try {
                    jsonObject = JSONObject.parseObject(resultStr);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("[PopPay支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
                if (jsonObject.containsKey("error") && StringUtils.isNotBlank(jsonObject.getString("error"))
                        && jsonObject.containsKey("result") && StringUtils.isNotBlank(jsonObject.getString("result"))){

                    JSONObject jsonObject1 = JSONObject.parseObject(jsonObject.getString("error"));
                    if (null != jsonObject1 && jsonObject1.containsKey("code") && "200".equalsIgnoreCase(jsonObject1.getString("code"))){

                        JSONObject jsonObject2 = JSONObject.parseObject(jsonObject.getString("result"));
//                        String result_url = jsonObject2.getString("link");
//                        result.put(JUMPURL, result_url);
                        if (HandlerUtil.isWxSM(channelWrapper) || handlerUtil.isWapOrApp(channelWrapper)) {
                            String result_url = jsonObject2.getString("link");
                            result.put(JUMPURL, result_url);
                        }else{
                            String result_url = jsonObject2.getString("img");
                            result.put(QRCONTEXT, result_url);
                        }
                    }else {
                        log.error("[PopPay支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                        throw new PayException(resultStr);
                    }
            }else {
                    log.error("[PopPay支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                  }
        } catch (Exception e) {
            log.error("[PopPay支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[PopPay支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[PopPay支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}