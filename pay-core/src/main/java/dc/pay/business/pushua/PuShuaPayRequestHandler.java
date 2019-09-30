package dc.pay.business.pushua;

/**
 * ************************
 * @author tony 3556239829
 */

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
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
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestPayHandler("PUSHUAZHIFU")
public final class PuShuaPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(PuShuaPayRequestHandler.class);

    private static final String      version	 ="version";   //版本号，固定值1.0	是
    private static final String      merchantNo	 ="merchantNo";   //商户号（接入时下发,或通过接口进件获得）	是
    private static final String      paytype	 ="paytype";   //支付类型，取值编码及对应描述如下：
    private static final String      outTradeNo	 ="outTradeNo";   //接入方平台流水号（不超过30个字符）
    private static final String      amt	 ="amt";   //交易金额(单位：元)
    private static final String      retUrl	 ="retUrl";   //异步通知地址
    private static final String      callType	 ="callType";   //调用方式，建议填写固定值“url”；    当值为url时会返回支付链接，否则返回页面；
    private static final String      requestIp	 ="requestIp";   //付款客户端的ip
    private static final String      sign	 ="sign";


    private static final String       transAmount = "transAmount";//	金额（以元为单位，如：10.00表示为10元）
    private static final String       transType = "transType";//	扫码类型(“10000001”，“10000003” ，“10000004”)
    private static final String       notify_url = "notify_url";//	支付回调地址，若为空，则通知机构配置的地址，




    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(HandlerUtil.isWapOrApp(channelWrapper)){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(version,"1.0");
            payParam.put(merchantNo,channelWrapper.getAPI_MEMBERID());
            payParam.put(paytype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(outTradeNo,channelWrapper.getAPI_ORDER_ID());
            payParam.put(amt,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(retUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(callType,"url");
            payParam.put(requestIp,channelWrapper.getAPI_Client_IP());
        }else{
            payParam.put(merchantNo,channelWrapper.getAPI_MEMBERID());
            payParam.put(transAmount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(outTradeNo,channelWrapper.getAPI_ORDER_ID());
            payParam.put(transType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(notify_url, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
        }
        log.debug("[浦刷支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String pay_md5sign = null;
        if(HandlerUtil.isWapOrApp(channelWrapper)){
            List paramKeys = MapUtils.sortMapByKeyAsc(params);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < paramKeys.size(); i++) {
                if(StringUtils.isBlank(params.get(paramKeys.get(i))) || sign.equalsIgnoreCase(paramKeys.get(i).toString()    ))  //
                    continue;
                sb.append(params.get(paramKeys.get(i)));
            }
            sb.append(channelWrapper.getAPI_KEY());
            String signStr = sb.toString(); //.replaceFirst("&key=","")
            pay_md5sign = HandlerUtil.getMD5UpperCase(signStr).toLowerCase();
            log.debug("[浦刷支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
            return pay_md5sign;
        }else{
            //merchantNo+transAmount+outTradeNo+transType+key
            String paramsStr = String.format("%s%s%s%s%s",
                    params.get(merchantNo),
                    params.get(transAmount),
                    params.get(outTradeNo),
                    params.get(transType),
                    channelWrapper.getAPI_KEY());
            pay_md5sign = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
            log.debug("[浦刷支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
            return pay_md5sign;
        }

    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if ( 1==2 && HandlerUtil.isWY(channelWrapper) && HandlerUtil.isYLKJ(channelWrapper)  &&  HandlerUtil.isWapOrApp(channelWrapper)    ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
                payResultList.add(result);
            }else{
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.GET).trim();
                JSONObject jsonResultStr = JSON.parseObject(resultStr);
          
                    if(null!=jsonResultStr && jsonResultStr.containsKey("respCode") && ("0000".equalsIgnoreCase(jsonResultStr.getString("respCode")) || "00".equalsIgnoreCase(jsonResultStr.getString("respCode")) )  && ( jsonResultStr.containsKey("payUrl") || jsonResultStr.containsKey("codeurl") ) ){
                            if(StringUtils.isNotBlank(jsonResultStr.getString("payUrl"))){
                                if(HandlerUtil.isWapOrApp(channelWrapper)){
                                    result.put(JUMPURL, jsonResultStr.getString("payUrl"));
                                }else{
                                    result.put(QRCONTEXT, jsonResultStr.getString("payUrl"));
                                }
                                payResultList.add(result);
                            }else if(StringUtils.isNotBlank(jsonResultStr.getString("codeurl"))){
                                if(HandlerUtil.isWapOrApp(channelWrapper)){
                                    result.put(JUMPURL, jsonResultStr.getString("codeurl"));
                                }else{
                                    result.put(QRCONTEXT, HandlerUtil.UrlDecode(jsonResultStr.getString("codeurl")));
                                }
                                payResultList.add(result);
                            }
                    }else {
                        throw new PayException(resultStr);
                    }
            }
        } catch (Exception e) { 
             log.error("[浦刷支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[浦刷支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
        return payResultList;
    }


    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (null != resultListMap && !resultListMap.isEmpty()) {
            if (resultListMap.size() == 1) {
                Map<String, String> resultMap = resultListMap.get(0);
                requestPayResult = buildResult(resultMap,channelWrapper,requestPayResult);
            }
            if (ValidateUtil.requestesultValdata(requestPayResult)) {
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            } else {
                throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[浦刷支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}