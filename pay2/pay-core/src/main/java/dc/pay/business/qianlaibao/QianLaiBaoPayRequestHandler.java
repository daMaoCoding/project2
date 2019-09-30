package dc.pay.business.qianlaibao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

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
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.Sha1Util;
import dc.pay.utils.UnicodeUtil;
import dc.pay.utils.ValidateUtil;


/**
 * 
 * 
 * @author sunny
 * Dec 18, 2018
 */
@RequestPayHandler("QIANLAIBAO")
public final class QianLaiBaoPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(QianLaiBaoPayRequestHandler.class);

//    字段名 					填写类型 			长度 			说明
//    merchant_no 			必填 				String(16) 	商户号，商户后台获取。
//    version 				必填 				String(16) 	版本号 1.0
//    out_trade_no 			必填 				String(32) 	商户订单号，确保在商户系统中唯一。最大长度 32 位。
//    payment_type 			必填 				String(16) 	aliwap 支付宝手机扫码支付
//    notify_url			必填 				String(128) 服务器异步通知地址。提交的 url 地址必须外网能访问到,否则无法通知商户。
//    page_url  			必填 				String(128) 页面同步跳转地址。成功处理完请求后，当前页面自动跳转到同步通知页面路径。
//    total_fee 			必填 				Number 		交易金额，单位为元，精确到小数点后两位。
//    trade_time 			必填 				String(14) 	交易时间。格式：YYYYMMDDHHMISS
//    user_account 			必填				String(16) 	用户帐号。商户平台的用户帐号，保证用户帐号的唯一性。
//    body 					选填 				String(256) 商品描述。可用作透传参数。
//    sign 					必填 				String 		MD5 签名结果

    private static final String merchant_no               	="merchant_no";
    private static final String version           			="version";
    private static final String out_trade_no           		="out_trade_no";
    private static final String payment_type           		="payment_type";
    private static final String notify_url          		="notify_url";
    private static final String page_url              		="page_url";
    private static final String total_fee            		="total_fee";
    private static final String trade_time           		="trade_time";
    private static final String user_account            	="user_account";
    private static final String sign                		="sign";
    private static final String key                			="key";
    
    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merchant_no, channelWrapper.getAPI_MEMBERID().split("&")[0]);
                put(out_trade_no,channelWrapper.getAPI_ORDER_ID());
                put(total_fee,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(payment_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(page_url,channelWrapper.getAPI_WEB_URL());
                put(user_account,channelWrapper.getAPI_MEMBERID().split("&")[1]);
                put(trade_time,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
                put(version,"1.0");
                put("body",channelWrapper.getAPI_ORDER_ID());
            }
        };
        log.debug("[钱来宝支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
//            if (!pay_productname.equals(paramKeys.get(i)) && !pay_attach.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
            if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
            	signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        //最后一个&转换成#
        signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "" );
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[钱来宝支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return signMD5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());  //.replace("method='post'","method='get'"));
        payResultList.add(result);
        log.debug("[钱来宝支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[钱来宝支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}