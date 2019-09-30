package dc.pay.business.heibaozhifu;

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
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestPayHandler("HEIBAOZHIFU")
public final class HeiBaoZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(HeiBaoZhiFuPayRequestHandler.class);

     private static final String  appId="appId"; //	商户号	30	是	是	商户签约时，本系统分配给商家的唯一标识。
     private static final String  money="money"; //	交易金额	5	是	是	请参考下单金额限制规则
     private static final String  payType="payType"; //	支付方式	10	是	是	Ali 支付宝    QuickPayment 快捷支付    OnlineBank 网银    QQ QQ支付    WeXin微信支付
     private static final String  orderNumber="orderNumber"; //	订单号	32	是	是	商户下单的订单编号,必须唯一,长度限制为 8-32
     private static final String  notifyUrl="notifyUrl"; //	异步地址	200	是	是	用户支付成功，则按照该值给商户发送异步通知。
     private static final String  returnUrl="returnUrl"; //	同步地址	200	是	是	商户网站自定义，用户支付完毕系统原样跳转回该地址。
     private static final String  remark="remark"; //	备注	50	否	否	商户下单备注(如下单有此值回调接口返回原值)
     private static final String  signature="signature"; //	数据签名	32	是		对签名数据进行MD5加密的结果。


    private static final String   success ="success"; //   true,
    private static final String   msg ="msg"; //   "下单成功",
    private static final String   data ="data"; //  "http://web.payhb.com/pay/order/pay/page?orderNumber=20180901132029",


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(appId,channelWrapper.getAPI_MEMBERID());
            payParam.put(money,HandlerUtil.getYuanWithoutZero(channelWrapper.getAPI_AMOUNT()));
            payParam.put(payType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(orderNumber,channelWrapper.getAPI_ORDER_ID());
            payParam.put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(returnUrl,channelWrapper.getAPI_WEB_URL());
        }

        log.debug("[黑豹支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String paramsStr = String.format("%s&%s&%s&%s&%s&%s&%s",
                params.get(appId),
                params.get(money),
                params.get(payType),
                params.get(orderNumber),
                params.get(notifyUrl),
                params.get(returnUrl),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[黑豹支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (1==2 && HandlerUtil.isWY(channelWrapper) && HandlerUtil.isYLKJ(channelWrapper)  &&  HandlerUtil.isWapOrApp(channelWrapper)    ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='post'"));
                payResultList.add(result);
            }else{
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
				
				if(StringUtils.isNotBlank(resultStr) && resultStr.contains("<form") && !resultStr.contains("{")){
                    result.put(HTMLCONTEXT,resultStr);
                    payResultList.add(result);
                }else{
				
                   JSONObject jsonResultStr = JSON.parseObject(resultStr);
                    if(null!=jsonResultStr && jsonResultStr.containsKey(success) && "true".equalsIgnoreCase(jsonResultStr.getString(success)) && jsonResultStr.containsKey(data)){
                            if(StringUtils.isNotBlank(jsonResultStr.getString(data))){
                               if(HandlerUtil.isWapOrApp(channelWrapper)){
                                   result.put(JUMPURL,  jsonResultStr.getString(data));
                               }else{
                                   result.put(QRCONTEXT,  jsonResultStr.getString(data));
                               }
                                payResultList.add(result);
                            }
                    }else {
                        throw new PayException(resultStr);
                    }
				}
                 
            }
        } catch (Exception e) { 
             log.error("[黑豹支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[黑豹支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[黑豹支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}