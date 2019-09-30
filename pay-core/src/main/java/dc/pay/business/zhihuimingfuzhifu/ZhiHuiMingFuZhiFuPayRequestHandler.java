package dc.pay.business.zhihuimingfuzhifu;

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
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * @author Cobby
 * Apr 09, 2019
 */
@RequestPayHandler("ZHIHUIMINGFUZHIFU")
public final class ZhiHuiMingFuZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(ZhiHuiMingFuZhiFuPayRequestHandler.class);

    private static final String data                  ="data";     // 数据报文原始内容字符串使用 DES 平台公钥加密后的字符串数据
    private static final String timestamp             ="timestamp";// 时间戳
    private static final String nonce                 ="nonce";    // 生成的随机字符串
    private static final String signtype              ="signtype"; // 加密传递固定数值 "MD5
    private static final String appid                 ="appid";// 应用key String(32) 必填 平台生成的唯一应用 appid
    private static final String order_trano_in        ="order_trano_in";// 商户单号 String(32) 必填 商户单号(必须保证唯一)
    private static final String order_goods           ="order_goods";// 商品名称 String(32) 必填 商品名称
    private static final String order_amount          ="order_amount";// 订单金额，单位分 String(32) 必填 订单金额，单位分
    private static final String order_extend          ="order_extend";// 扩展参数(回传) String(64) 必填 扩展参数(回传)
    private static final String order_ip              ="order_ip";// 客户端真实ip String(32) 必填 客户端真实 ip
    private static final String order_return_url      ="order_return_url";// 成功后同步地址 String(32) 必填 成功后同步地址(暂时无效)
    private static final String order_notify_url      ="order_notify_url";// 异步通知地址 String(64) 必填 异步通知地址

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> dataParam = new TreeMap<String, String>() {
            {
                put(appid, channelWrapper.getAPI_MEMBERID());
                put(order_trano_in,channelWrapper.getAPI_ORDER_ID());
                put(order_goods,channelWrapper.getAPI_ORDER_ID());
                put(order_amount,  channelWrapper.getAPI_AMOUNT());
                put(order_extend,channelWrapper.getAPI_ORDER_ID());
                put(order_ip,channelWrapper.getAPI_Client_IP());
                put(order_return_url,channelWrapper.getAPI_WEB_URL());
                put(order_notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            }
        };
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(data, JSONObject.toJSONString(dataParam));
                put(timestamp,String.valueOf(System.currentTimeMillis()));
                put(nonce,ZhiHuiMingFuSignHelper.genNonceStr());
                put(signtype,"MD5");
            }
        };
        log.debug("[智慧明付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        Map<String,String > dataMap = (Map)JSON.parse(api_response_params.get(data));
        TreeMap<String, String> map = new TreeMap<String, String>();
        map.put("appid", dataMap.get(appid));// 应用key
        map.put("order_trano_in", dataMap.get(order_trano_in));// 商户单号
        map.put("order_goods", dataMap.get(order_goods));// 商品名称
        map.put("order_amount", dataMap.get(order_amount));// 订单金额，单位分 (不能小于100)
        map.put("order_extend", dataMap.get(order_extend));// 扩展参数，最大长度64位
        map.put("order_ip", dataMap.get(order_ip));// 客户端真实ip
        map.put("order_return_url", dataMap.get(order_return_url));// 成功后同步地址
        map.put("order_notify_url", dataMap.get(order_notify_url));// 异步通知地址

        String sortStr = ZhiHuiMingFuSignHelper.sortSign(map);
        String paramsStr = String.format("%s%s%s%s",
                api_response_params.get(timestamp),
                api_response_params.get(nonce),
                sortStr,
                channelWrapper.getAPI_KEY());
        // 私钥签名
        String signMd5 = ZhiHuiMingFuSignHelper.MD5(paramsStr);
        log.debug("[智慧明付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        // DES加密key 0-8
        String signDesKey = ZhiHuiMingFuSignHelper.MD5(payParam.get(timestamp)
                 + channelWrapper.getAPI_PUBLIC_KEY() + payParam.get(nonce)).substring(0,8);
        // 公钥加密
        String dataJson = ZhiHuiMingFuDesHelper.encrypt(payParam.get(data), signDesKey);
        payParam.put(data, dataJson);
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);

        HashMap<String, String> result = Maps.newHashMap();
        try {
                String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(payParam),MediaType.APPLICATION_JSON_VALUE).trim();
                JSONObject jsonObject;
                try {
                    jsonObject = JSONObject.parseObject(resultStr);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("[智慧明付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
                if (null != jsonObject && jsonObject.containsKey("code") && "1".equalsIgnoreCase(jsonObject.getString("code"))
                        && jsonObject.containsKey("data") && StringUtils.isNotBlank(jsonObject.getString("data"))) {
                    jsonObject = JSONObject.parseObject(jsonObject.getString("data"));
                    String code_url = jsonObject.getString("order_pay_url");
                    result.put(  JUMPURL , code_url);
                }else {
                    log.error("[智慧明付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }

        } catch (Exception e) {
            log.error("[智慧明付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[智慧明付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[智慧明付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}