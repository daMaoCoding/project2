package dc.pay.business.onepayzhifu;

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
import org.springframework.http.HttpMethod;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RequestPayHandler("ONEPAYZHIFU")
public final class OnePayZhiFuPayRequestHandler extends PayRequestHandler {
    //private static final Logger log = LoggerFactory.getLogger(GeFuPayRequestHandler.class);

     private static final String      app_id = "app_id";      //	Number(10)	Y	易付提供唯一商户号	752
     private static final String      currency = "currency";      //	String(3)	Y	请参考第11.1章货币类型	CNY
     private static final String      amount = "amount";      //	Number(14,2)	Y	订单总金额（元）	如果申请金额为1000，请传送1000.00（千位分隔符或逗号会导致请求失败）
     private static final String      order_no = "order_no";      //	String(50)	Y	商户提供唯一订单号	4530559221116650
     private static final String      payment_channel = "payment_channel";      //	String(10)	Y	钱包类型,
     private static final String      sign = "sign";      //	String(500)


      private static final String      version = "version";    //	版本号 	Char(2)	NO	1.0
      private static final String      inputCharset = "inputCharset";    //	字符集	Char(10)	NO	UTF-8
      private static final String      signType = "signType";    //	签名类型	Char(5)	NO	RSA
      private static final String      returnUrl = "returnUrl";    //	同步通知地址	Char(500)	NO	商户同步通知地址
      private static final String      notifyUrl = "notifyUrl";    //	异步通知地址	Char(500)	NO	商户异步通知地址
      private static final String      payType = "payType";    //	交易类型
      private static final String      merchantId = "merchantId";    //	商户号	Number	NO	易付提供唯一商户号
      private static final String      merchantTradeId = "merchantTradeId";    //	商户订单号	Char(50)	NO	商户提供唯一订单号
      private static final String      amountFee = "amountFee";    //	金额	Number	NO	订单总金额（元）
      private static final String      goodsTitle = "goodsTitle";    //	商品名称 	Char(100)	NO	提供商品说明
      private static final String      issuingBank = "issuingBank";    //	银行 ID	Char(10)	NO	固定值： UNIONPAY
      private static final String      cardType = "cardType";    //	银行卡类型	Char(10)	YES	借记卡: D    信用卡: C（暂不支持）




    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(HandlerUtil.isYLKJ(channelWrapper)){ //银联快捷支付
            payParam.put(version,"1.0");
            payParam.put(inputCharset,"UTF-8");
            payParam.put(signType,"RSA");
            payParam.put(returnUrl,channelWrapper.getAPI_WEB_URL());
            payParam.put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(payType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(merchantId,channelWrapper.getAPI_MEMBERID());
            payParam.put(merchantTradeId,channelWrapper.getAPI_ORDER_ID());
            payParam.put(amountFee,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(goodsTitle,PAYMENT);
            payParam.put(issuingBank,"UNIONPAY");
            payParam.put(cardType,"D");
            payParam.put(currency,"CNY");
        } else {  //HandlerUtil.isWY(channelWrapper)
            payParam.put(app_id,channelWrapper.getAPI_MEMBERID());
            payParam.put(order_no,channelWrapper.getAPI_ORDER_ID());
            payParam.put(currency,"CNY");
            payParam.put(amount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(payment_channel,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG() );
        }

        log.debug("[onepay支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String pay_md5sign = null;
        String serializationParam=SignatureUtil.generateSignContent(params);
        String sign=SignatureUtil.sign(serializationParam,"RSA",channelWrapper.getAPI_KEY());
        pay_md5sign=SignatureUtil.tenToSixteen(sign);
        log.debug("[onepay支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        if(HandlerUtil.isYLKJ(channelWrapper)){ //银联快捷，需要sign,扫码不需要。。。
            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        }

        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper)) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='post'"));
                payResultList.add(result);
            }else{
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
                if(StringUtils.isNotBlank(resultStr) && resultStr.contains("Error!")) throw new PayException(resultStr);
				
				if(StringUtils.isNotBlank(resultStr) && resultStr.contains("<form") && !resultStr.contains("{")){
                    result.put(HTMLCONTEXT,resultStr);
                    payResultList.add(result);
                }else if(StringUtils.isNotBlank(resultStr) ){
                    JSONObject jsonResultStr = JSON.parseObject(resultStr);
                    if(null!=jsonResultStr && jsonResultStr.containsKey("flag") && "SUCCESS".equalsIgnoreCase(jsonResultStr.getString("flag"))
                            && jsonResultStr.containsKey("data") && null!=jsonResultStr.getJSONObject("data") && jsonResultStr.getJSONObject("data").containsKey("qrUrl")
                            &&  StringUtils.isNotBlank( jsonResultStr.getJSONObject("data").getString("qrUrl")    )){
                        if(HandlerUtil.isWapOrApp(channelWrapper)){
                            result.put(JUMPURL,jsonResultStr.getJSONObject("data").getString("qrUrl") );
                        }else{
                            result.put(QRCONTEXT,jsonResultStr.getJSONObject("data").getString("qrUrl") );
                        }
                        payResultList.add(result);
                    }else {throw new PayException(resultStr); }
				}else{ throw new PayException(EMPTYRESPONSE);}
                 
            }
        } catch (Exception e) { 
             log.error("[onepay支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[onepay支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[onepay支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}