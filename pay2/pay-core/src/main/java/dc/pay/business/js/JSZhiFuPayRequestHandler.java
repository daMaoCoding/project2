package dc.pay.business.js;

import java.util.*;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayRequestHandler;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import dc.pay.business.RequestPayResult;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * @author cobby
 * Jan 17, 2019
 */
@RequestPayHandler("JSZHIFU")
public final class JSZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(JSZhiFuPayRequestHandler.class);


// MerchantNo/	是	商户号，平台分配
// OutTradeNo/	是	商户订单号，与商户号一起保持唯一
// ChannelType	是	接入类型，10：二维码转帐
// PayWay		是
// Body	String/	是	商品名称
// Amount		是	支付金额，分为单位（如1元=100）
// NotifyUrl	是	异步通知URL（必传），与后台配置的一致。
// Sign	String/	是	签名(MD5加密)
	private static final String MerchantNo        ="MerchantNo";  //商户号，平台分配
	private static final String OutTradeNo        ="OutTradeNo";//商户订单号，与商户号一起保持唯一
	private static final String ChannelType       ="ChannelType";//接入类型，10：二维码转帐
	private static final String PayWay            ="PayWay"; //支付方式，1：支付宝
	private static final String Body              ="Body";//商品名称
	private static final String Amount            ="Amount";//支付金额，分为单位（如1元=100）
	private static final String NotifyUrl         ="NotifyUrl";//异步通知URL（必传），与后台配置的一致。
	//signature    数据签名    32    是    　
	private static final String Sign  ="Sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {

        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(MerchantNo, channelWrapper.getAPI_MEMBERID());
	            put(OutTradeNo,channelWrapper.getAPI_ORDER_ID());
	            put(ChannelType,"10");
	            put(PayWay,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            put(Body,"name");
	            put(Amount,  channelWrapper.getAPI_AMOUNT());
	            String notifyurl = channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL();
	            //1.扫码通道，第三方需要绑定回调地址：http://域名:端口/respPayWeb/JSZHIFU_BANK_NULL_FOR_CALLBAK/
	            String str = notifyurl.substring(0 ,notifyurl.indexOf("/respPayWeb/"));
	            put(NotifyUrl,str+"/respPayWeb/JSZHIFU_BANK_NULL_FOR_CALLBAK/");
            }
        };
        log.debug("[JS支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> params) throws PayException {
    	//MD5(Amount + Body + ChannelType + MerchantNo + OutTradeNo + PayWay + 商户秘钥).ToLower()
	     String paramsStr = String.format("%s%s%s%s%s%s%s",
			     params.get(Amount),
			     params.get(Body),
			     params.get(ChannelType),
			     params.get(MerchantNo),
			     params.get(OutTradeNo),
			     params.get(PayWay),
			     channelWrapper.getAPI_KEY());
	     String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
	     log.debug("[JS支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
	     return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        try {
	        String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");

                if (StringUtils.isBlank(resultStr)) {
                    log.error("[JS支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
                if (!resultStr.contains("{") || !resultStr.contains("}")) {
                   log.error("[JS支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                   throw new PayException(resultStr);
                }
                //JSONObject resJson = JSONObject.parseObject(resultStr);
                JSONObject resJson;
                try {
                    resJson = JSONObject.parseObject(resultStr);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("[JS支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
	            JSONObject resJson1;
                if (null != resJson && resJson.containsKey("Code") && "1000".equalsIgnoreCase(resJson.getString("Code"))
		                && resJson.containsKey("Data") && StringUtils.isNotBlank(resJson.getString("Data"))){
	                try {
		                String data = resJson.get("Data").toString();
		                resJson1 = JSONObject.parseObject(data);
	                } catch (Exception e) {
		                e.printStackTrace();
		                log.error("[JS支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
		                throw new PayException(resultStr);
	                }

                }else {
	                log.error("[JS支付]-[请求支付]-3.5.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	                throw new PayException(resultStr);
                }

                //只取正确的值，其他情况抛出异常
                if ( null != resJson1 &&resJson1.containsKey("PayHtml") && StringUtils.isNotBlank(resJson1.getString("PayHtml"))) {
                    String code_url = resJson1.getString("PayHtml");
	                result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
                }else {
                    log.error("[JS支付]-[请求支付]-3.6.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }

        } catch (Exception e) {
            log.error("[JS支付]-[请求支付]-3.7.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[JS支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[JS支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}