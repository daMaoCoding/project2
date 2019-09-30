package dc.pay.business.hongtaizhifu;

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
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Cobby
 * June 17, 2019
 */
@RequestPayHandler("HONGTAIZHIFU")
public final class HongTaiZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(HongTaiZhiFuPayRequestHandler.class);

    private static final String payType    = "payType";//    是    string    支付类型 , ALI_SCAN支付宝扫码
    private static final String umId       = "umId";//    是    int    商户ID
    private static final String umNo       = "umNo";//    是    string    商户订单号
    private static final String amount     = "amount";//    是    int    金额 ,单位分
    private static final String subject    = "subject";//    是    String    支付商品标题
    private static final String body       = "body";//    是    String    支付商品详情
    private static final String notifyUrl  = "notifyUrl";//    是    String    支付回调地址
    private static final String returnUrl  = "returnUrl";//    是    String    支付返回地址
    private static final String errorUrl   = "errorUrl";//    是    String    支付错误地址
    private static final String backParams = "backParams";//    是    String    支付参数附加参数,传什么,将在回调地址中返回


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(payType, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(umId, channelWrapper.getAPI_MEMBERID());
                put(umNo, channelWrapper.getAPI_ORDER_ID());
                put(amount, channelWrapper.getAPI_AMOUNT());
                put(subject, "name");
                put(body, "name");
                put(notifyUrl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(returnUrl, channelWrapper.getAPI_WEB_URL());
                put(errorUrl, channelWrapper.getAPI_WEB_URL());
                put(backParams, channelWrapper.getAPI_ORDER_ID());
            }
        };
        log.debug("[鸿泰支付]-[请求支付]-1.组装请求参数完成：{}", JSON.toJSONString(payParam));
        return payParam;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
        List          paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc   = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5   = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[鸿泰支付]-[请求支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);

        HashMap<String, String> result = Maps.newHashMap();
        try {
            String     resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, "UTF-8");
            JSONObject resJson;
            try {
                resJson = JSONObject.parseObject(resultStr);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("[鸿泰支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            //只取正确的值，其他情况抛出异常
            if (null != resJson && resJson.containsKey("msg") && "OK".equalsIgnoreCase(resJson.getString("msg"))
                    && resJson.containsKey("data") && StringUtils.isNotBlank(resJson.getString("data"))) {
                resJson = JSONObject.parseObject(resJson.getString("data"));
//                    result.put(  JUMPURL , code_url);
                if (StringUtils.isNotBlank(resJson.getString("htmlUrl"))) {
                    String code_url = resJson.getString("htmlUrl");
                    result.put(JUMPURL, code_url);
                } else if (StringUtils.isNotBlank(resJson.getString("html"))) {
                    String code_url = resJson.getString("html");
                    result.put(HTMLCONTEXT, code_url);
                } else {
                    String code_url = resJson.getString("qrCode");
                    result.put(QRCONTEXT, code_url);
                }
            } else {
                log.error("[鸿泰支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }

        } catch (Exception e) {
            log.error("[鸿泰支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null", e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[鸿泰支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));
        return payResultList;
    }

    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (null != resultListMap && !resultListMap.isEmpty()) {
            if (resultListMap.size() == 1) {
                Map<String, String> resultMap = resultListMap.get(0);
                requestPayResult = buildResult(resultMap, channelWrapper, requestPayResult);
            }
            if (ValidateUtil.requestesultValdata(requestPayResult)) {
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            } else {
                throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[鸿泰支付]-[请求支付]-4.处理请求响应成功：{}", JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}