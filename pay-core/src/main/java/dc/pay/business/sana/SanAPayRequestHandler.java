package dc.pay.business.sana;

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
 * Dec 18, 2018
 */
@RequestPayHandler("SANA")
public final class SanAPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(SanAPayRequestHandler.class);

//    参数名			参数含义			参考值			备注
//    name			商户名称			PT_AAA01	
//    time			秒级时间戳		1543315597	
//    attach		自定义参数		5sa6d6sads	
//    redirect_url	商户默认回调地址	http://www.cctelecom.cn/api/redirect_tes	
//    sign			参数签名			16452057528362BB8350DA170C326318	参数签名由参数：name+time+attach+redirect_url ，以字符串凭据为一个新字符串进行MD5后转为大写完成
    
//    token			商户凭据			D2K42057528362BB8350DA170C326318	
//    time			秒级时间戳		1543315597 	
//    order_num		商户订单号		RXTE95598568995681543313924	
//    order_price	订单价格			2000	
//    attach		自定义参数		RX123353D	
//    third_num		商户名称			PT_AAA01	
//    is_img_url	使用二维码访问地址	1或0	可选参数，选择返回二维码访问地址必填大于0表示选此项
//    redirect_url	订单回调地址		http://www.cctelecom.cn/api/redirect_tes	用于订单成功后发送成回调
//    is_page		使用我端付款页面	1或0	可选参数，选择使用我端提供的支付页面必填大于0表示选此项，默认选择此项
//    sign			参数签名			D2K42057528362BB8350DA170C326318	由参数：token+ time+ order_num+ order_price+ attach 进行字符串拼接后进行MD5加密转大写

    private static final String name                ="name";
    private static final String time           		="time";
    private static final String attach           	="attach";
    private static final String redirect_url        ="redirect_url";
    private static final String signType            ="sign";
    
    private static final String token            	="token";
    private static final String order_num           ="order_num";
    private static final String order_price         ="order_price";
    private static final String third_num           ="third_num";
    private static final String is_img_url          ="is_img_url";
    private static final String is_page            	="is_page";
    private static final String sign                ="sign";
    private static final String key                 ="key";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(name, channelWrapper.getAPI_MEMBERID());
                put(order_num,channelWrapper.getAPI_ORDER_ID());
                put(order_price,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(redirect_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(time,System.currentTimeMillis()+"");
                put(attach,UUID.randomUUID().toString().replaceAll("-", ""));
                put(is_img_url,"1");
                put(is_page,"1");
                put(third_num,channelWrapper.getAPI_MEMBERID());
            }
        };
        log.debug("[3A支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
    	 String signSrc=String.format("%s",
    			 channelWrapper.getAPI_KEY()+
    			 api_response_params.get(time).substring(0, 10)+
    			 api_response_params.get(order_num)+
    			 api_response_params.get(order_price)+
    			 api_response_params.get(attach)
         		);
    	String signMD5 = HandlerUtil.getMD5UpperCase(signSrc); 
        log.debug("[3A支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return signMD5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
    	HashMap<String, String> postMap = Maps.newHashMap();
    	postMap.put(token,channelWrapper.getAPI_KEY());
    	postMap.put(time, payParam.get(time).substring(0, 10));
    	postMap.put(order_num, payParam.get(order_num));
    	postMap.put(order_price, payParam.get(order_price));
    	postMap.put(attach, payParam.get(attach));
    	postMap.put(third_num, payParam.get(name));
    	postMap.put(is_img_url, payParam.get(is_img_url));
    	postMap.put(redirect_url, payParam.get(redirect_url));
    	postMap.put(is_page, payParam.get(is_page));
    	postMap.put(sign, pay_md5sign);
        String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), postMap);
        if (StringUtils.isBlank(resultStr)) {
            log.error("[3A支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(EMPTYRESPONSE);
        }
        resultStr = UnicodeUtil.unicodeToString(resultStr);
        resultStr=resultStr.replaceAll("\\\\", "");
        if (!resultStr.contains("{") || !resultStr.contains("}")) {
            log.error("[3A支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        JSONObject resJson;
        try {
            resJson = JSONObject.parseObject(resultStr);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[3A支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        if (null != resJson && resJson.containsKey("code") && resJson.getString("code").equals("1")) {
            String code_url = resJson.getString("pay_url");
            result.put(JUMPURL, code_url);
        }else {
            log.error("[3A支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        payResultList.add(result);
        log.debug("[3A支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[3A支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}