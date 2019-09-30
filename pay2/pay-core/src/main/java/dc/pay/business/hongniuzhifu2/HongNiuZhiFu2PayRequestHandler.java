package dc.pay.business.hongniuzhifu2;

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
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Cobby
 * June 17, 2019
 */
@RequestPayHandler("HONGNIUZHIFU2")
public final class HongNiuZhiFu2PayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(HongNiuZhiFu2PayRequestHandler.class);


    private static final String data         = "data";         // 参数列表
    private static final String requestTime  = "requestTime";  // 时间戳(10位)
    private static final String brokerID     = "brokerID";     // 第三方平台在红牛平台注册的经纪公司ID
    private static final String thirdOrderNo = "thirdOrderNo"; // 经纪公司订单号
    private static final String amount       = "amount";       // 充值金额
    private static final String mobile       = "mobile";       // 网红手机号
    private static final String payType      = "payType";      // 支付方式，1支付宝，2微信
    private static final String reserved1    = "reserved1";    // 支付宝uid

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
        if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 2) {
            log.error("[红牛支付2]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&通道类型编码值（向第三方获取当前使用通道编码值）" );
            throw new PayException("[红牛支付2]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&通道类型编码值（向第三方获取当前使用通道编码值）" );
        }
        
        Map<String, String> dataParam = new LinkedHashMap<>();
        dataParam.put(brokerID, channelWrapper.getAPI_MEMBERID().split("&")[0]);
        dataParam.put(thirdOrderNo, channelWrapper.getAPI_ORDER_ID());
        dataParam.put(amount, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
        dataParam.put(mobile, "13113111311");
//        dataParam.put(payType, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
        dataParam.put(payType, channelWrapper.getAPI_MEMBERID().split("&")[1]);
        dataParam.put(reserved1, channelWrapper.getAPI_ORDER_ID());
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(data, JSON.toJSONString(dataParam));
                put(requestTime, System.currentTimeMillis() / 1000 + "");
            }
        };
        log.debug("[红牛支付2]-[请求支付]-1.组装请求参数完成：{}", JSON.toJSONString(payParam));
        return payParam;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
        StringBuffer signSrc = new StringBuffer();
        signSrc.append(api_response_params.get(data));
        signSrc.append(api_response_params.get(requestTime));
        signSrc.append(channelWrapper.getAPI_KEY());
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        String paramsStr = signSrc.toString();
        String signMd5   = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[红牛支付2]-[请求支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);

        HashMap<String, String> result = Maps.newHashMap();
        String                  url    = channelWrapper.getAPI_CHANNEL_BANK_URL() + "/out/platform/create";
        try {
            String resultStr = RestTemplateUtil.postForm(url, payParam, "UTF-8");
            if (StringUtils.isBlank(resultStr)) {
                log.error("[红牛支付2]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            //JSONObject resJson = JSONObject.parseObject(resultStr);
            JSONObject resJson;
            try {
                resJson = JSONObject.parseObject(resultStr);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("[红牛支付2]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            //只取正确的值，其他情况抛出异常
            if (null != resJson && resJson.containsKey("code") && "200".equalsIgnoreCase(resJson.getString("code"))
                    && resJson.containsKey("data") && StringUtils.isNotBlank(resJson.getString("data"))) {
                resJson = JSONObject.parseObject(resJson.getString("data"));
                String code_url = resJson.getString("orderNo");
                if (HandlerUtil.isZFB(channelWrapper)) {
                    result.put(JUMPURL, channelWrapper.getAPI_CHANNEL_BANK_URL() + "/h5/pay/alipay.html?orderNo=" + code_url);
                } else {
                    result.put(JUMPURL, channelWrapper.getAPI_CHANNEL_BANK_URL() + "/h5/pay/wxpay.html?orderNo=" + code_url);
                }
            } else {
                log.error("[红牛支付2]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }

        } catch (Exception e) {
            log.error("[红牛支付2]-[请求支付]-3.3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null", e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[红牛支付2]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));
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
        log.debug("[红牛支付2]-[请求支付]-4.处理请求响应成功：{}", JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}