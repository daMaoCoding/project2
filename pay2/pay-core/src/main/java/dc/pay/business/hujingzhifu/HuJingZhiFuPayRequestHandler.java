package dc.pay.business.hujingzhifu;

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

import java.security.SignatureException;
import java.util.*;

/**
 * @author Cobby
 * June 18, 2019
 */
@RequestPayHandler("HUJINGZHIFU")
public final class HuJingZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(HuJingZhiFuPayRequestHandler.class);

    private static final String merchantId      = "merchantId";      // 商户id
    private static final String merchantOrderId = "merchantOrderId"; // 商户订单id
    private static final String merchantUid     = "merchantUid";     // 商户玩家id
    private static final String money           = "money";           // 充值金额
    private static final String payType         = "payType";         // 支付方式 --（ alipay_h5 - 支付宝  全国网银通道（云闪付）-unionpay）
    private static final String timestamp       = "timestamp";       // 时间戳
    private static final String goodsName       = "goodsName";       // 商品名称
    private static final String notifyUrl       = "notifyUrl";       // 回调地址
    private static final String resultFormat    = "resultFormat";    // form 默认值


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merchantId, channelWrapper.getAPI_MEMBERID());
                put(merchantOrderId, channelWrapper.getAPI_ORDER_ID());
                put(merchantUid, channelWrapper.getAPI_ORDER_ID());
                put(money, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(payType, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(timestamp, System.currentTimeMillis() / 1000 + "");
                put(goodsName, channelWrapper.getAPI_ORDER_ID());
                put(notifyUrl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(resultFormat, "form");
            }
        };
        log.debug("[虎鲸支付]-[请求支付]-1.组装请求参数完成：{}", JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
        //验签字符串例子：
        //    merchantId=1000243&merchantOrderId=1901201354290000462772320&merchantUid=142&money=500.00
        // &notifyUrl=http://t964&payType=alipay_h5&timestamp=1547963669
        StringBuffer signSrc = new StringBuffer();
        signSrc.append(merchantId + "=").append(api_response_params.get(merchantId)).append("&");
        signSrc.append(merchantOrderId + "=").append(api_response_params.get(merchantOrderId)).append("&");
        signSrc.append(merchantUid + "=").append(api_response_params.get(merchantUid)).append("&");
        signSrc.append(money + "=").append(api_response_params.get(money)).append("&");
        signSrc.append(notifyUrl + "=").append(api_response_params.get(notifyUrl)).append("&");
        signSrc.append(payType + "=").append(api_response_params.get(payType)).append("&");
        signSrc.append(timestamp + "=").append(api_response_params.get(timestamp));
        String paramsStr = signSrc.toString();
        String signMd5 = null;
        try {
            signMd5 = RSAUtil.rsaSign(channelWrapper.getAPI_KEY(), paramsStr);
        } catch (SignatureException e) {
            e.printStackTrace();
        }
        log.debug("[虎鲸支付]-[请求支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);

        HashMap<String, String> result = Maps.newHashMap();
        try {

            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam).toString());

        } catch (Exception e) {
            log.error("[虎鲸支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null", e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[虎鲸支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));
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
        log.debug("[虎鲸支付]-[请求支付]-4.处理请求响应成功：{}", JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}