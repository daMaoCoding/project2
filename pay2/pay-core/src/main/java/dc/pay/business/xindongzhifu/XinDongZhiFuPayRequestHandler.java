package dc.pay.business.xindongzhifu;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import dc.pay.utils.*;
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

/**
 * @author Cobby
 * Apr 11, 2019
 */
@RequestPayHandler("XINDONGZHIFU")
public final class XinDongZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XinDongZhiFuPayRequestHandler.class);

    private static final String m_id            ="m_id";        //商户号      是    商户在我司注册的账号。    我司维度唯一
    private static final String accounts        ="accounts";    //商户用户    是    在商户注册的账号。    商户维度唯一。    必须为字母或数字            注意不是商户号
    private static final String order_id        ="order_id";    //商户订单号   是    商户生成的订单号。    商户维度唯一。    必须为字母或数字
    private static final String amount          ="amount";      //订单交易金额 是    订单的支付金额。    单位：分
    private static final String m               ="m";           //通道类型    是    充值通道的类型。            1–微信支付2–支付宝原生支付3–银联扫码(云闪付)7–支付宝个人码收款
    private static final String attach          ="attach";      //附加信息    否    附加信息。    异步通知时会完整返回
    private static final String order_ip        ="order_ip";    //下单IP     是    用户提交订单的IP地址
    private static final String r               ="r";           //随机字符串   是    固定长度32个字节的随机字串。    必须为字母或数字。
    private static final String return_url      ="return_url";  //页面返回地址 是    支付完成后页面自动跳转地址。    需要urlencode编码    经过urlencode编码转换的字符必须是大写，比如%3a必须是%3A，%2f必须是%2F
    private static final String notify_url      ="notify_url";  //异步通知地址 是    支付结果异步通知地址。    需要urlencode编码    经过urlencode编码转换的字符必须是大写，比如%3a必须是%3A，%2f必须是%2F
    private static final String api             ="api";         //API模式     否    1 – 启用API模式 2 – 常规模式 自动跳转

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        String returnUrl = "";
        String notifyUrl = "";
        try {
            returnUrl = URLEncoder.encode(channelWrapper.getAPI_WEB_URL(), "GBK");
            notifyUrl = URLEncoder.encode(channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL(), "GBK");
        } catch (Exception e) {
            e.printStackTrace();
        }
        String finalReturnUrl = returnUrl;
        String finalNotifyUrl = notifyUrl;
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(m_id, channelWrapper.getAPI_MEMBERID());
                put(accounts,channelWrapper.getAPI_ORDER_ID());
                put(order_id,channelWrapper.getAPI_ORDER_ID());
                put(amount,  channelWrapper.getAPI_AMOUNT());
                put(m,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(attach,"attach");
                put(order_ip,channelWrapper.getAPI_Client_IP());
                put(r,HandlerUtil.getRandomStr(32));
                put(return_url, finalReturnUrl);
                put(notify_url, finalNotifyUrl);
                if (HandlerUtil.isWxSM(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) ){
                    put(api,"2"); 
                }else {
                    put(api,"1");
                }
            }
        };
        log.debug("[鑫东支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
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
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[鑫东支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        HashMap<String, String> result = Maps.newHashMap();
        try {
            if (HandlerUtil.isWxSM(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            }else{
                String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
                resultStr = UnicodeUtil.unicodeToString(resultStr);
                JSONObject jsonObject;
                try {
                    jsonObject = JSONObject.parseObject(resultStr);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("[鑫东支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
                if (null != jsonObject && jsonObject.containsKey("code") && "000000".equalsIgnoreCase(jsonObject.getString("code"))
                        && jsonObject.containsKey("url") && StringUtils.isNotBlank(jsonObject.getString("url"))) {
                    String code_url = jsonObject.getString("url");
                    result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
                }else {
                    log.error("[鑫东支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
            }
            
        } catch (Exception e) {
            log.error("[鑫东支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[鑫东支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[鑫东支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}