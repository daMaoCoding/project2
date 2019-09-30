package dc.pay.business.paywusanyi;

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
import dc.pay.utils.JsonUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Oct 29, 2018
 */
@RequestPayHandler("PAYWUSANYI")
public final class PayWuSanYiPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(PayWuSanYiPayRequestHandler.class);

    //字段名          变量名              必填           类型               说明
    //接口类型        service              是           String(32)           接口类型：支付宝扫码：pay.alipay.native    支付宝H5: pay.alipayh5.native   微信扫码:  pay.weixin.native   微信H5:  pay.weixinh5.native
    //商户号          mch_id               是           String(10)           商户号，由平台分配
    //商户订单号      out_trade_no         是           String(32)           商户系统内部的订单号 ,32个字符内、 可包含字母,确保在商户系统唯一
    //附加信息        attach               否           String(127)           商户附加信息，可做扩展参数
    //总金额          total_fee            是           Int                  总金额，以分为单位，不允许包含任何字、符号
    //终端IP          mch_create_ip        是           String(16)           终端用户 IP
    //通知地址        notify_url           是           String(255)           接收平台通知的URL，需给绝对路径，255字符内格式如:http://wap.tenpay.com/tenpay.asp，确保平台能通过互联网访问该地址
    //签名            sign                 是           String(32)           MD5签名结果，详见“安全规范”
    private static final String service                   ="service";
    private static final String mch_id                    ="mch_id";
    private static final String out_trade_no              ="out_trade_no";
    private static final String attach                    ="attach";
    private static final String total_fee                 ="total_fee";
    private static final String mch_create_ip             ="mch_create_ip";
    private static final String notify_url                ="notify_url";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(service,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(mch_id, channelWrapper.getAPI_MEMBERID());
                put(out_trade_no,channelWrapper.getAPI_ORDER_ID());
                put(attach,"name");
                put(total_fee, channelWrapper.getAPI_AMOUNT());
                put(mch_create_ip,channelWrapper.getAPI_Client_IP());
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            }
        };
        log.debug("[pay531]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
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
        //最后一个&转换成#
        //signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "#" );
        signSrc.append(key +"="+channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[pay531]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JsonUtil.stringify(payParam),MediaType.APPLICATION_JSON_UTF8_VALUE.toString());
        if (StringUtils.isBlank(resultStr)) {
            log.error("[pay531]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        if (!resultStr.contains("{") || !resultStr.contains("}")) {
           log.error("[pay531]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
           throw new PayException(resultStr);
        }
        //JSONObject resJson = JSONObject.parseObject(resultStr);
        JSONObject resJson;
        try {
            resJson = JSONObject.parseObject(resultStr);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[pay531]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        //只取正确的值，其他情况抛出异常
        if (null != resJson && resJson.containsKey("result_code") && "0".equalsIgnoreCase(resJson.getString("result_code"))  && resJson.containsKey("code_url") && StringUtils.isNotBlank(resJson.getString("code_url"))) {
            String code_url = resJson.getString("code_url");
            if (handlerUtil.isWapOrApp(channelWrapper)) {
                result.put(JUMPURL, code_url);
            }else{
                result.put(QRCONTEXT, code_url);
            }
        }else {
            log.error("[pay531]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[pay531]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[pay531]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}