package dc.pay.business.lufubaozhifu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
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
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.Sha1Util;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Apr 25, 2019
 */
@RequestPayHandler("LUFUBAOZHIFU")
public final class LuFuBaoZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(LuFuBaoZhiFuPayRequestHandler.class);

    //参数  类型  必须  说明  示例  备注
    //version string  是   通信协议版本号 1.0 
    private static final String version               ="version";
    //appid   string  是   商户号 APP_100752  
    private static final String appid               ="appid";
    //trade_no    string  是   订单号 Y2008101101 必须唯一
    private static final String trade_no               ="trade_no";
    //price   string  是   金额  20.50   因部分编程语言可能将金额1转换成String后变成1.0导致签名错误，浮点统一使用字符串类型
    private static final String price               ="price";
    //callback    string  是   异步回调地址  http://www.baidu.com/callback   服务器使用该地址通知商户该笔订单的状态
    private static final String callback               ="callback";
    //return  string  否   返回地址    http://www.baidu.com/return 用户支付完成或者取消支付后返回的地址
//    private static final String return_my               ="return";
    //subject string  否   商品名称    点卡充值    
//    private static final String subject               ="subject";
    //gateway int 是   支付网关    0   0:  支付宝手机网站、    1:  支付宝PC网站、    3： 支付宝扫码、    13：微信扫码    1001：支付宝扫码转银行卡
    private static final String gateway               ="gateway";
    //sign    string  是   请求参数的SHA1值      将所有请求参数加上token后按key的ascii码进行排序再进行参数拼接，再计算SHA1值
//    private static final String sign               ="sign";

    private static final String key        ="token";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(version,"1.0");
                put(appid, channelWrapper.getAPI_MEMBERID());
                put(trade_no,channelWrapper.getAPI_ORDER_ID());
                put(price,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(callback,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(gateway,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            }
        };
        log.debug("[陆付宝支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
         Map<String,String> map = new TreeMap<>(api_response_params);
         map.put(key, channelWrapper.getAPI_KEY());
         List paramKeys = MapUtils.sortMapByKeyAsc(map);
         StringBuilder signSrc = new StringBuilder();
         for (int i = 0; i < paramKeys.size(); i++) {
             signSrc.append(paramKeys.get(i)).append(map.get(paramKeys.get(i)));
         }
         //最后一个&转换成#
         //signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "#" );
         //删除最后一个字符
         //signSrc.deleteCharAt(paramsStr.length()-1);
//         signSrc.append(key + channelWrapper.getAPI_KEY());
         String paramsStr = signSrc.toString();
//         System.out.println("签名源串=========>"+paramsStr);
//         String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
         String signMd5 = null;
         try {
             signMd5 = Sha1Util.getSha1(paramsStr).toUpperCase();
         } catch (Exception e) {
             log.error("[陆付宝支付]-[请求支付]-2.生成加密URL签名出错，签名出错：{}",e.getMessage(),e);
             throw new PayException(e.getMessage(),e);
         }
         log.debug("[陆付宝支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();

        String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(payParam),MediaType.APPLICATION_JSON_VALUE).trim();
//        String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), map,"UTF-8");
        //if (StringUtils.isBlank(resultStr)) {
        //    log.error("[通扫]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
        //    throw new PayException(resultStr);
        //    //log.error("[通扫]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
        //    //throw new PayException("返回空,参数："+JSON.toJSONString(map));
        //}
//        System.out.println("请求返回=========>"+resultStr);
        //if (!resultStr.contains("{") || !resultStr.contains("}")) {
        //    log.error("[通扫]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
        //    throw new PayException(resultStr);
        //}
        JSONObject jsonObject = null;
        try {
            jsonObject = JSONObject.parseObject(new String(resultStr.getBytes("ISO-8859-1"), "GBK"));
        } catch (Exception e) {
            log.error("[陆付宝支付]-[请求支付]-3.2.发送支付请求，及获取支付请求出错，订单号：" + channelWrapper.getAPI_ORDER_ID() + ",通道名称：" + channelWrapper.getAPI_CHANNEL_BANK_NAME() + ",postUrl:" + channelWrapper.getAPI_CHANNEL_BANK_URL() + ",payForManagerCGIResultJsonObj" + e.getMessage(), e);
            //log.error("[盛盈付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + resultStr + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(e.getMessage(),e);
        }          
        //只取正确的值，其他情况抛出异常
        //if (null != jsonObject && jsonObject.containsKey("result_code") && "SUCCESS".equalsIgnoreCase(jsonObject.getString("result_code"))  && 
        //(jsonObject.containsKey("qrcode") && StringUtils.isNotBlank(jsonObject.getString("qrcode")) || 
        // jsonObject.containsKey("pay_url") && StringUtils.isNotBlank(jsonObject.getString("pay_url")))
        //){
        if (null != jsonObject && jsonObject.containsKey("status") && "200".equalsIgnoreCase(jsonObject.getString("status"))  && jsonObject.containsKey("url") && StringUtils.isNotBlank(jsonObject.getString("url"))) {
                result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, jsonObject.getString("url"));
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
//            result.put("第三方返回",jsonObject.toString()); //保存全部第三方信息，上面的拆开没必要
        }else {
            log.error("[陆付宝支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[陆付宝支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[陆付宝支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}