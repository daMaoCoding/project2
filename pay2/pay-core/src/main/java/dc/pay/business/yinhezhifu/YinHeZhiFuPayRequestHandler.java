package dc.pay.business.yinhezhifu;

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
import dc.pay.utils.DateUtil;
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
@RequestPayHandler("YINHEZHIFU")
public final class YinHeZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(YinHeZhiFuPayRequestHandler.class);

//    字段						输入项名称			属性				注释				长度
//    versionId					服务版本号			必输	 			1.1当前			1
//    orderAmount				订单金额				必输				以分为单位		25
//    orderDate					订单日期				必输				yyyyMMddHHmmss	14
//    currency					货币类型				必输				RMB：人民币  其他币种代号另行提供	8
//    transType					交易类别				必输				默认填写 0008	4
//    asynNotifyUrl				异步通知URL			必输				结果返回URL，1.7接口用到。支付系统处理完请求后，将处理结果返回给这个URL	200
//    synNotifyUrl				同步返回URL			必输				针对该交易的交易状态同步通知接收URL	120
//    signType					加密方式				必输				MD5	4
//    merId						商户编号				必输				30
//    prdOrdNo					商户订单号			必输				30
//    payMode					支付方式				必输				00021-支付宝扫码	50
//    receivableType			到账类型				必输				D00	10
//    prdAmt					商品价格				必输				以分为单位	13
//    prdName					商品名称				必输				50
//    signData					加密数据				必输				signTgpe为MD5时：将把所有参数按名称a-z排序

    private static final String versionId               ="versionId";
    private static final String orderAmount             ="orderAmount";
    private static final String orderDate           	="orderDate";
    private static final String currency           		="currency";
    private static final String transType          		="transType";
    private static final String asynNotifyUrl           ="asynNotifyUrl";
    private static final String synNotifyUrl            ="synNotifyUrl";
    private static final String signType           		="signType";
    private static final String merId            		="merId";
    private static final String prdOrdNo                ="prdOrdNo";
    private static final String payMode                 ="payMode";
    private static final String receivableType          ="receivableType";
    private static final String prdAmt          		="prdAmt";
    private static final String prdName          		="prdName";
    private static final String signData          		="signData";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merId, channelWrapper.getAPI_MEMBERID());
                put(prdOrdNo,channelWrapper.getAPI_ORDER_ID());
                put(prdAmt,channelWrapper.getAPI_AMOUNT());
                put(asynNotifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(payMode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(synNotifyUrl,channelWrapper.getAPI_WEB_URL());
                put(orderDate,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
                put(signType,"MD5");
                put(versionId,"1.1");
                put(orderAmount,channelWrapper.getAPI_AMOUNT());
                put(currency,"RMB");
                put(transType,"0008");
                put(receivableType,"D00");
                put(prdName,channelWrapper.getAPI_ORDER_ID());
            }
        };
        log.debug("[银河支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
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
        //signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "" );
        signSrc.append("key="+channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[银河支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
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
	            log.error("[银河支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(EMPTYRESPONSE);
	        }
	        resultStr = UnicodeUtil.unicodeToString(resultStr);
	        //resultStr=resultStr.replaceAll("\\\\", "");
	        if (!resultStr.contains("{") || !resultStr.contains("}")) {
	            log.error("[银河支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        JSONObject resJson;
	        try {
	            resJson = JSONObject.parseObject(resultStr);
	        } catch (Exception e) {
	            e.printStackTrace();
	            log.error("[银河支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        if (null != resJson && resJson.containsKey("retCode") && resJson.getString("retCode").equals("1")) {
	        	if(HandlerUtil.isZfbSM(channelWrapper)){
	        		 String code_url = resJson.getString("qrcode");
	        		 result.put(QRCONTEXT, code_url);
	        	}
	            if(HandlerUtil.isWapOrApp(channelWrapper)){
	            	String code_url = resJson.getString("htmlText");
	            	result.put(HTMLCONTEXT, code_url);
	            }
	        }else {
	            log.error("[银河支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        payResultList.add(result);
        }
        log.debug("[银河支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[银河支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}