package dc.pay.business.huibaozhifu;

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
import dc.pay.business.ruijietong.RuiJieTongUtil;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.RsaUtil;
import dc.pay.utils.ValidateUtil;
import dc.pay.utils.qr.QRCodeUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Mar 15, 2019
 */
@RequestPayHandler("HUIBAOZHIFU")
public final class HuiBaoZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(HuiBaoZhiFuPayRequestHandler.class);

    //字段 ⻓度 可否为空 注释
    //partner 32 否 商户合作号，由平台注册提供
    private static final String partner                ="partner";
    //input_charset 10 否 编码格式:UTF-8
    private static final String input_charset                ="input_charset";
    //sign_type 15 否 签名⽅式:SHA1WITHRSA
    private static final String sign_type                ="sign_type";
    //sign 256 否 签名字符串
    private static final String sign                ="sign";
    //request_time 20 可 YYMMDDHHmmss
    private static final String request_time                ="request_time";
    //content 1024 否 业务参数加密密⽂
    private static final String content                ="content";
    
    //字段 ⻓度 可否为空 注释
    //service 32 否 ⽀付宝⽀付:S1006
     private static final String service                 ="service";
    //out_trade_no 40 否 原始商户订单
     private static final String out_trade_no                 ="out_trade_no";
    //amount 40 否 ⾦额（0.01～9999999999.99）
     private static final String amount                 ="amount";
    //subject 20 否 商品名称(不要带特殊字符)
     private static final String subject                 ="subject";
    //sub_body 20 否 商品描述(不要带特殊字符)
     private static final String sub_body                 ="sub_body";
    //remark 20 可 备注
//     private static final String remark                 ="remark";
    //notify_url 1024 否 后台通知地址
     private static final String notify_url                 ="notify_url";
     
    //支付宝
    //ali_pay_type 10 否 ⽀付宝扫码:ali_sm
    private static final String ali_pay_type                 ="ali_pay_type";

    //银联扫码
    //china_bank_scan_type 10 否 银联扫码:china_bank_scan_sm
    private static final String china_bank_scan_type                 ="china_bank_scan_type";

    //银联快捷
    //tran_ip 20 否 ***.***.***.**
    private static final String tran_ip                 ="tran_ip";
    
    //网银类
//    //tran_ip 20 否 ***.***.***.**
//    private static final String tran_ip                 ="tran_ip";
    //bank_sn 32 可 银⾏编码
    private static final String bank_sn                 ="bank_sn";
    
    private static final String key        ="verfication_code";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        String api_KEY = channelWrapper.getAPI_KEY();
        if (null == api_KEY || !api_KEY.contains("-") || api_KEY.split("-").length != 2) {
            log.error("[汇宝支付]-[请求支付]-“密钥（私钥）”输入数据格式为【中间使用-分隔】：MD5key-RSA商戶私鑰" );
            throw new PayException("[汇宝支付]-[请求支付]-“密钥（私钥）”输入数据格式为【中间使用-分隔】：MD5key-RSA商戶私鑰" );
        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(service,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
                put(out_trade_no,channelWrapper.getAPI_ORDER_ID());
                put(amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                if (handlerUtil.isZFB(channelWrapper)) {
                    put(ali_pay_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
                    put(sign_type,"SHA1WITHRSA");
                }else if (handlerUtil.isYLSM(channelWrapper)) {
                    put(china_bank_scan_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
                    put(sign_type,"SHA1WITHRSA");
                }else if (handlerUtil.isWebYlKjzf(channelWrapper)) {
                    put(tran_ip,channelWrapper.getAPI_Client_IP());
                    put(sign_type,"MD5");
                }else if (handlerUtil.isWY(channelWrapper)) {
                    put(tran_ip,channelWrapper.getAPI_Client_IP());
                    put(bank_sn,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
                    put(sign_type,"MD5");
                }
                 put(subject,"name");
                 put(sub_body,"name");
                 put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                 put(request_time,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
                 put(partner,channelWrapper.getAPI_MEMBERID());
                 put(input_charset,"UTF-8");
            }
        };
        log.debug("[汇宝支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
        Map<String,String> map = new TreeMap<>(api_response_params);
        map.remove(input_charset);
        map.remove(sign_type);
        List paramKeys = MapUtils.sortMapByKeyAsc(map);
        StringBuilder signSrc = new StringBuilder();
        String signMd5="";
        if (handlerUtil.isWebYlKjzf(channelWrapper) || handlerUtil.isWY(channelWrapper)) {
            for (int i = 0; i < paramKeys.size(); i++) {
                if (!bank_sn.equals(paramKeys.get(i)) && !sub_body.equals(paramKeys.get(i))) {
                    signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
                }
            }
            signSrc.append(key).append("=").append(channelWrapper.getAPI_KEY().split("-")[0]);
            String paramsStr = signSrc.toString();
            signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        }else {
            for (int i = 0; i < paramKeys.size(); i++) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
            signSrc.deleteCharAt(signSrc.length()-1);
            String paramsStr = signSrc.toString();
            try {
                signMd5 = RsaUtil.signByPrivateKey(paramsStr,channelWrapper.getAPI_KEY().split("-")[1],"SHA1WithRSA");    // 签名
            } catch (Exception e) {
                log.error("[汇宝支付]-[请求支付]-2.生成加密URL签名出错，签名出错：{}",e.getMessage(),e);
                throw new PayException(e.getMessage(),e);
            }
        }
        log.debug("[汇宝支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
//        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        Map<String,String> map = new TreeMap<>(payParam);
        map.remove(input_charset);
        map.remove(sign_type);
        List<String> paramKeys = MapUtils.sortMapByKeyAsc(map);
        StringBuilder signSrc = new StringBuilder();
        Map<String, String> param = new TreeMap<String, String>();
        if (handlerUtil.isWebYlKjzf(channelWrapper) || handlerUtil.isWY(channelWrapper)) {
            for (int i = 0; i < paramKeys.size(); i++) {
                param.put(paramKeys.get(i),payParam.get(paramKeys.get(i)));
            }
        }else {
            for (int i = 0; i < paramKeys.size(); i++) {
                signSrc.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
            }
            signSrc.deleteCharAt(signSrc.length()-1);
            String paramsStr = signSrc.toString();
            try {
                byte[] dataStr = RuiJieTongUtil.encryptByPublicKey(paramsStr.getBytes(RuiJieTongUtil.CHARSET),channelWrapper.getAPI_PUBLIC_KEY());
                param.put(content,java.util.Base64.getEncoder().encodeToString(dataStr));
            } catch (Exception e) {
                e.printStackTrace();
                log.error("[汇宝支付]-[请求支付]-2.生成加密URL签名出错，签名出错：{}",e.getMessage(),e);
                throw new PayException(e.getMessage(),e);
            }    
        }
        param.put(partner,payParam.get(partner));
        param.put(input_charset,payParam.get(input_charset));
        param.put(sign_type,payParam.get(sign_type));
        param.put(sign,pay_md5sign);
        param.put(request_time,payParam.get(request_time));
        HashMap<String, String> result = Maps.newHashMap();
        if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWebYlKjzf(channelWrapper)) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),param).toString());
        }else{
            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), param,"UTF-8");
            //if (StringUtils.isBlank(resultStr)) {
            //    log.error("[汇宝支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            //    throw new PayException(resultStr);
            //    //log.error("[汇宝支付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
            //    //throw new PayException("返回空,参数："+JSON.toJSONString(map));
            //}
//            System.out.println("请求返回=========>"+resultStr);
            //if (!resultStr.contains("{") || !resultStr.contains("}")) {
            //   log.error("[汇宝支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            //   throw new PayException(resultStr);
            //}
            //JSONObject jsonObject = JSONObject.parseObject(resultStr);
            try {
                JSONObject jsonObject = JSONObject.parseObject(resultStr);
              //只取正确的值，其他情况抛出异常
                if (null != jsonObject && jsonObject.containsKey("is_succ") && "T".equalsIgnoreCase(jsonObject.getString("is_succ"))  && 
                        jsonObject.containsKey("response") && StringUtils.isNotBlank(jsonObject.getString("response"))
                ){
                    byte[] resultf = RuiJieTongUtil.decryptByPrivateKey(org.apache.shiro.codec.Base64.decode(jsonObject.getString("response")),channelWrapper.getAPI_KEY().split("-")[1]);
                    String resultData = new String(resultf, RuiJieTongUtil.CHARSET);
                    JSONObject jsonObject2 = JSONObject.parseObject(resultData);
                    if (handlerUtil.isZFB(channelWrapper)) {
                        if (jsonObject2.containsKey("base64QRCode") && StringUtils.isNotBlank(jsonObject2.getString("base64QRCode"))){
                            String qr = QRCodeUtil.decodeByBase64(jsonObject2.getString("base64QRCode"));
                            if (StringUtils.isBlank(qr)) {
                                log.error("[汇宝支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultData) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                                throw new PayException(resultData);
                                //log.error("[汇宝支付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
                                //throw new PayException("返回空,参数："+JSON.toJSONString(map));
                            }
//                            result.put( JUMPURL, qr);
                            result.put( handlerUtil.isWEBWAPAPP_SM(channelWrapper) ? QRCONTEXT : JUMPURL , qr);
                        }else if (jsonObject2.containsKey("ali_pay_sm_url") && StringUtils.isNotBlank(jsonObject2.getString("ali_pay_sm_url"))) {
                            result.put( JUMPURL, jsonObject2.getString("ali_pay_sm_url"));
                        } else {
                            log.error("[汇宝支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultData) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                            throw new PayException(resultData);
                        }
                    }else if (handlerUtil.isYL(channelWrapper)) {
                        if (jsonObject2.containsKey("base64QRCode") && StringUtils.isNotBlank(jsonObject2.getString("base64QRCode"))){
                            String qr = QRCodeUtil.decodeByBase64(jsonObject2.getString("base64QRCode"));
                            if (StringUtils.isBlank(qr)) {
                                log.error("[汇宝支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultData) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                                throw new PayException(resultData);
                                //log.error("[汇宝支付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
                                //throw new PayException("返回空,参数："+JSON.toJSONString(map));
                            }
//                            result.put( JUMPURL, qr);
                            result.put( QRCONTEXT, qr);
                        }else if (jsonObject2.containsKey("china_bank_scan_sm_url") && StringUtils.isNotBlank(jsonObject2.getString("china_bank_scan_sm_url"))) {
                            result.put( JUMPURL, jsonObject2.getString("china_bank_scan_sm_url"));
                        } else {
                            log.error("[汇宝支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultData) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                            throw new PayException(resultData);
                        }
                    }
                }else {
                    log.error("[汇宝支付]-[请求支付]-3.5.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
            } catch (Exception e) {
                e.printStackTrace();
                log.error("[汇宝支付]-[请求支付]-3.6.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[汇宝支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[汇宝支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}