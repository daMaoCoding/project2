package dc.pay.business.caimengzhifu;

import java.util.Base64;
import java.util.LinkedHashMap;
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
import dc.pay.business.ruijietong.RuiJieTongUtil;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 
 * @author andrew
 * Aug 20, 2019
 */
@RequestPayHandler("CAIMENGZHIFU")
public final class CaiMengZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(CaiMengZhiFuPayRequestHandler.class);

    //3.1.2 请求 3.1.2.1 请求格式 
    //参数代码 参数名称 类型 必须 备注 
    //merId 商户编号 String 是 平台分配的商户 ID 
    private static final String merId                ="merId";
    //version 版本号 String 是 固定值：1.1 
    private static final String version                ="version";
    //data 业务加密数据 String 是
    private static final String data                ="data";
    
     //Data 业务数据 
     //参数代码 参数名称 类型 必须 参与签名 备注 
     //merOrderNo 商户订单号 String 是 是 
    private static final String merOrderNo                ="merOrderNo";
     //amount 支付金额 Decimal 是 是 单位（元）
    private static final String amount                ="amount";
     //payType 支付类型 String 是 是 固定值：ebank 
    private static final String payType                ="payType";
     //businessType 订单类型 Integer 是 是 1.1版本后新增订 单类型：1（对 私）,2（对公）。 如果版本号是1.0 则默认 1（对私）。 目前统一填写对 私：1 
    private static final String businessType                ="businessType";
     //notifyUrl 回调地址 String 是 是
    private static final String notifyUrl                ="notifyUrl";
     //expireTime 过期时间 Int 是 是 订单有效时间（单 位：分钟） 
    private static final String expireTime                ="expireTime";
     //bankCode 银行编码 String 是 是 固定值： SPABANK
    private static final String bankCode                ="bankCode";
     //orderIp 订单请求 IP String 是 是 
    private static final String orderIp                ="orderIp";
     //submitTime 提交时间 String 是 是 时间戳格式(毫 秒)，与服务器时 间不能相差一天
    private static final String submitTime                ="submitTime";
     //returnViewUrl 回显地址 String 否 否 
//    private static final String returnViewUrl                ="returnViewUrl";
     //remarks 备注 String 否 否
//    private static final String remarks                ="remarks";
    //sign 签名信息 String 是 否 参照章节：《2.3》 
//    private static final String sign                ="sign";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
        if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 2) {
            log.error("[彩盟支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&通道类型编码值（向第三方获取当前使用通道编码值）" );
            throw new PayException("[彩盟支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&通道类型编码值（向第三方获取当前使用通道编码值）" );
        }
        String api_KEY = channelWrapper.getAPI_KEY();
        if (null == api_KEY || !api_KEY.contains("-") || api_KEY.split("-").length != 2) {
            log.error("[彩盟支付]-[请求支付]-“密钥（私钥）”输入数据格式为【中间使用-分隔】：支付MD5key-RSA私钥" );
            throw new PayException("[彩盟支付]-[请求支付]-“密钥（私钥）”输入数据格式为【中间使用-分隔】：支付MD5key-RSA私钥" );
        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
               put(merOrderNo, channelWrapper.getAPI_ORDER_ID());
               put(amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
               put(payType,channelWrapper.getAPI_MEMBERID().split("&")[1].trim());
//               put(payType,"ebank");
               put(businessType,"1");
               put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
               put(expireTime,"8");
               put(bankCode,"SPABANK");
               put(orderIp,channelWrapper.getAPI_Client_IP());
               put(submitTime,System.currentTimeMillis()+"");
            }
        };
        log.debug("[彩盟支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
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
        signSrc.append(key +"="+ channelWrapper.getAPI_KEY().split("-")[0]);
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[彩盟支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
//        byte[] resultbyte = RuiJieTongUtil.decryptByPrivateKey(org.apache.shiro.codec.Base64.decode(JSONObject.toJSONString(payParam)),channelWrapper.getAPI_KEY().split("-")[1]);
//        byte[] resultbyte = RuiJieTongUtil.encryptByPublicKey(org.apache.shiro.codec.Base64.decode(JSONObject.toJSONString(payParam)),channelWrapper.getAPI_KEY().split("-")[1]);
        
        // 使用非对称加密加密此dataMap
        String my_data = null;
        try {
            byte[] contentAndPubkeyBytes = RuiJieTongUtil.encryptByPublicKey(JSON.toJSONString(new TreeMap<>(payParam)).getBytes(RuiJieTongUtil.CHARSET),channelWrapper.getAPI_PUBLIC_KEY());
            my_data = Base64.getEncoder().encodeToString(contentAndPubkeyBytes);

        } catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        Map<String,String> map = new LinkedHashMap<>();
        map.put(merId, channelWrapper.getAPI_MEMBERID().split("&")[0].trim());
        map.put(version, "1.1");
        map.put(data, my_data);

        Map<String,String> result = Maps.newHashMap();
//        if(HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)){
        if(false){
            StringBuffer htmlContent = HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), map);
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
            //保存第三方返回值
            result.put(HTMLCONTEXT,htmlContent.toString());
        }else{
//            String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(payParam),MediaType.APPLICATION_JSON_VALUE);
//            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), map,"UTF-8");
            String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), map);
            //if (StringUtils.isBlank(resultStr)) {
            //    log.error("[彩盟支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            //    throw new PayException(resultStr);
            //    //log.error("[彩盟支付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
            //    //throw new PayException("返回空,参数："+JSON.toJSONString(map));
            //}
//            System.out.println("请求返回=========>"+resultStr);
            //if (!resultStr.contains("{") || !resultStr.contains("}")) {
            //    log.error("[彩盟支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            //    throw new PayException(resultStr);
            //}
            JSONObject jsonObject = null;
            try {
                jsonObject = JSONObject.parseObject(new String(resultStr.getBytes("ISO-8859-1"), "GBK"));
            } catch (Exception e) {
                log.error("[彩盟支付]-[请求支付]-3.3.发送支付请求，及获取支付请求出错，订单号：" + channelWrapper.getAPI_ORDER_ID() + ",通道名称：" + channelWrapper.getAPI_CHANNEL_BANK_NAME() + ",postUrl:" + channelWrapper.getAPI_CHANNEL_BANK_URL() + ",payForManagerCGIResultJsonObj" + e.getMessage(), e);
                //log.error("[盛盈付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + resultStr + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(e.getMessage(),e);
            }          
            //只取正确的值，其他情况抛出异常
            if (null != jsonObject && jsonObject.containsKey("code") && "200".equalsIgnoreCase(jsonObject.getString("code"))  && 
            (jsonObject.containsKey("data") && StringUtils.isNotBlank(jsonObject.getString("data")) || 
             jsonObject.getJSONObject("data").containsKey("payUrl") && StringUtils.isNotBlank(jsonObject.getJSONObject("data").getString("payUrl")))
            ){
//            if (null != jsonObject && jsonObject.containsKey("code") && "200".equalsIgnoreCase(jsonObject.getString("code"))  && jsonObject.containsKey("payUrl") && StringUtils.isNotBlank(jsonObject.getString("payUrl"))) {
                    result.put( JUMPURL, jsonObject.getJSONObject("data").getString("payUrl"));
                    //if (handlerUtil.isWapOrApp(channelWrapper)) {
                    //    result.put(JUMPURL, code_url);
                    //}else{
                    //    result.put(QRCONTEXT, code_url);
                    //}
                //按不同的请求接口，向不同的属性设置值
                //if(channelWrapper.getAPI_CHANNEL_BANK_NAME().contains(PayEumeration.CHANNEL_TYPE._WAP_.name())||channelWrapper.getAPI_CHANNEL_BANK_NAME().contains(PayEumeration.CHANNEL_TYPE._WAPAPP_.name())) {
                //    result.put(JUMPURL, jsonObject.getString("barCode"));
                //}else{
                //    result.put(QRCONTEXT, jsonObject.getString("barCode"));
                //}
//                result.put("第三方返回",jsonObject.toString()); //保存全部第三方信息，上面的拆开没必要
            }else {
                log.error("[彩盟支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
        }
        List<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[彩盟支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[彩盟支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}