package dc.pay.business.aijia;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
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
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Aug 2, 2018
 */
@RequestPayHandler("AIJIA")
public final class AiJiaPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(AiJiaPayRequestHandler.class);

    //字段名              变量名             必填            类型              说明
    //商户号              mechno             是            String(32)          由平台分配
    //总金额              amount             是            Int                 支付的总金额，单位为分
    //商品名称            body               是            String(128)         商品的名称
    //异步通知地址        notifyurl          是            String(128)         支付成功后，服务端通知地址，此参数的地址不能带任何参数信息。
    //页面跳转地址        returl             否            String(128)         支付成功后，页面将跳转到该地址
    //订单ip              orderip            否            String(15)          订单ip
    //扩展字段            extraparam         否            String(50)          扩展字段，异步返回
    //商户订单号          orderno            是            String(64)          商户订单号
    //支付方式            payway             是            String(10)          微信：WECHAT
    //支付类别            paytype            是            String(10)          微信公众号：WECHAT_OPENPAY
    //支付用户id          payerId            否            String(64)          快捷支付时必填（确保用户的id唯一且保持不变，该参数用来记录用户的身份信息，相同的payerId会回显上次的银行账户信息，错误的payerId会回显他人的银行账户信息，切记不能随机填写。用户第一次支付时可填空字符串，会要求用户填写银行账户信息）
    //签名                sign               是            String(64)          除sign外的请求参数按字典排序后加上密钥生MD5签名，最后会附上java、php版的签名算法，注意:所有参与签名的都是原值，不要URLEncode之后再参与签名！！！生成签名之后可以再对中文的参数值进行UrlEncode
    private static final String mechno                ="mechno";
    private static final String amount                ="amount";
    private static final String body                  ="body";
    private static final String notifyurl             ="notifyurl";
    private static final String returl                ="returl";
//    private static final String orderip               ="orderip";
    private static final String extraparam            ="extraparam";
    private static final String orderno               ="orderno";
    private static final String payway                ="payway";
    private static final String paytype               ="paytype";
//    private static final String payerId               ="payerId";
    
    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(mechno, channelWrapper.getAPI_MEMBERID());
                put(amount,  channelWrapper.getAPI_AMOUNT());
                put(body,"name");
                put(notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(returl,channelWrapper.getAPI_WEB_URL());
                put(extraparam, channelWrapper.getAPI_MEMBERID());
                put(orderno,channelWrapper.getAPI_ORDER_ID());
                put(payway,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
                put(paytype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
            }
        };
        log.debug("[艾加]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        signSrc.append(key +"="+ channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[艾加]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    @Override
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
        }
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		payResultList.add(result);
        log.debug("[艾加]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[艾加]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}