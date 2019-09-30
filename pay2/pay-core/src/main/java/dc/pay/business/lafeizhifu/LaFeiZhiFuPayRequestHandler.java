package dc.pay.business.lafeizhifu;

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
import dc.pay.utils.MapUtils;
import dc.pay.utils.ValidateUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Cobby
 * July 17, 2019
 */
@RequestPayHandler("LAFEIZHIFU")
public final class LaFeiZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(LaFeiZhiFuPayRequestHandler.class);

    private static final String merId     = "merId";     //    商户编号   商户在支付平台中的唯一标识    是
    private static final String orderNo   = "orderNo";   //    商户订单号 商户业务系统中重生的订单号，同一个商户订单号不可重复，长度不超过50个字符    是
    private static final String amount    = "amount";    //    订单金额   单位元，保留两位小数，格式如1.00    是
    private static final String payType   = "payType";   //    支付类型   详情见 7、支付方式编码列表    是
    private static final String goods     = "goods";     //    商品名称   长度不超过20个字符，中文或特殊字符请utf-8编码    是
    private static final String notifyUrl = "notifyUrl"; //    通知url    异步通知，特殊字符请utf-8编码    是
    //  private static final String returnUrl           ="returnUrl"; //    页面回调url        否
//  private static final String payBank             ="payBank";   //    付款银行        否
//  private static final String memo                ="memo";      //    扩展信息   该字段内容在交易完成后，原样返回给商户，中文或特殊字符请utf-8编码    否
    //signature    数据签名    32    是    　
    private static final String signature = "sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merId, channelWrapper.getAPI_MEMBERID());
                put(orderNo, channelWrapper.getAPI_ORDER_ID());
                put(amount, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(payType, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(goods, "name");
                put(notifyUrl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            }
        };
        log.debug("[拉斐支付]-[请求支付]-1.组装请求参数完成：{}", JSON.toJSONString(payParam));
        return payParam;
    }


    @Override
    protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
        List          paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc   = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!signature.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(api_response_params.get(paramKeys.get(i)));
            }
        }
        String paramsStr = signSrc.toString();
        String signMd5   = "";
        try {
            signMd5 = RSAUtil.signByPrivate(paramsStr, channelWrapper.getAPI_KEY(), "UTF-8");    // 签名
        } catch (Exception e) {
            log.error("[拉斐支付]-[请求支付]-2.生成加密URL签名出错，签名出错：{}", e.getMessage(), e);
        }
        log.debug("[拉斐支付]-[请求支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);

        HashMap<String, String> result = Maps.newHashMap();
        try {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam).toString());

        } catch (Exception e) {
            log.error("[拉斐支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null", e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[拉斐支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));
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
        log.debug("[拉斐支付]-[请求支付]-4.处理请求响应成功：{}", JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}