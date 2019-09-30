package dc.pay.business.yingzhifu;

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
 * Apr 13, 2019
 */
@RequestPayHandler("YINGZHIFU")
public final class YingZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(YingZhiFuPayRequestHandler.class);

    //交易请求报文：
    //编号  接口字段    接口字段名称  类型  备注
    //1   funCode 交易类型    M   2005
    private static final String funCode                ="funCode";
    //2   platOrderId 订单号 M   最长32位字母数字(重复请求会提示该笔订单请求失败)
    private static final String platOrderId                ="platOrderId";
    //3   platMerId   商户号 M   约定值
    private static final String platMerId                ="platMerId";
    //5   tradeTime   交易时间    M   终端发起交易的时间
    private static final String tradeTime                ="tradeTime";
    //6   amt 消费总金额   M   单位：分    快捷支付4最低20000（低于会报该笔订单请求失败）   微信支付0定额见批注
    private static final String amt                ="amt";
    //7   body    交易说明    M   一般为产品信息
    private static final String body                ="body";
    //8   subject 订单标题    M   
    private static final String subject                ="subject";
    //9   payMethod   支付方法    M   0微信，1支付宝， 2  QQ，3 京东，4 快捷，6银联二维码，7网银使用需要业务要不会报无风控
    private static final String payMethod                ="payMethod";
    //10  funName 默认  M   默认（prepay）
    private static final String funName                ="funName";
    //11  orderTime   超时时间    M   设置未付款交易的超时时间，一旦超时，该笔交易就会自动被关闭。(非必要)，单位为分钟，纯数字，不接    受小数；取值范围：1~30；若大于30，或者不传该参数则默认为2；
    private static final String orderTime                ="orderTime";
    //13  subOpenId   子商户的用户 asopenid M   用子商户的    微信appid调用OAuth2.0接口生成；仅当商户需要发起微信内线上支付    的时候需要传递此参数，否则不用；    (非必要)
//    private static final String subOpenId                ="subOpenId";
    //14  notifyUrl   异步通知地址  M   必要
    private static final String notifyUrl                ="notifyUrl";
    //15  frontUrl    支付跳转地址  M   必要
    private static final String frontUrl                ="frontUrl";
    //16  sign    签名  M   
//    private static final String sign                ="sign";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
//        String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
//        if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 3) {
//            log.error("[鹰支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&机构号" );
//            throw new PayException("[鹰支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&机构号" );
//        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(funCode,"2005");
                put(platOrderId,channelWrapper.getAPI_ORDER_ID());
                put(platMerId, channelWrapper.getAPI_MEMBERID());
                put(tradeTime,  System.currentTimeMillis()+"");
                put(amt,  channelWrapper.getAPI_AMOUNT());
                put(body,"name");
                put(subject,"name");
                put(payMethod,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(funName,"prepay");
                put(orderTime,"5");
                put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(frontUrl,channelWrapper.getAPI_WEB_URL());
            }
        };
        log.debug("[鹰支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
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
        signSrc.append(key +"="+ channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
//        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[鹰支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        Map<String, String> param = new TreeMap<String, String>() {
            {
                put("reqJson",JSON.toJSONString(payParam));
            }
        };

        String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), param,"UTF-8");
//        String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL()+"?reqJson={"+JSON.toJSONString(payParam)+"}", payParam);
        //if (StringUtils.isBlank(resultStr)) {
        //    log.error("[鹰支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
        //    throw new PayException(resultStr);
        //    //log.error("[鹰支付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
        //    //throw new PayException("返回空,参数："+JSON.toJSONString(map));
        //}
//        System.out.println("请求返回=========>"+resultStr);
        //if (!resultStr.contains("{") || !resultStr.contains("}")) {
        //   log.error("[鹰支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
        //   throw new PayException(resultStr);
        //}
        //JSONObject jsonObject = JSONObject.parseObject(resultStr);
        JSONObject jsonObject;
        try {
            jsonObject = JSONObject.parseObject(resultStr);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[鹰支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        //只取正确的值，其他情况抛出异常
        //if (null != jsonObject && jsonObject.containsKey("result_code") && "SUCCESS".equalsIgnoreCase(jsonObject.getString("result_code"))  && 
        //(jsonObject.containsKey("qrcode") && StringUtils.isNotBlank(jsonObject.getString("qrcode")) || 
        // jsonObject.containsKey("pay_url") && StringUtils.isNotBlank(jsonObject.getString("pay_url")))
        //){
        if (null != jsonObject && jsonObject.containsKey("retCode") && "0000".equalsIgnoreCase(jsonObject.getString("retCode"))  && jsonObject.containsKey("codeUrl") && StringUtils.isNotBlank(jsonObject.getString("codeUrl"))) {
            String code_url = jsonObject.getString("codeUrl");
            result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
            //if (handlerUtil.isWapOrApp(channelWrapper)) {
            //    result.put(JUMPURL, code_url);
            //}else{
            //    result.put(QRCONTEXT, code_url);
            //}
        }else {
            log.error("[鹰支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[鹰支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[鹰支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}