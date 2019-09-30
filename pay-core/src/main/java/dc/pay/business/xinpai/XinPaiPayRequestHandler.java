package dc.pay.business.xinpai;

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
 * Jul 3, 2018
 */
@RequestPayHandler("XINPAI")
public final class XinPaiPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XinPaiPayRequestHandler.class);

    //参数              参数名称       长度              说明                     是否可空       样例
    //payKey            支付key                          平台分配                 不可       
    //orderPrice        订单金额                         [0.01                    不可       
    //outTradeNo        订单编号       30                不可重复                 不可       
    //productType       商品类型                         QQ扫码:70000103          不可       
    //orderTime         下单时间                         yyyyMMDDHHMMSS           不可       
    //productName       商品名称                                                  不可       
    //orderIp           下单IP         50                                         不可       
    //remark            支付备注                                                       
    //notifyUrl         后台消息通知url                                           不可       
    //returnUrl         页面通知url                                               不可       
    //sign              签名           md5                                        不可       
    private static final String payKey                   ="payKey";
    private static final String orderPrice               ="orderPrice";
    private static final String outTradeNo               ="outTradeNo";
    private static final String productType              ="productType";
    private static final String orderTime                ="orderTime";
    private static final String productName              ="productName";
    private static final String orderIp                  ="orderIp";
    private static final String remark                   ="remark";
    private static final String notifyUrl                ="notifyUrl";
    private static final String returnUrl                ="returnUrl";

    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(payKey, channelWrapper.getAPI_MEMBERID());
                put(orderPrice,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(outTradeNo,channelWrapper.getAPI_ORDER_ID());
                put(productType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(orderTime,HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "yyyyMMddHHmmss"));
                put(productName,"name");
                put(remark,"name");
                put(orderIp,channelWrapper.getAPI_Client_IP());
                put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(returnUrl,channelWrapper.getAPI_WEB_URL());
            }
        };
        log.debug("[新派]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
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
        signSrc.append("paySecret=" + channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[新派]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
        if (StringUtils.isBlank(resultStr)) {
            log.error("[新派]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(payParam));
            throw new PayException("返回空,参数："+JSON.toJSONString(payParam));
        }
        JSONObject resJson = JSONObject.parseObject(resultStr);
        if (!resJson.containsKey("resultCode") || !"0000".equals(resJson.getString("resultCode"))) {
            log.error("[新派]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        //payMessageType	返回消息内容		支付信息类型:       	0:url(默认) 1:html
        result.put("1".equals(resJson.getString("payMessageType")) ? HTMLCONTEXT : JUMPURL, resJson.getString("payMessage"));
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[新派]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[新派]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}