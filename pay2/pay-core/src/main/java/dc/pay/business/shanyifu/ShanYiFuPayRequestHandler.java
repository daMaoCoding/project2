package dc.pay.business.shanyifu;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

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
import dc.pay.utils.JsonUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 
 * @author andrew
 * Apr 19, 2018
 */
@RequestPayHandler("SHANYIFU")
public final class ShanYiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(ShanYiFuPayRequestHandler.class);

    //参数名			参数含义					参数长度	是否必填
    //version		版本号，固定值：V2.0.0.0		8		是
    //merNo			商户号					16		是
    //subMerNo		子商户号					16		是
    //netway		支付网关代码，参考附录 3.1		16		是
    //random		随机数					4		是
    //orderNum		订单号，必须按当前时间的 yyyyMMdd 开头，并以北京时间为准（例如：2017080800001）20 是
    //amount		金额（单位：分）				14		是
    //goodsName		商品名称					20		是
    //callBackUrl		支付结果通知地址			128		是
    //callBackViewUrl	回显地址				128		是
    //charset		客户端系统编码格式，UTF-8，GBK  10		是
    //sign			签名（字母大写）				32		是
    private static final String version		 = "version";
    private static final String merNo		 = "merNo";
//    private static final String subMerNo	 = "subMerNo";
    private static final String netway		 = "netway";
    private static final String random		 = "random";
    private static final String orderNum	 = "orderNum";
    private static final String amount		 = "amount";
    private static final String goodsName	 = "goodsName";
    private static final String callBackUrl		 = "callBackUrl";
    private static final String callBackViewUrl	 = "callBackViewUrl";
    private static final String charset		 = "charset";
    
    private static final String data		 = "data";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
            	put(orderNum, channelWrapper.getAPI_ORDER_ID());
            	put(version, "V2.0.0.0");
            	put(charset, "UTF-8");
            	put(random, HandlerUtil.getRandomStr(4));
            	put(merNo, channelWrapper.getAPI_MEMBERID());
            	put(netway, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            	put(amount,channelWrapper.getAPI_AMOUNT());
            	put(callBackUrl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            	put(callBackViewUrl, channelWrapper.getAPI_WEB_URL());
            	put(goodsName, "name");
            }
        };
        log.debug("[闪亿付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }
    
	 protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
		Map map = MapUtils.sortMapByKeyAsc2(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        String str = JsonUtil.stringify(map);
        signSrc.append(str);
        signSrc.append(channelWrapper.getAPI_KEY());
		String paramsStr = signSrc.toString();
		String pay_md5sign = HandlerUtil.getMD5UpperCase(paramsStr,"UTF-8");
        log.debug("[闪亿付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }
	 
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
    	String reqParam = data+"=" + JsonUtil.stringify(payParam);
        String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), reqParam,MediaType.APPLICATION_FORM_URLENCODED_VALUE,"Keep-Alive",MediaType.APPLICATION_FORM_URLENCODED_VALUE).trim();
		if (StringUtils.isBlank(resultStr)) {
			log.error("[闪亿付]3.1.发送支付请求，获取支付请求返回值异常:返回空"+",参数："+JSON.toJSONString(payParam));
			throw new PayException("返回空"+",参数："+JSON.toJSONString(payParam));
		}
		try {
			resultStr = new String(resultStr.getBytes("ISO-8859-1"), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			log.error("[闪亿付]3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
			throw new PayException(resultStr);
		}
		JSONObject resJson = JSONObject.parseObject(resultStr);
		if (resJson == null || !resJson.containsKey("stateCode") || !"00".equals(resJson.getString("stateCode"))) {
			log.error("[闪亿付]3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
			throw new PayException(resultStr);
		}
		Map result = Maps.newHashMap();
		result.put((HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isFS(channelWrapper)) ? JUMPURL : QRCONTEXT, resJson.getString("qrcodeUrl"));
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[闪亿付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
        return payResultList;
    }

    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (null != resultListMap && !resultListMap.isEmpty()) {
            if (resultListMap.size() == 1) {
                requestPayResult =  buildResult(resultListMap.get(0), channelWrapper, requestPayResult);
            }
            if (ValidateUtil.requestesultValdata(requestPayResult)) {
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            } else {
                throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[闪亿付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}