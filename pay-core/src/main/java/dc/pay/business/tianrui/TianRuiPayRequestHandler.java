package dc.pay.business.tianrui;

import java.util.ArrayList;
import java.util.HashMap;
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

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Aug 6, 2018
 */
@RequestPayHandler("TIANRUI")
public final class TianRuiPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(TianRuiPayRequestHandler.class);

    //字段名称               含义              长度              是否必须           备注
    //merchant_no            商户号            20                   是              例如： MER0000001 
    //nonce_str              随机串            32                   是              例如：5bced2873dc
    //request_no             请求单号          32                   是              商户订单号(商户应保证唯一性) 
    //amount                 总金额            16                   是              订单总金额,以元为单位
    //pay_channel            通道              20                   是              ALP:支付宝扫码; WXP：微信扫码; WXH5： 微信H5； ALH5：支付宝H5； KJP:快捷支付 QQP QQ钱包扫码；WYP 网银支付; YLP 银联扫码 JDP 京东扫码；JDH5 京东WAP 
    //request_time           时间戳            10                   是              下单请求时间戳 
    //account_type           结算方式          1                    是              1 表示D0结算
    //notify_url             回调地址          100                  否              回调地址，如果不传此参数， 以WEB后台设置为准，如果后台也未设置，不会触发回调
    //body                   自定义            100                  否              商户自定义字段，回调时平台原样返回
    //ip_addr                用户IP地址        64                   是              终端客户IP地址， 注意不是商城服务器IP地址或中间平台服务器I P地址 return_url 同步回调地址 100 否 扫码类支付无须此地址
    //bankname               银行编码          20                   否              网银支付时需要提供银行编码，比如：ICBC 表示工商银行
    //cardtype               银行卡类型        2                    否              网银支付时需要此参数(00 表示贷记卡 01 表示借记卡)
    //clienttype             终端类型          1                    否              网银支付时需要此参数（1 表示PC端 2表示手机端）
    //sign                   签名              32                   是              例如： AA477964B59E28FE8B87D3BD0D03B5B6 
    private static final String merchant_no             ="merchant_no";
    private static final String nonce_str               ="nonce_str";
    private static final String request_no              ="request_no";
    private static final String amount                  ="amount";
    private static final String pay_channel             ="pay_channel";
    private static final String request_time            ="request_time";
    private static final String account_type            ="account_type";
    private static final String notify_url              ="notify_url";
//    private static final String body                    ="body";
    private static final String ip_addr                 ="ip_addr";
    private static final String bankname                ="bankname";
    private static final String cardtype                ="cardtype";
    private static final String clienttype              ="clienttype";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        long aa = System.currentTimeMillis()/1000;
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merchant_no, channelWrapper.getAPI_MEMBERID());
                put(nonce_str,handlerUtil.getRandomStr(10));
                put(request_no,channelWrapper.getAPI_ORDER_ID());
                put(amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(request_time,String.valueOf(aa));
                // DOTO 
                put(account_type,"1");
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
//                put(body,channelWrapper.getAPI_MEMBERID());
                put(ip_addr,channelWrapper.getAPI_Client_IP());
                put(pay_channel,(handlerUtil.isWY(channelWrapper) || handlerUtil.isWebYlKjzf(channelWrapper)) ? "WYP" : channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                if ((handlerUtil.isWY(channelWrapper) || handlerUtil.isWebYlKjzf(channelWrapper))) {
                    put(bankname,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    put(cardtype,"01");
                    put(clienttype,HandlerUtil.isOrderFromWapOrApp(channelWrapper.getAPI_ORDER_FROM()) ? "2" : "1");
                }
            }
        };
        log.debug("[天瑞]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
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
        log.debug("[天瑞]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
        if (StringUtils.isBlank(resultStr)) {
            log.error("[天瑞]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
            //log.error("[天瑞]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
            //throw new PayException("返回空,参数："+JSON.toJSONString(map));
        }
        JSONObject resJson = JSONObject.parseObject(resultStr);
        //只取正确的值，其他情况抛出异常
        if (null != resJson && resJson.containsKey("success") && "true".equalsIgnoreCase(resJson.getString("success"))  && resJson.containsKey("data") && StringUtils.isNotBlank(resJson.getString("data"))) {
            JSONObject resJson1 = JSONObject.parseObject(resJson.getString("data"));
            //只取正确的值，其他情况抛出异常
            if (null != resJson1 && resJson1.containsKey("status") && "2".equalsIgnoreCase(resJson1.getString("status"))  && resJson1.containsKey("bank_url") && StringUtils.isNotBlank(resJson1.getString("bank_url"))) {
                result.put((handlerUtil.isWY(channelWrapper) || handlerUtil.isWebYlKjzf(channelWrapper) || handlerUtil.isWapOrApp(channelWrapper)) ? JUMPURL : QRCONTEXT, resJson1.getString("bank_url"));
            }else {
                log.error("[天瑞]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
        }else {
            log.error("[天瑞]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
    
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		payResultList.add(result);
        log.debug("[天瑞]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[天瑞]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}