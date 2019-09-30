package dc.pay.business.beifuzhifu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import dc.pay.business.tianyi.DesHelper;
import dc.pay.business.tianyi.SignHelper;
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
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 天一,大千,贝富
 */
@RequestPayHandler("BEIFUZHIFU")
public final class BeiFuZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(BeiFuZhiFuPayRequestHandler.class);

     private static final String order_trano_in               ="order_trano_in";
    private static final String order_goods                  ="order_goods";
    private static final String order_amount                 ="order_amount";
    private static final String order_extend                 ="order_extend";
    private static final String order_ip                     ="order_ip";
    private static final String order_return_url             ="order_return_url";
    private static final String order_notify_url             ="order_notify_url";

    private static final String timeStamp                    ="timeStamp";
    private static final String nonce                        ="nonce";


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
            	put(order_trano_in,channelWrapper.getAPI_ORDER_ID());
            	put(order_goods,channelWrapper.getAPI_ORDER_ID());
            	put(order_amount,  channelWrapper.getAPI_AMOUNT());
            	put(order_extend,channelWrapper.getAPI_MEMBERID());
            	put(order_ip,channelWrapper.getAPI_Client_IP());
            	put(order_return_url,channelWrapper.getAPI_WEB_URL());
            	put(order_notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            	put(timeStamp,System.currentTimeMillis()+"");
            	put(nonce,handlerUtil.getRandomStr(32));
            }
        };
        log.debug("[贝富支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
    	TreeMap<String, String> treeMap = new TreeMap<>(api_response_params);
    	// key的字母排序asc
		String data = SignHelper.sortSign(treeMap);
		// 排序后的数据进行MD5加密
		String signMd5 = SignHelper.MD5(api_response_params.get(timeStamp) + api_response_params.get(nonce) + data );
        log.debug("[贝富支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
    	// 将需要发送的Json数据进行DES加密
    	String content = DesHelper.encrypt(JSON.toJSONString(payParam), payParam.get(timeStamp) + channelWrapper.getAPI_MEMBERID() + payParam.get(nonce)).toUpperCase();
        HashMap<String, String> result = Maps.newHashMap();
    	Map<String,String> headersMap = new HashMap<>();
//		// // 设置文件字符集:
    	headersMap.put("Charset", "UTF-8");
//		// 转换为字节数组
		byte[] data = (content.toString()).getBytes();
//		// 设置文件长度
		headersMap.put("Content-Length", String.valueOf(data.length));
		headersMap.put("key", channelWrapper.getAPI_MEMBERID());
		headersMap.put("timestamp", payParam.get(timeStamp));
		headersMap.put("nonce", payParam.get(nonce));
		headersMap.put("signtype", "MD5");
		headersMap.put("signature", pay_md5sign);
		String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), content, headersMap);
		if (StringUtils.isBlank(resultStr)) {
//			log.error("[贝富支付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(payParam));
//			throw new PayException("返回空,参数："+JSON.toJSONString(payParam));
			log.error("[贝富支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
			throw new PayException(resultStr);
		}
		if (!resultStr.contains("{") || !resultStr.contains("}")) {
			log.error("[贝富支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
		    throw new PayException(resultStr);
		}
		JSONObject resJson = JSONObject.parseObject(resultStr);
		if (!resJson.containsKey("code") || !"1".equals(resJson.getString("code"))) {
		    log.error("[贝富支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
		    throw new PayException(resultStr);
		}
		String string = resJson.getString("data");
		if (StringUtils.isBlank(string)) {
			log.error("[贝富支付]-[请求支付]-3.4.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(payParam));
			throw new PayException(resultStr);
		}
		result.put(JUMPURL, JSONObject.parseObject(string).getString("order_pay_url"));
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		payResultList.add(result);
        log.debug("[贝富支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[贝富支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}