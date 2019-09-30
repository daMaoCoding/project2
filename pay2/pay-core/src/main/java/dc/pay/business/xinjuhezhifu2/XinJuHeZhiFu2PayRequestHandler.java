package dc.pay.business.xinjuhezhifu2;

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
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Cobby
 * July 29, 2019
 */
@RequestPayHandler("XINJUHEZHIFU2")
public final class XinJuHeZhiFu2PayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XinJuHeZhiFu2PayRequestHandler.class);


    private static final String ac          = "ac";            // 请求类型     是     是     固定值：pays
    private static final String pay_version = "pay_version";   // 系统接口版本     是     是     固定值: 1.7
    private static final String appid       = "appid";         // 商户号     是     是     平台分配商户号
    private static final String pay_orderid = "pay_orderid";   // 订单号     是     是     上送订单号唯一, 字符长度 20
    private static final String pay_time    = "pay_time";      // 提交时间    是    是     时间格式(yyyyMMddHHmmss)：20161226181818
    private static final String pay_payment = "pay_payment";   // 订单金额     是     是     商品金额（单位元）1-   100000000
    private static final String notify_url  = "notify_url";    // 异步回调地址    是    否    异步通知支付结果，不参与签名

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(ac, "pays");
                put(pay_version, "1.7");
                put(appid, channelWrapper.getAPI_MEMBERID());
                put(pay_orderid, channelWrapper.getAPI_ORDER_ID());
                put(pay_time, DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
                put(pay_payment, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(notify_url, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            }
        };
        log.debug("[新聚合2]-[请求支付]-1.组装请求参数完成：{}", JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
        //stringSignTemp="ac=ac&appid=appid&pay_orderid=pay_orderid&pay_payment=pay_payment&pay_time=pay_time&pay_version=pay_version"+ appkey
        StringBuffer signSrc = new StringBuffer();
        signSrc.append(ac + "=").append(api_response_params.get(ac)).append("&");
        signSrc.append(appid + "=").append(api_response_params.get(appid)).append("&");
        signSrc.append(pay_orderid + "=").append(api_response_params.get(pay_orderid)).append("&");
        signSrc.append(pay_payment + "=").append(api_response_params.get(pay_payment)).append("&");
        signSrc.append(pay_time + "=").append(api_response_params.get(pay_time)).append("&");
        signSrc.append(pay_version + "=").append(api_response_params.get(pay_version));
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5   = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[新聚合2]-[请求支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);

        HashMap<String, String> result = Maps.newHashMap();
        try {
            String     resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, "UTF-8");
            JSONObject resJson;
            try {
                resJson = JSONObject.parseObject(resultStr);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("[新聚合2]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            //只取正确的值，其他情况抛出异常
            if (null != resJson && resJson.containsKey("code") && "1".equalsIgnoreCase(resJson.getString("code"))
                    && resJson.containsKey("payresult") && StringUtils.isNotBlank(resJson.getString("payresult"))) {
                String code_url = resJson.getString("payresult");
                result.put(JUMPURL, code_url);
            } else {
                log.error("[新聚合2]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }

        } catch (Exception e) {
            log.error("[新聚合2]-[请求支付]-3.3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null", e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[新聚合2]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));
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
        log.debug("[新聚合2]-[请求支付]-4.处理请求响应成功：{}", JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}