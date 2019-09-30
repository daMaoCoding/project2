package dc.pay.business.xinluzhifu;

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

@RequestPayHandler("XINLUZHIFU")
public final class XinLuZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XinLuZhiFuPayRequestHandler.class);

    private static final String    userId = "userId";  //	商户号
    private static final String    orderNo = "orderNo";  //	商户交易号
    private static final String    tradeType = "tradeType";  //	交易类型
    private static final String    payAmt = "payAmt";  //	支付金额	单位：元
    private static final String    bankId = "bankId";  //	银行id
    private static final String    goodsName = "goodsName";  //	商品名称
    private static final String    returnUrl = "returnUrl";  //	同步通知地址
    private static final String    notifyUrl = "notifyUrl";  // 异步    回调
    private static final String    sign = "sign";  //	签名字符串
    private static final String    goodsDesc = "goodsDesc";


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(userId,channelWrapper.getAPI_MEMBERID());
            payParam.put(orderNo,channelWrapper.getAPI_ORDER_ID());

            if(HandlerUtil.isWY(channelWrapper)){
                payParam.put(tradeType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
                payParam.put(bankId,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
            }else{
                payParam.put(tradeType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            }
            payParam.put(payAmt,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(goodsName,channelWrapper.getAPI_ORDER_ID());
            payParam.put(returnUrl,channelWrapper.getAPI_WEB_URL());
            payParam.put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
        }

        log.debug("[新陆支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if(StringUtils.isBlank(params.get(paramKeys.get(i))) || sign.equalsIgnoreCase(paramKeys.get(i).toString())
                    || bankId.equalsIgnoreCase(paramKeys.get(i).toString())
                    || goodsName.equalsIgnoreCase(paramKeys.get(i).toString())
                    || goodsDesc.equalsIgnoreCase(paramKeys.get(i).toString()))  //
                continue;
            sb.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i))).append("&");
        }
        sb.append("key=" + channelWrapper.getAPI_KEY());
        String signStr = sb.toString(); //.replaceFirst("&key=","")
        pay_md5sign = HandlerUtil.getMD5UpperCase(signStr).toLowerCase();
        log.debug("[新陆支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (1==2 && HandlerUtil.isWY(channelWrapper) &&  HandlerUtil.isYLKJ(channelWrapper)  &&  HandlerUtil.isWapOrApp(channelWrapper)    ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='post'"));
                payResultList.add(result);
            }else{

                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
				
				if(StringUtils.isNotBlank(resultStr) && resultStr.contains("<form") && !resultStr.contains("{")){
                    result.put(HTMLCONTEXT,resultStr);
                    payResultList.add(result);
                }else{
				
                   JSONObject jsonResultStr = JSON.parseObject(resultStr);
                    if(null!=jsonResultStr && jsonResultStr.containsKey("retCode") && "0".equalsIgnoreCase(jsonResultStr.getString("retCode")) && jsonResultStr.containsKey("payUrl")){
                            if(StringUtils.isNotBlank(jsonResultStr.getString("payUrl"))){
                                if(HandlerUtil.isWY(channelWrapper) ||  HandlerUtil.isYLKJ(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper)  ){
                                     result.put(HTMLCONTEXT,  jsonResultStr.getString("payUrl"));

                                }else{
                                    result.put(QRCONTEXT, jsonResultStr.getString("payUrl"));
                                }
                                payResultList.add(result);
                            }
                    }else {
                        throw new PayException(resultStr);
                    }
				}
                 
            }
        } catch (Exception e) { 
             log.error("[新陆支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[新陆支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[新陆支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}