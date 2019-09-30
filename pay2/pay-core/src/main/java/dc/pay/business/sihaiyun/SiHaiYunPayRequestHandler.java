package dc.pay.business.sihaiyun;

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
@RequestPayHandler("SIHAIYUN")
public final class SiHaiYunPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(SiHaiYunPayRequestHandler.class);

//    参数名称	参数含义		是否必填		参数长度		参数类型		参数说明		签名顺序
//    p0_Cmd	业务类型		是			Max(20)		String		固定值“Buy”.	1
//    p1_MerId	商户编号		是			Max(11)		Int			商户在系统的唯一身份标识.获取方式请联系客服	2
//    p2_Order	商户订单号	是			Max(50)		String		提交的订单号必须在自身账户交易中唯一。	3
//    p3_Amt	支付金额		是			Max(20)		String		单位:元，精确到分.此参数为空则无法直连(如直连会报错：抱歉，交易金额太小。)	4
//    p4_Cur	交易币种		是			Max(10)		String		固定值“CNY”.	5
//    p5_Pid	商品名称		是			Max(20)		String		用于支付时显示在网关左侧的订单产品信息.此参数如用到中文，请注意转码.	6
    private static final String p0_Cmd               	="p0_Cmd";
    private static final String p1_MerId           		="p1_MerId";
    private static final String p2_Order           		="p2_Order";
    private static final String p3_Amt           		="p3_Amt";
    private static final String p4_Cur          		="p4_Cur";
    private static final String p5_Pid              	="p5_Pid";
    private static final String p6_Pcat            		="p6_Pcat";
    private static final String p7_Pdesc          		="p7_Pdesc";
    private static final String p8_Url            		="p8_Url";
    private static final String pa_MP                	="pa_MP";
    private static final String pd_FrpId                ="pd_FrpId";
    private static final String pr_NeedResponse         ="pr_NeedResponse";
    private static final String hmac         			="hmac";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(p1_MerId, channelWrapper.getAPI_MEMBERID());
                put(p2_Order,channelWrapper.getAPI_ORDER_ID());
                put(p3_Amt,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(p8_Url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(pd_FrpId,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(p0_Cmd,"Buy");
                put(p4_Cur,"CNY");
                put(p5_Pid,channelWrapper.getAPI_ORDER_ID());
                put(p6_Pcat,channelWrapper.getAPI_ORDER_ID());
                put(p7_Pdesc,channelWrapper.getAPI_ORDER_ID());
                put(pa_MP,channelWrapper.getAPI_ORDER_ID());
                put(pr_NeedResponse,"1");
            }
        };
        log.debug("[四海云付支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
//            if (!pay_productname.equals(paramKeys.get(i)) && !pay_attach.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
            if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
            	signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        //最后一个&转换成#
        signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "" );
        String paramsStr = signSrc.toString();
        String signMD5 = DigestUtil1.hmacSign(paramsStr, channelWrapper.getAPI_KEY());
        log.debug("[四海云付支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return signMD5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        if (HandlerUtil.isWapOrApp(channelWrapper)) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());  //.replace("method='post'","method='get'"));
            payResultList.add(result);
        }else{
        	String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,null);
	        if (StringUtils.isBlank(resultStr)) {
	            log.error("[四海云付支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(EMPTYRESPONSE);
	        }
	        resultStr = UnicodeUtil.unicodeToString(resultStr);
	        resultStr=resultStr.replaceAll("\\\\", "");
	        if (!resultStr.contains("{") || !resultStr.contains("}")) {
	            log.error("[四海云付支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        JSONObject resJson;
	        try {
	            resJson = JSONObject.parseObject(resultStr);
	        } catch (Exception e) {
	            e.printStackTrace();
	            log.error("[四海云付支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        if (null != resJson && resJson.containsKey("status") && resJson.getString("status").equals("0")) {
	            String code_url = resJson.getString("payImg");
	            result.put(JUMPURL, code_url);
	        }else {
	            log.error("[四海云付支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        payResultList.add(result);
        }
        log.debug("[四海云付支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[四海云付支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}