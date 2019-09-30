package dc.pay.business.hongshanzhifu;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

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
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Feb 26, 2019
 */
@RequestPayHandler("HONGSHANZHIFU")
public final class HongShanZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(HongShanZhiFuPayRequestHandler.class);

    //3.1.1请求参数
    //字段名 属性  名称  说明
    //mchntCode*  AN20    商户号 平台下发的商户号 
    private static final String mchntCode                ="mchntCode";
    //channelCode*    A..20   支付渠道    见5.2支付渠道说明
    private static final String channelCode                ="channelCode";
    //ts* N17 时间戳 时间戳格式为：年[4位]月[2位]日[2位]时[2位]分[2位]秒[2位]毫秒[3位]    //例如：20071117020101221
    private static final String ts                ="ts";
    //mchntOrderNo*   AN..8-30    商户订单号   商户端唯一
    private static final String mchntOrderNo                ="mchntOrderNo";
    //orderAmount*    N..15   订单金额    单位为对应币种的最小货币单位，人民币为分。如订单总金额为1元，orderAmount为100
    private static final String orderAmount                ="orderAmount";
    //clientIp*   NS..15  客户端ip地址 发起支付请求客户端的 IPv4 地址，如: 127.0.0.1
    private static final String clientIp                ="clientIp";
    //subject*    ANS..32 商品的标题   该参数最长为 32 个 Unicode 字符 
    private static final String subject                ="subject";
    //body*   ANS..128    商品的描述信息 该参数最长为 128 个 Unicode 字符
    private static final String body                ="body";
    //notifyUrl*  ANS..255    异步结果通知地址    需要是绝对地址，支付平台通过改地址通知支付结果
    private static final String notifyUrl                ="notifyUrl";
    //pageUrl ANS..255    支付完成后，跳转到商户方的页面 需要是绝对地址，支付平台直接将响应结果Get到pageUrl，长度小于256，合法URL
    private static final String pageUrl                ="pageUrl";
    //orderTime*  N14 订单提交时间  格式：年[4位]月[2位]日[2位]时[2位]分[2位]秒[2位]    //例如：20071117020101
    private static final String orderTime                ="orderTime";
    //orderExpireTime*    N14 订单失效时间  格式：年[4位]月[2位]日[2位]时[2位]分[2位]秒[2位]    //例如：20071117020101    //建议：下单时间加5~10分钟
    private static final String orderExpireTime                ="orderExpireTime";
    //description ANS..255    订单附加说明  订单附加说明，最多 255 个 Unicode 字符。
    private static final String description                ="description";
    //extra1  ANS..2046   额外参数    JSON格式。如：{“openid”:” oMIsT0kMSsdDnrk5HJYUCzLPQxg8”}
//    private static final String extra1                ="extra1";
    //sign*   ANS..64 签名  详见签名生成算法
//    private static final String sign                ="sign";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        Date now = new Date();
        Date afterDate = new Date(now .getTime() + 300000);

        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(mchntCode, channelWrapper.getAPI_MEMBERID());
                put(channelCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(ts,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
                put(mchntOrderNo,channelWrapper.getAPI_ORDER_ID());
                put(orderAmount,  channelWrapper.getAPI_AMOUNT());
                put(clientIp,channelWrapper.getAPI_Client_IP());
                put(subject,"name");
                put(body,"name");
                put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(pageUrl,channelWrapper.getAPI_WEB_URL());
                put(orderTime,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
//                String formatDateTimeStrByParam = DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss");
                put(orderExpireTime,sdf.format(afterDate));
                put(description,"name");
            }
        };
        log.debug("[红杉支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
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
        //最后一个&转换成#
        //signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "#" );
        //删除最后一个字符
        signSrc.deleteCharAt(signSrc.length()-1);
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[红杉支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(payParam),MediaType.APPLICATION_JSON_VALUE).trim();
        //if (!resultStr.contains("{") || !resultStr.contains("}")) {
        //   log.error("[红杉支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
        //   throw new PayException(resultStr);
        //}
        //JSONObject jsonObject = JSONObject.parseObject(resultStr);
        JSONObject jsonObject;
        try {
            jsonObject = JSONObject.parseObject(resultStr);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[红杉支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        if (null != jsonObject && jsonObject.containsKey("retCode") && "0000".equalsIgnoreCase(jsonObject.getString("retCode"))){
            if (jsonObject.containsKey("codeUrl") && StringUtils.isNotBlank(jsonObject.getString("codeUrl"))) {
                result.put( JUMPURL, jsonObject.getString("codeUrl"));                    
            }else if (jsonObject.containsKey("imgSrc") && StringUtils.isNotBlank(jsonObject.getString("imgSrc"))) {
                result.put( JUMPURL, jsonObject.getString("imgSrc"));
            }else if (jsonObject.containsKey("payUrl") && StringUtils.isNotBlank(jsonObject.getString("payUrl"))) {
                result.put( JUMPURL, jsonObject.getString("payUrl"));
            } else {
                log.error("[红杉支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
        }else {
            log.error("[红杉支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[红杉支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[红杉支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}