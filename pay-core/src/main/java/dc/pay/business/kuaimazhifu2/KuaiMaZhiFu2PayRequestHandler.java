package dc.pay.business.kuaimazhifu2;

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
 * July 06, 2019
 */
@RequestPayHandler("KUAIMAZHIFU2")
public final class KuaiMaZhiFu2PayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(KuaiMaZhiFu2PayRequestHandler.class);

    private static final String m_id         = "m_id";        //    是    Int 10    商户ID、在商户后台首页可以看到获取商户的ID    10000
    private static final String pay_aisle    = "pay_aisle";   //    是    Int 10    通道开通需要跟客服确认开启哪些通道和确定费率，    目前可用通道类型
    private static final String out_trade_no = "out_trade_no";//    是    String 255    商户订单号，在您的平台生成的订单号    2018062668945
    private static final String amount       = "amount";      //    是    Float    单位(元)    支付金额，必须保留2位小数点    1.00
    private static final String callback_url = "callback_url";//    是    String 255    异步通知地址，服务器异步通知
    private static final String success_url  = "success_url"; //    是    String 255    同步通知地址，支付成功后网页自动跳转返回的地址


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(m_id, channelWrapper.getAPI_MEMBERID());
                put(pay_aisle, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(out_trade_no, channelWrapper.getAPI_ORDER_ID());
                put(amount, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(callback_url, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(success_url, channelWrapper.getAPI_WEB_URL());
            }
        };
        log.debug("[快马支付2]-[请求支付]-1.组装请求参数完成：{}", JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
        //m_id=xxxpay_aisle=xxx=out_trade_no=xxx=amount=xxx=callback_url=xxx=success_ur=xxx
        StringBuffer signSrc = new StringBuffer();
        signSrc.append(m_id + "=").append(api_response_params.get(m_id));
        signSrc.append(pay_aisle + "=").append(api_response_params.get(pay_aisle));
        signSrc.append(out_trade_no + "=").append(api_response_params.get(out_trade_no));
        signSrc.append(amount + "=").append(api_response_params.get(amount));
        signSrc.append(callback_url + "=").append(api_response_params.get(callback_url));
        signSrc.append(success_url + "=").append(api_response_params.get(success_url));
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5   = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[快马支付2]-[请求支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        try {

            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam).toString());

        } catch (Exception e) {
            log.error("[快马支付2]-[请求支付]-3.1.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null", e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[快马支付2]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));
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
        log.debug("[快马支付2]-[请求支付]-4.处理请求响应成功：{}", JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}