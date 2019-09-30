package dc.pay.business.judingzhifu;

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
 * Jun 22, 2019
 */
@RequestPayHandler("JUDINGZHIFU")
public final class JuDingZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(JuDingZhiFuPayRequestHandler.class);
    
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
//    private static final String bankname  ="bankname";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";
    
    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(ordercode,channelWrapper.getAPI_ORDER_ID());
                put(amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(goodsId,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(statedate, DateUtil.formatDateTimeStrByParam("yyyyMMdd"));
                put(merNo, channelWrapper.getAPI_MEMBERID());
                put(callbackurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(callbackMemo,"01");
                put(notifyurl,channelWrapper.getAPI_WEB_URL());
            }
        };
        log.debug("[聚鼎支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
        Map<String, String> treeMap = new TreeMap<>(api_response_params);
        treeMap.put(key, channelWrapper.getAPI_KEY());
        
        List paramKeys = MapUtils.sortMapByKeyAsc(treeMap);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!signature.equals(paramKeys.get(i)) && StringUtils.isNotBlank(treeMap.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(treeMap.get(paramKeys.get(i))).append("&");
            }
        }
        //最后一个&转换成#
        //signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "#" );
        //去除最后一个&符
        //paramsStr = paramsStr.substring(0,paramsStr.length()-1);
        //删除最后一个字符
        signSrc.deleteCharAt(signSrc.length()-1);
//        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[聚鼎支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }
    
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        Map<String,String> result = Maps.newHashMap();
//        String resultStr = RestTemplateUtil.postStr(api_CHANNEL_BANK_URL, pay_md5sign,MediaType.ALL_VALUE.toString(),"Keep-Alive");
//        String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(payParam),MediaType.APPLICATION_JSON_VALUE).trim();
//        String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
        String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
       
//        if (StringUtils.isBlank(resultStr)) {
//            log.error("[聚鼎支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//            throw new PayException(resultStr);
//        }
//        JSONObject jsonObject = JSONObject.parseObject(DesUtil.desDecrypt(tmpStr,  channelWrapper.getAPI_KEY()));
        JSONObject jsonObject = null;
        try {
//            jsonObject = JSONObject.parseObject(DesUtil.desDecrypt(resultStr,  channelWrapper.getAPI_KEY()));
            jsonObject = JSONObject.parseObject(resultStr);
        } catch (Exception e) {
            log.error("[聚鼎支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(jsonObject) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(JSON.toJSONString(jsonObject));
        } 
        //实际返回：{"codeUrl":"http://oss4.hebcxkj.com/gate/4d961a8b9be14d17813b86bf0c256fb6.html","imageValue":"http://code.hebcxkj.com:28080/YFServlet/QrCreate?qrtext=http://oss4.hebcxkj.com/gate/4d961a8b9be14d17813b86bf0c256fb6.html","result":"200","sign":"01977627d86dd72225b6ae0dabb7d24c"}
        if (null != jsonObject && jsonObject.containsKey("result") && "200".equalsIgnoreCase(jsonObject.getString("result"))  && jsonObject.containsKey("codeUrl") && StringUtils.isNotBlank(jsonObject.getString("codeUrl"))) {
            result.put( JUMPURL, jsonObject.getString("codeUrl"));
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
        }else {
            log.error("[聚鼎支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        
        List<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[聚鼎支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
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
        log.debug("[聚鼎支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}