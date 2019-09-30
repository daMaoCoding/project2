package dc.pay.business.xinwannengchong2;

import java.util.ArrayList;
import java.util.HashMap;
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
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Apr 17, 2019
 */
@RequestPayHandler("XINWANNENGCHONG2")
public final class XinWanNengChong2PayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XinWanNengChong2PayRequestHandler.class);

    //2.1.3详细参数说明
    //● 请求报文
    //字段名 变量名 必填  类型  示例值 描述
    //商户号 merchNo 是   String(32)      由分配给商户的商户唯一编码
    private static final String merchNo                ="merchNo";
    //商户单号    orderNo 是   String(23)  4392849234723987    商户上送订单号，保持唯一值。
    private static final String orderNo                ="orderNo";
    //交易金额    amount  是   String(20)  10.00   以元为单位，如10.00元
    private static final String amount                ="amount";
    //币种  currency    是   String(20)  CNY 目前只支持CNY
    private static final String currency                ="currency";
    //支付类型    outChannel  是   String(10)  qq  详见附录4.1
    private static final String outChannel                ="outChannel";
    //银行编号    bankCode    是   String(10)  1001    当支付类型为网关支付时，需要传该参数详见附录4.3支付类型为：jfali,jfwx可填任意值
    private static final String bankCode                ="bankCode";
    //订单标题    title   是   String(20)  消费  用于描述该笔交易的主题
    private static final String title                ="title";
    //商品描述    product 是   String(500) 消费  用于描述该笔交易商品的主体信息
    private static final String product                ="product";
    //商品备注    memo    是   String(500) 消费  用于描述该笔交易或商品的主体信息
    private static final String memo                ="memo";
    //同步回调地址  returnUrl   是   String(255) http://abc.cn/returnUrl 商户服务器用来接收同步通知的http地址
    private static final String returnUrl                ="returnUrl";
    //异步通知地址  notifyUrl   是   String(255) http://abc.cn/notifyUrl 商户服务器用来接收异步通知的http地址
    private static final String notifyUrl                ="notifyUrl";
    //下单时间    reqTime 是   string(128) 20170808161616  满足格式yyyyMMddHHmmss的下单时间
    private static final String reqTime                ="reqTime";
    //客户标识    userId  是   String(32)  12345   用来标识商户系统中的用户唯一编码，可用于单用户限额等控制0-9数字组成的字符串，保证唯一性
    private static final String userId                ="userId";

    private static final String context                ="context";
    private static final String encryptType                ="encryptType";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
//        String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
//        if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 3) {
//            log.error("[新万能充2]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&机构号" );
//            throw new PayException("[新万能充2]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&机构号" );
//        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merchNo, channelWrapper.getAPI_MEMBERID());
                put(orderNo,channelWrapper.getAPI_ORDER_ID());
                put(amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(currency,"CNY");
                put(outChannel,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(bankCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(title,"name");
                put(product,channelWrapper.getAPI_MEMBERID());
                put(memo,channelWrapper.getAPI_ORDER_ID());
                put(returnUrl,channelWrapper.getAPI_WEB_URL());
                put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(reqTime,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
                put(userId,handlerUtil.getRandomStr(8));
            }
        };
        log.debug("[新万能充2]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
        //2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
        String string = new String(java.util.Base64.getEncoder().encode(JSON.toJSONString(api_response_params).getBytes()));
        System.out.println("签名源串=========>"+JSON.toJSONString(api_response_params));
        String signMd5 = HandlerUtil.getMD5UpperCase(string+channelWrapper.getAPI_KEY());
        log.debug("[新万能充2]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
//        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        Map<String, String> param = new TreeMap<String, String>() {
            {
                put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
                put(context,JSON.toJSONString(payParam));
                put(encryptType,"MD5");
                put(outChannel,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            }
        };
        
        HashMap<String, String> result = Maps.newHashMap();

        if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),param).toString());
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),param).toString().replace("method='post'","method='get'"));
        }else{
            String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(param),MediaType.APPLICATION_JSON_VALUE).trim();
//            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), param, String.class, HttpMethod.POST);
            //String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), param,"UTF-8");
            //if (StringUtils.isBlank(resultStr)) {
            //    log.error("[新万能充2]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            //    throw new PayException(resultStr);
            //    //log.error("[新万能充2]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
            //    //throw new PayException("返回空,参数："+JSON.toJSONString(map));
            //}
            //if (!resultStr.contains("{") || !resultStr.contains("}")) {
            //   log.error("[新万能充2]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            //   throw new PayException(resultStr);
            //}
            //JSONObject jsonObject = JSONObject.parseObject(resultStr);
            JSONObject jsonObject;
            try {
                jsonObject = JSONObject.parseObject(resultStr);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("[新万能充2]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            //只取正确的值，其他情况抛出异常
            if (null != jsonObject && jsonObject.containsKey("code") && "0".equalsIgnoreCase(jsonObject.getString("code"))  && 
            (jsonObject.containsKey("context") && StringUtils.isNotBlank(jsonObject.getString("context")) &&
             jsonObject.getJSONObject("context").containsKey("qrcode_url") && StringUtils.isNotBlank(jsonObject.getJSONObject("context").getString("qrcode_url")))
            ){
//            if (null != jsonObject && jsonObject.containsKey("status") && "1".equalsIgnoreCase(jsonObject.getString("status"))  && jsonObject.containsKey("codeimg") && StringUtils.isNotBlank(jsonObject.getString("codeimg"))) {
                String code_url = jsonObject.getJSONObject("context").getString("qrcode_url");
                result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
                //if (handlerUtil.isWapOrApp(channelWrapper)) {
                //    result.put(JUMPURL, code_url);
                //}else{
                //    result.put(QRCONTEXT, code_url);
                //}
            }else {
                log.error("[新万能充2]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[新万能充2]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[新万能充2]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}