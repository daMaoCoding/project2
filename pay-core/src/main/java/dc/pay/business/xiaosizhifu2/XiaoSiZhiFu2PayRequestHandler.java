package dc.pay.business.xiaosizhifu2;

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
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Jun 26, 2019
 */
@RequestPayHandler("XIAOSIZHIFU2")
public final class XiaoSiZhiFu2PayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XiaoSiZhiFu2PayRequestHandler.class);

    //参数  字段名 类型  必传  最大长度    描述  签名
    //pid 商户编号    Int 是   Max(8)  收单商户编号  是
    private static final String pid                ="pid";
    //cid 渠道编号    Int 是   Max(2)  支付渠道编号  是
    private static final String cid                ="cid";
    //type    支付类型    String  是   Max(10) 默认为空    是
    private static final String type                ="type";
    //oid 商户订单号   String  是   Max(30) 商户订单号   是
    private static final String oid                ="oid";
    //uid 用户id    String  是   Max(32) 用户唯一标识  是
    private static final String uid                ="uid";
    //amount  订单金额    Int 是   Max(8)  (单位:分)1元=100    是
    private static final String amount                ="amount";
    //sname   商品名称    String  否   Max(30) 显示的商品名称 否
//    private static final String sname                ="sname";
    //burl    同步回调地址  String  是   Max(200)    完成后同步回调 是
    private static final String burl                ="burl";
    //nurl    异步回调地址  String  是   Max(200)    完成后异步回调 是
    private static final String nurl                ="nurl";
    //eparam  扩展参数    String  是   Max(200)    特殊支付配置使用    是
    private static final String eparam                ="eparam";
    //ip  用户IP    string  是   Max(32) 用户IP    是
    private static final String ip                ="ip";
    //stype   签名类型    String  是   Max(10) 目前只支持MD5    是
    private static final String stype                ="stype";
    //sign    签名  String  是   Max(32) 签名规则见示例 否
//    private static final String sign                ="sign";
    ////如需要返回json格式,增加参数 format 值为 json 即可返回相应格式.
    private static final String format                ="format";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="pay_md5sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
        if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 2) {
            log.error("[小四支付2]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&通道类型编码值（向第三方获取当前使用通道编码值）" );
            throw new PayException("[小四支付2]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&通道类型编码值（向第三方获取当前使用通道编码值）" );
        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
//                put(pid, channelWrapper.getAPI_MEMBERID());
                put(pid, channelWrapper.getAPI_MEMBERID().split("&")[0]);
//                put(cid,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(cid,channelWrapper.getAPI_MEMBERID().split("&")[1]);
//                put(type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(oid,channelWrapper.getAPI_ORDER_ID());
                put(uid,handlerUtil.getRandomStr(5));
                put(amount,  channelWrapper.getAPI_AMOUNT());
                put(burl,channelWrapper.getAPI_WEB_URL());
                put(nurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(eparam, channelWrapper.getAPI_MEMBERID().split("&")[0]);
                put(ip, channelWrapper.getAPI_Client_IP());
                put(stype, "MD5");
                //如需要返回json格式,增加参数 format 值为 json 即可返回相应格式.
                put(format, "json");
            }
        };
        log.debug("[小四支付2]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
         StringBuffer signSrc= new StringBuffer();
         signSrc.append(amount+"=").append(api_response_params.get(amount)).append("&");
         signSrc.append(burl+"=").append(api_response_params.get(burl)).append("&");
         signSrc.append(cid+"=").append(api_response_params.get(cid)).append("&");
         signSrc.append(eparam+"=").append(api_response_params.get(eparam)).append("&");
         signSrc.append(ip+"=").append(api_response_params.get(ip)).append("&");
         signSrc.append(nurl+"=").append(api_response_params.get(nurl)).append("&");
         signSrc.append(oid+"=").append(api_response_params.get(oid)).append("&");
         signSrc.append(pid+"=").append(api_response_params.get(pid)).append("&");
         signSrc.append(stype+"=").append(api_response_params.get(stype)).append("&");
//         signSrc.append(type+"=").append(api_response_params.get(type)).append("&");
         signSrc.append(type+"=").append("&");
         signSrc.append(uid+"=").append(api_response_params.get(uid)).append("&");
         signSrc.append(key+"=").append(channelWrapper.getAPI_KEY());
         //删除最后一个字符
         //signSrc.deleteCharAt(paramsStr.length()-1);
         String paramsStr = signSrc.toString();
         String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[小四支付2]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
//        if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
        if (false) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
//            result.put(HTMLCONTEXT, getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
        }
        
        else{
            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
//        String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
//        String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
            if (StringUtils.isBlank(resultStr)) {
                log.error("[小四支付2]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            if (!resultStr.contains("{") || !resultStr.contains("}")) {
                log.error("[小四支付2]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            JSONObject jsonObject = null;
            try {
                jsonObject = JSONObject.parseObject(resultStr);
            } catch (Exception e) {
                log.error("[小四支付2]-[请求支付]-3.3.发送支付请求，及获取支付请求出错，订单号：" + channelWrapper.getAPI_ORDER_ID() + ",通道名称：" + channelWrapper.getAPI_CHANNEL_BANK_NAME() + ",postUrl:" + channelWrapper.getAPI_CHANNEL_BANK_URL() + ",payForManagerCGIResultJsonObj" + e.getMessage(), e);
                //log.error("[盛盈付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + resultStr + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(e.getMessage(),e);
            }          
            //只取正确的值，其他情况抛出异常
            if (null != jsonObject && jsonObject.containsKey("code") && "101".equalsIgnoreCase(jsonObject.getString("code"))  && jsonObject.containsKey("data") && StringUtils.isNotBlank(jsonObject.getString("data"))) {
                JSONObject jsonObject2 = jsonObject.getJSONObject("data");
                if (null != jsonObject2 && jsonObject2.containsKey("payurl") && StringUtils.isNotBlank(jsonObject2.getString("payurl"))) {
                        result.put(JUMPURL, jsonObject2.getString("payurl"));
//                    if (handlerUtil.isWapOrApp(channelWrapper) || handlerUtil.isWebWyKjzf(channelWrapper)) {
//                        result.put(JUMPURL, jsonObject2.getString("payurl"));
//                    }else {
//                        String qr = QRCodeUtil.decodeByUrl(jsonObject2.getString("payurl"));
//                        if (StringUtils.isBlank(qr)) {
//                            log.error("[小四支付2]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                            throw new PayException(resultStr);
//                        }
//                        result.put(QRCONTEXT, qr);
//                    }
                }else {
                    log.error("[小四支付2]-[请求支付]-3.5.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
            }else {
                log.error("[小四支付2]-[请求支付]-3.6.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[小四支付2]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[小四支付2]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
    
}