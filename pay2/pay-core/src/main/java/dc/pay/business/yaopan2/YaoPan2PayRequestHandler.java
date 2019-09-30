package dc.pay.business.yaopan2;

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
import dc.pay.utils.JsonUtil;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.StrUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Dec 27, 2018
 */
@RequestPayHandler("YAOPAN2")
public final class YaoPan2PayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(YaoPan2PayRequestHandler.class);
    
    //ordercode     订单号 商户唯一    Varchar(24)----数字加字母格式
    private static final String ordercode  ="ordercode";
    //amount        金额      元为单位    
    private static final String amount  ="amount";
    //goodsId       交易交品号           132：银联扫码   142：微信     152：QQ钱包     172：支付宝    232：PC快捷   242：WAP快捷  252：苏宁钱包    212：京东钱包   213：五码合一
    private static final String goodsId  ="goodsId";
    //statedate     交易日期            YYYYMMDD年月日
    private static final String statedate  ="statedate";
    //merNo         商户号     
    private static final String merNo  ="merNo";
    //callbackurl   回调地址            系统通知回调信息
    private static final String callbackurl  ="callbackurl";
    //callbackMemo  回调附加信息      回调时原样送回
    private static final String callbackMemo  ="callbackMemo";
    private static final String notifyurl  ="notifyurl";
    
//  //version       版本号 01  固定值：01
//  private static final String version  ="version";
//  //ret_url       返回URL       结果返回URL，仅适用于立即返回处理结果的接口。创新支付处理完请求
//  private static final String ret_url  ="ret_url";
//  //sign          签名字段        数据的加密校验字符串，目前只支持使用MD5签名算法对待签名数据进行签名
//  private static final String sign  ="sign";
    //bankname      银行名称        输入中文，比如“农业银行”（与表1中的‘银行名称’必须一致）  --仅网银支付需提交该参数（产品代码为192）
    private static final String bankname  ="bankname";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merNo, channelWrapper.getAPI_MEMBERID());
//                put(amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(callbackurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                //TODO andrew
                put(ordercode,channelWrapper.getAPI_ORDER_ID());
//                put(ordercode,System.currentTimeMillis()+"");
                put(callbackMemo,"01");
                put(statedate, DateUtil.formatDateTimeStrByParam("yyyyMMdd"));
                //如果是网银支付
                if(HandlerUtil.isWY(channelWrapper)){
                    put(notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                    put(bankname,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    put(goodsId,"192");

/*
                    Double OrMoney = Double.valueOf(channelWrapper.getAPI_AMOUNT());
                    //网银 ：1.提交请求，原金额减去100分之内随机数
                    Double RaMoney = Double.valueOf((double)handlerUtil.getRandomNumber(1, 100));
                    BigDecimal bd1 = new BigDecimal(Double.toString(OrMoney)); 
                    BigDecimal bd2 = new BigDecimal(Double.toString(RaMoney));
                    //因这里最小单位已是分，所以，不必担心存在精度 问题
                    int intValue = bd1.subtract(bd2).intValue();
*/
                   long longValue =  StrUtil.parseLong(channelWrapper.getAPI_AMOUNT()) - HandlerUtil.getRandomNumber(1,100);
                    put(amount, HandlerUtil.getYuan(String.valueOf(longValue)));
                }else {
                    put(amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                    if (HandlerUtil.isYLKJ(channelWrapper)) {
                        put(goodsId,HandlerUtil.isOrderFromWapOrApp(channelWrapper.getAPI_ORDER_FROM()) ? "242" : "232");
                    }else {
                        put(goodsId,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    }
                }
            }
        };
        log.debug("[瑶槃2]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        String desEncrypt = null;
        /*
          待加密数据是请求参数按照以下方式组装成的字符串：
        请求参数按照接口要求参数生成标准的JSON串。
        将JSON串用加密验证码进行全部加密。加密验证码商户可通过登录商户平台系统下载，假设安全校验码为12345678.
        注意事项：
        没有值的参数无需传递，也无需包含到待加密数据中。
        加密时将字符转化成字节流时指定的字符集与utf-8保持一致。
         */
        String stringify = JsonUtil.stringify(api_response_params);
        try {
            desEncrypt = DesUtil.desEncrypt(stringify,channelWrapper.getAPI_KEY());
        } catch (Exception e) {
            e.printStackTrace();
        }
        log.debug("[瑶槃2]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(desEncrypt));
        return desEncrypt;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        List<Map<String, String>> payResultList = Lists.newArrayList();
        String api_channel_bank_name = channelWrapper.getAPI_CHANNEL_BANK_NAME();
        Map<String,String> result = Maps.newHashMap();
        String api_CHANNEL_BANK_URL = channelWrapper.getAPI_CHANNEL_BANK_URL()+"/initOrder?merNo="+payParam.get(merNo);
        try {
            String tmpStr = RestTemplateUtil.postStr(api_CHANNEL_BANK_URL, pay_md5sign,MediaType.ALL_VALUE.toString(),"Keep-Alive").trim();
            if (StringUtils.isBlank(tmpStr)) {
                log.error("[瑶槃2]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + tmpStr+ "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(tmpStr);
            }
            JSONObject jsonObject = JSONObject.parseObject(DesUtil.desDecrypt(tmpStr,  channelWrapper.getAPI_KEY()));
            //200：下单成功 404：下单不成功 504：请更换订单号重新提交
            if (null == jsonObject || (jsonObject.containsKey("result")  && !"200".equals(jsonObject.getString("result")))) {
                log.error("[瑶槃2]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + jsonObject.toString()+ "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(jsonObject.toString());
                
//                log.error("[奥邦]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
            }
            //按不同的请求接口，向不同的属性设置值
//            if(HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper) || HandlerUtil.isWY(channelWrapper)) {
//              result.put(JUMPURL, jsonObject.get("codeUrl").toString());
//            }else{
//              result.put(QRCONTEXT, jsonObject.get("codeUrl").toString());
//            }
            if(HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper) || HandlerUtil.isWY(channelWrapper)) {
                result.put(JUMPURL, jsonObject.get("codeUrl").toString());
            }else{
                // result.put(QRCONTEXT, QRCodeUtil.decodeByUrl(jsonObject.get("imageValue").toString()));  //已经有了，还去解析二维码？？
                result.put(QRCONTEXT, jsonObject.get("codeUrl").toString());
            }
            result.put("第三方返回",jsonObject.toString()); //保存全部第三方信息，上面的拆开没必要
            payResultList.add(result);
        } catch (Exception e) {
            log.error("[瑶槃2]-[请求支付]-3.3.发送支付请求，及获取支付请求出错，订单号：" + channelWrapper.getAPI_ORDER_ID() + ",通道名称：" + api_channel_bank_name + ",postUrl:" + api_CHANNEL_BANK_URL + ",payForManagerCGIResultJsonObj" + e.getMessage(), e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        log.debug("[瑶槃2]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
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
        log.debug("[瑶槃2]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}