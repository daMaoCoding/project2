package dc.pay.business.qingtingzhifu;

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
 * @author sunny
 * 01 15, 2019
 */
@RequestPayHandler("QINGTINGZHIFU")
public final class QingTingZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(QingTingZhiFuPayRequestHandler.class);

//    字段名			字段说明			最大长度	 	是否必填		备注
//    version		版本号			3			是			固定值：1.0
//    charSet		字符集			6			是			固定：UTF8
//    remark		订单备注			32			是			订单备注说明，可与商品名称一致
//    returnurl		同步支付跳转	Strign(100)		是			支付成功后，通过页面跳转的方式跳转到商户指定的网站页面，例如：https://www.baidu.com/
//    merchno		商户号			8			是			商户注册签约后，支付平台分配的唯一标识号
//    traceno		商户订单号		32			是			由商户系统生成的唯一订单编号，最大长度为32位
//    paytype		支付方式			Strign		是			见支付方式说明
//    commodityname	商品名称			32			是			商品名称
//    total_fee		商户订单总金额	Number(10)		是			订单总金额以分为单位
//    notifyurl		通知地址			100			是			支付成功后，支付平台会主动通知商家系统，因此商家必须指定接收通知的地址，例如： https://www.baidu.com/
//    sign			数据签名			32			是			对签名数据进行两次MD5加密后转大写
    private static final String version               ="version";
    private static final String charSet           	  ="charSet";
    private static final String remark           	  ="remark";
    private static final String returnurl             ="returnurl";
    private static final String merchno          	  ="merchno";
    private static final String traceno               ="traceno";
    private static final String paytype               ="paytype";
    private static final String commodityname         ="commodityname";
    private static final String total_fee             ="total_fee";
    private static final String notifyurl             ="notifyurl";
    private static final String sign                  ="sign";
    private static final String key                   ="key";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merchno, channelWrapper.getAPI_MEMBERID());
                put(traceno,channelWrapper.getAPI_ORDER_ID());
                put(total_fee,channelWrapper.getAPI_AMOUNT());
                put(notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(paytype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(returnurl,channelWrapper.getAPI_WEB_URL());
                put(version,"1.0");
                put(charSet,"UTF8");
                put(remark,channelWrapper.getAPI_ORDER_ID());
                put(commodityname,channelWrapper.getAPI_ORDER_ID());
            }
        };
        log.debug("[蜻蜓支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
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
        //signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "#" );
        signSrc.append(key+"="+channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMD5 =HandlerUtil.getPhpMD5(HandlerUtil.getPhpMD5(paramsStr)).toUpperCase();
        log.debug("[蜻蜓支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
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
	        String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
	        if (StringUtils.isBlank(resultStr)) {
	            log.error("[蜻蜓支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(EMPTYRESPONSE);
	        }
	        resultStr = UnicodeUtil.unicodeToString(resultStr);
	        if (!resultStr.contains("{") || !resultStr.contains("}")) {
	            log.error("[蜻蜓支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        JSONObject resJson;
	        try {
	            resJson = JSONObject.parseObject(resultStr);
	        } catch (Exception e) {
	            e.printStackTrace();
	            log.error("[蜻蜓支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        if (null != resJson && resJson.containsKey("respCode") && resJson.getString("respCode").equals("0000")) {
	            String code_url = resJson.getString("barUrl");
	            result.put(JUMPURL , code_url);
	        }else {
	            log.error("[蜻蜓支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        payResultList.add(result);
        }
        log.debug("[蜻蜓支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[蜻蜓支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}