package dc.pay.business.zhongnuozhifu;

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
 * @author Cobby
 * May 06, 2019
 */
@RequestPayHandler("ZHONGNUOZHIFU")
public final class ZhongNuoZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(ZhongNuoZhiFuPayRequestHandler.class);

    private static final String merchantNo         ="merchantNo";  //    String(50)    必填    商户编号
    private static final String orderAmount        ="orderAmount"; //    String(50)    必填    商户订单金额，单位分格式：10000=100元
    private static final String orderNo            ="orderNo";     //    String(32)    必填    商户订单号
    private static final String notifyUrl          ="notifyUrl";   //    String(255)    必填    异步通知：服务器端处理通知接口格式：如：http://biz.domain.com/noti
    private static final String callbackUrl        ="callbackUrl"; //    String(255)    必填    页面回调：支付成功后会向该地址发送通知，该地址可以带参数,注意：如不填callbackUrl
    private static final String payType            ="payType";     //    String(10)    必填
    private static final String productName        ="productName"; //    String(255)    必填    商品名称
    private static final String productDesc        ="productDesc"; //    String(255)    必填    商品描述
    private static final String ip                 ="ip";          //    String(50)    必填    支付人的ip地址
    private static final String deviceType         ="deviceType";  //    String(20)    非必填    payType=3/13是必填，1PC(扫码) 2ios(H5) 3Android(H5)
    private static final String mchAppId           ="mchAppId";    //    String(200)    非必填   payType=1/3/5/13 时必填
    private static final String mchAppName         ="mchAppName";  //    String(200)    非必填    payType=3/5/13 时必填，应用名称

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merchantNo, channelWrapper.getAPI_MEMBERID());
                put(orderAmount,  channelWrapper.getAPI_AMOUNT());
                put(orderNo,channelWrapper.getAPI_ORDER_ID());
                put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(callbackUrl,channelWrapper.getAPI_WEB_URL());
                put(payType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(productName,channelWrapper.getAPI_ORDER_ID());
                put(productDesc,channelWrapper.getAPI_ORDER_ID());
                put(ip,channelWrapper.getAPI_Client_IP());
                if (HandlerUtil.isZFB(channelWrapper)){
                    put(deviceType,HandlerUtil.isOrderFromWapOrApp(channelWrapper.getAPI_ORDER_FROM())?"H5":"PC");
                    put(mchAppId,"mchAppId");
                    put(mchAppName,"mchAppName");
                }
            }
        };
        log.debug("[中诺支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
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
//      删除最后一个字符
        signSrc.deleteCharAt(signSrc.length()-1);
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[中诺支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        HashMap<String, String> result = Maps.newHashMap();
        try {
                String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
                JSONObject jsonObject;
                try {
                    jsonObject = JSONObject.parseObject(resultStr);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("[中诺支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
                if (null != jsonObject && jsonObject.containsKey("status") && "T".equalsIgnoreCase(jsonObject.getString("status"))
                        && jsonObject.containsKey("payUrl") && StringUtils.isNotBlank(jsonObject.getString("payUrl"))) {
                    String code_url = jsonObject.getString("payUrl");
//                    result.put( JUMPURL , code_url);
                    if (handlerUtil.isWapOrApp(channelWrapper)) {
                        result.put(JUMPURL, code_url);
                    }else{
                        result.put(QRCONTEXT, code_url);
                    }
                }else {
                    log.error("[中诺支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }

        } catch (Exception e) {
            log.error("[中诺支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[中诺支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[中诺支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}