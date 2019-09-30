package dc.pay.business.dianfupayzhifu;

import com.alibaba.fastjson.JSON;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Cobby
 * July 25, 2019
 */
@RequestPayHandler("DIANFUPAYZHIFU")
public final class DianFuPayZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(DianFuPayZhiFuPayRequestHandler.class);

    private static final String version    = "version"; // 版本号     VARCHAR(10)    必填    v1.0.0
    private static final String orderId    = "orderId"; // 订单号     VARCHAR(32)    必填    32位以下的订单号
    private static final String merCode    = "merCode"; // 商户编码   VARCHAR(32)    必填    商户编号
    private static final String payWay     = "payWay"; // 支付通道   VARCHAR(20)    必填    支付通道,如：weixin、alipay、qqpay,jdpay,bdpay,unionpay,weixinH5
    private static final String tranTime   = "tranTime"; // 申请时间   VARCHAR(14)    必填    发起交易的时间，格式yyyyMMddhhmmss
    private static final String totalAmt   = "totalAmt"; // 订单金额   Number(13,1)    必填    格式：12.0  保留1位小数
    private static final String title      = "title"; // 订单标题   VARCHAR(256)    必填    支付宝渠道：显示在用户app上的订单信息
    private static final String encodeType = "encodeType"; // 签名方式   VARCHAR(10)    必填    md5加密方式传md5
    private static final String notifyUrl  = "notifyUrl"; // 通知地址   VARCHAR(128)    必填
    private static final String attach     = "attach"; // 通知地址   VARCHAR(128)    必填

    private static final String key = "key";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(version, "v1.0.0");
                put(orderId, channelWrapper.getAPI_ORDER_ID());
                put(merCode, channelWrapper.getAPI_MEMBERID());
                put(payWay, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(tranTime, DateUtil.formatDateTimeStrByParam("yyyyMMddhhmmss"));
                put(totalAmt, HandlerUtil.getYuanWithoutOne(channelWrapper.getAPI_AMOUNT()));
                put(title, "name");
                put(encodeType, "md5");
                put(attach, "attach");
                put(notifyUrl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            }
        };
        log.debug("[点付支付]-[请求支付]-1.组装请求参数完成：{}", JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
        //MD5.sign(version=v1.0.0&orderId=XXX&merCode=XXX&payWay=XXX&tranTime=XXX&totalAmt=XXX&title=XXX&encodeType=md5&notifyUrl=XXX&attach=XXX&key=XXX)
        StringBuffer signSrc = new StringBuffer();
        signSrc.append(version + "=").append(api_response_params.get(version)).append("&");
        signSrc.append(orderId + "=").append(api_response_params.get(orderId)).append("&");
        signSrc.append(merCode + "=").append(api_response_params.get(merCode)).append("&");
        signSrc.append(payWay + "=").append(api_response_params.get(payWay)).append("&");
        signSrc.append(tranTime + "=").append(api_response_params.get(tranTime)).append("&");
        signSrc.append(totalAmt + "=").append(api_response_params.get(totalAmt)).append("&");
        signSrc.append(title + "=").append(api_response_params.get(title)).append("&");
        signSrc.append(encodeType + "=").append(api_response_params.get(encodeType)).append("&");
        signSrc.append(notifyUrl + "=").append(api_response_params.get(notifyUrl)).append("&");
        signSrc.append(attach + "=").append(api_response_params.get(attach)).append("&");
        signSrc.append(key + "=").append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5   = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
//        String signMd5       = MD5Util.MD5(paramsStr);
        log.debug("[点付支付]-[请求支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);

        HashMap<String, String> result = Maps.newHashMap();
        try {
            String     resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, "UTF-8");
            //只取正确的值，其他情况抛出异常
            if (resultStr.contains("<form")) {
                result.put(HTMLCONTEXT, resultStr);
            } else {
                log.error("[点付支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }

//            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam).toString());

        } catch (Exception e) {
            log.error("[点付支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null", e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[点付支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));
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
        log.debug("[点付支付]-[请求支付]-4.处理请求响应成功：{}", JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}