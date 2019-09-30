package dc.pay.business.ugbizhifu;

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
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.RsaUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Jul 9, 2019
 */
@RequestPayHandler("UGBIZHIFU")
public final class UGBiZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(UGBiZhiFuPayRequestHandler.class);

    //参数 参数名称 长度 使用 说明
    private static final String data                ="data";
    //merchCardNo 商户收币地址 42 必填 商户收币地址
    private static final String merchCardNo                ="merchCardNo";
    //charset 编码格式，固定值：UTF-8 10 必填 固定值：UTF-8
    private static final String charset                ="charset";
    //goodsName 商品名称（英文或拼音） 50 必填 商品名称 例如：UG
    private static final String goodsName                ="goodsName";
    //merchNo 商户号 16 必填 merchNo 例如：M10000004
    private static final String merchNo                ="merchNo";
    //random 随机数 4 必填 4 位随机数
    private static final String random                ="random";
    //version 版本号，固定值：V1.0.0 8 必填 固定值：V1.0.0
    private static final String version                ="version";
    //amount 支付金额(单位:UG) 20 必填 支付金额(单位:UG)
    private static final String amount                ="amount";
    //merchOrderSn 商户订单号 42 必填 商户订单号务必保证唯一
    private static final String merchOrderSn                ="merchOrderSn";
    //payCallBackUrl 支付状态回调地址 256 必填 支付状态回调地址
    private static final String payCallBackUrl                ="payCallBackUrl";
    //payViewUrl 回显页面地址 256 必填 回显页面地址
    private static final String payViewUrl                ="payViewUrl";
    //extra 附加内容 256 非必填 附加内容,非必填
//    private static final String extra                ="extra";
    //sign 签名 32 必填 签名
//    private static final String sign                ="sign";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
//        String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
//        if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 2) {
//            log.error("[UG币支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号merchNo&商户收币地址merchCardNo" );
//            throw new PayException("[UG币支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号merchNo&商户收币地址merchCardNo" );
//        }
        String api_KEY = channelWrapper.getAPI_KEY();
        if (null == api_KEY || !api_KEY.contains("-") || api_KEY.split("-").length != 4) {
            log.error("[UG币支付]-[请求支付]-“密钥（私钥）”输入数据格式为【中间使用-分隔】：钱包地址-MD5密钥-DES密钥-RSA支付私钥" );
            throw new PayException("[UG币支付]-[请求支付]-“密钥（私钥）”输入数据格式为【中间使用-分隔】：钱包地址-MD5密钥-DES密钥-RSA支付私钥" );
        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merchCardNo, api_KEY.split("-")[0]);
                put(charset,"UTF-8");
                put(goodsName,"UG");
                put(merchNo, channelWrapper.getAPI_MEMBERID());
                put(random,  HandlerUtil.getRandomStr(6));
                put(version,"V1.0.0");
                put(amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(merchOrderSn,channelWrapper.getAPI_ORDER_ID());
                put(payCallBackUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(payViewUrl,channelWrapper.getAPI_WEB_URL());
            }
        };
        log.debug("[UG币支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
         Map<String,String> map = new TreeMap<>(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        signSrc.append(JSON.toJSONString(map));
        signSrc.append(channelWrapper.getAPI_KEY().split("-")[1]);
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[UG币支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        Map<String,String> map = new TreeMap<>(payParam);

        String encryptByPublicKey = null;
        try {
            encryptByPublicKey = RsaUtil.encryptToBase64(JSON.toJSONString(map), channelWrapper.getAPI_PUBLIC_KEY());
        } catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
            log.error("[UG币支付]-[请求支付]-3.0.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(map) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(JSON.toJSONString(map));
        }
        Map<String, String> param = new TreeMap<String, String>();
        param.put(data, encryptByPublicKey);
        param.put(merchNo, channelWrapper.getAPI_MEMBERID());
        param.put(version,"V1.0.0");

        HashMap<String, String> result = Maps.newHashMap();

        if (false) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),param).toString());
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
        }else{
//            String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(param),MediaType.APPLICATION_JSON_VALUE).trim();
//            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), param, String.class, HttpMethod.POST);
            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), param,"UTF-8");
            //if (StringUtils.isBlank(resultStr)) {
            //    log.error("[UG币支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            //    throw new PayException(resultStr);
            //    //log.error("[UG币支付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
            //    //throw new PayException("返回空,参数："+JSON.toJSONString(map));
            //}
//            System.out.println("请求返回=========>"+resultStr);
            //if (!resultStr.contains("{") || !resultStr.contains("}")) {
            //   log.error("[UG币支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            //   throw new PayException(resultStr);
            //}
            //JSONObject jsonObject = JSONObject.parseObject(resultStr);
            JSONObject jsonObject;
            try {
                jsonObject = JSONObject.parseObject(resultStr);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("[UG币支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            //只取正确的值，其他情况抛出异常
            if (null != jsonObject && jsonObject.containsKey("code") && "0".equalsIgnoreCase(jsonObject.getString("code"))  &&
                    jsonObject.containsKey("data") && StringUtils.isNotBlank(jsonObject.getString("data")) &&
            ( jsonObject.getJSONObject("data").containsKey("webRrcode") && StringUtils.isNotBlank(jsonObject.getJSONObject("data").getString("webRrcode"))) ||
             jsonObject.getJSONObject("data").containsKey("h5PayUrl") && StringUtils.isNotBlank(jsonObject.getJSONObject("data").getString("h5PayUrl"))
            ){
//            if (null != jsonObject && jsonObject.containsKey("code") && "1".equalsIgnoreCase(jsonObject.getString("code"))  && jsonObject.containsKey("codeimg") && StringUtils.isNotBlank(jsonObject.getString("codeimg"))) {
                result.put( JUMPURL, handlerUtil.isWapOrApp(channelWrapper) ? jsonObject.getJSONObject("data").getString("h5PayUrl") : jsonObject.getJSONObject("data").getString("webRrcode"));
//                result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
                //if (handlerUtil.isWapOrApp(channelWrapper)) {
                //    result.put(JUMPURL, code_url);
                //}else{
                //    result.put(QRCONTEXT, code_url);
                //}
            }else {
                log.error("[UG币支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[UG币支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[UG币支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}