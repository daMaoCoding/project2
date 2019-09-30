package dc.pay.business.zhuzhuzhifu;

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

import dc.pay.base.processor.ChannelWrapper;
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
import dc.pay.utils.XmlUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Sep 26, 2019
 */
@RequestPayHandler("ZHUZHUZHIFU")
public final class ZhuZhuZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(ZhuZhuZhiFuPayRequestHandler.class);

    //请求数据
    //请求地址： https://api.xmy77.com/auth/access-token
    //请求方式： GET / POST
    //请求参数：
    private static final String TOKEN_URL                ="https://api.xmy77.com/auth/access-token";
    //变量名 必填  类型  说明
    //appid    是    String    商户号
    private static final String appid                ="appid";
    //secretid    是    String    商户密钥
    private static final String secretid                ="secretid";
    
    //请求参数
    //请求网关： https://api.xmy77.com/sig/v2/unifiedorder
    //请求参数：POST/XML格式内容体进行请求
    //字段名 变量名 必填  类型  说明
    //版本号    version    否    String    接口版本号，默认：2
    private static final String version                ="version";
    //字符集    charset    否    String    可选值UTF-8 ，默认：UTF-8
    private static final String charset                ="charset";
    //签名方式    sign_type    否    String    签名类型，默认：MD5
    private static final String sign_type                ="sign_type";
    //商户号    mch_id    是    String    商户号，由平台分配
    private static final String mch_id                ="mch_id";
    //商户订单号    out_trade_no    是    String    商户系统内部订单号,确保在商户系统唯一
    private static final String out_trade_no                ="out_trade_no";
    //商品描述    body    是    String
    private static final String body                ="body";
    //商品描述    总金额    total_fee    是    Int    订单金额，以分为单位，不允许包含任何字、符号
    private static final String total_fee                ="total_fee";
    //交易类型    trade_type    是    String    交易类型，详见 “接口规则-交易类型”
    private static final String trade_type                ="trade_type";
    //终端IP    mch_create_ip    是    String    订单生成的IP地址
    private static final String mch_create_ip                ="mch_create_ip";
    //通知地址    notify_url    是    String    接收平台通知的URL，需给绝对路径，确保平台能通过互联网访问该地址
    private static final String notify_url                ="notify_url";
    //附加信息    attach    否    String    商户附加信息，可做扩展参数，支付成功后，原路返回
    private static final String attach                ="attach";
    //订单生成时间    time_start    否    String    订单生成时间，格式为yyyyMMddHHmmss，时区为GMT+8
//    private static final String time_start                ="time_start";
    //订单超时时间    time_expire    否    String    订单失效时间，格式为yyyyMMddHHmmss，时区为GMT+8
//    private static final String time_expire                ="time_expire";
    //设备号    device_info    否\    String    终端设备号
//    private static final String device_info                ="device_info";
    //操作员    op_user_id    否    String    操作员帐号,默认为商户号
//    private static final String op_user_id                ="op_user_id";
    //商品标记    goods_tag    否    String    商品标记
//    private static final String goods_tag                ="goods_tag";
    //商品ID    product_id    否    String    商户ID
//    private static final String product_id                ="product_id";
    //随机字符串    nonce_str    是    String    随机字符串
    private static final String nonce_str                ="nonce_str";
    //签名    sign    是    String    MD5签名结果，详见 “接口规则-安全规范”
//    private static final String sign                ="sign";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
//        String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
//        if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 3) {
//            log.error("[猪猪支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&机构号" );
//            throw new PayException("[猪猪支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&机构号" );
//        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(version, "1.0");
                put(charset, "UTF-8");
                put(sign_type, "MD5");
                put(mch_id, channelWrapper.getAPI_MEMBERID());
                put(out_trade_no, channelWrapper.getAPI_ORDER_ID());
                put(body, "name");
                put(total_fee, channelWrapper.getAPI_AMOUNT());
                put(mch_create_ip, channelWrapper.getAPI_Client_IP());
                put(notify_url, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(nonce_str, handlerUtil.getRandomStr(6));
                
//                put(trade_type, "ALIPAY_NATIVE");
                put(attach, channelWrapper.getAPI_ORDER_ID());
            }
        };
        log.debug("[猪猪支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
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
        //signSrc.deleteCharAt(paramsStr.length()-1);
        signSrc.append(key +"="+ channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[猪猪支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        HashMap<String, String> result = Maps.newHashMap();

//        String url = channelWrapper.getAPI_CHANNEL_BANK_URL() + "?token=" + getToken(channelWrapper);
        String url = channelWrapper.getAPI_CHANNEL_BANK_URL();
        if (false) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
        }else{
            
            String resultStr = RestTemplateUtil.postXml(url, XmlUtil.map2Xml(payParam, false, "xml", true));
//            String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(payParam),MediaType.APPLICATION_JSON_VALUE).trim();
//            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
            //String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
            //if (StringUtils.isBlank(resultStr)) {
            //    log.error("[猪猪支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            //    throw new PayException(resultStr);
            //    //log.error("[猪猪支付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
            //    //throw new PayException("返回空,参数："+JSON.toJSONString(map));
            //}
//            System.out.println("请求返回=========>"+resultStr);
            //if (!resultStr.contains("{") || !resultStr.contains("}")) {
            //   log.error("[猪猪支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            //   throw new PayException(resultStr);
            //}
            //JSONObject jsonObject = JSONObject.parseObject(resultStr);
            
            Map<String, String> responseMap = XmlUtil.xml2Map(resultStr);
            if (responseMap != null && responseMap.size() > 0 &&responseMap.containsKey("status") && responseMap.containsKey("result_code") && 
                    "0".equals(responseMap.get("status").toString()) && "0".equals(responseMap.get("result_code"))) {
                if (handlerUtil.isWEBWAPAPP_SM(channelWrapper) && StringUtils.isNotBlank((String) responseMap.get("code_url"))) {
                    result.put( JUMPURL, (String) responseMap.get("code_url"));
                }else if (handlerUtil.isWapOrApp(channelWrapper) && StringUtils.isNotBlank((String) responseMap.get("redirect_url"))) {
                    result.put( JUMPURL, (String) responseMap.get("redirect_url"));
                } else {
                    log.error("[猪猪支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
            }else {
                log.error("[猪猪支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[猪猪支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[猪猪支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
    
    public static String getToken(ChannelWrapper channelWrapper){
        Map<String, Object> payParam = new HashMap<>();
        payParam.put(appid, channelWrapper.getAPI_MEMBERID());
        payParam.put(secretid, channelWrapper.getAPI_KEY());
        String resultStr = RestTemplateUtil.postXml(TOKEN_URL, XmlUtil.map2Xml(payParam, false, "xml", false));
        Map<String, String> responseMap = XmlUtil.xml2Map(resultStr);
        if (responseMap != null && responseMap.containsKey("token")) {
            String token = (String) responseMap.get("token");
            return token;
        } else {
            return null;
        }
    }
    
}