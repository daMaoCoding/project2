package dc.pay.business.huaxin;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
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
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;
import dc.pay.business.ruijietong.RuiJieTongUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Jul 5, 2018
 */
@RequestPayHandler("HUAXIN")
public final class HuaXinPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(HuaXinPayRequestHandler.class);

    //输入项              输入项名称        属性         注释                                数据类型 
    //merchNo             商户号            M                                                as..32 
    //version             版本号            M            V3.1.0.0                            as..20  
    //data                加密参数          M            通过 RSA加密                        as..100 
    //data包含以下参数 
    //数据类 
    //输入项             输入项名称        属性          注释                                型 
    //orderNum           订单号            M            商户生成的订单号                     as..32
    //version            版本号            M            V3.1.0.0                             as..20                  
    //charset            字符编码          M            UTF-8                                As..20                  
    //random             4位随机数         M                                                 As..10                  
    //merNo              商户号            M                                                 as..30                  
    //netway             网关类型          M            见 1.7.2附录                         As..20                  
    //amount             金额（分）        M            100标识人民币  1元                   as..256                 
    //goodsName          商品名称          M                                                 as..256                 
    //callBackUrl        回调地址          M            http://api.hxfglobal.com/            As..256                 
    //callBackViewUrl    回显地址          M                                                 ans..64                 
    //sign               签名              M            签名方法见 1.5.1.1签名机制           As..64   
//    private static final String merchNo                    ="merchNo";
    private static final String version                    ="version";
//    private static final String data                       ="data";
    
    private static final String orderNum                    ="orderNum";
//    private static final String version                     ="version";
    private static final String charset                     ="charset";
    private static final String random                      ="random";
    private static final String merNo                       ="merNo";
    private static final String netway                      ="netway";
    private static final String amount                      ="amount";
    private static final String goodsName                   ="goodsName";
    private static final String callBackUrl                 ="callBackUrl";
    private static final String callBackViewUrl             ="callBackViewUrl";

    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        String api_KEY = channelWrapper.getAPI_KEY();
        if (null == api_KEY || !api_KEY.contains("-") || api_KEY.split("-").length != 2) {
            log.error("[华信]-[请求支付]-“密钥（私钥）”输入数据格式为【中间使用-分隔】：MD5秘钥-支付私钥" );
            throw new PayException("[华信]-[请求支付]-“密钥（私钥）”输入数据格式为【中间使用-分隔】：MD5秘钥-支付私钥" );
        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
            	put(orderNum,channelWrapper.getAPI_ORDER_ID());
            	put(version,"V3.1.0.0");
            	put(charset,"UTF-8");
            	put(random,handlerUtil.getRandomStr(4));
            	put(merNo, channelWrapper.getAPI_MEMBERID());
            	put(netway,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            	put(amount, channelWrapper.getAPI_AMOUNT());
            	put(goodsName,"name");
            	put(callBackUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            	put(callBackViewUrl,channelWrapper.getAPI_WEB_URL());            	
            }
        };
        log.debug("[华信]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        Map asc2 = MapUtils.sortMapByKeyAsc2(api_response_params);
        String signMd5 = HandlerUtil.getMD5UpperCase(JSON.toJSONString(asc2)+channelWrapper.getAPI_KEY().split("-")[0]);
        log.debug("[华信]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        HashMap<String, String> result = Maps.newHashMap();
    	String resultStr = null;
    	try {
    		byte[] dataStr = RuiJieTongUtil.encryptByPublicKey(JSON.toJSONString(new TreeMap<>(payParam)).getBytes(RuiJieTongUtil.CHARSET),channelWrapper.getAPI_PUBLIC_KEY());
    		String reqParam = "data=" + URLEncoder.encode(java.util.Base64.getEncoder().encodeToString(dataStr), RuiJieTongUtil.CHARSET) + "&merchNo=" + payParam.get(merNo)+ "&version=" + payParam.get(version);
			resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), reqParam,MediaType.APPLICATION_FORM_URLENCODED_VALUE).trim();
			if (StringUtils.isBlank(resultStr)) {
				log.error("[华信]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(payParam));
				throw new PayException("返回空,参数："+JSON.toJSONString(payParam));
			}
			resultStr = new String(resultStr.getBytes("ISO-8859-1"), "UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
			log.error("[华信]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
			 throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
		}
    	JSONObject resJson = JSONObject.parseObject(resultStr);
    	if (!resJson.containsKey("stateCode") || !"00".equals(resJson.getString("stateCode"))) {
             log.error("[华信]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
             throw new PayException(resultStr);
    	}
    	result.put(handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, resJson.getString("qrcodeUrl"));
    	payResultList.add(result);
        log.debug("[华信]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[华信]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}