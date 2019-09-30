package dc.pay.business.heyifuzhifu;

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

@RequestPayHandler("HEYIFUZHIFU")
public final class HeYiFuZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(HeYiFuZhiFuPayRequestHandler.class);


     private static final String       bankCode	 = "bankCode";            //银行编码，如果使用网银支付，该参数为必传
     private static final String       productNo	 = "productNo";            //产品编码，支付方式编码列表
     private static final String       memberGoods	 = "memberGoods";            //商品信息，必须使用商户订单号
     private static final String       noticeSysaddress	 = "noticeSysaddress";            //异步通知地址
     private static final String       requestAmount	 = "requestAmount";            //订单金额，单位:元
     private static final String       trxMerchantNo	 = "trxMerchantNo";            //商户编号，户在系统的唯一身份标识。
     private static final String       trxMerchantOrderno	 = "trxMerchantOrderno";            //商户订单号，只能传递数字和字母，禁止传递特殊字符
     private static final String       hmac = "hmac";            //签名数据




   //     	网银


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
           payParam.put(productNo,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(memberGoods,channelWrapper.getAPI_ORDER_ID());
            payParam.put(noticeSysaddress,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(requestAmount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(trxMerchantNo,channelWrapper.getAPI_MEMBERID());
            payParam.put(trxMerchantOrderno,channelWrapper.getAPI_ORDER_ID());
        if(HandlerUtil.isWY(channelWrapper)){
            payParam.put(bankCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(productNo,"EBANK-JS");
        }
        log.debug("[和壹付支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if(StringUtils.isBlank(params.get(paramKeys.get(i))) || hmac.equalsIgnoreCase(paramKeys.get(i).toString()    ))  //
                continue;
            sb.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i))).append("&");
        }
        sb.append("key=" + channelWrapper.getAPI_KEY());
        String signStr = sb.toString(); //.replaceFirst("&key=","")
        pay_md5sign = HandlerUtil.getMD5UpperCase(signStr).toLowerCase();
        log.debug("[和壹付支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (1==2 && HandlerUtil.isWY(channelWrapper)) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
                payResultList.add(result);
            }else{
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.GET).trim();
                JSONObject jsonResultStr = JSON.parseObject(resultStr);
          
                    if(null!=jsonResultStr && jsonResultStr.containsKey("code") && "00000".equalsIgnoreCase(jsonResultStr.getString("code")) && jsonResultStr.containsKey("payUrl")){
                            if(StringUtils.isNotBlank(jsonResultStr.getString("payUrl"))){

                                if(HandlerUtil.isWapOrApp(channelWrapper) ||HandlerUtil.isWY(channelWrapper)){
                                    result.put(JUMPURL, jsonResultStr.getString("payUrl"));
                                }else{
                                    String qrContx=jsonResultStr.getString("payUrl").substring(jsonResultStr.getString("payUrl").indexOf("payUrl=")+7);
                                    if(StringUtils.isNotBlank(qrContx)){
                                        result.put(QRCONTEXT,qrContx );
                                    }else{ throw new PayException(resultStr);}
                                }
                                payResultList.add(result);
                            }else{ throw new PayException(resultStr);}
                    }else {
                        throw new PayException(resultStr);
                    }

                 
            }
        } catch (Exception e) { 
             log.error("[和壹付支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[和壹付支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[和壹付支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}