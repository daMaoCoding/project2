package dc.pay.business.yinhe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

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
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.UnicodeUtil;
import dc.pay.utils.ValidateUtil;


/**
 * 
 * 
 * @author sunny
 * Dec 25, 2018
 */
@RequestPayHandler("YINHE")
public final class YinHePayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(YinHePayRequestHandler.class);

//    键名				必填			描述							最大长度
//    version			真			协议版本						4
//    chennelID			真			渠道id						8
//    money				真			申请付款金额					8
//    orderNum			真			C方提供的订单号				16
//    time				真	C方发起时间,不做验证,防止重复提交,可为空	24
//    back_url			真	C方提供的通知地址，用于接收订单状态改变时的通知	100
//    sign				真	数据签名 md5(具体签名算法参照下方的签名算法)	32

  private static final String version                 ="version";
  private static final String chennelID               ="chennelID";
  private static final String money                   ="money";
  private static final String time           		  ="time";
  private static final String back_url                ="back_url";
  private static final String sign             		  ="sign";
  private static final String orderNum                ="orderNum";
  
  private static final String key             		  ="key";


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(time, HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "yyyyMMddHHmmss"));
                put(chennelID,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(money,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(orderNum,channelWrapper.getAPI_ORDER_ID());
                put(back_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(version,"1.0");
            }
        };
        log.debug("[银河支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        /*List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
            	signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "" );	
        signSrc.append(channelWrapper.getAPI_KEY());*/
    	 
        String paramsStr = getPrePaySignParameters(api_response_params,channelWrapper.getAPI_KEY());
        String signMD5 = HandlerUtil.getPhpMD5(paramsStr);
        log.debug("[银河支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return signMD5;
    }

     protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
    	 
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        /*if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
        }*/
        String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.GET);
        if (StringUtils.isBlank(resultStr)) {
            log.error("[银河支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        resultStr = UnicodeUtil.unicodeToString(resultStr);
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
        if (null != resJson && resJson.containsKey("ret") && resJson.getString("ret").equals("1")) {
            String code_url = resJson.getString("payUrl");
            result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
        }else {
            log.error("[银河支付]]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
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
    
    // 按顺序生成sign (MD5值)
 	public static String getPrePaySignParameters(Map<String,String> map, String key){
 		ArrayList<String> list = new ArrayList<String>();
 		for (Map.Entry<String, String> entry : map.entrySet()) 
 		{
 			if (entry.getKey().equals("sign")) 
 			{
 				continue;
 			}
 			list.add(entry.getKey() + "=" + entry.getValue() + "&");
 		}
 		int size = list.size();
 		String[] arrayToSort = list.toArray(new String[size]);
 		Arrays.sort(arrayToSort);
 		StringBuilder sb = new StringBuilder();
 		for (int i = 0; i < size; i++) 
 		{
 			sb.append(arrayToSort[i]);
 		}
 		String result = sb.toString();
 		if (result.endsWith("&")) 
 		{
 			result = result.substring(0, result.length() - 1);
 		}
 		result = result + key;
 		return result;
 	}
}