package dc.pay.business.rongyaokejizhifu;


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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
/**
 * @author Cobby
 * June 14, 2018
 */
@RequestPayHandler("RONGYAOKEJIZHIFU")
public final class RongYaoKeJiZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(RongYaoKeJiZhiFuPayRequestHandler.class);

     private static final String    partner       = "partner";     //     ID
     private static final String    banktype      = "banktype";    //     类型
     private static final String    paymoney      = "paymoney";    //     金额
     private static final String    ordernumber   = "ordernumber"; //     商户订单号
     private static final String    callbackurl   = "callbackurl"; //     下行异步通知地址
     private static final String    hrefbackurl   = "hrefbackurl"; //     下行同步通知地址
//   private static final String    bankcode      = "bankcode";


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){ 
            payParam.put(partner,channelWrapper.getAPI_MEMBERID());
            payParam.put(banktype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(paymoney,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));  //其他的有0
            payParam.put(ordernumber,channelWrapper.getAPI_ORDER_ID());
            payParam.put(callbackurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(hrefbackurl,channelWrapper.getAPI_WEB_URL());
        }
        log.debug("[荣耀科技支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> params) throws PayException {
        //partner={}&banktype={}&paymoney={}&ordernumber={}&callbackurl={}key

        String paramsStr = String.format("partner=%s&banktype=%s&paymoney=%s&ordernumber=%s&callbackurl=%s%s",
                params.get(partner),
                params.get(banktype),
                params.get(paymoney),
                params.get(ordernumber),
                params.get(callbackurl),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[荣耀科技支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        try {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
                payResultList.add(result);

        } catch (Exception e) {
             log.error("[荣耀科技支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[荣耀科技支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[荣耀科技支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}