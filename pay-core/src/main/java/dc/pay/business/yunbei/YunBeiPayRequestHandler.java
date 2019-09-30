package dc.pay.business.yunbei;

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
 * Oct 26, 2018
 */
@RequestPayHandler("YUNBEI")
public final class YunBeiPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(YunBeiPayRequestHandler.class);

    //参数名            是否必须        意义        
    //cpId               是             商户ID        
    //serviceId          是             服务ID        
    //payType            是             支付类型（见附录2）        
    //fee                是             支付金额（单位分）        
    //subject            是             商品名        
    //description        是             商品说明        
    //orderIdCp          是             商户订单号        
    //notifyUrl          是             异步通知地址        
    //callbackUrl        是             前端回调地址        
    //cpParam            否             透传参数        
    //bankCode           否             银行编码（注        1）        
    //userIdentity       否             用户识别码（注2）        
    //timestamp          是             当前时间戳（13位）        
    //ip                 是             用户ip        
    //version            是             版本号（固定1）        
    //sign               是             签名（规则见附录1）        
    private static final String cpId                     ="cpId";
    private static final String serviceId                ="serviceId";
    private static final String payType                  ="payType";
    private static final String fee                      ="fee";
    private static final String subject                  ="subject";
    private static final String description              ="description";
    private static final String orderIdCp                ="orderIdCp";
    private static final String notifyUrl                ="notifyUrl";
    private static final String callbackUrl              ="callbackUrl";
    private static final String cpParam                  ="cpParam";
//    private static final String bankCode                 ="bankCode";
//    private static final String userIdentity             ="userIdentity";
    private static final String timestamp                ="timestamp";
    private static final String ip                       ="ip";
    private static final String version                  ="version";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(cpId, channelWrapper.getAPI_MEMBERID());
                put(serviceId, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
                put(payType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
                put(fee,  channelWrapper.getAPI_AMOUNT());
                put(subject,"name");
                put(description,"name");
                put(orderIdCp,channelWrapper.getAPI_ORDER_ID());
                put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(callbackUrl,channelWrapper.getAPI_WEB_URL());
                put(cpParam,channelWrapper.getAPI_MEMBERID());
                put(timestamp,System.currentTimeMillis()+"");
                put(ip,channelWrapper.getAPI_Client_IP());
                put(version,"1");
            }
        };
        log.debug("[云贝]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!cpParam.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        //最后一个&转换成#
        //signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "#" );
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[云贝]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
        if (StringUtils.isBlank(resultStr)) {
            log.error("[云贝]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        if (!resultStr.contains("{") || !resultStr.contains("}")) {
           log.error("[云贝]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
           throw new PayException(resultStr);
        }
        //JSONObject resJson = JSONObject.parseObject(resultStr);
        JSONObject resJson;
        try {
            resJson = JSONObject.parseObject(resultStr);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[云贝]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        //只取正确的值，其他情况抛出异常
        if (null != resJson && resJson.containsKey("status") && "0".equalsIgnoreCase(resJson.getString("status"))  
                && resJson.containsKey("payUrl") && StringUtils.isNotBlank(resJson.getString("payUrl"))
                ) {
            String code_url = resJson.getString("payUrl");
            if (handlerUtil.isWapOrApp(channelWrapper) || handlerUtil.isWebYlKjzf(channelWrapper)) {
                result.put(JUMPURL, code_url);
            }else {
                result.put(QRCONTEXT, code_url);
            }
        }else {
            log.error("[云贝]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[云贝]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[云贝]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}