package dc.pay.business.yuandazhifu;

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

@RequestPayHandler("YUANDAZHIFU")
public final class YuanDaZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(YuanDaZhiFuPayRequestHandler.class);

    private static final String     	outOid = "outOid"; //	           商户订单号     String
    private static final String     	merchantCode = "merchantCode"; //	           平台商户编号     String
    private static final String     	payType = "payType"; //           支付类型     。
    private static final String     	payAmount = "payAmount"; //	           支付金额     String	必填
    private static final String     	goodName = "goodName"; //	           商品名称     String	必填
    private static final String     	notifyUrl = "notifyUrl"; //           异步通知地址
    private static final String     	sign = "sign"; //           签名


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(outOid,channelWrapper.getAPI_ORDER_ID());
            payParam.put(merchantCode,channelWrapper.getAPI_MEMBERID());
            payParam.put(payType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(payAmount,channelWrapper.getAPI_AMOUNT());
            payParam.put(goodName,channelWrapper.getAPI_ORDER_ID());
            payParam.put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
        }
        log.debug("[远大支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if(StringUtils.isBlank(params.get(paramKeys.get(i))) || sign.equalsIgnoreCase(paramKeys.get(i).toString())   || notifyUrl.equalsIgnoreCase(paramKeys.get(i).toString())   )  //
                continue;
            sb.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i))).append("&");
        }
        sb.append("key=" + channelWrapper.getAPI_KEY());
        String signStr = sb.toString(); //.replaceFirst("&key=","")
        pay_md5sign = HandlerUtil.getMD5UpperCase(signStr);
        log.debug("[远大支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if ( 1==2 && HandlerUtil.isWY(channelWrapper) &&  HandlerUtil.isYLKJ(channelWrapper)  &&  HandlerUtil.isWapOrApp(channelWrapper)    ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='post'"));
                payResultList.add(result);
            }else{
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
                JSONObject jsonResultStr = JSON.parseObject(resultStr);
          
                    if(null!=jsonResultStr && jsonResultStr.containsKey("code") && "000000".equalsIgnoreCase(jsonResultStr.getString("code")) && jsonResultStr.containsKey("value")){
                        JSONObject value = jsonResultStr.getJSONObject("value");
                        if(null!=value && StringUtils.isNotBlank(value.getString("payUrl")) && !ValidateUtil.isHaveChinese(value.getString("payUrl")) ){
                                result.put(QRCONTEXT,  value.getString("payUrl"));
                                payResultList.add(result);
                          }else{throw new PayException(resultStr);}

                    }else {
                        throw new PayException(resultStr);
                    }

                 
            }
        } catch (Exception e) { 
             log.error("[远大支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[远大支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[远大支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}