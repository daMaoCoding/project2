package dc.pay.business.daqian;

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
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Aug 7, 2018
 */
@RequestPayHandler("DAQIAN")
public final class DaQianPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(DaQianPayRequestHandler.class);

    //order_trano_in           商户单号                   Strng(32)             必填             商户单号 
    //order_goods              商品名称                   String(32)            必填             商品名称 
    //order_price              商品单价                   Int (32)              可选             商品单价，单位分 
    //order_num                商品数量                   Int(32)               可选             商品数量 
    //order_amount             订单金额                   Int(32)               必填             订单金额，单位分 
    //order_extend             商户自定义拓展参数         String(32)            可选             自定义参数，最大长度 64 位 
    //order_imsi               设备imsi                   String(32)            可选             设备 imsi 
    //order_mac                设备mac                    String(32)            可选             设备 mac 
    //order_brand              设备品牌                   String(32)            可选             设备品牌 
    //order_version            设备版本                   String(32)            可选             设备版本 
    //order_ip                 客户端ip                   String(32)            必填             客户端 ip 
    //order_return_url         同步地址                   String(32)            必填             同步地址 
    //order_notify_url         异步通知地址               String(32)            必填             异步通知地址 
    //order_bank_code          银行代码                   String(32)            必填             网关支付必填项（2018-6-14新增标红） 
    private static final String order_trano_in               ="order_trano_in";
    private static final String order_goods                  ="order_goods";
//    private static final String order_price                  ="order_price";
//    private static final String order_num                    ="order_num";
    private static final String order_amount                 ="order_amount";
    private static final String order_extend                 ="order_extend";
//    private static final String order_imsi                   ="order_imsi";
//    private static final String order_mac                    ="order_mac";
//    private static final String order_brand                  ="order_brand";
//    private static final String order_version                ="order_version";
    private static final String order_ip                     ="order_ip";
    private static final String order_return_url             ="order_return_url";
    private static final String order_notify_url             ="order_notify_url";
//    private static final String order_bank_code              ="order_bank_code";
    
    private static final String timeStamp                    ="timeStamp";
    private static final String nonce                        ="nonce";

    //signature    数据签名    32    是    　
//    private static final String signature  ="signature";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
            	put(order_trano_in,channelWrapper.getAPI_ORDER_ID());
            	put(order_goods,"name");
            	put(order_amount,  channelWrapper.getAPI_AMOUNT());
            	put(order_extend,channelWrapper.getAPI_MEMBERID());
            	put(order_ip,channelWrapper.getAPI_Client_IP());
            	put(order_return_url,channelWrapper.getAPI_WEB_URL());
            	put(order_notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            	put(timeStamp,System.currentTimeMillis()+"");
            	put(nonce,handlerUtil.getRandomStr(32));
            }
        };
        log.debug("[大千]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
    	TreeMap<String, String> treeMap = new TreeMap<>(api_response_params);
    	// key的字母排序asc
		String data = SignHelper.sortSign(treeMap);
		// 排序后的数据进行MD5加密
		String signMd5 = SignHelper.MD5(api_response_params.get(timeStamp) + api_response_params.get(nonce) + data );
        log.debug("[大千]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
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
//			log.error("[大千]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(payParam));
//			throw new PayException("返回空,参数："+JSON.toJSONString(payParam));
			log.error("[大千]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
			throw new PayException(resultStr);
		}
		if (!resultStr.contains("{") || !resultStr.contains("}")) {
			log.error("[大千]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
		    throw new PayException(resultStr);
		}
		JSONObject resJson = JSONObject.parseObject(resultStr);
		if (!resJson.containsKey("code") || !"1".equals(resJson.getString("code"))) {
		    log.error("[大千]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
		    throw new PayException(resultStr);
		}
		String string = resJson.getString("data");
		if (StringUtils.isBlank(string)) {
			log.error("[大千]-[请求支付]-3.4.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(payParam));
			throw new PayException(resultStr);
		}
		result.put(JUMPURL, JSONObject.parseObject(string).getString("order_pay_url"));
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		payResultList.add(result);
        log.debug("[大千]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[大千]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}