package dc.pay.business.tiandizhifu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
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
 * 
 * @author andrew
 * Aug 19, 2019
 */
@RequestPayHandler("TIANDIZHIFU")
public final class TianDiZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(TianDiZhiFuPayRequestHandler.class);

    //支付提交参数说明
    //参数名称 参数含义 是否必填 参数说明 参数长度
    //merId 商户号 是 平台申请的商户号 MAX(30)
    private static final String merId                ="merId";
    //prdOrNo 商户订单号 是 提交的订单号必须是唯一的 MAX(30)
    private static final String prdOrNo                ="prdOrNo";
    //orderTime 订单时间 是    以北京时间为准，格式：2018-01-01    16:20:36    MAX(19)
    private static final String orderTime                ="orderTime";
    //payCode 支付通道编码 是 登录商户后台查看 MAX(15)
    private static final String payCode                ="payCode";
    //bankCode 银行编码 否 见附录一（网银支付时必填） MAX(15)
    private static final String bankCode                ="bankCode";
    //orderAmount 支付金额 是 以分为单位 MAX(25)
    private static final String orderAmount                ="orderAmount";
    //userOrderIP 商户 ip 是 客户的 ip MAX(25)
    private static final String userOrderIP                ="userOrderIP";
    //asyncNotifyUrl 异步通知 URL 是    结果返回 URL，系统处理完请求后，    将处理结果返回给这个 URL    MAX(200)
    private static final String asyncNotifyUrl                ="asyncNotifyUrl";
    //syncNotifyUrl 同步通知地址 是 同步通知地址 MAX(200)
    private static final String syncNotifyUrl                ="syncNotifyUrl";
    //merParam 自定义返回值 否 原样返回 MAX(30)
//    private static final String merParam                ="merParam";
    //signStr 签名字符串 是    此参数不参与签名，用 MD5 方式加    密后转换成大写，key（商户密钥）需    要参与签名，参数中如有中文，请用    MAX(50)
//    private static final String signStr                ="signStr";
    
    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="signStr";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
        if ((null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 2 ) && !handlerUtil.isWY(channelWrapper)) {
            log.error("[天地支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&通道类型编码值（向第三方获取当前使用通道编码值）" );
            throw new PayException("[天地支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&通道类型编码值（向第三方获取当前使用通道编码值）" );
        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merId, channelWrapper.getAPI_MEMBERID().split("&")[0]);
                put(prdOrNo,channelWrapper.getAPI_ORDER_ID());
                put(orderTime, DateUtil.formatDateTimeStrByParam("yyyy-MM-dd HH:mm:ss"));
//                put(payCode,channelWrapper.getAPI_MEMBERID().split("&")[1]);
                if (handlerUtil.isWY(channelWrapper)) {
                    put(payCode,"wypay");
                    put(bankCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                }else {
                    put(payCode, channelWrapper.getAPI_MEMBERID().split("&")[1]);
                }
                put(orderAmount,  channelWrapper.getAPI_AMOUNT());
                put(userOrderIP,channelWrapper.getAPI_Client_IP());
                put(asyncNotifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(syncNotifyUrl,channelWrapper.getAPI_WEB_URL());
            }
        };
        log.debug("[天地支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
        }
        //最后一个&转换成#
        //signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "#" );
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        signSrc.append(key +"="+ channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[天地支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        HashMap<String, String> result = Maps.newHashMap();
        
//        if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
//        if (true) {
        if (false) {
//            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
        }
        
        else{
//            String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(payParam),MediaType.APPLICATION_JSON_VALUE);
//            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
//            String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
            //if (StringUtils.isBlank(resultStr)) {
            //    log.error("[天地支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            //    throw new PayException(resultStr);
            //    //log.error("[天地支付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
            //    //throw new PayException("返回空,参数："+JSON.toJSONString(map));
            //}
//            System.out.println("请求返回=========>"+resultStr);
            //if (!resultStr.contains("{") || !resultStr.contains("}")) {
            //   log.error("[天地支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            //   throw new PayException(resultStr);
            //}
            //JSONObject jsonObject = JSONObject.parseObject(resultStr);
            JSONObject jsonObject;
            try {
                jsonObject = JSONObject.parseObject(resultStr);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("[天地支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            //只取正确的值，其他情况抛出异常
            //if (null != jsonObject && jsonObject.containsKey("result_code") && "SUCCESS".equalsIgnoreCase(jsonObject.getString("result_code"))  && 
            //(jsonObject.containsKey("qrcode") && StringUtils.isNotBlank(jsonObject.getString("qrcode")) || 
            // jsonObject.containsKey("pay_url") && StringUtils.isNotBlank(jsonObject.getString("pay_url")))
            //){
            if (null != jsonObject && jsonObject.containsKey("code") && "1".equalsIgnoreCase(jsonObject.getString("code"))  && jsonObject.containsKey("msg") && StringUtils.isNotBlank(jsonObject.getString("msg"))) {
                String code_url = jsonObject.getString("msg");
//                result.put( JUMPURL, code_url);
                //支付链接类型 0：二维码信息 （需要商户自行生成二维码）；1:支付链接（直接重定向即可）
                if ("0".equalsIgnoreCase(jsonObject.getString("payUrlType"))) {
                    result.put(QRCONTEXT, code_url);
                }else{
                    result.put(JUMPURL, code_url);
                }
            }else {
                log.error("[天地支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[天地支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[天地支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}