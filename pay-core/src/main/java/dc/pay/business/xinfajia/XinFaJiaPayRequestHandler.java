package dc.pay.business.xinfajia;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayRequestHandler;
import dc.pay.business.RequestPayResult;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.ValidateUtil;


/**
 * 
 * @author andrew
 * Aug 8, 2019
 */
@RequestPayHandler("XINFAJIA")
public final class XinFaJiaPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XinFaJiaPayRequestHandler.class);

    //参数名 必选  类型  说明
    //shopAccountId   是   string  商户ID (商户后台获取)
    private static final String shopAccountId                ="shopAccountId";
    //shopUserId  是   string  商户自己平台用户ID,记录作用，值可以为空，参数必须保留
    private static final String shopUserId                ="shopUserId";
    //amountInString  是   string  订单⾦金金额，单位元，如:0.01表示⼀一分 钱;
    private static final String amountInString                ="amountInString";
    //payChannel  是   string  固定值:alipay，云闪付：unionpay
    private static final String payChannel                ="payChannel";
    //shopNo  是   string  商户订单号，⻓度不超过40;不允许出现”&”符号
    private static final String shopNo                ="shopNo";
    //shopCallbackUrl 是   string  订单支付成功回调地址(具体参数详见接口2，如果为空平台会调用商家在WEB端设置的订单回调地址;否则，平台会调用该地址，WEB端设置的地址不会被调用);
    private static final String shopCallbackUrl                ="shopCallbackUrl";
    //returnUrl   是   string  ⼆维码扫码支付模式下:支付成功⻚页面‘返回商家端’按钮点击后的跳转地址; 如果商家采用自有界⾯面，则忽略该参数;
    private static final String returnUrl                ="returnUrl";
    //target  是   string  跳转方式 1，手机跳转(PC和H5自适应) 3、返回json
    private static final String target                ="target";
    //sign    是   string  验签字符串，MD5(shopAccountId&shopUserId&amountInString&shopNo&payChannel &KEY);字段之间使用”&”符号链接;字符串拼接计算MD5值;shopAccountId 和KEY登陆商家后台可以查看(注意：小写); 注意 & 符号表示各种语言的连接字符串符号，并不是直接加&，其他接口也同样表示
//    private static final String sign                ="sign";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
//        String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
//        if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 3) {
//            log.error("[新发家]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&机构号" );
//            throw new PayException("[新发家]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&机构号" );
//        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(shopAccountId, channelWrapper.getAPI_MEMBERID());
                put(shopUserId,  HandlerUtil.getRandomNumber(6));
                put(amountInString,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(payChannel,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(shopNo,channelWrapper.getAPI_ORDER_ID());
                put(shopCallbackUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(returnUrl,channelWrapper.getAPI_WEB_URL());
                put(target,"1");
            }
        };
        log.debug("[新发家]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
        //2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(api_response_params.get(shopAccountId)).append("&");
        signSrc.append(api_response_params.get(shopUserId)).append("&");
        signSrc.append(api_response_params.get(amountInString)).append("&");
        signSrc.append(api_response_params.get(shopNo)).append("&");
        signSrc.append(api_response_params.get(payChannel)).append("&");
        signSrc.append(channelWrapper.getAPI_KEY());
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[新发家]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }
    
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        HashMap<String, String> result = Maps.newHashMap();

//        if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
        if (true) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
        }
        
//        else{
////            String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(payParam),MediaType.APPLICATION_JSON_VALUE);
////            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
//            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
//            //if (StringUtils.isBlank(resultStr)) {
//            //    log.error("[新发家]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//            //    throw new PayException(resultStr);
//            //    //log.error("[新发家]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
//            //    //throw new PayException("返回空,参数："+JSON.toJSONString(map));
//            //}
//            System.out.println("请求返回=========>"+resultStr);
//            //if (!resultStr.contains("{") || !resultStr.contains("}")) {
//            //   log.error("[新发家]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//            //   throw new PayException(resultStr);
//            //}
//            //JSONObject jsonObject = JSONObject.parseObject(resultStr);
//            JSONObject jsonObject;
//            try {
//                jsonObject = JSONObject.parseObject(resultStr);
//            } catch (Exception e) {
//                e.printStackTrace();
//                log.error("[新发家]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//            }
//            //只取正确的值，其他情况抛出异常
//            if (null != jsonObject && jsonObject.containsKey("code") && "0".equalsIgnoreCase(jsonObject.getString("code"))  && 
//            (jsonObject.containsKey("page_url") && StringUtils.isNotBlank(jsonObject.getString("page_url")))){
////            if (null != jsonObject && jsonObject.containsKey("status") && "1".equalsIgnoreCase(jsonObject.getString("status"))  && jsonObject.containsKey("codeimg") && StringUtils.isNotBlank(jsonObject.getString("codeimg"))) {
//                String code_url = jsonObject.getString("page_url");
//                result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
//                //if (handlerUtil.isWapOrApp(channelWrapper)) {
//                //    result.put(JUMPURL, code_url);
//                //}else{
//                //    result.put(QRCONTEXT, code_url);
//                //}
//            }else {
//                log.error("[新发家]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//            }
//        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[新发家]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[新发家]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}