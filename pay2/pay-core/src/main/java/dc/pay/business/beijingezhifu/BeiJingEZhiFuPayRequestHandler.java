package dc.pay.business.beijingezhifu;

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
 * June 14, 2018
 */
@RequestPayHandler("BEIJINGEZHIFU")
public final class BeiJingEZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(BeiJingEZhiFuPayRequestHandler.class);

    private static final String partner     = "partner";    //  是     string     商户号
    private static final String banktype    = "banktype";   //  是     string     支付类型（ALIPAY=支付宝扫码，ALIPAYWAP=支付宝WAP，BANKPAY=网银支付，EXPRESS=网银快捷，QQPAY=QQ扫码，JDPAY=京东扫码，WEIXINWAP=微信WAP，QQWAP=QQWAP，BANKSCAN=银联扫码，WEIXIN=微信扫码）
    private static final String paymoney    = "paymoney";   //  是     string     金额（元）
    private static final String ordernumber = "ordernumber";//  是     string     订单号
    private static final String callbackurl = "callbackurl";//  是     string     异步回调地址

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(partner, channelWrapper.getAPI_MEMBERID());
                put(banktype, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(paymoney, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(ordernumber, channelWrapper.getAPI_ORDER_ID());
                put(callbackurl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            }
        };
        log.debug("[北京E支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
         //partner=1&banktype=EXPRESS&paymoney=10&ordernumber=20180926155113&callbackurl=https://www.baidu.com
         String paramsStr = String.format("partner=%s&banktype=%s&paymoney=%s&ordernumber=%s&callbackurl=%s%s",
                 api_response_params.get(partner),
                 api_response_params.get(banktype),
                 api_response_params.get(paymoney),
                 api_response_params.get(ordernumber),
                 api_response_params.get(callbackurl),
                 channelWrapper.getAPI_KEY());
         String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[北京E支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return signMD5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        try {

            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam).toString().replace("method='post'", "method='get'"));

        } catch (Exception e) {
            log.error("[北京E支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null", e);
        }
//            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,null);
//            if (StringUtils.isBlank(resultStr)) {
//                log.error("[北京E支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(EMPTYRESPONSE);
//            }
//            JSONObject resJson;
//            try {
//                resJson = JSONObject.parseObject(resultStr);
//            } catch (Exception e) {
//                e.printStackTrace();
//                log.error("[北京E支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//            }
//            if (null != resJson && resJson.containsKey("errCode") && resJson.getString("errCode").equals("0000")) {
//                if(StringUtils.isNotBlank(resJson.getString("qrCode"))){
//                    String code_url = resJson.getString("qrCode");
//                    result.put(JUMPURL, code_url);
//                }else{
//                    String code_url = resJson.getString("retHtml");
//                    result.put(JUMPURL, code_url);
//                }
//            }else {
//                log.error("[北京E支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//            }
        payResultList.add(result);
        log.debug("[北京E支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
        return payResultList;
    }

    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (null != resultListMap && !resultListMap.isEmpty()) {
            if (resultListMap.size() == 1) {
                Map<String, String> resultMap = resultListMap.get(0);
                requestPayResult=  buildResult(resultMap, channelWrapper,requestPayResult);
            }
            if (ValidateUtil.requestesultValdata(requestPayResult)) {
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            } else {
                throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[北京E支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}