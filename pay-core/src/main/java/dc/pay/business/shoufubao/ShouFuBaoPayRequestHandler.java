package dc.pay.business.shoufubao;

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
import java.util.TreeMap;

@RequestPayHandler("SHOUFUBAO")
public final class ShouFuBaoPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(ShouFuBaoPayRequestHandler.class);

    private static final String       payKey = "payKey";//	String	否	32	商户支付Key
    private static final String       orderPrice = "orderPrice";//	Float	否	12	订单金额，单位：元,保留小数点后两位
    private static final String       outTradeNo = "outTradeNo";//	String	否	30	商户支付订单号（长度30以内）
    private static final String       productType = "productType";//	String	否	8	请参考支付方式编码
    private static final String       orderTime = "orderTime";//	String	否	14	下单时间，格式(yyyyMMddHHmmss)
    private static final String       productName = "productName";//	String	否	200	支付产品名称
    private static final String       orderIp = "orderIp";//	String	否	15	下单IP
    private static final String       returnUrl = "returnUrl";//	String	否	300	页面通知地址
    private static final String       notifyUrl = "notifyUrl";//	String	否	300	后台异步通知地址
    private static final String       sign = "sign";//	String	否	50	MD5大写签名
    private static final String       bankAccountType = "bankAccountType";
    private static final String       bankCode = "bankCode";
    private static final String       remark = "remark";




    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        if(!channelWrapper.getAPI_MEMBERID().contains("&")){
            throw new PayException("商户号格式错误，正确格式请使用&符号链接 商户号和paykey,如：商户号&paykey");
        }
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(payKey,channelWrapper.getAPI_MEMBERID().split("&")[1]);
            payParam.put(orderPrice,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(outTradeNo,channelWrapper.getAPI_ORDER_ID());

            if(HandlerUtil.isWY(channelWrapper)){
               payParam.put(productType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
               payParam.put(bankCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
               payParam.put(bankAccountType,"PRIVATE_DEBIT_ACCOUNT");
              // payParam.put(remark,remark);
            }else{
               payParam.put(productType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            }


            payParam.put(orderTime,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
            payParam.put(productName,channelWrapper.getAPI_ORDER_ID());
            payParam.put(orderIp,channelWrapper.getAPI_Client_IP()); //channelWrapper.getAPI_Client_IP()
            payParam.put(returnUrl,channelWrapper.getAPI_WEB_URL());
            payParam.put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
        }

        log.debug("[收付宝支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if(StringUtils.isBlank(params.get(paramKeys.get(i))) || sign.equalsIgnoreCase(paramKeys.get(i).toString())    )  //
                continue;
            sb.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i))).append("&");
        }
        sb.append("paySecret=" + channelWrapper.getAPI_KEY());
        String signStr = sb.toString(); //.replaceFirst("&key=","")
        pay_md5sign = HandlerUtil.getMD5UpperCase(signStr);
        log.debug("[收付宝支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        payParam = new TreeMap<>(payParam);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (  HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)  || channelWrapper.getAPI_CHANNEL_BANK_NAME().endsWith("_WAP_JD_SM") ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='post'"));
                payResultList.add(result);
            }else{
               //  String urlWithParam = HttpUtil.getURLWithParam(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
               // System.out.println(urlWithParam);

                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
                JSONObject jsonResultStr = JSON.parseObject(resultStr);
                    if(null!=jsonResultStr && jsonResultStr.containsKey("resultCode") && "0000".equalsIgnoreCase(jsonResultStr.getString("resultCode")) && jsonResultStr.containsKey("payMessage")){
                            if(StringUtils.isNotBlank(jsonResultStr.getString("payMessage"))){
                                if(HandlerUtil.isWapOrApp(channelWrapper)){
                                   result.put(JUMPURL, jsonResultStr.getString("payMessage"));
                                }else{
                                   result.put(QRCONTEXT, jsonResultStr.getString("payMessage"));
                                }
                                payResultList.add(result);
                            }
                    }else {
                        throw new PayException(resultStr);
                    }

                 
            }
        } catch (Exception e) { 
             log.error("[收付宝支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[收付宝支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[收付宝支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}