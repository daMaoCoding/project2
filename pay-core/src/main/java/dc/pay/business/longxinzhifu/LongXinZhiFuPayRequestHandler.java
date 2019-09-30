package dc.pay.business.longxinzhifu;

import java.util.*;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayRequestHandler;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

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
 * Jan 15, 2019
 */
@RequestPayHandler("LONGXINZHIFU")
public final class LongXinZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(LongXinZhiFuPayRequestHandler.class);

	private static final String  companyId = "companyId";     //	用户ID 由商务分配
	private static final String  userOrderId = "userOrderId";     //	用户自定义订单同步时候会返回
	private static final String  payType = "payType";     //	支付方式
	private static final String  item = "item";     //	商品名
	private static final String  fee = "fee";     //	价格 (单位分)
//	private static final String  expire = "expire";     //	超时时间(可选参数,单位:秒)
	private static final String  callbackUrl = "callbackUrl";     //	前端回调地址(不是所有通道都能用)
	private static final String  syncUrl = "syncUrl";     //	异步通知地址
	private static final String  ip = "ip";     //	终端用户的IP
	private static final String  mobile = "mobile";     //	手机号/或者用户在贵方系统中的唯一会员ID 仅在快捷支付时候需要使用
//	private static final String  name = "name";     //	持卡人姓名,仅在快捷支付时候使用,可选
//	private static final String  idCardNo = "idCardNo";     //	持卡人身份证号码,仅在快捷支付时候使用,可选
//	private static final String  sign = "sign";     //	签名=MD5(companyId_userOrderId_fee_用户密钥) 小写 参数之间用下划线连接

    @Override
    protected Map<String, String> buildPayParam() throws PayException {

        Map<String, String> payParam = new TreeMap<String, String>() {
            {
	            put(companyId,channelWrapper.getAPI_MEMBERID());
	            put(userOrderId,channelWrapper.getAPI_ORDER_ID());
	            put(payType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            put(item,channelWrapper.getAPI_ORDER_ID());
	            put(fee,channelWrapper.getAPI_AMOUNT());
	            put(callbackUrl,channelWrapper.getAPI_WEB_URL() );
	            put(syncUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL() );
	            put(ip,channelWrapper.getAPI_Client_IP() );
	            put(mobile,HandlerUtil.getRandomNumber(11) );
            }
        };
        log.debug("[龙信支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
	    //签名=MD5(companyId_userOrderId_fee_用户密钥) 小写
	    String paramsStr = String.format("%s_%s_%s_%s",
			    api_response_params.get(companyId),
			    api_response_params.get(userOrderId),
			    api_response_params.get(fee),
			    channelWrapper.getAPI_KEY());
	    String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
	    log.debug("[龙信支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
	    return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
	    payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
	    ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
	    Map result = Maps.newHashMap();
	    String resultStr;
	    try {
		    if (1==2  &&  HandlerUtil.isWY(channelWrapper) ) {
			    result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='post'"));
			    payResultList.add(result);
		    }else{
			    resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);

			    if(StringUtils.isNotBlank(resultStr) && resultStr.contains("<form") && !resultStr.contains("{")){
				    result.put(HTMLCONTEXT,resultStr);
				    payResultList.add(result);
			    }else if(StringUtils.isNotBlank(resultStr) ){
				    JSONObject jsonResultStr = JSON.parseObject(resultStr);
				    if(null!=jsonResultStr && jsonResultStr.containsKey("result") && "0".equalsIgnoreCase(jsonResultStr.getString("result"))
						    && jsonResultStr.containsKey("param") && StringUtils.isNotBlank(jsonResultStr.getString("param"))){
					    if(HandlerUtil.isWapOrApp(channelWrapper)){
						    result.put(JUMPURL, jsonResultStr.getString("param"));
					    }else{
						    result.put(QRCONTEXT, jsonResultStr.getString("param"));   //param为浏览器跳转地址 ，param为二维码图片地址 ,qrData为用于生成二维码的原始数据
					    }
					    payResultList.add(result);
				    }else {throw new PayException(resultStr); }
			    }else{ throw new PayException(EMPTYRESPONSE);}

		    }
	    } catch (Exception e) {
		    log.error("[龙信支付]3.发送支付请求，及获取支付请求结果出错：", e);
		    throw new PayException(e.getMessage(), e);
	    }
	    log.debug("[龙信支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[龙信支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}