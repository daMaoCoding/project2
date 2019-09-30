package dc.pay.business.baofutong;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import dc.pay.utils.XmlUtil;
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
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Dec 5, 2017
 */
@RequestPayHandler("BAOFUTONG")
public final class BaoFuTongPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(BaoFuTongPayRequestHandler.class);

    // 字段名 变量名 必填  类型  说明
    // 业务参数
    // 接口类型    service 是   String(32)  参照4.1
    // 版本号 version 否   String(8)   版本号，version默认值是2.0
    // 字符集 charset 否   String(8)   可选值 UTF-8 ，默认为 UTF-8
    // 签名方式    sign_type   否   String(8)   签名类型，取值：MD5默认：MD5
    // 商户号 mch_id  是   String(32)  商户号，由平台分配
    // 大商户编号   groupno 否   String(32)  大商户模式下专用
    // 商户订单号   out_trade_no    是   String(32)  商户订单号 ,32个字符内、 可包含字母,确保在商户系统唯一
    // 商品描述    body    是   String(127) 商品描述
    // 附加信息    attach  否   String(127) 商户附加信息，可做扩展参数
    // 总金额 total_fee   是   Int 总金额，以分为单位，不允许包含任何字、符号
    // 终端IP    mch_create_ip   是   String(16)  订单生成的机器 IP
    // 前台地址    callback_url    否   String(255) 前端页面跳转的URL
    // 通知地址    notify_url  是   String(255) 接收平台通知的URL，需给绝对路径，255字符内格式，确保平台能通过互联网访问该地址
    // 订单生成时间  time_start  否   String(14)  订单生成时间，格式为yyyyMMddHHmmss
    // 订单超时时间  time_expire 否   String(14)  订单失效时间，格式为yyyyMMddHHmmss
    // 商品标记    goods_tag   否   String(32)  商品标记
    // 应用类型    device_info 是   String(16)  如果是用于苹果app应用里值为iOS_SDK；如果是用于安卓app应用里值为AND_SDK；如果是用于手机网站，值为iOS_WAP或AND_WAP均可
    // 应用名 mch_app_name    是   String(256) 如果是用于苹果或安卓app应用中，传分别对应在AppStore和安桌分发市场中的应用名
    // 应用标识    mch_app_id  是   String(128) 如果是用于苹果或安卓app应用中，苹果传IOS 应用
    // 随机字符串   nonce_str   是   String(32)  随机字符串，不长于 32 位
    // 签名  sign    是   String(32)  MD5签名结果
    private static final String service                ="service";
    private static final String version                 ="version";
    private static final String charset                ="charset";
    private static final String sign_type                ="sign_type";
    private static final String mch_id                ="mch_id";
//    private static final String groupno                ="groupno";
    private static final String out_trade_no                ="out_trade_no";
    private static final String body                ="body";
    private static final String attach                ="attach";
    private static final String total_fee                ="total_fee";
    private static final String mch_create_ip                ="mch_create_ip";
//    private static final String callback_url                ="callback_url";
    private static final String notify_url                ="notify_url";
//    private static final String time_start                ="time_start";
//    private static final String time_expire                ="time_expire";
//    private static final String goods_tag                ="goods_tag";
    private static final String device_info                ="device_info";
    private static final String mch_app_name                ="mch_app_name";
    private static final String mch_app_id                ="mch_app_id";
    private static final String nonce_str                ="nonce_str";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(service,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(version,"2.0");
                put(charset,"UTF-8");
                put(sign_type,"MD5");
                put(body,"name");
                put(mch_id, channelWrapper.getAPI_MEMBERID());
                put(out_trade_no,channelWrapper.getAPI_ORDER_ID());
                put(attach,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(total_fee,channelWrapper.getAPI_AMOUNT());
                put(mch_create_ip,  channelWrapper.getAPI_Client_IP());
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                //我平台定义：3 APP-Android，4 APP-IOS，5 APP-Other，6 WEB，7 Windows，8 Mac,9 WAP
                //应用类型    device_info 是   String(16)  如果是用于苹果app应用里值为iOS_SDK；如果是用于安卓app应用里值为AND_SDK；如果是用于手机网站，值为iOS_WAP或AND_WAP均可
                // 应用名 mch_app_name    是   String(256) 如果是用于苹果或安卓app应用中，传分别对应在AppStore和安桌分发市场中的应用名
               // 应用标识    mch_app_id  是   String(128) 如果是用于苹果或安卓app应用中，苹果传IOS 应用
                //安卓
                if ("3".equals(channelWrapper.getAPI_ORDER_FROM())) {
                    put(device_info,"AND_SDK");
                    put(mch_app_name,"APP-Android");
                    put(mch_app_id,"Android");
                //苹果
                }else if ("4".equals(channelWrapper.getAPI_ORDER_FROM())) {
                    put(device_info,"iOS_SDK");
                    put(mch_app_name,"APP-IOS");
                    put(mch_app_id,"IOS");
                //其他
                }else if (handlerUtil.isWapOrApp(channelWrapper)) {
                    put(device_info,"AND_WAP");
                    put(mch_app_name,"other");
                    put(mch_app_id,"other");
                }
                put(nonce_str,handlerUtil.getRandomStr(8));
            }
        };
        log.debug("[宝付通]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
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
        signSrc.append(key + "="+channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[宝付通]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        String resultStr = RestTemplateUtil.postXml(channelWrapper.getAPI_CHANNEL_BANK_URL(), XmlUtil.map2Xml(payParam, false, "xml", false));
//        String resultStr = RestTemplateUtil.postXml(channelWrapper.getAPI_CHANNEL_BANK_URL(), XmlUtil.map2Xml(payParam, false, "xml", true));RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
        //String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
        if (StringUtils.isBlank(resultStr)) {
            log.error("[宝付通]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
            //log.error("[宝付通]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
            //throw new PayException("返回空,参数："+JSON.toJSONString(map));
        }
        if (!resultStr.contains("result_code")) {
            log.error("[宝付通]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        Map<String, String> xml2Map = XmlUtil.xml2Map(resultStr);
        //只取正确的值，其他情况抛出异常
        if (null != xml2Map && xml2Map.containsKey("result_code") && "0".equalsIgnoreCase(xml2Map.get("result_code"))  && xml2Map.containsKey("pay_info") && StringUtils.isNotBlank(xml2Map.get("pay_info"))) {
            String code_url = xml2Map.get("pay_info");
//            result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
            result.put( JUMPURL, code_url);
            //if (handlerUtil.isWapOrApp(channelWrapper)) {
            //    result.put(JUMPURL, code_url);
            //}else{
            //    result.put(QRCONTEXT, code_url);
            //}
        }else {
            log.error("[宝付通]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[宝付通]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[宝付通]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}