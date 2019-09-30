package dc.pay.business.longxin2;

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
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Feb 25, 2019
 */
@RequestPayHandler("LONGXIN2")
public final class LongXin2PayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(LongXin2PayRequestHandler.class);

    //请求参数(键值对格式)
    //companyId   用户ID 由商务分配
    private static final String companyId             ="companyId";
    //userOrderId 用户自定义订单同步时候会返回
    private static final String userOrderId             ="userOrderId";
    //payType 支付方式
    private static final String payType             ="payType";
    //item    商品名
    private static final String item             ="item";
    //fee 价格 (单位分)
    private static final String fee             ="fee";
    //expire  超时时间(可选参数,单位:秒)
//    private static final String expire             ="expire";
    //callbackUrl 前端回调地址(不是所有通道都能用)
    private static final String callbackUrl             ="callbackUrl";
    //syncUrl 异步通知地址
    private static final String syncUrl             ="syncUrl";
    //ip  终端用户的IP
    private static final String ip             ="ip";
    //mobile  手机号/或者用户在贵方系统中的唯一会员ID     仅在快捷支付时候需要使用
    private static final String mobile                ="mobile";
    //sign    签名=MD5(companyId_userOrderId_fee_用户密钥) 小写    参数之间用下划线连接
//    private static final String sign             ="sign";
    
//    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(companyId, channelWrapper.getAPI_MEMBERID());
                put(userOrderId,channelWrapper.getAPI_ORDER_ID());
                put(payType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(item,"name");
                put(fee,  channelWrapper.getAPI_AMOUNT());
                put(callbackUrl,channelWrapper.getAPI_WEB_URL());
                put(syncUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(ip,channelWrapper.getAPI_Client_IP());
                put(mobile,handlerUtil.getRandomStr(8));
            }
        };
        log.debug("[龙信2]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        StringBuilder signSrc = new StringBuilder();
        signSrc.append(api_response_params.get(companyId)).append("_");
        signSrc.append(api_response_params.get(userOrderId)).append("_");
        signSrc.append(api_response_params.get(fee)).append("_");
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[龙信2]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
//        String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
        String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
        if (StringUtils.isBlank(resultStr)) {
            log.error("[龙信2]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
            //log.error("[龙信2]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
            //throw new PayException("返回空,参数："+JSON.toJSONString(map));
        }
//        if (!resultStr.contains("{") || !resultStr.contains("}")) {
//           log.error("[龙信2]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//           throw new PayException(resultStr);
//        }
        //JSONObject resJson = JSONObject.parseObject(resultStr);
        JSONObject resJson;
        try {
            resJson = JSONObject.parseObject(resultStr);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[龙信2]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        //只取正确的值，其他情况抛出异常
        if (null != resJson && resJson.containsKey("result") && "0".equalsIgnoreCase(resJson.getString("result"))  && resJson.containsKey("param") && StringUtils.isNotBlank(resJson.getString("param"))) {
//            result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, resJson.getString("qrData"));
                result.put( JUMPURL, resJson.getString("param"));
        }else {
            log.error("[龙信2]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[龙信2]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[龙信2]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}