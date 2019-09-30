package dc.pay.business.jinyi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import org.springframework.http.HttpMethod;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Jul 5, 2018
 */
@RequestPayHandler("JINYI")
public final class JinYiPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(JinYiPayRequestHandler.class);

    //参数名称                  参数含义        是否必填        参数说明
    //pay_memberid              商户ID              是         
    //pay_orderid               订单号              是        可以为空，为空时系统自动生成订单号，如果不为空请保证订单号不重复，此字段可以为空，但必须参加加密
    //pay_amount                金额                是        订单金额，单位：元，精确到分
    //pay_applydate             订单提交时间        是        订单提交的时间: 如： 2017-12-26 18:18:18
    //pay_bankcode              银行编号            是        银行编码-icbc支付宝-alipay 微信-weixin
    //pay_notifyurl             服务端返回地址      是        服务端返回地址.（POST返回数据）
    //pay_callbackurl           页面返回地址        是        页面跳转返回地址（POST返回数据）
    //pay_reserved1             扩展字段1           否        此字段在返回时按原样返回
    //pay_reserved2             扩展字段2           否        此字段在返回时按原样返回
    //pay_reserved3             扩展字段2           否        此字段在返回时按原样返回
    //pay_productname           商品名称            否        
    //pay_productnum            商户品数量          否        
    //pay_productdesc           商品描述            否        
    //pay_producturl            商户链接地址        否        
    //pay_md5sign               MD5签名字段         是        请看MD5签名字段格式
    private static final String pay_memberid                 ="pay_memberid";
    private static final String pay_orderid                  ="pay_orderid";
    private static final String pay_amount                   ="pay_amount";
    private static final String pay_applydate                ="pay_applydate";
    private static final String pay_bankcode                 ="pay_bankcode";
    private static final String pay_notifyurl                ="pay_notifyurl";
    private static final String pay_callbackurl              ="pay_callbackurl";
//    private static final String pay_reserved1                ="pay_reserved1";
//    private static final String pay_reserved2                ="pay_reserved2";
//    private static final String pay_reserved3                ="pay_reserved3";
    private static final String pay_productname              ="pay_productname";
//    private static final String pay_productnum               ="pay_productnum";
//    private static final String pay_productdesc              ="pay_productdesc";
//    private static final String pay_producturl               ="pay_producturl";
    private static final String pay_tongdao               ="pay_tongdao";



    private static final String payurl               ="payurl";

    //signature    数据签名    32    是    　
//    private static final String signature  ="pay_md5sign";

    @Override
//    protected Map<String, String> buildPayParam() throws PayException {
//        Map<String, String> payParam = new TreeMap<String, String>() {
//            {
//                put(pay_memberid, channelWrapper.getAPI_MEMBERID());
//                put(pay_orderid,channelWrapper.getAPI_ORDER_ID());
//                put(pay_amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
//                put(pay_applydate,HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "yyyy-MM-dd HH:mm:ss"));
//                put(pay_bankcode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                put(pay_notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
//                put(pay_callbackurl,channelWrapper.getAPI_WEB_URL());
//                put(pay_productname,"name");
//                //支付宝H5：Jinyih5，QQH5：Jinyiqq网银：Yinju
//                if (handlerUtil.isWY(channelWrapper)) {
//                    put(pay_tongdao,"Yinju");
//                }else if (channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("JINYI_BANK_WAP_ZFB_SM")) {
//                    put(pay_tongdao,"Jinyih5");
//                }else if (channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("JINYI_BANK_WAP_WX_SM")) {
//                    put(pay_tongdao,"Jinyiwx");
//                }else if (channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("JINYI_BANK_WAP_QQ_SM")) {
//                    put(pay_tongdao,"Jinyiqq");
//                }else if (channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("JINYI_BANK_WEBWAPAPP_YL_KJZF")) {
//                    put(pay_tongdao,"Quickpayment");
//                }
//            }
//        };
//        log.debug("[金蚁]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
//        return payParam;
//    }
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
            	put(pay_memberid, channelWrapper.getAPI_MEMBERID());
            	put(pay_orderid,channelWrapper.getAPI_ORDER_ID());
            	put(pay_amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            	put(pay_applydate,HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "yyyy-MM-dd HH:mm:ss"));
            	put(pay_bankcode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
            	put(pay_notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            	put(pay_callbackurl,channelWrapper.getAPI_WEB_URL());
            	put(pay_productname,"name");
            	put(pay_tongdao,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
            }
        };
        log.debug("[金蚁]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!pay_tongdao.equals(paramKeys.get(i)) && !pay_productname.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        signSrc.append("key=" + channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[金蚁]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)  || HandlerUtil.isWapOrApp(channelWrapper)    ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='post'"));
                payResultList.add(result);
            }else{
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
                if(StringUtils.isNotBlank(resultStr) && resultStr.contains("<form") && !resultStr.contains("{")){
                    result.put(HTMLCONTEXT,resultStr);
                    payResultList.add(result);
                }else{
                    JSONObject jsonResultStr = JSON.parseObject(resultStr);
                    if(null!=jsonResultStr && jsonResultStr.containsKey("msg") && "获取成功".equalsIgnoreCase(jsonResultStr.getString("msg"))
                            && jsonResultStr.containsKey(payurl) && StringUtils.isNotBlank(jsonResultStr.getString(payurl))){
                        if(HandlerUtil.isWapOrApp(channelWrapper)){
                            result.put(JUMPURL, jsonResultStr.getString(payurl));
                        }else{
                            result.put(QRCONTEXT, jsonResultStr.getString(payurl));
                        }
                        payResultList.add(result);
                    }else if(StringUtils.isBlank(resultStr) ){  throw new PayException(EMPTYRESPONSE);  }else {throw new PayException(resultStr); }
                }

            }
        } catch (Exception e) {
            log.error("[个付]3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(e.getMessage(), e);
        }
        log.debug("[个付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[金蚁]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}