package dc.pay.business.jufangyun;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestPayHandler("JUFANGYUN")
public final class JuFangYunPayRequestHandler extends PayRequestHandler {
         private static final Logger log = LoggerFactory.getLogger(JuFangYunPayRequestHandler.class);

    	 private static final String     tradeType = "tradeType";     //	  交易类型   是	String(32)	cs.pay.submit
    	 private static final String     version = "version";     //	  版本   是	String(8)	版本号，1.5
    	 private static final String     channel = "channel";     //	  支付类型   是	String(24)	支付使用的第三方支付类型，见附件“支付类型”
    	 private static final String     mchId = "mchId";     //	  商户号   是	String(32)	由平台分配的商户号
    	 private static final String     sign = "sign";     //	  签名   是	String(32)	签名，详见签名生成算法
    	 private static final String     body = "body";     //	  商品描述   是	String(128)	商品或支付单简要描述
    	 private static final String     outTradeNo = "outTradeNo";     //	  商户订单号   是	String(32)	商户系统内部的订单号,32个字符内、可包含字母, 确保在商户系统唯一
    	 private static final String     amount = "amount";     //	  交易金额   是	Number	单位为元，小数两位
    	 private static final String     settleCycle = "settleCycle";     //
    	 private static final String     bankType = "bankType";     //
    	 private static final String     accountType = "accountType";     //
    	 private static final String     notifyUrl = "notifyUrl";     //




    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(tradeType,"cs.pay.submit");
            payParam.put(version,"1.5");
            payParam.put(settleCycle,"0");

            payParam.put(mchId,channelWrapper.getAPI_MEMBERID());
            payParam.put(body,channelWrapper.getAPI_ORDER_ID());
            payParam.put(outTradeNo,channelWrapper.getAPI_ORDER_ID());
            payParam.put(amount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());


            if(HandlerUtil.isWY(channelWrapper)){
                payParam.put(channel,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
                payParam.put(bankType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
                payParam.put(accountType,"1");
            }else{
                payParam.put(channel,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            }

        }
        log.debug("[聚方云]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if(StringUtils.isBlank(params.get(paramKeys.get(i))) || sign.equalsIgnoreCase(paramKeys.get(i).toString()    ))  //
                continue;
            sb.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i))).append("&");
        }
        sb.append("key=" + channelWrapper.getAPI_KEY());
        String signStr = sb.toString(); //.replaceFirst("&key=","")
        pay_md5sign = HandlerUtil.getMD5UpperCase(signStr);
        log.debug("[聚方云]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (1==2 && HandlerUtil.isYLWAP(channelWrapper) ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='post'"));
                payResultList.add(result);
            }else{
                resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam).trim();
                if(HandlerUtil.isYLWAP(channelWrapper) ){
                    result.put(HTMLCONTEXT, resultStr);
                    payResultList.add(result);
                }else{
                    JSONObject jsonResultStr = JSON.parseObject(resultStr);
                    if(null!=jsonResultStr && jsonResultStr.containsKey("returnCode") && "0".equalsIgnoreCase(jsonResultStr.getString("returnCode")) && jsonResultStr.containsKey("resultCode") && "0".equalsIgnoreCase(jsonResultStr.getString("resultCode")) && (jsonResultStr.containsKey("codeUrl") ||jsonResultStr.containsKey("payCode")) ){
                        if(channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().equalsIgnoreCase("wxPubQR") ||
                                channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().equalsIgnoreCase("jdQR") ||
                                channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().equalsIgnoreCase("unionpayQR") ||
                                channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().equalsIgnoreCase("alipayQR") ||
                                channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().equalsIgnoreCase("qqQr")  ){
                            if(StringUtils.isNotBlank(jsonResultStr.getString("codeUrl"))){
                                result.put(QRCONTEXT, jsonResultStr.getString("codeUrl"));
                                payResultList.add(result);
                            }else{
                                throw new PayException(resultStr);
                            }
                        }else if(
                                channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().equalsIgnoreCase("wxApp") ||
                                channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().equalsIgnoreCase("wxPub") ||
                                channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().equalsIgnoreCase("wxH5") ||
                                channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().equalsIgnoreCase("qpay") ||
                                channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().equalsIgnoreCase("jdPay") ||
                                channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().equalsIgnoreCase("jdGateway") ||
                                channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().contains("gateway") ||
                                channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().equalsIgnoreCase("unionpayH5") ||
                                channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().equalsIgnoreCase("alipayH5") ||
                                channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().equalsIgnoreCase("unionpayNewPay") ){
                            if(StringUtils.isNotBlank(jsonResultStr.getString("payCode"))){
                                if( HandlerUtil.isWY(channelWrapper) ||HandlerUtil.isYLKJ(channelWrapper)){
                                    result.put(HTMLCONTEXT, jsonResultStr.getString("payCode"));
                                }else if(HandlerUtil.isYLKJ(channelWrapper)  &&  HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isZFB(channelWrapper)){
                                    result.put(JUMPURL, jsonResultStr.getString("payCode"));
                                }else{
                                    result.put(QRCONTEXT, jsonResultStr.getString("payCode"));
                                }
                                payResultList.add(result);
                            }else{
                                throw new PayException(resultStr);
                            }
                        }else{
                            throw new PayException(resultStr);
                        }
                    }else {
                        throw new PayException(resultStr);
                    }
                }



                 
            }
        } catch (Exception e) { 
             log.error("[聚方云]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[聚方云]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[聚方云]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}