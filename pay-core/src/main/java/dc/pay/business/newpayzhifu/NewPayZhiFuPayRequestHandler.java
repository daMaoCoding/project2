package dc.pay.business.newpayzhifu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

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
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * @author Cobby
 * Apr 25, 2019
 */
@RequestPayHandler("NEWPAYZHIFU")
public final class NewPayZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(NewPayZhiFuPayRequestHandler.class);

    private static final String mch_id                ="mch_id";      //        必填    商户id
    private static final String cp_order_no           ="cp_order_no"; //        必填    商户订单号，必须唯一，最长32个字符
    private static final String order_uid             ="order_uid";   //        必填    玩家角色id，必须唯一，最长16个字符,请用玩家的真实ID
    private static final String order_amount          ="order_amount";//    int    必填    订单金额，单位：分
    private static final String trade_type            ="trade_type";  //    int    必填    订单类型：1 微信扫码，2 支付宝扫码，3 QQ扫码（暂时只支持支付宝）
    private static final String notify_url            ="notify_url";  //        必填    支付结果通知地址，最长200个字符
    private static final String goods_id              ="goods_id";    //        可选    商品id，最长32个字符
    private static final String goods_name            ="goods_name";  //        可选    名称，最长10个中文，英文最长40个
    private static final String ip                    ="ip";          //        必填    玩家IP地址，方便分配固定二维码


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(mch_id, channelWrapper.getAPI_MEMBERID());
                put(cp_order_no,channelWrapper.getAPI_ORDER_ID());
                put(order_uid,System.currentTimeMillis()+"");
                put(order_amount,  channelWrapper.getAPI_AMOUNT());
                put(trade_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(goods_id,channelWrapper.getAPI_ORDER_ID());
                put(goods_name,"goods_name");
                put(ip,channelWrapper.getAPI_Client_IP());
            }
        };
        log.debug("[NewPay支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
//        md5("cp_order_no="+cp_order_no+"&mch_id="+mch_id+"&notify_url="+notify_url+"&order_amount="+order_amount+mch_key)
        StringBuilder signStr = new StringBuilder();
        signStr.append(cp_order_no+"=").append(api_response_params.get(cp_order_no)).append("&");
        signStr.append(mch_id+"=").append(api_response_params.get(mch_id)).append("&");
        signStr.append(notify_url+"=").append(api_response_params.get(notify_url)).append("&");
        signStr.append(order_amount+"=").append(api_response_params.get(order_amount));
        signStr.append(channelWrapper.getAPI_KEY());
        String paramsStr =signStr.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[NewPay支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        Map<String, Object> payParamJson = new HashMap<>();
        payParamJson.put(mch_id,payParam.get(mch_id));
        payParamJson.put(cp_order_no,payParam.get(cp_order_no));
        payParamJson.put(order_uid,payParam.get(order_uid));
        payParamJson.put(order_amount,Integer.valueOf(payParam.get(order_amount)));
        payParamJson.put(trade_type,Integer.valueOf(payParam.get(trade_type)));
        payParamJson.put(notify_url,payParam.get(notify_url));
        payParamJson.put(goods_id,payParam.get(goods_id));
        payParamJson.put(goods_name,payParam.get(goods_name));
        payParamJson.put(ip,payParam.get(ip));
        payParamJson.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);

        HashMap<String, String> result = Maps.newHashMap();
        try {
                String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(payParamJson),MediaType.APPLICATION_JSON_VALUE);
//                String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
//                String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
                JSONObject jsonObject;
                try {
                    jsonObject = JSONObject.parseObject(resultStr);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("[NewPay支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
                if (null != jsonObject && jsonObject.containsKey("retcode") && "0".equalsIgnoreCase(jsonObject.getString("retcode"))
                        && jsonObject.containsKey("data") && StringUtils.isNotBlank(jsonObject.getString("data"))) {
                    jsonObject = JSONObject.parseObject(jsonObject.getString("data"));
                    String code_url = jsonObject.getString("pay_url");
                    result.put( JUMPURL , code_url);
//                    if (handlerUtil.isWapOrApp(channelWrapper)) {
//                        result.put(JUMPURL, jsonObject.getString("pay_url"));
//                    }else{
//                        result.put(QRCONTEXT, jsonObject.getString("qrcode"));
//                    }
                }else {
                    log.error("[NewPay支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }

        } catch (Exception e) {
            log.error("[NewPay支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[NewPay支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[NewPay支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}