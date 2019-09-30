package dc.pay.business.xinzhangtuozhifu;

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
import dc.pay.utils.RsaUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 
 * @author andrew
 * Aug 12, 2019
 */
@RequestPayHandler("XINZHANGTUOZHIFU")
public final class XinZhangTuoZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XinZhangTuoZhiFuPayRequestHandler.class);

    //参数名称 是否必须 数据类型 默认值 描述
    //merNo true string 商户号（平台提供）
    private static final String merNo                ="merNo";
    //channelNum true string 支付通道标识（通道标识列表）
    private static final String channelNum                ="channelNum";
    //orderNo true string 订单号
    private static final String orderNo                ="orderNo";
    //amount true number 交易金额(元)
    private static final String amount                ="amount";
    //notifyUrl true string 异步回调地址
    private static final String notifyUrl                ="notifyUrl";
    //returnUrl true string 支付成功返回地址
    private static final String returnUrl                ="returnUrl";
    //nonceStr true string 32位随机数
    private static final String nonceStr                ="nonceStr";
    //goodsName true string 订单名称
    private static final String goodsName                ="goodsName";
    //clientIp true string 用户支付IP地址
    private static final String clientIp                ="clientIp";
    //sign true string RSA签名
    private static final String sign                ="sign";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
        if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 2) {
            log.error("[新掌托支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&通道类型编码值（向第三方获取当前使用通道编码值）" );
            throw new PayException("[新掌托支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&通道类型编码值（向第三方获取当前使用通道编码值）" );
        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merNo, channelWrapper.getAPI_MEMBERID().split("&")[0]);
//                put(channelNum,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(channelNum,channelWrapper.getAPI_MEMBERID().split("&")[1]);
                put(orderNo,channelWrapper.getAPI_ORDER_ID());
                put(amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(returnUrl,channelWrapper.getAPI_WEB_URL());
                put(nonceStr,XinZhangTuoZhiFuPayRequestHandler.this.handlerUtil.getRandomNumber(32));
                put(goodsName,"name");
                put(clientIp,channelWrapper.getAPI_Client_IP());
            }
        };
        log.debug("[新掌托支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
//        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
//        StringBuilder signSrc = new StringBuilder();
//        for (int i = 0; i < paramKeys.size(); i++) {
//            if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
//                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
//            }
//        }
//        //最后一个&转换成#
//        //signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "#" );
//        //删除最后一个字符
//        signSrc.deleteCharAt(signSrc.length()-1);
////        signSrc.append(key + channelWrapper.getAPI_KEY());
//        String paramsStr = signSrc.toString();
//        String signMd5 = CmcPayTool.getSignString(api_response_params, channelWrapper.getAPI_KEY());
        String signMd5 = CmcPayTool.getSignString(api_response_params, channelWrapper.getAPI_KEY());
////        RsaUtil.signByPrivateKey(data, privateKey, sigType)
////        String data, String privateKey,  String input_charset
////        String signMd5 = RsaUtil.signByPrivateKey2(paramsStr, channelWrapper.getAPI_KEY(), "utf-8");
////        String data, String privateKey,String sigType
//        String signMd5 = null;
//        try {
////            signMd5 = RsaUtil.signByPrivateKey(paramsStr, channelWrapper.getAPI_KEY(), "SHA1WithRSA");
//            signMd5 = RsaUtil.signByPrivateKey2(paramsStr, channelWrapper.getAPI_KEY());
//        } catch (Exception e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
        
        log.debug("[新掌托支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        HashMap<String, String> result = Maps.newHashMap();
//        if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
        if (false) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
        }else{
            
//            String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(payParam),MediaType.APPLICATION_FORM_URLENCODED_VALUE);
            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
//            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
//            String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
            //if (StringUtils.isBlank(resultStr)) {
            //    log.error("[新掌托支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            //    throw new PayException(resultStr);
            //    //log.error("[新掌托支付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
            //    //throw new PayException("返回空,参数："+JSON.toJSONString(map));
            //}
//            System.out.println("请求返回=========>"+resultStr);
            //if (!resultStr.contains("{") || !resultStr.contains("}")) {
            //   log.error("[新掌托支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            //   throw new PayException(resultStr);
            //}
            //JSONObject jsonObject = JSONObject.parseObject(resultStr);
            JSONObject jsonObject;
            try {
                jsonObject = JSONObject.parseObject(resultStr);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("[新掌托支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            //只取正确的值，其他情况抛出异常
            //if (null != jsonObject && jsonObject.containsKey("result_code") && "SUCCESS".equalsIgnoreCase(jsonObject.getString("result_code"))  && 
            //(jsonObject.containsKey("qrcode") && StringUtils.isNotBlank(jsonObject.getString("qrcode")) || 
            // jsonObject.containsKey("pay_url") && StringUtils.isNotBlank(jsonObject.getString("pay_url")))
            //){
            
            if (handlerUtil.isWapOrApp(channelWrapper) && null != jsonObject && jsonObject.containsKey("code") && "00".equalsIgnoreCase(jsonObject.getString("code")) && StringUtils.isNotBlank(jsonObject.getString("redirectUrl")+"")) {
                result.put( HTMLCONTEXT, jsonObject.getString("redirectUrl")+"");
            }else if (null != jsonObject && jsonObject.containsKey("code") && "00".equalsIgnoreCase(jsonObject.getString("code"))  && jsonObject.containsKey("sign") && StringUtils.isNotBlank(jsonObject.getString("sign"))) {
                //表示请求成功  需要用平台公钥解密 密文
                String my_sign= jsonObject.getString(sign);
                String my_data = null;
                try {
                    my_data = new String(RSAUtils.decryptByPublicKey(Base64.decode(my_sign), channelWrapper.getAPI_PUBLIC_KEY()));
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("[新掌托支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
//                System.out.println("平台公钥解密："+my_data);//私钥解密
                //获取解密后数据
                Map<String,Object> map= StringJSONUtil.paramsParse(my_data);
//                System.out.println(map.toString());
////                JSONObject jsonObject2 = JSONObject.parseObject(my_data);
//                System.out.println("【==============解密后获取信息===============】");
//                System.out.println(jsonObject2.toString());
                
//                if (handlerUtil.isWEBWAPAPP_SM(channelWrapper) && (handlerUtil.isZFB(channelWrapper) || handlerUtil.isWxSM(channelWrapper)) && StringUtils.isNotBlank(jsonObject2.getString("redirectUrl"))) {
                if (handlerUtil.isWEBWAPAPP_SM(channelWrapper) && (handlerUtil.isZFB(channelWrapper) || handlerUtil.isWxSM(channelWrapper)) && StringUtils.isNotBlank(map.get("redirectUrl")+"")) {
                    result.put( JUMPURL, map.get("redirectUrl")+"");
                } else if (handlerUtil.isYLSM(channelWrapper) && StringUtils.isNotBlank(map.get("qrCodeUrl")+"")) {
                    result.put( QRCONTEXT, map.get("qrCodeUrl")+"");
                } else if (StringUtils.isNotBlank(map.get("redirectUrl")+"")) {
                    result.put( JUMPURL, map.get("redirectUrl")+"");
                } else if (StringUtils.isNotBlank(map.get("qrCodeUrl")+"")) {
                    result.put( QRCONTEXT, map.get("qrCodeUrl")+"");
                }
            }else {
                log.error("[新掌托支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[新掌托支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[新掌托支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}