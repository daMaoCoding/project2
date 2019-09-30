package dc.pay.business.anyizhifu;

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

@RequestPayHandler("ANYIZHIFU")
public final class AnYiZhiFuPayRequestHandler extends PayRequestHandler {
     private static final Logger log = LoggerFactory.getLogger(AnYiZhiFuPayRequestHandler.class);

     private static final String       platSource	  = "platSource";         //              是	商户编号/平台来源
     private static final String       payType	  = "payType";              //              是	支付类型(当前仅支持alipay/wechat/unionpay三种类型值)  其中银联扫码类型unionpay支持京东，美团，美团外卖，云闪付等多种常用扫码支付软件。
     private static final String       payAmt	  = "payAmt";              //              是	交易金额（单位：元）
     private static final String       orderNo	  = "orderNo";            //              是	订单编号
     private static final String       notifyUrl	  = "notifyUrl";     //              是	异步通知回调URL地址
     private static final String       sign	  = "sign";                    //              是	MD5加密签名（大写）
     private static final String       ip	  = "ip";                    //              是	终端IP地址(H5)



    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
            payParam.put(platSource,channelWrapper.getAPI_MEMBERID());
            payParam.put(payType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(payAmt,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(orderNo,channelWrapper.getAPI_ORDER_ID());
            payParam.put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            if(channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().equalsIgnoreCase("alipayH5")){//需要ip
               payParam.put(ip,channelWrapper.getAPI_Client_IP());
            }
        log.debug("[安亿支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if(StringUtils.isBlank(params.get(paramKeys.get(i))) || sign.equalsIgnoreCase(paramKeys.get(i).toString() ) ||  StringUtils.isBlank( paramKeys.get(i).toString() )    )  //
                continue;
            //sb.append(paramKeys.get(i)).append("=")
              sb.append(params.get(paramKeys.get(i))).append("|");
        }
        sb.append(channelWrapper.getAPI_KEY());
        String signStr = sb.toString(); //.replaceFirst("&key=","")
        pay_md5sign = HandlerUtil.getMD5UpperCase(signStr);
        log.debug("[安亿支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if ( 1==2 && HandlerUtil.isWY(channelWrapper) &&  HandlerUtil.isYLKJ(channelWrapper)    ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='post'"));
                payResultList.add(result);
            }else{
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
                JSONObject jsonResultStr = JSON.parseObject(resultStr);

                if(  HandlerUtil.isWapOrApp(channelWrapper) ){
                    if(null!=jsonResultStr && jsonResultStr.containsKey("code") && "1".equalsIgnoreCase(jsonResultStr.getString("code")) && jsonResultStr.containsKey("codeUrl")){
                            String jumpUrl=jsonResultStr.getString("codeUrl");
                            if(StringUtils.isNotBlank(jumpUrl)){
                                result.put(JUMPURL, jumpUrl);
                                payResultList.add(result);
                            }
                    }else {
                        throw new PayException(resultStr);
                    }
                }else{
                    if(null!=jsonResultStr && jsonResultStr.containsKey("success") && "true".equalsIgnoreCase(jsonResultStr.getString("success")) && jsonResultStr.containsKey("info")){
                        if(null!=jsonResultStr.getJSONObject("info") && jsonResultStr.getJSONObject("info").containsKey("qrCode")){
                            String qr=jsonResultStr.getJSONObject("info").getString("qrCode");
                            if(StringUtils.isNotBlank(qr)){
                                result.put(QRCONTEXT, qr);
                                payResultList.add(result);
                            }
                        }
                    }else {
                        throw new PayException(resultStr);
                    }
                }




                 
            }
        } catch (Exception e) { 
             log.error("[安亿支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[安亿支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[安亿支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}