package dc.pay.business.xingdeng;

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
 * 
 * 
 * @author sunny
 * Dec 18, 2018
 */
@RequestPayHandler("XINGDENG")
public final class XingDengPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XingDengPayRequestHandler.class);

//    参数				名称				类型				必填			是否参与签名		备注
//    version			版本				string(8)		是				是			固定填写1.1
//    appid				商户号			string(20)		是				是			系统统一分配的商户号
//    out_trade_no		商户系统订单号		string(32)		是				是			商户系统生成的唯一订单号。仅可以使用大小写英文字符、数字、下划线、中划线组成。
//    total_fee			订单金额（元）		string(12)		是				是			保留2位小数，例如100.00
//    pay_id			支付方式			String(20)		是				是			QWJ_QUICK：快捷支付，参数2.0支付方式表
//    return_url		返回地址			String(255)		是				是			同步返回地址
//    notify_url		回调地址			String(255)		是				是			异步回调地址
//    sign				签名				String(32)		是				是			签名

    private static final String version               	="version";
    private static final String appid           		="appid";
    private static final String out_trade_no            ="out_trade_no";
    private static final String total_fee           	="total_fee";
    private static final String pay_id          		="pay_id";
    private static final String return_url              ="return_url";
    private static final String notify_url           	="notify_url";
    
    private static final String sign                ="sign";
    private static final String key                 ="key";
    


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(appid, channelWrapper.getAPI_MEMBERID());
                put(out_trade_no,channelWrapper.getAPI_ORDER_ID());
                put(total_fee,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(pay_id,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(return_url,channelWrapper.getAPI_WEB_URL());
                put(version,"1.1");
            }
        };
        log.debug("[星灯支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        String signSrc=String.format("%s%s%s%s%s%s%s%s",
        		appid+"="+api_response_params.get(appid)+"&",
        		notify_url+"="+api_response_params.get(notify_url)+"&",
        		out_trade_no+"="+api_response_params.get(out_trade_no)+"&",
        		pay_id+"="+api_response_params.get(pay_id)+"&",
        		return_url+"="+api_response_params.get(return_url)+"&",
        		total_fee+"="+api_response_params.get(total_fee)+"&",
        		version+"="+api_response_params.get(version),
        		channelWrapper.getAPI_KEY()
        );
        String paramsStr = signSrc.toString();
        String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[星灯支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
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
        	String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,null);
	        if (StringUtils.isBlank(resultStr)) {
	            log.error("[星灯支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(EMPTYRESPONSE);
	        }
	        resultStr = UnicodeUtil.unicodeToString(resultStr);
	        if (!resultStr.contains("{") || !resultStr.contains("}")) {
	            log.error("[星灯支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        JSONObject resJson;
	        try {
	            resJson = JSONObject.parseObject(resultStr);
	        } catch (Exception e) {
	            e.printStackTrace();
	            log.error("[星灯支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        if (null != resJson && resJson.containsKey("data")) {
	        	JSONObject data = resJson.getJSONObject("data");
	        	if(null != data&&data.containsKey("resp_code")&&data.getString("resp_code").equals("00")){
	        		result.put(JUMPURL, data.getString("payment"));
	        	}else{
	        		log.error("[星灯支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
		            throw new PayException(resultStr);
	        	}
	        }else {
	            log.error("[星灯支付]-[请求支付]-3.5.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        payResultList.add(result);
        }
        log.debug("[星灯支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[星灯支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}