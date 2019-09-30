package dc.pay.business.huanqiuzhifu2;


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
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.util.*;

/**
 * @author Cobby
 * July 15, 2019
 */
@RequestPayHandler("HUANQIUZHIFU2")
public final class HuanQiuZhiFu2PayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(HuanQiuZhiFu2PayRequestHandler.class);


    private static final String mchId      = "mchId";     // 商户ID
    private static final String appId      = "appId";     // 应用ID
    private static final String productId  = "productId"; // 支付产品ID
    private static final String mchOrderNo = "mchOrderNo";// 商户订单号
    private static final String currency   = "currency";  // 三位货币代码,人民币:cny
    private static final String amount     = "amount";    // 支付金额,单位分
    private static final String returnUrl  = "returnUrl"; // 支付结果前端跳转URL
    private static final String notifyUrl  = "notifyUrl"; // 支付结果后台回调URL
    private static final String subject    = "subject";   // 商品主题
    private static final String body       = "body";      // 商品描述信息
    private static final String key        = "key";
//  private static final String extra              = "extra";    // 附加参数  是    String    {‘userid’:’1204564’}    支付成功后会原样返回

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
        if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 2) {
            log.error("[环球支付2]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户ID&应用ID（向第三方获取当前使用通道编码值）");
            throw new PayException("[环球支付2]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户ID&应用ID（向第三方获取当前使用通道编码值）");
        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(mchId, channelWrapper.getAPI_MEMBERID().split("&")[0]);
                put(appId, channelWrapper.getAPI_MEMBERID().split("&")[1]);
                put(productId, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(mchOrderNo, channelWrapper.getAPI_ORDER_ID());
                put(currency, "cny");
                put(amount, channelWrapper.getAPI_AMOUNT());
                put(returnUrl, channelWrapper.getAPI_WEB_URL());
                put(notifyUrl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(subject, "subject");
                put(body, "body");
            }
        };
        log.debug("[环球支付2]-[请求支付]-1.组装请求参数完成：{}", JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
        List          paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc   = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        signSrc.append(key + "=" + channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5   = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[环球支付2]-[请求支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        Map<String, String> payParam1 = new HashMap<>();
        payParam1.put("params", JSON.toJSONString(payParam));
        HashMap<String, String> result = Maps.newHashMap();
        try {
            String     resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam1, "UTF-8");
            JSONObject jsonObject;
            try {
                jsonObject = JSONObject.parseObject(resultStr);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("[环球支付2]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            if (null != jsonObject && jsonObject.containsKey("retCode") && "SUCCESS".equalsIgnoreCase(jsonObject.getString("retCode"))
                    && jsonObject.containsKey("payParams") && StringUtils.isNotBlank(jsonObject.getString("payParams"))) {
                String     payParams   = jsonObject.getString("payParams");
                JSONObject jsonObject1 = JSONObject.parseObject(payParams);
                String     payUrl      = jsonObject1.getString("payUrl");
                String     encode      = URLEncoder.encode("订单编号", "UTF-8");
                payUrl = payUrl.replaceAll("订单编号", encode);
                result.put(JUMPURL, payUrl);
            } else {
                log.error("[环球支付2]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }

        } catch (Exception e) {
            log.error("[环球支付2]-[请求支付]-3.3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null", e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[环球支付2]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));
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
        log.debug("[环球支付2]-[请求支付]-4.处理请求响应成功：{}", JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}