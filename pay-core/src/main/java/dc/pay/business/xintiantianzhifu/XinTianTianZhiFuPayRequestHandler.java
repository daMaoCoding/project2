package dc.pay.business.xintiantianzhifu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.codec.binary.Base64;
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
import dc.pay.utils.AES128ECB;
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.RsaUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Apr 16, 2019
 */
@RequestPayHandler("XINTIANTIANZHIFU")
public final class XinTianTianZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XinTianTianZhiFuPayRequestHandler.class);

    //1.HEAD报文
    //说明：根据业务组装不加密报文
    //参数  类型  是否必填    描述  示例
    //method  VARCHAR(32) 是   方法名称    参考交易接口实现目录
//    private static final String method                ="method";
    //channelNo   VARCHAR(32) 是   商户登录账号编号（商户登录帐号）    CH100001
//    private static final String channelNo                ="channelNo";
    //userReqNo   VARCHAR(32) 是   商户方请求流水号    
//    private static final String userReqNo                ="userReqNo";
    //reqTime CHAR(14)    是   商户方请求时间 yyyyMMddHHmmss.s
//    private static final String reqTime                ="reqTime";
    //encryptKey  VARCHAR(1024)   是   RSA加密后的AES key  
//    private static final String encryptKey                ="encryptKey";
    //version VARCHAR(6)  是   版本号 V1.0.0
//    private static final String version                ="version";
    //sign    VARCHAR(1024)   是   签名串 
//    private static final String sign                ="sign";

    private static final String methodName                ="trade.quick";
    
    //4.2.4 支付宝/微信支付接口
    //1.处理流程  
    //2.请求报文  body
    //参数  类型  是否必填    描述  示例
    //merchantNo  VARCHAR(32) 是   商户号 1026804123154124800
    private static final String merchantNo                ="merchantNo";
    //userOrderNo VARCHAR(32) 是   商户方请求订单号    
    private static final String userOrderNo                ="userOrderNo";
    //payCode VARCHAR(20) 是   支付宝：ZFBSMZF    微信：ZFBSMZF  微信:    payCode=WXSMZF    支付宝:    payCode=ZFBSMZF
    private static final String payCode                ="payCode";
    //orderAmt    DECIMAL(12,2)   是   交易金额    
    private static final String orderAmt                ="orderAmt";
    //orderTitle  VARCHAR(256)    是   订单标题    
    private static final String orderTitle                ="orderTitle";
    //orderDesc   VARCHAR(512)    否   订单描述    
//    private static final String orderDesc                ="orderDesc";
    //notifyUrl   VARCHAR(255)    是   通知地址:接收平台通知的 URL，需给绝对路径，255 字符内格式，确保平台能通过互联网访问该地址   
    private static final String notifyUrl                ="notifyUrl";
    //returnUrl   VARCHAR(255)    是   前台地址:交易完成后跳转的 URL，需给绝对路径，255 字字符格式式如:http://wap.pp pay.com/callback.asp 注:该地址只作为前端页面的一个跳转，须使用notifyurl通知结果作为支付最终结果。 
    private static final String returnUrl                ="returnUrl";
    
    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
        if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 2) {
            log.error("[新天天支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号merchantNo&渠道号channelNo" );
            throw new PayException("[新天天支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&机构号" );
        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merchantNo, channelWrapper.getAPI_MEMBERID().split("&")[0]);
                put(userOrderNo,channelWrapper.getAPI_ORDER_ID());
                put(payCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(orderAmt,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(orderTitle,"name");
                put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(returnUrl,channelWrapper.getAPI_WEB_URL());
            }
        };
        log.debug("[新天天支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        Map<String, Object> message = new HashMap<String, Object>();
        message.put("body", api_response_params);
        String signMd5="";
        try {
            signMd5 = RsaUtil.signByPrivateKey(JSON.toJSONString(message),channelWrapper.getAPI_KEY(),"SHA1withRSA");    // 签名
            //signMd5  = RsaUtil.signByPrivateKey(signInfo,channelWrapper.getAPI_KEY());
            //signMd5  = RsaUtil.signByPrivateKey2(signInfo,channelWrapper.getAPI_KEY());
           // signMd5  = RsaUtil.signByPublicKey(signInfo,channelWrapper.getAPI_PUBLIC_KEY());
        } catch (Exception e) {
            log.error("[新天天支付]-[请求支付]-2.生成加密URL签名出错，签名出错：{}",e.getMessage(),e);
            throw new PayException(e.getMessage(),e);
        }
        log.debug("[新天天支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
//        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        //设置body加密后转换成Message对象
        Map<String, Object> message = new HashMap<String, Object>();
        message.put("body", payParam);
        
        ParamsMessage params = new ParamsMessage();
        HeaderMessage head = new HeaderMessage();
        String aesKey = handlerUtil.getRandomStr(16);
        try {
            String encryptData = new AES128ECB().Encrypt(JSON.toJSONString(message), aesKey);
            String encrtptKey = RsaUtil.encryptToBase64(aesKey, channelWrapper.getAPI_PUBLIC_KEY());
            head.setSign(pay_md5sign);
            head.setEncryptKey(encrtptKey);
            params.setBody(encryptData);
        } catch (Exception e2) {
            e2.printStackTrace();
            log.error("[新天天支付]-[请求支付]-3.0.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(message) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(e2.getMessage(),e2);
        }
        head.setMethod(methodName);
        head.setVersion("V1.0.0");
        head.setReqTime( DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
        head.setUserReqNo(DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
        head.setChannelNo(channelWrapper.getAPI_MEMBERID().split("&")[1]);
        head.setMethod(methodName);
        params.setHead(head);
        String str = JSON.toJSONString(params);
        String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), str,MediaType.APPLICATION_JSON_VALUE).trim();
        JSONObject jsonObject;
        try {
            jsonObject = JSONObject.parseObject(resultStr);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[新天天支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        //只取正确的值，其他情况抛出异常
        if (null != jsonObject && jsonObject.containsKey("head") && 
           jsonObject.getJSONObject("head").containsKey("respCode") && StringUtils.isNotBlank(jsonObject.getJSONObject("head").getString("respCode")) && "000000".equalsIgnoreCase(jsonObject.getJSONObject("head").getString("respCode"))
        ){
            try {
                Encrypter encrypter = new Encrypter(channelWrapper.getAPI_PUBLIC_KEY(), channelWrapper.getAPI_KEY());
                byte[] decodeBase64KeyBytes = Base64.decodeBase64(jsonObject.getJSONObject("head").getString("encryptKey").getBytes("utf-8"));
                byte[] merchantAESKeyBytes = encrypter.RSADecrypt(decodeBase64KeyBytes);
                // 使用base64解码商户请求报文
                byte[] decodeBase64DataBytes = Base64.decodeBase64(jsonObject.getString("body").getBytes("utf-8"));
                byte[] realText = encrypter.AESDecrypt(decodeBase64DataBytes, merchantAESKeyBytes);
                jsonObject = JSON.parseObject(new String(realText, "utf-8"));
            } catch (Exception e) {
                e.printStackTrace();
                log.error("[新天天支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            String code_url = jsonObject.get("payStr")==null?"":jsonObject.get("payStr").toString();
            result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
        }else {
            log.error("[新天天支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[新天天支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[新天天支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}