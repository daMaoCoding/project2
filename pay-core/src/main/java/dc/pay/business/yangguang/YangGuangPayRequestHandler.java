package dc.pay.business.yangguang;

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
import dc.pay.utils.*;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestPayHandler("YANGGUANGZHIFU")
public final class YangGuangPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(YangGuangPayRequestHandler.class);

    private static final String     merchId  = "merchId";//	int	Y
    private static final String     merchOrderId  = "merchOrderId";//	string(50)	Y
    private static final String     time  = "time";//	string(14)	Y
    private static final String     product  = "product";//	string(50)	Y
    private static final String     amount  = "amount";//	int	Y
    private static final String     notifyUrl  = "notifyUrl";//	string(150)	Y
    private static final String     type  = "type";//	string
    private static final String     cip  = "cip";//	string(20)	N  下单用户IP地址(建议传，微信H5必传)
    private static final String     sign  = "sign";//


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
            payParam.put(merchId,channelWrapper.getAPI_MEMBERID());
            payParam.put(merchOrderId,channelWrapper.getAPI_ORDER_ID());
            payParam.put(time,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
            payParam.put(product,channelWrapper.getAPI_ORDER_ID());
            payParam.put(amount,channelWrapper.getAPI_AMOUNT());
            payParam.put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(cip,channelWrapper.getAPI_Client_IP());
        log.debug("[阳光支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        //merchId+merchSecret+ merchOrderId+time+product+amount+notifyUrl+type
        String paramsStr = String.format("%s%s%s%s%s%s%s%s",
                params.get(merchId),
                channelWrapper.getAPI_KEY(),
                params.get(merchOrderId),
                params.get(time),
                params.get(product),
                params.get(amount),
                params.get(notifyUrl),
                params.get(type)
        );
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[阳光支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (1==2 && HandlerUtil.isWY(channelWrapper) && HandlerUtil.isYLKJ(channelWrapper)  && HandlerUtil.isWapOrApp(channelWrapper)    ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
                payResultList.add(result);
            }else{
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.GET).trim();
                JSONObject jsonResultStr = JSON.parseObject(resultStr);
          
                    if(null!=jsonResultStr && jsonResultStr.containsKey("code") && "200".equalsIgnoreCase(jsonResultStr.getString("code")) && jsonResultStr.containsKey("payUrl")){
                            if(StringUtils.isNotBlank(jsonResultStr.getString("payUrl"))){
                                if(HandlerUtil.isWapOrApp(channelWrapper) ){
                                    result.put(JUMPURL,  jsonResultStr.getString("payUrl"));
                                }else{
                                    result.put(QRCONTEXT,  jsonResultStr.getString("payUrl"));
                                }
                                payResultList.add(result);
                            }
                    }else {
                        throw new PayException(resultStr);
                    }

                 
            }
        } catch (Exception e) { 
             log.error("[阳光支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[阳光支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[阳光支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}