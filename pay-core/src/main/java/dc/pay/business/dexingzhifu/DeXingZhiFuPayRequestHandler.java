package dc.pay.business.dexingzhifu;

import java.util.*;

import dc.pay.utils.*;
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

/**
 * @author Cobby
 * Jan 30, 2019
 */
@RequestPayHandler("DEXINGZHIFU")
public final class DeXingZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(DeXingZhiFuPayRequestHandler.class);

    private static final String payKey                ="payKey";      //支付KEY 必填 支付主键；我方平台分配
    private static final String productCode           ="productCode"; //交易产品编码 必填 交易产品；我方平台分配
    private static final String goodsName             ="goodsName";   //商品名称 必填
    private static final String orderNo               ="orderNo";     //商户订单号 必填
    private static final String orderAmount           ="orderAmount"; //订单金额（元） 必填
    private static final String notifyUrl             ="notifyUrl";   //异步通知地址 必填
    private static final String returnUrl             ="returnUrl";
    private static final String orderDate             ="orderDate";   //订单日期 必填 订单日期yyyy-MM-dd 请填写当前系统日期如果日期与系统当前日志不一致则返回失败;

    private static final String paySecret             ="paySecret";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {

        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(payKey, channelWrapper.getAPI_MEMBERID());
	            put(productCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            put(goodsName,"name");
	            put(orderNo,channelWrapper.getAPI_ORDER_ID());
	            put(orderAmount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
	            put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(orderDate, DateUtil.formatDateTimeStrByParam("yyyy-MM-dd"));
            }
        };
        log.debug("[德兴支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
         List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
         StringBuilder signSrc = new StringBuilder();
         for (int i = 0; i < paramKeys.size(); i++) {
             if (!signature.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                 signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
             }
         }
         signSrc.append(paySecret +"="+ channelWrapper.getAPI_KEY());
         String paramsStr = signSrc.toString();
         String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
         log.debug("[德兴支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
         return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        HashMap<String, String> result = Maps.newHashMap();
        try {

            if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) ) {
	            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            }else{
		        String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam,"UTF-8");
	            resultStr = new String(resultStr.getBytes("ISO-8859-1"), "UTF-8");
                JSONObject jsonObject;
                try {
                    jsonObject = JSONObject.parseObject(resultStr);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("[德兴支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
	            if (null != jsonObject && jsonObject.containsKey("apiCode") && "YES".equalsIgnoreCase(jsonObject.getString("apiCode"))
			            && jsonObject.containsKey("codeUrl") && StringUtils.isNotBlank(jsonObject.getString("codeUrl"))) {
                    String code_url = jsonObject.getString("codeUrl");
		            result.put(HTMLCONTEXT , code_url );
                }else {
                    log.error("[德兴支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
            }
            
        } catch (Exception e) {
            log.error("[德兴支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[德兴支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[德兴支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }

}