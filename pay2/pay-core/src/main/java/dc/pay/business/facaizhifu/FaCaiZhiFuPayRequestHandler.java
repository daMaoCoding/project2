package dc.pay.business.facaizhifu;

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
 * @author mikey
 * Jun 5, 2019
 */
@RequestPayHandler("FACAIZHIFU")
public final class FaCaiZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(FaCaiZhiFuPayRequestHandler.class);
/*
 参数名			参数		可空	加入签名	说明
 商户号			merchant	N		Y			下发的商户号
 金额			amount		N		Y			单位元（人民币），2位小数
    支付方式		pay_code	N		Y			填写相应的支付方式编码
    商户订单号		order_no	N		Y			订单号，max(50),该值需在商户系统内唯一
    异步通知地址	notify_url	N		Y			异步通知地址，需要以http://开头且没有任何参数
   同步通知地址	return_url	N		Y			同步跳转地址，支付成功后跳回
    请求返回方式	json		Y		N			固定值：json; 注意：只适用于扫码付款
    备注消息		attach		Y		有值加入		回调时原样返回
    请求时间		order_time	Y		Y			格式YYYY-MM-DD hh:ii:ss，回调时原样返回
    商户的用户id	cuid		Y		有值加入		商户名下的能表示用户的标识，方便对账，回调时原样返回
MD5签名		sign		N		N			32位小写MD5签名值
*/
    
    private static final String merchant	= "merchant";	//下发的商户号
    private static final String amount		= "amount";		//单位元（人民币），2位小数
    private static final String pay_code	= "pay_code";	//填写相应的支付方式编码
    private static final String order_no	= "order_no";	//订单号，max(50),该值需在商户系统内唯一
    private static final String notify_url	= "notify_url";	//异步通知地址，需要以http://开头且没有任何参数
    private static final String return_url	= "return_url";	//同步跳转地址，支付成功后跳回
    private static final String json		= "json";		//固定值：json; 注意：只适用于扫码付款
    private static final String order_time	= "order_time";	//格式YYYY-MM-DD hh:ii:ss，回调时原样返回
    private static final String sign		= "sign";		//32位小写MD5签名值
    private static final String key        	= "key";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>();
        payParam.put(merchant, channelWrapper.getAPI_MEMBERID());
        payParam.put(amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
        payParam.put(pay_code,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
        payParam.put(order_no,channelWrapper.getAPI_ORDER_ID());
        payParam.put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
        payParam.put(return_url,channelWrapper.getAPI_WEB_URL());
        if (handlerUtil.isWEBWAPAPP_SM(channelWrapper)) {	//注意：只适用于扫码付款
        	payParam.put(json,"json");
        }
        payParam.put(order_time,DateUtil.formatDateTimeStrByParam("yyyy-MM-dd HH:mm:ss") );
        log.debug("[发财支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!json.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        signSrc.append(key +"="+ channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[发财支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(this.sign, pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        StringBuffer htmlContent = HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
        result.put(HTMLCONTEXT,htmlContent.toString());
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[发财支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[发财支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}