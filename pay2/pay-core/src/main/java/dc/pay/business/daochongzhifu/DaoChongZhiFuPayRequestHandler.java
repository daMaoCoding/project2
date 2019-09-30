package dc.pay.business.daochongzhifu;

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
import dc.pay.utils.ValidateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


/**
 * @author Cobby
 * July 26, 2019
 */
@RequestPayHandler("DAOCHONGZHIFU")
public final class DaoChongZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(DaoChongZhiFuPayRequestHandler.class);

    private static final String pay_memberid    = "pay_memberid";    // 商户号    是    是    平台分配商户号
    private static final String pay_orderid     = "pay_orderid";     // 订单号    是    是    上送订单号唯一, 字符长度20
    private static final String pay_applydate   = "pay_applydate";   // 提交时间    是    是    时间格式：2016-12-26 18:18:18
    private static final String pay_service     = "pay_service";     // 支付类型    是    是    支付类型参看 附1
    private static final String pay_notifyurl   = "pay_notifyurl";   // 服务端通知    是    是    服务端返回地址.（POST返回数据）
    private static final String pay_callbackurl = "pay_callbackurl"; // 页面跳转通知    是    是    页面跳转返回地址（POST返回数据）
    private static final String pay_amount      = "pay_amount";      // 订单金额    是    是    商品金额
    private static final String pay_clientIp    = "pay_clientIp";    // 客户IP    是    否    个必填 客户端真实IP


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(pay_memberid, channelWrapper.getAPI_MEMBERID());
                put(pay_orderid, channelWrapper.getAPI_ORDER_ID());
                put(pay_applydate, DateUtil.formatDateTimeStrByParam("yyyy-MM-dd HH:mm:ss"));
                put(pay_service, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(pay_notifyurl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(pay_callbackurl, channelWrapper.getAPI_WEB_URL());
                put(pay_amount, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(pay_clientIp, channelWrapper.getAPI_Client_IP());
            }
        };
        log.debug("[道冲支付]-[请求支付]-1.组装请求参数完成：{}", JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
//        pay_amount=&pay_applydate=&pay_callbackurl=&pay_memberid=&pay_notifyurl= &pay_orderid= &pay_service=&key=
        String paramsStr = String.format("pay_amount=%s&pay_applydate=%s&pay_callbackurl=%s&pay_memberid=%s&pay_notifyurl=%s&pay_orderid=%s&pay_service=%s&key=%s",
                api_response_params.get(pay_amount),
                api_response_params.get(pay_applydate),
                api_response_params.get(pay_callbackurl),
                api_response_params.get(pay_memberid),
                api_response_params.get(pay_notifyurl),
                api_response_params.get(pay_orderid),
                api_response_params.get(pay_service),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[道冲支付]-[请求支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        try {

            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam).toString().replace("method='post'", "method='post'"));
        } catch (Exception e) {
            log.error("[道冲支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null", e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[道冲支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));
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
        log.debug("[道冲支付]-[请求支付]-4.处理请求响应成功：{}", JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}