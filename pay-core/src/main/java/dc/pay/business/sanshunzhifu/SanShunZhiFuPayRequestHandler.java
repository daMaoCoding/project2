package dc.pay.business.sanshunzhifu;

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

@RequestPayHandler("SANSHUNZHIFU")
public final class SanShunZhiFuPayRequestHandler extends PayRequestHandler {
        private static final Logger log = LoggerFactory.getLogger(SanShunZhiFuPayRequestHandler.class);

         private static final String     pay_memberid	 = "pay_memberid";    //是	string	商户ID	商户ID
         private static final String     pay_orderid	 = "pay_orderid";    //是	string	订单号	订单号
         private static final String     pay_amount	 = "pay_amount";    //是	string	订单金额	订单金额，单位：元，精确到分
         private static final String     pay_applydate	 = "pay_applydate";    //是	string	订单提交时间	订单提交的时间: 如： 2014-12-26 18:18:18
         private static final String     pay_bankcode	 = "pay_bankcode";    //是	string	银行编号	银行编码（查看下方银行编码参数）
         private static final String     pay_notifyurl	 = "pay_notifyurl";    //是	string	服务端返回地址	服务端返回地址.（POST返回数据）
         private static final String     pay_callbackurl	 = "pay_callbackurl";    //是	string	页面返回地址	页面跳转返回地址（POST返回数据）
         private static final String     pay_md5sign	 = "pay_md5sign";    //是	string	MD5签名字段	查看签名算法部分
         private static final String     pay_productname	 = "pay_productname";    //是	商品名称



    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
            payParam.put(pay_memberid,channelWrapper.getAPI_MEMBERID());
            payParam.put(pay_orderid,channelWrapper.getAPI_ORDER_ID());
            payParam.put(pay_amount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(pay_applydate,DateUtil.formatDateTimeStrByParam(DateUtil.dateTimeString));
            payParam.put(pay_bankcode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(pay_notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(pay_callbackurl,channelWrapper.getAPI_WEB_URL());
            payParam.put(pay_productname,channelWrapper.getAPI_ORDER_ID());
        log.debug("[三顺支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String pay_md5signA = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if(StringUtils.isBlank(params.get(paramKeys.get(i))) || pay_md5sign.equalsIgnoreCase(paramKeys.get(i).toString())  ||pay_productname.equalsIgnoreCase(paramKeys.get(i).toString())    )  //
                continue;
            sb.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i))).append("&");
        }
        sb.append("key=" + channelWrapper.getAPI_KEY());
        String signStr = sb.toString(); //.replaceFirst("&key=","")
        pay_md5signA = HandlerUtil.getMD5UpperCase(signStr);
        log.debug("[三顺支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5signA));
        return pay_md5signA;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper) || channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().equalsIgnoreCase("904") ) { //对接时候只能使用微信，支付宝wap(返回html)
                 result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='post'"));
                 payResultList.add(result);
            }else{
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
                JSONObject jsonResultStr = JSON.parseObject(resultStr);
          
                    if(null!=jsonResultStr && jsonResultStr.containsKey("status") && "0".equalsIgnoreCase(jsonResultStr.getString("status")) && jsonResultStr.containsKey("codeUrl")){
                            if(StringUtils.isNotBlank(jsonResultStr.getString("codeUrl"))){
                                result.put(JUMPURL, HandlerUtil.UrlDecode(jsonResultStr.getString("codeUrl")));
                                payResultList.add(result);
                            }
                    }else {
                        throw new PayException(resultStr);
                    }

                 
            }
        } catch (Exception e) { 
             log.error("[三顺支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[三顺支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[三顺支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}