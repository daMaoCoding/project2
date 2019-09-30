package dc.pay.business.huihuangzhifu2;

import com.alibaba.fastjson.JSON;
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
import java.util.*;

/**
 * @author Cobby
 * Mar 23, 2019
 */
@RequestPayHandler("HUIHUANGZHIFU2")
public final class HuiHuangZhiFu2PayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(HuiHuangZhiFu2PayRequestHandler.class);

    private static final String version       ="version";    //    是    固定写1.0
    private static final String mchId         ="mchId";      //    是    商户号
    private static final String userId        ="userId";     //    是    用户唯一标识
    private static final String orderId       ="orderId";    //    是    订单号
    private static final String applyDate     ="applyDate";  //    是    支付申请时间（年月日 时分秒）
    private static final String channelCode   ="channelCode";//    是    通道code
    private static final String notifyUrl     ="notifyUrl";  //    是    回调通知url
    private static final String returnUrl     ="returnUrl";  //    是    支付成功跳转url
    private static final String totalFee      ="totalFee";   //    是    支付金额
    private static final String body          ="body";       //    是    商品描述
    private static final String signType      ="signType";   //    是    MD5

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(version,  "1.0");
                put(mchId, channelWrapper.getAPI_MEMBERID());
                put(userId,channelWrapper.getAPI_ORDER_ID());
                put(orderId,channelWrapper.getAPI_ORDER_ID());
                put(applyDate,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
                put(channelCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(returnUrl,channelWrapper.getAPI_WEB_URL());
                put(totalFee,  channelWrapper.getAPI_AMOUNT());
                put(body,"body");
                put(signType,"MD5");
//                put(returnUrl,channelWrapper.getAPI_WEB_URL());
            }
        };
        log.debug("[辉煌支付2]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!signature.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        //去除最后一个&符
        //paramsStr = paramsStr.substring(0,paramsStr.length()-1);
        signSrc.append(key +"="+ channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[辉煌支付2]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        HashMap<String, String> result = Maps.newHashMap();
//        Map<String, String> headersMap = new HashMap<>();
//        headersMap.put(mchId,payParam.get(mchId));
//        headersMap.put("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        try {
                  result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
//                String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
//                JSONObject jsonObject;
//                try {
//                    jsonObject = JSONObject.parseObject(resultStr);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    log.error("[辉煌支付2]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                    throw new PayException(resultStr);
//                }
//                if (null != jsonObject && jsonObject.containsKey("success") && "true".equalsIgnoreCase(jsonObject.getString("success"))
//                        && jsonObject.containsKey("data") && StringUtils.isNotBlank(jsonObject.getString("data"))) {
//                    String data = jsonObject.getString("data");
//                    JSONObject jsonObject1 = jsonObject = JSONObject.parseObject(data);
//                    String code_url = jsonObject1.getString("payUrl");
//                    result.put( JUMPURL , code_url);
//
//                }else {
//                    log.error("[辉煌支付2]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                    throw new PayException(resultStr);
//                }

        } catch (Exception e) {
            log.error("[辉煌支付2]-[请求支付]-3.3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[辉煌支付2]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[辉煌支付2]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }

}