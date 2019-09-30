package dc.pay.business.adtzhifu;

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
import dc.pay.utils.*;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Cobby
 * July 20, 2019
 */
@RequestPayHandler("ADTZHIFU")
public final class ADTZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(ADTZhiFuPayRequestHandler.class);

    private static final String MerchNo   = "MerchNo";// String N 商户号
    private static final String OrderNo   = "OrderNo";// String N 订单号，确保在商户系统不重复
    private static final String Money     = "Money";// decimal N 金额（元），最多 2 位小数
    private static final String Channal   = "Channal";// int N 支付通道，附录 1
    private static final String NotifyUrl = "NotifyUrl";// String N 回调地址
    private static final String Time      = "Time";// String N 请求时间，格式：yyyyMMddHHmmss
//  private static final String Attach               ="Attach"    ;// String Y 附加参数，回调时会原样返回
//  private static final String BankCode             ="BankCode"  ;// string Y 银行编码，通道为 1 和 2 时必填，附录 2

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(MerchNo, channelWrapper.getAPI_MEMBERID());
                put(OrderNo, channelWrapper.getAPI_ORDER_ID());
                put(Money, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(Channal, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(NotifyUrl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(Time, DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
            }
        };
        log.debug("[ADT支付]-[请求支付]-1.组装请求参数完成：{}", JSON.toJSONString(payParam));
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
        signSrc.deleteCharAt(signSrc.length() - 1);
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5   = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[ADT支付]-[请求支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);

        HashMap<String, String> result = Maps.newHashMap();
        try {

            Map<String, String> headerMap = Maps.newHashMap();
            headerMap.put("Content-Type", "application/json");
            JSONObject resJson = HttpUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), headerMap, payParam);
            //只取正确的值，其他情况抛出异常
            if (null != resJson && resJson.containsKey("code") && "1".equalsIgnoreCase(resJson.getString("code"))) {
//                    result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
                if (resJson.containsKey("payUrl") && StringUtils.isNotBlank(resJson.getString("payUrl"))) {
                    String code_url = resJson.getString("payUrl");
                    result.put(JUMPURL, code_url);
                } else if (resJson.containsKey("qrCode") && StringUtils.isNotBlank(resJson.getString("qrCode"))) {
                    String code_url = resJson.getString("qrCode");
                    result.put(QRCONTEXT, code_url);
                }
            } else {
                log.error("[ADT支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resJson) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(JSON.toJSONString(resJson));
            }

        } catch (Exception e) {
            log.error("[ADT支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null", e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[ADT支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));
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
        log.debug("[ADT支付]-[请求支付]-4.处理请求响应成功：{}", JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}