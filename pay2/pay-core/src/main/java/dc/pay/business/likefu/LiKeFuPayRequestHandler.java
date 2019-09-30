package dc.pay.business.likefu;

/**
 * ************************
 * @author tony 3556239829
 */

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
import dc.pay.utils.HttpUtil;
import dc.pay.utils.Result;
import dc.pay.utils.ValidateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@RequestPayHandler("LIKEFU")
public final class LiKeFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(LiKeFuPayRequestHandler.class);
    private static final String PARTNER = "partner";
    private static final String BANKTYPE = "banktype";
    private static final String PAYMONEY = "paymoney";
    private static final String ORDERNUMBER = "ordernumber";
    private static final String CALLBACKURL = "callbackurl";
    private static final String SIGN = "sign";
    private static final String QRCONTEXT = "QrContext";
    private static final String HTMLCONTEXT = "HtmlContext";
    private static final String PARSEHTML = "parseHtml";

    private static final String ISSHOW = "isshow";
    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(PARTNER, channelWrapper.getAPI_MEMBERID());
                put(BANKTYPE, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(PAYMONEY, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(ORDERNUMBER, channelWrapper.getAPI_ORDER_ID());
                put(CALLBACKURL, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(ISSHOW, "1");

            }
        };
        log.debug("[立刻付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }
    protected String buildPaySign(Map payParam) throws PayException {
        String paramsStr = String.format("partner=%s&banktype=%s&paymoney=%s&ordernumber=%s&callbackurl=%s%s",
                payParam.get(PARTNER),
                payParam.get(BANKTYPE),
                payParam.get(PAYMONEY),
                payParam.get(ORDERNUMBER),
                payParam.get(CALLBACKURL),
                channelWrapper.getAPI_KEY());
        String pay_md5sign = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[立刻付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        try {
            HashMap<String, String> result = Maps.newHashMap();
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replaceFirst("method='post'","method='get'"));
            payResultList.add(result);
        } catch (Exception e) {
            log.error("[立刻付]3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(e.getMessage(), e);
        }
        log.debug("[立刻付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
        return payResultList;
    }


    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (null != resultListMap && !resultListMap.isEmpty()) {
            if (resultListMap.size() == 1) {
                Map<String, String> resultMap = resultListMap.get(0);
                requestPayResult =  buildResult(resultMap,channelWrapper,requestPayResult);
            }
            if (ValidateUtil.requestesultValdata(requestPayResult)) {
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            } else {
                throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[立刻付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}