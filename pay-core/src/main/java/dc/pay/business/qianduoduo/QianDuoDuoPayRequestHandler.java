package dc.pay.business.qianduoduo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

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
import dc.pay.utils.UnicodeUtil;
import dc.pay.utils.ValidateUtil;

/**
 * ************************
 * @author beck 2229556569
 */

@RequestPayHandler("QIANDUODUO")
public final class QianDuoDuoPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(QianDuoDuoPayRequestHandler.class);

    private static final String pay_fs = "pay_fs";
    private static final String pay_MerchantNo = "pay_MerchantNo";
    private static final String pay_orderNo = "pay_orderNo";
    private static final String pay_Amount = "pay_Amount";
    private static final String pay_NotifyUrl = "pay_NotifyUrl";
    private static final String pay_ewm = "pay_ewm";
    private static final String tranType = "tranType";
    private static final String pay_ip = "pay_ip";
    private static final String pay_bankName = "pay_bankName";
    private static final String pay_returnUrl = "pay_returnUrl";
    //private static final String sign = "sign"; 

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();

        payParam.put(pay_fs, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
        payParam.put(pay_MerchantNo, this.getMerchantNo());
        payParam.put(pay_orderNo, channelWrapper.getAPI_ORDER_ID());
        payParam.put(pay_Amount, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
        payParam.put(pay_NotifyUrl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
        payParam.put(pay_ewm, "No");
        payParam.put(tranType, "2");
        payParam.put(pay_ip, channelWrapper.getAPI_Client_IP());
        payParam.put(pay_bankName, channelWrapper.getAPI_CHANNEL_BANK_NAME());
        payParam.put(pay_returnUrl, channelWrapper.getAPI_WEB_URL());
        
        log.debug("[钱多多]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String, String> params) throws PayException {
        StringBuilder sb = new StringBuilder();
        sb.append(params.get(pay_fs));
        sb.append(this.getJiGouNo());
        sb.append(params.get(pay_orderNo));
        sb.append(params.get(pay_Amount));
        sb.append(params.get(pay_NotifyUrl));
        sb.append(params.get(pay_ewm));
        sb.append(this.channelWrapper.getAPI_KEY());

        String signStr = sb.toString();

        String pay_md5sign = HandlerUtil.getMD5UpperCase(signStr).toLowerCase();
        log.debug("[钱多多]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (HandlerUtil.isWY(channelWrapper) && !handlerUtil.isWebWyKjzf(channelWrapper)) {

                String htmlContent = HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam).toString();
                result.put(HTMLCONTEXT, htmlContent);
//            }else if(HandlerUtil.isYLKJ(channelWrapper)){    
//                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam, String.class, HttpMethod.POST).trim();
//                if (StringUtils.isBlank(resultStr)) {
//                    log.error("[钱多多]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                    throw new PayException(resultStr);
//                    //log.error("[通扫]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
//                    //throw new PayException("返回空,参数："+JSON.toJSONString(map));
//                }
//                JSONObject jsonResult = JSON.parseObject(resultStr);
//                if (null != jsonResult && jsonResult.containsKey("pay_Status") && "100".equalsIgnoreCase(jsonResult.getString("pay_Status"))) {
//                    String qrinfo = jsonResult.getString("pay_Code");
//                    result.put(JUMPURL, qrinfo);
//                }else {
//                    log.error("[钱多多]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                    throw new PayException(resultStr);
//                }
            } else {
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam, String.class, HttpMethod.POST).trim();
                if (StringUtils.isBlank(resultStr)) {
                    log.error("[钱多多]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                    //log.error("[通扫]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
                    //throw new PayException("返回空,参数："+JSON.toJSONString(map));
                }
                resultStr = UnicodeUtil.unicodeToString(resultStr);
                JSONObject jsonResult = JSON.parseObject(resultStr);

                if (null != jsonResult && jsonResult.containsKey("pay_Status") && "100".equalsIgnoreCase(jsonResult.getString("pay_Status"))) {
                    if (StringUtils.isNotBlank(jsonResult.getString("pay_Code"))) {
                        String qrinfo = jsonResult.getString("pay_Code");
                        if(HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper) || handlerUtil.isWebWyKjzf(channelWrapper)){
                            result.put(JUMPURL, qrinfo);
                        }else{
                            result.put(QRCONTEXT, qrinfo);
                        }
                    }
                } else {
                    log.error("[钱多多]-[请求支付]3.1.发送支付请求，及获取支付请求结果出错：{}", resultStr);
                    throw new PayException(resultStr);
                }
            }
        } catch (Exception e) {
            log.error("[钱多多]-[请求支付]3.2.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(e.getMessage(), e);
        }
        
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[钱多多]-[请求支付]-3.3.发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));
        return payResultList;
    }

    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (null != resultListMap && !resultListMap.isEmpty()) {
            if (resultListMap.size() == 1) {
                Map<String, String> resultMap = resultListMap.get(0);
                requestPayResult = buildResult(resultMap, channelWrapper, requestPayResult);
            }
            if (ValidateUtil.requestesultValdata(requestPayResult)) {
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            } else {
                throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[钱多多]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
    
    /**
     * 获取商户号
     * */
    private String getMerchantNo() throws PayException{
        String memberInfos[] = this.splitMemberID();
        
        return memberInfos[0];
    }
    
    
    /**
     * 获取机构号
     * */
    private String getJiGouNo() throws PayException{
        String memberInfos[] = this.splitMemberID();
        
        return memberInfos[1];
    }
    
    /**
     * 分割商户ID
     * */
    private String[] splitMemberID() throws PayException {
        String memberInfos[] =  this.channelWrapper.getAPI_MEMBERID().split("&");
        if(memberInfos.length < 2){
            String errorMsg = "[钱多多]-[请求支付]-5. 商户信息填写错误，填写格式：商户号&机构号。";
            log.error(errorMsg);
            throw new PayException(errorMsg);
        }
        return memberInfos;
    }
}