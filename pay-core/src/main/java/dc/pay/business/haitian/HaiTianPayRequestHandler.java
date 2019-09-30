package dc.pay.business.haitian;

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
import dc.pay.utils.UnicodeUtil;
import dc.pay.utils.ValidateUtil;
import dc.pay.utils.qr.QRCodeUtil;

/**
 *
 * 
 * @author kevin
 * Aug 21, 2018
 */
@RequestPayHandler("HAITIAN")
public final class HaiTianPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(HaiTianPayRequestHandler.class);

    //NO  参数名称    参数含义    必填  数据类型    说明
    //1.  transCode   交易码 Y   String(3)   001：移动支付
    private static final String      transCode         = "transCode";
    //2.  service 服务类型    Y   String(4)   0002 微信扫码支付    0005 微信H5    0010 支付宝扫码支付    0011 支付宝 H5    0015 QQ钱包支付    0016 QQH5    0020 百度钱包    0030 京东金融    0040 银联钱包    0041 银联钱包小额
    private static final String      service         = "service";
    //3.  customerNo  商户编号    Y   String(30)  由我司提供。
    private static final String      customerNo         = "customerNo";
    //4.  externalId  订单号 Y   String（32）  请求方提供，务必保证唯一性.
    private static final String      externalId         = "externalId";
    //5.  transAmount 交易金额    Y   string  正整数。分为单位。
    private static final String      transAmount         = "transAmount";
    //6.  description 商品描述信息  N   String(50)  
    private static final String      description         = "description";
    //7.  reqDate 请求日期    Y   String(8)   格式yyyyMMdd
    private static final String      reqDate         = "reqDate";
    //8.  reqTime 请求时间    Y   String(6)   格式hhMMss。
    private static final String      reqTime         = "reqTime";
    //9.  callbackUrl 页面跳转地址  N   String(255) 页面同步跳转地址
    private static final String      callbackUrl         = "callbackUrl";
    //10. bgReturnUrl 回调地址    Y   String(255) 异步通知地址
    private static final String      bgReturnUrl         = "bgReturnUrl";
    //13. requestIp   来源IP    Y   String(20)  请求方公网IP,生产环境只有经过允许的IP方可请求交易
    private static final String      requestIp         = "requestIp";
    //14. userId  商户端用户真实IP   N   String（32）  获取用户终端的真实外网IP，当service字段为0005时，userId字段为必填。
    private static final String      userId         = "userId";
    //15. sign    签名  Y   String(100) 通过签名算法计算得出
//    private static final String      sign         = "sign";
    
    //支付宝类
    //参数名    是    否    必 须    类型 说明
    //merchant 是 string(32) 商户名
    private static final String merchant            ="merchant";
    //orderNo 是 string(32) 商户订单编号，不可重复
    private static final String orderNo            ="orderNo";
    //amount 是 decimal(11,2) 订单金额，保留两位小数
    private static final String amount            ="amount";
    //notify 是 string(255) 后台通知URL，订单完成后会通到此URL
    private static final String notify            ="notify";
    //payType 是 string    支付类型 (h5 | qr | we | up | rp | r5) h5=>支付宝h5 qr=>    支付宝扫码 we=>微信支付 up=>云闪付 rp=>支付宝红包    r5=>支付宝红包(WAP)
    private static final String payType            ="payType";
    //isManual 否 int 是否手动补单；已弃用，所有提单均做为新订单
//    private static final String isManual            ="isManual";
    //extra 否 json string 附加信息；所有字段均为可选；
//    private static final String extra            ="extra";
    //method: createOrd
    private static final String method            ="method";
    private static final String params            ="params";
    
    private static final String      key         = "key";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                if (handlerUtil.isZFB(channelWrapper)) {
                    put(merchant,channelWrapper.getAPI_MEMBERID());
                    put(orderNo,channelWrapper.getAPI_ORDER_ID());
                    put(amount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                    put(notify,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                    put(payType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                }else {
                    put(transCode,"001");
                    put(service,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    put(customerNo,channelWrapper.getAPI_MEMBERID());
                    put(externalId,channelWrapper.getAPI_ORDER_ID());
                    put(transAmount,channelWrapper.getAPI_AMOUNT());
                    put(description,"name");
                    put(reqDate,DateUtil.formatDateTimeStrByParam("yyyyMMdd"));
                    put(reqTime,DateUtil.formatDateTimeStrByParam("HHmmss"));
                    put(callbackUrl,channelWrapper.getAPI_WEB_URL());
                    put(bgReturnUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
//                put(requestIp,channelWrapper.getAPI_Client_IP());
                    //。 2019/3/20 15:49:22
                    //随便写一个你们已报备的值
                    put(requestIp,"52.175.24.192");
                    put(userId,channelWrapper.getAPI_Client_IP());
                }
            }
        };
        log.debug("[海天]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
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
        
        if (handlerUtil.isZFB(channelWrapper)) {
            //删除最后一个字符
            signSrc.deleteCharAt(signSrc.length()-1);
            signSrc.append(channelWrapper.getAPI_KEY());
        }else {
            signSrc.append(key +"="+ channelWrapper.getAPI_KEY());
        }
        
        String paramsStr = signSrc.toString();
//        String pay_md5sign = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        String pay_md5sign = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[海天]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        Map<String,Object> param = null;
        if (handlerUtil.isZFB(channelWrapper)) {
            param = new TreeMap<String, Object>() {
                {
                    put(method,"createOrder");
                    put(params,payParam);
                    put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
                }
            };
        }else {
            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        }
        Map<String,String> result = Maps.newHashMap();
//        String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(),JSON.toJSONString(map),MediaType.APPLICATION_JSON_UTF8_VALUE,"Keep-Alive",MediaType.APPLICATION_JSON_UTF8_VALUE.toString());
        String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), handlerUtil.isZFB(channelWrapper) ? JSON.toJSONString(param) : JSON.toJSONString(payParam),MediaType.APPLICATION_JSON_VALUE).trim();
//        String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
//        String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
       
//        if (StringUtils.isBlank(resultStr)) {
//            log.error("[海天]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//            throw new PayException(resultStr);
//        }
//        if (!resultStr.contains("{") || !resultStr.contains("}")) {
//           log.error("[海天]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//           throw new PayException(resultStr);
//        }
        JSONObject jsonObject;
        try {
            resultStr = UnicodeUtil.unicodeToString(resultStr);
            jsonObject = JSONObject.parseObject(resultStr);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[海天]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        //只取正确的值，其他情况抛出异常
        //if (null != jsonObject && jsonObject.containsKey("result_code") && "SUCCESS".equalsIgnoreCase(jsonObject.getString("result_code"))  && 
        //(jsonObject.containsKey("qrcode") && StringUtils.isNotBlank(jsonObject.getString("qrcode")) || 
        // jsonObject.containsKey("pay_url") && StringUtils.isNotBlank(jsonObject.getString("pay_url")))
        //){
        if (handlerUtil.isZFB(channelWrapper)) {
          //只取正确的值，其他情况抛出异常
            if (null != jsonObject && jsonObject.containsKey("error") && StringUtils.isNotBlank(jsonObject.getString("error"))) {
                JSONObject error = JSONObject.parseObject(jsonObject.getString("error"));
                if (null != error && error.containsKey("code") && "200".equalsIgnoreCase(error.getString("code"))) {
                    String my_result = jsonObject.getString("result");
                    if (StringUtils.isNotBlank(my_result)){
                        JSONObject resultJson = JSONObject.parseObject(my_result);
                        if (handlerUtil.isWapOrApp(channelWrapper)) {
                            result.put(JUMPURL, resultJson.getString("link"));
                        }else{
//                            result.put(QRCONTEXT, QRCodeUtil.decodeByBase64(resultJson.getString("img")));
                            String qr = QRCodeUtil.decodeByUrl(resultJson.getString("img"));
                            if (StringUtils.isBlank(qr)) {
                                log.error("[海天]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                                throw new PayException(resultStr);
                            }
                            result.put(QRCONTEXT, qr);
                        }
                    }
                }else {
                    log.error("[海天]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
            }else {
                log.error("[海天]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
        } else {
            if (null != jsonObject && jsonObject.containsKey("code") && "10".equalsIgnoreCase(jsonObject.getString("code"))  && jsonObject.containsKey("payUrl") && StringUtils.isNotBlank(jsonObject.getString("payUrl"))) {
                String code_url = jsonObject.getString("payUrl");
                result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
                //if (handlerUtil.isWapOrApp(channelWrapper)) {
                //    result.put(JUMPURL, code_url);
                //}else{
                //    result.put(QRCONTEXT, code_url);
                //}
            }else {
                log.error("[海天]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
        }
        List<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[海天]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
        return payResultList;
    }
    
    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (null != resultListMap && !resultListMap.isEmpty()) {
            if (resultListMap.size() == 1) {
                Map<String, String> resultMap = resultListMap.get(0);
                requestPayResult = buildResult(resultMap,channelWrapper,requestPayResult);
            }
            if (ValidateUtil.requestesultValdata(requestPayResult)) {
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            } else {
                throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[海天]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }

}