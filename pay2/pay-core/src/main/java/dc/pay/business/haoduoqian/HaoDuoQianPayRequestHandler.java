package dc.pay.business.haoduoqian;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Base64;
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
import dc.pay.utils.RsaUtil;
import dc.pay.utils.ValidateUtil;
import dc.pay.business.ruijietong.RuiJieTongUtil;
import dc.pay.utils.qr.QRCodeUtil;

/**
 * 
 * @author andrew
 * Dec 12, 2018
 */
@RequestPayHandler("HAODUOQIAN")
public final class HaoDuoQianPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(HaoDuoQianPayRequestHandler.class);

    // private static final String merchNo = "merchNo"; // 商户号
//    private static final String version = "version"; // 参数列表

    // bank_code 字串 (16) 必填 银行代码：目前只有服务类型网关与提现会使用到，其他请放空值。
    private static final String bank_code = "bank_code";
    // service_type 字串 (2) 必填 服务类型：请参考附件 ( 服务类型列表)
    private static final String service_type = "service_type";
    // amount 字串 (12) 必填 金额：此金额为存款金额
    private static final String amount = "amount";
    // merchant_user 字串 (64) 必填 商户用户号
    private static final String merchant_user = "merchant_user";
    // risk_level 字串 (8) 必填 请带入常数值1
    private static final String risk_level = "risk_level";
    // merchant_order_no 字串 (64) 必填 支付平台返回商户订单号(商户代码必须是唯一的否则会被退回)
    private static final String merchant_order_no = "merchant_order_no";
    // platform 字串 (8) 必填 请带入常数值PC
    private static final String platform = "platform";
    // callback_url 字串 (4000) 非必填 用于存款回调通知的地址(如果这是空白，那么将是一个成功的存款）
    private static final String callback_url = "callback_url";
    // note 字串 (255) 非必填 备注
    private static final String note = "note";

    // signature 数据签名 32 是
    // private static final String signature ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                if (handlerUtil.isWY(channelWrapper)) {
                    put(bank_code, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                }
                put(service_type, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(amount, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(merchant_user, channelWrapper.getAPI_MEMBERID());
                put(risk_level, "1");
                put(merchant_order_no, channelWrapper.getAPI_ORDER_ID());
                put(platform, "PC");
                put(callback_url, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(note, "-");
            }
        };
        log.debug("[好多钱]-[请求支付]-1.组装请求参数完成：{}", JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String, String> payParam) throws PayException {
        String sign = null;
        try {
            byte[] contentAndPubkeyBytes = RuiJieTongUtil.encryptByPublicKey(JSON.toJSONString(new TreeMap<>(payParam)).getBytes(RuiJieTongUtil.CHARSET),channelWrapper.getAPI_PUBLIC_KEY());
            String step2 = Base64.getEncoder().encodeToString(contentAndPubkeyBytes);
            //sun方法
            sign = RsaUtil.signByPrivateKey(step2, channelWrapper.getAPI_KEY(),"SHA1WithRSA");
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[好多钱]-[请求支付]-2.生成加密URL签名出错，签名出错【本第三方，签名在sendRequestGetResult()方法里执行】：{}",e.getMessage(),e);
            throw new PayException(e.getMessage(),e);
        }
        log.debug("[好多钱]-[请求支付]-2.生成加密URL签名完成【本第三方，签名在sendRequestGetResult()方法里执行】：" + JSON.toJSONString(sign));
        return sign;
    }
    
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        // payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),
        Map<String,String> result = Maps.newHashMap();
        StringBuilder sb = new StringBuilder();
        try {
            byte[] contentAndPubkeyBytes = RuiJieTongUtil.encryptByPublicKey(JSON.toJSONString(new TreeMap<>(payParam)).getBytes(RuiJieTongUtil.CHARSET),channelWrapper.getAPI_PUBLIC_KEY());
            String step2 = Base64.getEncoder().encodeToString(contentAndPubkeyBytes);
            String sign = RsaUtil.signByPrivateKey(step2, channelWrapper.getAPI_KEY(),"SHA1WithRSA");
            sb.append("merchant_code=").append(URLEncoder.encode(channelWrapper.getAPI_MEMBERID(), "UTF-8")).append("&");
            sb.append("data=").append(URLEncoder.encode(step2, "UTF-8")).append("&");
            sb.append("sign=").append(URLEncoder.encode(sign, "UTF-8"));
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[好多钱]-[请求支付]-3.1.生成加密URL签名出错，签名出错：{}",e.getMessage(),e);
            throw new PayException(e.getMessage(),e);
        }
        String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), sb.toString(),MediaType.APPLICATION_FORM_URLENCODED_VALUE);
        if (StringUtils.isBlank(resultStr)) {
            log.error("[好多钱]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        if (!resultStr.contains("{") || !resultStr.contains("}")) {
            log.error("[好多钱]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        JSONObject jsonObject = null;
        try {
            jsonObject = JSONObject.parseObject(resultStr);
        } catch (Exception e) {
            log.error("[好多钱]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(e.getMessage(),e);
        }          
        //只取正确的值，其他情况抛出异常
        if (null != jsonObject && jsonObject.containsKey("status") && "1".equalsIgnoreCase(jsonObject.getString("status"))  && jsonObject.containsKey("data") && StringUtils.isNotBlank(jsonObject.getString("data"))) {
            JSONObject jsonObject2 = null;
            try {
                String decryptedData = RSAutilJava8.decryptByPrivateKey(jsonObject.getString("data"), channelWrapper.getAPI_KEY());
//                String decryptedData = RsaUtil.signByPrivateKey(jsonObject.getString("data"), channelWrapper.getAPI_KEY(),"SHA1WithRSA");
                if (StringUtils.isBlank(decryptedData)) {
                    log.error("[好多钱]-[请求支付]-3.5.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(decryptedData) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(decryptedData);
                }
                if (!decryptedData.contains("{") || !decryptedData.contains("}")) {
                    log.error("[好多钱]-[请求支付]-3.6.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(decryptedData) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(decryptedData);
                }
                jsonObject2 = JSONObject.parseObject(decryptedData);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("[好多钱]-[请求支付]-3.7.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            //为了方便贵司使用我司的支付，若贵司使用QR性质的服务类型 请先检查qr_image_url参数是否为空, 若为空再进一步从transaction_url取值
            String transaction_url = jsonObject2.getString("transaction_url");
            if (handlerUtil.isWEBWAPAPP_SM(channelWrapper)) {
                String qr_image_url = jsonObject2.getString("qr_image_url");
                if (StringUtils.isBlank(qr_image_url)) {
                    result.put(JUMPURL, transaction_url);
                }else{
                    result.put(QRCONTEXT, QRCodeUtil.decodeByUrl(qr_image_url));
                }
            }else {
                result.put(JUMPURL, transaction_url);
            }
        }else {
            log.error("[好多钱]-[请求支付]-3.8.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[好多钱]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));
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
        log.debug("[好多钱]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}