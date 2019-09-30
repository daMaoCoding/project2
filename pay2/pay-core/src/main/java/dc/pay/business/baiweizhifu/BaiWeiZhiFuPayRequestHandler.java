package dc.pay.business.baiweizhifu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.springframework.http.HttpMethod;

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
import dc.pay.utils.UnicodeUtil;
import dc.pay.utils.ValidateUtil;


/**
 * @author sunny
 * Dec 18, 2018
 */
@RequestPayHandler("BAIWEIZHIFU")
public final class BaiWeiZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(BaiWeiZhiFuPayRequestHandler.class);

//    输入项				输入项名称		注释
//    mch_id			商户编号	
//    out_trade_no		商户订单号	
//    body				商户描述	
//    sub_openid		用户的openid		微信公众号支付时候必须填写，其他情况不用传参数
//    callback_url		前台通知地址	
//    notify_url		后台通知地址	
//    total_fee			金额	以“元”为单位必须保留两位小数
//    service			接口类型			wx:微信 al:支付宝qq:qq钱包jd:京东wy:网银支付kj:快捷支付yl:银联二维码
//    way				支付方式			pay：扫码或者网关支付 wap：h5支付
//    Appid				应用编号			没有应用时可以不传此参数
//    format			返回数据格式		json或者xml这两个格式当为json时直接返回json数据，xml时跳转到平台收银台 
//    mch_create_ip		请求客户的ip		wap支付时，wap发起H5终端IP，和微信客户端获得IP需要为同一个IP
//    sub_openid		用户的openid		公众号支付时必须填写，其他支付不用填写，必须是报备公众号获取的用户openid

    private static final String mch_id                 ="mch_id";
    private static final String out_trade_no           ="out_trade_no";
    private static final String body           		   ="body";
    private static final String sub_openid             ="sub_openid";
    private static final String callback_url           ="callback_url";
    private static final String notify_url             ="notify_url";
    private static final String total_fee              ="total_fee";
    private static final String service                ="service";
    private static final String way                    ="way";
    private static final String Appid                  ="Appid";
    private static final String format                 ="format";
    private static final String mch_create_ip          ="mch_create_ip";
    private static final String device_info            ="device_info";
    private static final String attach                 ="attach";
    private static final String sign                   ="sign";
    


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(mch_id, channelWrapper.getAPI_MEMBERID());
                put(out_trade_no,channelWrapper.getAPI_ORDER_ID());
                put(total_fee,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(callback_url,channelWrapper.getAPI_WEB_URL());
                put(service,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
                put(way,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
                put(format,"json");
                put(body,channelWrapper.getAPI_ORDER_ID());
            }
        };
        log.debug("[百威支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        String signSrc=String.format("%s%s%s%s%s%s%s%s%s", 
        		api_response_params.get(mch_id),
        		api_response_params.get(out_trade_no),
        		api_response_params.get(callback_url),
        		api_response_params.get(notify_url),
        		api_response_params.get(total_fee),
        		api_response_params.get(service),
        		api_response_params.get(way),
        		api_response_params.get(format),
        		channelWrapper.getAPI_KEY()
        		);
        String paramsStr = signSrc.toString();
        String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[百威支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return signMD5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        if (1==2&&(HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)  ||  HandlerUtil.isWapOrApp(channelWrapper))) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());  //.replace("method='post'","method='get'"));
            payResultList.add(result);
        }else{
	        String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.GET);
	        if (StringUtils.isBlank(resultStr)) {
	            log.error("[百威支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(EMPTYRESPONSE);
	        }
	        resultStr = UnicodeUtil.unicodeToString(resultStr);
	        if (!resultStr.contains("{") || !resultStr.contains("}")) {
	            log.error("[百威支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        JSONObject resJson;
	        try {
	            resJson = JSONObject.parseObject(resultStr);
	        } catch (Exception e) {
	            e.printStackTrace();
	            log.error("[百威支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        if (null != resJson && resJson.containsKey("success") && resJson.getString("success").equals("true")) {
	            String code_url = resJson.getString("pay_info");
	            if(HandlerUtil.isYL(channelWrapper)||HandlerUtil.isZfbSM(channelWrapper)||HandlerUtil.isJDSM(channelWrapper)){
	            	 result.put(QRCONTEXT,code_url);
	            }else{
	            	 result.put(JUMPURL,code_url);
	            }
	        }else {
	            log.error("[百威支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        payResultList.add(result);
        }
        log.debug("[百威支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[百威支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}