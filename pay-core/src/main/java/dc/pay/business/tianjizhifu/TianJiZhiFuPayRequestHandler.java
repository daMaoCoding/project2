package dc.pay.business.tianjizhifu;

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
import dc.pay.utils.qr.QRCodeUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestPayHandler("TIANJIZHIFU")
public final class TianJiZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(TianJiZhiFuPayRequestHandler.class);


     private static final String   	payTypeKey = "payTypeKey";   //	         支付类型      Y	String(32)	pay.wx.h5
     private static final String   	tradeNo = "tradeNo";   //	         商户号      Y	String(32)	商户号
     private static final String   	outTradeNo = "outTradeNo";   //	         商户订单号      Y	String(32)	商户系统内部的订单号 ,32个字符内、 可包含字母,确保在商户系统唯一
     private static final String   	body = "body";   //	         商品描述      Y	String(127)	商品描述
     private static final String   	totalFee = "totalFee";   //	         总金额      Y	Int	分为单位，支付金额只能以下几种:5000,10000,20000,30000,40000,50000，具体以接口返回为准
     private static final String   	requestIp = "requestIp";   //	         终端IP      Y	String(16)	用户IP
     private static final String   	nonceStr = "nonceStr";   //	         随机字符串      Y	String(32)	随机字符串，不长于 32 位
     private static final String   	payIdentity = "payIdentity";   //	         支付用户标识      Y	String(32)	如支付用户的手机、用户Id等只要是用户唯一标识都可以
     private static final String   	notifyUrl = "notifyUrl";   //	         异步通知地址      Y	String(32)	异步通知地址
     private static final String   	sign = "sign";   //	         签名      Y	String(32)	MD5签名结果，详见“安全规范”

     private static final String   	mark = "mark";



    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(payTypeKey,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(tradeNo,channelWrapper.getAPI_MEMBERID());
            payParam.put(outTradeNo,channelWrapper.getAPI_ORDER_ID());
            payParam.put(body,channelWrapper.getAPI_ORDER_ID());
            payParam.put(totalFee,channelWrapper.getAPI_AMOUNT());
            payParam.put(requestIp,channelWrapper.getAPI_Client_IP());
            payParam.put(nonceStr,HandlerUtil.getRandomStr(10) );
            payParam.put(payIdentity,HandlerUtil.getRandomNumber(10) );
            payParam.put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL() );
        }
        log.debug("[天际支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }


    protected String buildPaySign(Map<String,String> params) throws PayException {
        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
              if(StringUtils.isBlank(params.get(paramKeys.get(i))) || sign.equalsIgnoreCase(paramKeys.get(i).toString()) ||  mark.equalsIgnoreCase(paramKeys.get(i).toString())   )  //
                continue;
            sb.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i))).append("&");
        }
        sb.append("key=" + channelWrapper.getAPI_KEY());
        String signStr = sb.toString(); //.replaceFirst("&key=","")
        pay_md5sign = HandlerUtil.getMD5UpperCase(signStr);
        log.debug("[天际支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
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
                resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
				if(StringUtils.isNotBlank(resultStr) && resultStr.contains("<form") && !resultStr.contains("{")){
                    result.put(HTMLCONTEXT,resultStr);
                    payResultList.add(result);
                }else if(StringUtils.isNotBlank(resultStr) ){
                    JSONObject jsonResultStr = JSON.parseObject(resultStr);
                    if(null!=jsonResultStr && jsonResultStr.containsKey("returnCode") && "SUCCESS".equalsIgnoreCase(jsonResultStr.getString("returnCode")) ){
                        if(jsonResultStr.containsKey("redirectUrl") && StringUtils.isNotBlank(jsonResultStr.getString("redirectUrl")) ){
                            result.put(JUMPURL, jsonResultStr.getString("redirectUrl"));
                            payResultList.add(result);
                        }else if( jsonResultStr.containsKey("codeUrl") && StringUtils.isNotBlank(jsonResultStr.getString("codeUrl"))){
                            result.put(QRCONTEXT, QRCodeUtil.decodeByUrl(jsonResultStr.getString("codeUrl")));
                            payResultList.add(result);
                        }else {throw new PayException(resultStr); }

                    }else {throw new PayException(resultStr); }
				}else{ throw new PayException(EMPTYRESPONSE);}
            }
        } catch (Exception e) { 
             log.error("[天际支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[天际支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[天际支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}