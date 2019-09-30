package dc.pay.business.zhonghuitong;

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
import org.springframework.http.MediaType;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Oct 29, 2018
 */
@RequestPayHandler("ZHONGHUITONG")
public final class ZhongHuiTongPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(ZhongHuiTongPayRequestHandler.class);

    private static final String mch_id                ="mch_id";       //    商户号        我司提供的商户号
    private static final String pay_model             ="pay_model";    //    交易方式编码        详情见上方交易方式表格
    private static final String amount                ="amount";       //    交易金额    金额0.01就是1分    金额格式最好采用”0.01”,”1.00”正常金额小数点后两位
    private static final String order_down_no         ="order_down_no";//    商户订单号    不要超过36位（可以36位）    商户订单号（由商户生成，必须保证唯一，36位内数字、字母的组合）
    private static final String random_string         ="random_string";//    随机字符串        每次请求保证唯一的随机字符串，理论上长度不限
    private static final String down_callback         ="down_callback";//    异步通知地址        商户接收支付结果的API接口地址
    private static final String ip                    ="ip";           //    请求IP或者会员IP

    private static final String key                   ="key";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(mch_id, channelWrapper.getAPI_MEMBERID());
                put(pay_model,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(order_down_no,channelWrapper.getAPI_ORDER_ID());
                put(random_string,HandlerUtil.randomStr(10));
                put(down_callback,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(ip,channelWrapper.getAPI_Client_IP());
            }
        };
        log.debug("[众汇通]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
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
//        删除最后一个字符
//        signSrc.deleteCharAt(signSrc.length()-1);
        signSrc.append(key+ "=" +channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[众汇通]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(payParam),MediaType.APPLICATION_JSON_VALUE);
        resultStr = UnicodeUtil.unicodeToString(resultStr);
        if (StringUtils.isBlank(resultStr)) {
            log.error("[众汇通]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        if (!resultStr.contains("{") || !resultStr.contains("}")) {
           log.error("[众汇通]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
           throw new PayException(resultStr);
        }
        JSONObject resJson;
        try {
            resJson = JSONObject.parseObject(resultStr);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[众汇通]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        //只取正确的值，其他情况抛出异常
        if (null != resJson && resJson.containsKey("code") && "200".equalsIgnoreCase(resJson.getString("code"))
                && resJson.containsKey("data") && StringUtils.isNotBlank(resJson.getString("data"))) {
            resJson = JSONObject.parseObject(resJson.getString("data"));
            String code_url = resJson.getString("web_address");
            String code_url_type = resJson.getString("web_address_type");
//            所有扫码需要我自行展示二维码 ？
//            16:56:12
//            小肉肉 2019/8/28 16:56:12
//            @王 
//            王 2019/8/28 16:56:25
//            都可以展示
            if (handlerUtil.isWEBWAPAPP_SM(channelWrapper)) {
                result.put(QRCONTEXT, code_url);
            } else if (code_url_type.equalsIgnoreCase("1")) {
                result.put(JUMPURL, code_url);
            }else{
                result.put(HTMLCONTEXT, code_url);
            }
        }else {
            log.error("[众汇通]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
    
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[众汇通]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[众汇通]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}