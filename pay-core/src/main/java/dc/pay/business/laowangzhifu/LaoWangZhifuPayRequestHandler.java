package dc.pay.business.laowangzhifu;

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
import dc.pay.utils.MapUtils;
import dc.pay.utils.ValidateUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Cobby
 * June 17, 2019
 */
@RequestPayHandler("LAOWANGZHIFU")
public final class LaoWangZhifuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(LaoWangZhifuPayRequestHandler.class);

    private static final String merchNo = "merchNo";     //商户号      是    10001    由分配给商户的商户唯一编码
    private static final String orderNo = "orderNo";     //商户单号    是    A2019a06b06    商户上送订单号，保持唯一值。
    private static final String amount = "amount";      //交易金额    是    1000    以分为单位，如1000
    private static final String currency = "currency";    //币种        是    CNY    默认为CNY
    private static final String outChannel = "outChannel";  //支付类型     是    ZHIh5    详见附录4.1
    private static final String product = "product";     //商品描述     是    消费    用于描述该笔交易商品的主体信息
    private static final String memo = "memo";        //商品备注     是    消费    回调中将此数据原样返回
    private static final String returnUrl = "returnUrl";   //同步回调地址  是    http://www.xxx.com/return    商户服务器用来接收同步通知的http地址
    private static final String notifyUrl = "notifyUrl";   //异步通知地址  是    http://www.xxx.com/callback    商户服务器用来接收异步通知的http地址
    private static final String reqTime = "reqTime";     //下单时间      是    20190601050225    满足格式yyyyMMddHHmmss的下单时间
    private static final String bankCode = "bankCode";    //银行代码      否    12345    填写请按照附录 或为空（空串，不可null）

    private static final String key = "key";
    //signature    数据签名    32    是    　
    private static final String signature = "sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merchNo, channelWrapper.getAPI_MEMBERID());
                put(orderNo, channelWrapper.getAPI_ORDER_ID());
                put(amount, channelWrapper.getAPI_AMOUNT());
                put(currency, "CNY");
                put(outChannel, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(product, channelWrapper.getAPI_ORDER_ID());
                put(memo, channelWrapper.getAPI_ORDER_ID());
                put(returnUrl, channelWrapper.getAPI_WEB_URL());
                put(notifyUrl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(reqTime, DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
                put(bankCode, "");
            }
        };
        log.debug("[老旺支付]-[请求支付]-1.组装请求参数完成：{}", JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
        api_response_params.put(key, channelWrapper.getAPI_KEY());
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!signature.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        //删除最后一个字符
        signSrc.deleteCharAt(signSrc.length() - 1);
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[老旺支付]-[请求支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);

        HashMap<String, String> result = Maps.newHashMap();
        try {

            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam).toString());

        } catch (Exception e) {
            log.error("[老旺支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null", e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[老旺支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));
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
        log.debug("[老旺支付]-[请求支付]-4.处理请求响应成功：{}", JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}