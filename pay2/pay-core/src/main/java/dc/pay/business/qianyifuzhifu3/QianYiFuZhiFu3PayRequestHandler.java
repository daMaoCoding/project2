package dc.pay.business.qianyifuzhifu3;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayRequestHandler;
import dc.pay.business.RequestPayResult;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.ValidateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Cobby
 * June 24, 2019
 */
@RequestPayHandler("QIANYIFUZHIFU3")
public final class QianYiFuZhiFu3PayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(QianYiFuZhiFu3PayRequestHandler.class);

    private static final String paytype     = "paytype";    //   是    是    支付方式(支付宝:101,微信:102,QQ:103,银行卡:104)
    private static final String amount      = "amount";     //   是    是    支付的金额,带两位小数(例如:1.00)
    private static final String orderId     = "orderId";    //   是    是    商户的订单号
    private static final String callbackurl = "callbackurl";//   是    是    回调地址(必须外网可访问,不带参数)
    private static final String uuid        = "uuid";       //   是    是    商户平台的用户的唯一标(例如:userid)

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(paytype, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(amount, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(orderId, channelWrapper.getAPI_ORDER_ID());
                put(callbackurl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(uuid, channelWrapper.getAPI_MEMBERID());
            }
        };
        log.debug("[千益付支付3]-[请求支付]-1.组装请求参数完成：{}", JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
        //paytype| amount| orderId| callbackurl| uuid|ABCDEFGQWERTYUIOPLKJ
        String paramsStr = String.format("%s|%s|%s|%s|%s|%s",
                api_response_params.get(paytype),
                api_response_params.get(amount),
                api_response_params.get(orderId),
                api_response_params.get(callbackurl),
                api_response_params.get(uuid),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[千益付支付3]-[请求支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);

        HashMap<String, String> result = Maps.newHashMap();
        try {

            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam).toString());

        } catch (Exception e) {
            log.error("[千益付支付3]-[请求支付]-3.1.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null", e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[千益付支付3]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));
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
        log.debug("[千益付支付3]-[请求支付]-4.处理请求响应成功：{}", JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}