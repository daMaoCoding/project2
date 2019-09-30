package dc.pay.business.kuaifubaozhifu;

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
 * July 19, 2019
 */
@RequestPayHandler("KUAIFUBAOZHIFU")
public final class KuaiFuBaoZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(KuaiFuBaoZhiFuPayRequestHandler.class);


    private static final String uid        = "uid";       //    商户uid        必填
    private static final String price      = "price";     //    价格           必填。单位：元。精确小数点后2位
    private static final String istype     = "istype";    //    支付渠道        必填。1：支付宝；2：微信支付
    private static final String notify_url = "notify_url";//    通知回调网址     必填。
    private static final String return_url = "return_url";//    跳转网址        必填。
    private static final String orderid    = "orderid";   //    商户自定义订单号  必填。
    private static final String orderuid   = "orderuid";  //    商户自定义客户号  选填
    private static final String goodsname  = "goodsname"; //    商品名称        选填。

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(uid, channelWrapper.getAPI_MEMBERID());
                put(price, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(istype, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(notify_url, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(return_url, channelWrapper.getAPI_WEB_URL());
                put(orderid, channelWrapper.getAPI_ORDER_ID());
                put(goodsname, "name");
                put(orderuid, channelWrapper.getAPI_ORDER_ID());
            }
        };
        log.debug("[快付宝支付]-[请求支付]-1.组装请求参数完成：{}", JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
        //做md5-32位加密，取字符串小写。注意字串之间要添加一个“+”字符
//        goodsname + istype + notify_url + orderid + orderuid + price + return_url + token + uid
        String paramsStr = String.format("%s%s%s%s%s%s%s%s%s",
                api_response_params.get(goodsname),
                api_response_params.get(istype),
                api_response_params.get(notify_url),
                api_response_params.get(orderid),
                api_response_params.get(orderuid),
                api_response_params.get(price),
                api_response_params.get(return_url),
                channelWrapper.getAPI_KEY(),
                api_response_params.get(uid));
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[快付宝支付]-[请求支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);

        HashMap<String, String> result = Maps.newHashMap();
        try {

            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam).toString());

        } catch (Exception e) {
            log.error("[快付宝支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null", e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[快付宝支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));
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
        log.debug("[快付宝支付]-[请求支付]-4.处理请求响应成功：{}", JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}