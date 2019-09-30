package dc.pay.business.weisaojinfu;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author sunny 02 02 , 2019
 */
@RequestPayHandler("WEISAOJINFU")
public final class WeiSaoJinFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(WeiSaoJinFuPayRequestHandler.class);

    // 参数名 必选 类型 说明
    // parter 是 string 接口调用ID
    // value 是 int 金额，元为单位
    // type 是 string
    // 支付类型：wx=微信,wxwap=微信WAP,ali=支付宝,aliwap=支付宝WAP,qq=QQ,qqwap=QQWAP
    // orderid 是 string 商家订单号
    // notifyurl 是 string 异步通知地址
    // callbackurl 是 string 支付成功后跳转到该页面
    // getcode 否 int
    // 默认0，为1的时候返回二维码内容，自行使用Curl方式获取，获取例子：{“code”:”200”,”codeimg”:”二维码内容”}。非常建议跳转到我方页面，不建议获取内容
    // sign 是 string 签名
    private static final String parter = "parter";
    private static final String value = "value";
    private static final String type = "type";
    private static final String orderid = "orderid";
    private static final String notifyurl = "notifyurl";
    private static final String callbackurl = "callbackurl";
    private static final String getcode = "getcode";

    // signature 数据签名 32 是
    private static final String signature = "sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(parter, channelWrapper.getAPI_MEMBERID());
                put(value, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(type, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(orderid, channelWrapper.getAPI_ORDER_ID());
                put(notifyurl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(callbackurl, channelWrapper.getAPI_WEB_URL());
                // 默认0，为1的时候返回二维码内容，自行使用Curl方式获取，获取例子：{“code”:”200”,”codeimg”:”二维码内容”}。非常建议跳转到我方页面，不建议获取内容
                put(getcode, "1");
            }
        };
        log.debug("[微扫金服]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i)))
                        .append("&");
            }
        }
        signSrc.append("key=" + channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[微扫金服]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign)
            throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
//        if (HandlerUtil.isWapOrApp(channelWrapper)|| channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("WEISAOFU_BANK_WEBWAPAPP_JD_SM")) {
        if (true) {
            String html = HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam).toString().replace("method='post'", "method='get'");
            // 保存第三方返回值
            result.put(HTMLCONTEXT, html);
        } else {
            String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,String.class, HttpMethod.GET).trim();
            if (StringUtils.isBlank(resultStr)) {
//                log.error("[微扫付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空" + ",参数：" + JSON.toJSONString(payParam));
//                throw new PayException("返回空" + ",参数：" + JSON.toJSONString(payParam));
                log.error("[微扫付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号："+ channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            if (!resultStr.contains("\"code\":\"200\"")) {
                log.error("[微扫付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号："+ channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            JSONObject resJson = JSONObject.parseObject(resultStr);
            if (!resJson.containsKey("code") || !"200".equals(resJson.getString("code"))) {
                log.error("[微扫付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号："
                        + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            try {
                result.put(QRCONTEXT, URLDecoder.decode(resJson.getString("codeimg"), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                log.error("[微扫付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号："
                        + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[微扫金服]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
        return payResultList;
    }

    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (null != resultListMap && !resultListMap.isEmpty()) {
            if (resultListMap.size() == 1) {
                Map<String, String> resultMap = resultListMap.get(0);
                requestPayResult = buildResult(resultMap, channelWrapper,requestPayResult);
            }
            if (ValidateUtil.requestesultValdata(requestPayResult)) {
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            } else {
                throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[微扫金服]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}