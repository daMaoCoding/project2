package dc.pay.business.muzhifu;

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
import dc.pay.utils.UnicodeUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Dec 7, 2018
 */
@RequestPayHandler("MUZHIFU")
public final class MuZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(MuZhiFuPayRequestHandler.class);

    //merchno    商户号    15    是    　
    //amount    交易金额    12    是    单位/元
    //traceno    商户流水号    32    是    由商户网站系统产生的订单号，订单号不可重复。
    //payType    支付方式    1    是    1-支付宝     2-微信    3-百度钱包 （待开发）    4-QQ钱包    5-京东钱包
    //goodsName    商品名称    30    是    默认取商户名称
    //notifyUrl    通知地址    50    否    交易成功，则给商户发送异步通知。
    //默认T+1
    
    //1 pid 商户APPID int 必填。    
    private static final String pid                ="pid";
    //2 type    支付类型    string  必填。可选值：alipay,wxpay,qqpay
    private static final String type                 ="type";
    //3 out_trade_no    商户订单号   string  必填。   
    private static final String out_trade_no                ="out_trade_no";
    //4 notify_url  服务器异步通知地址   string  必填。
    private static final String notify_url                ="notify_url";
    //5 return_url  通知回调网址  string  必填
    private static final String return_url              ="return_url";
    //6 name    商品名称    string  必填。
    private static final String name              ="name";
    //7 money   订单金额(元) decimal(18,2)   必填。单位为人民币，精确到分
    private static final String money             ="money";
    //9 sign_type   签名类型    String  必填。默认为MD5
    private static final String sign_type             ="sign_type";
    //8 sign    签名  string  必填。签名算法与支付宝签名算法相同
//    private static final String sign             ="sign";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(pid, channelWrapper.getAPI_MEMBERID());
                put(type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(out_trade_no,channelWrapper.getAPI_ORDER_ID());
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(return_url,channelWrapper.getAPI_WEB_URL());
                put(name,"name");
                put(money,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(sign_type,"MD5");
            }
        };
        log.debug("[拇指付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }


    @Override
    protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!sign_type.equals(paramKeys.get(i)) && !signature.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        //最后一个&转换成#
        //signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "#" );
        //去除最后一个&符
        //paramsStr = paramsStr.substring(0,paramsStr.length()-1);
        //删除最后一个字符
        signSrc.deleteCharAt(signSrc.length()-1);
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[拇指付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
//        String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
      String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.GET);
//      String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
//      String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
//        String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(payParam),MediaType.APPLICATION_FORM_URLENCODED_VALUE.toString(),"Keep-Alive");
        if (StringUtils.isBlank(resultStr)) {
            log.error("[拇指付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
            //log.error("[拇指付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
            //throw new PayException("返回空,参数："+JSON.toJSONString(map));
        }
        resultStr = UnicodeUtil.unicodeToString(resultStr);
        if (!resultStr.contains("{") || !resultStr.contains("}")) {
           log.error("[拇指付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
           throw new PayException(resultStr);
        }
        //JSONObject resJson = JSONObject.parseObject(resultStr);
        JSONObject resJson;
        try {
            resJson = JSONObject.parseObject(resultStr);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[拇指付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        //只取正确的值，其他情况抛出异常
        if (null != resJson && resJson.containsKey("code") && "200".equalsIgnoreCase(resJson.getString("code"))  && resJson.containsKey("payurl") && StringUtils.isNotBlank(resJson.getString("payurl"))) {
            String code_url = resJson.getString("payurl");
            result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
            //if (handlerUtil.isWapOrApp(channelWrapper)) {
            //    result.put(JUMPURL, code_url);
            //}else{
            //    result.put(QRCONTEXT, code_url);
            //}
        }else {
            log.error("[拇指付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[拇指付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[拇指付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}