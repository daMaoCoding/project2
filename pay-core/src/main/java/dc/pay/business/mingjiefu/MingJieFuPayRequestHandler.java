package dc.pay.business.mingjiefu;

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
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * May 5, 2018
 */
@RequestPayHandler("MINGJIEFU")
public final class MingJieFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(MingJieFuPayRequestHandler.class);

	//参数名					参数含义					类型/长度			是否必填
	//version				版本号，固定值：V3.0.0.0		String/8			是
	//merchNo				商户号					String/16			是
	//netwayCode			支付网关代码，参考附录7.1		String/16			是
	//randomNum				随机数					String/8			是
	//orderNum				商户订单号，唯一				tring/32			是
	//amount				金额（单位：分）				String/16			是
	//goodsName				商品名称					String/20			是
	//callBackUrl			支付结果通知地址				String/128			是
	//callBackViewUrl		回显地址					String/128			是
	//charset				客户端系统编码格式，UTF-8、GBK	String/5			是
	//sign					签名（字母大写）				String/32			是
	private static final String version			="version";
	private static final String merchNo			="merchNo";
	private static final String netwayCode		  ="netwayCode";
	private static final String randomNum		  ="randomNum";
	private static final String orderNum		  ="orderNum";
	private static final String amount			="amount";
	private static final String goodsName		  ="goodsName";
	private static final String callBackUrl		  ="callBackUrl";
	private static final String callBackViewUrl	  ="callBackViewUrl";
	private static final String charset		  ="charset";

	//signature	数据签名	32	是	　
//	private static final String signature  ="sign";
	
	public static final String PAY_PUBLIC_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCZB0vtpqUIedv3Law11dmx7KAR2qNbfP0MblBNGeuymYmqGFIGNp5HHi7fhIviK7ttuDQsK7hHVxT44S9JZ+sEUgs1+Rl3s8rtkMbArnFN2hPLrFt1XMuAOeMEABfzX4iFoSX13on8vJPoBcfFYXsV2CRD0Vl54nL7E7+Ad0/M/wIDAQAB";
    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
            	put(version,"V2.0.0.0");
            	put(merchNo,channelWrapper.getAPI_MEMBERID());
            	put(netwayCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            	put(randomNum,HandlerUtil.getRandomStr(4));
            	put(orderNum,channelWrapper.getAPI_ORDER_ID());
            	put(amount,  channelWrapper.getAPI_AMOUNT());
            	put(goodsName,"name");
            	put(callBackUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            	put(callBackViewUrl,channelWrapper.getAPI_WEB_URL());
            	put(charset,"UTF-8");
            }
        };
        log.debug("[明捷付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
		String sort = YiDao2Util.mapToJson(api_response_params);
        String signMd5 = HandlerUtil.getMD5UpperCase(sort+channelWrapper.getAPI_KEY());
        log.debug("[明捷付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }
    
    @Override
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
    	payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
		JSONObject jsonObject = null;
    	try {
    		String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), "data=" + JsonUtil.stringify(payParam),MediaType.APPLICATION_FORM_URLENCODED_VALUE,"Keep-Alive",MediaType.APPLICATION_JSON_UTF8_VALUE.toString());
            if (StringUtils.isBlank(resultStr)) {
            	log.error("[明捷付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空"+",参数："+JSON.toJSONString(payParam));
            	throw new PayException("第三方返回异常:返回空"+",参数："+JSON.toJSONString(payParam));
            }
			jsonObject = JSONObject.parseObject(new String(resultStr.getBytes("ISO-8859-1"), "UTF-8"));
    	} catch (Exception e) {
    		log.error("[明捷付]-[请求支付]-3.2.发送支付请求，及获取支付请求出错，订单号：" + channelWrapper.getAPI_ORDER_ID() + ",通道名称：" + channelWrapper.getAPI_CHANNEL_BANK_NAME() + ",postUrl:" + channelWrapper.getAPI_CHANNEL_BANK_URL() + ",payForManagerCGIResultJsonObj" + e.getMessage(), e);
    		throw new PayException(e.getMessage(),e);
    	}          
        if (!jsonObject.containsKey("stateCode")  || !"00".equals(jsonObject.getString("stateCode"))) {
        	 log.error("[明捷付]-[请求支付]-3.3.发送支付请求，获取支付请求返回值异常:"+JSON.toJSONString(jsonObject));
             throw new PayException(JSON.toJSONString(jsonObject));
        }
        Map<String,String> result = Maps.newHashMap();
        //按不同的请求接口，向不同的属性设置值
//        result.put((handlerUtil.isWapOrApp(channelWrapper) || handlerUtil.isWebWyKjzf(channelWrapper)) ? JUMPURL : QRCONTEXT, jsonObject.getString("qrcodeUrl"));
        result.put(handlerUtil.isFS(channelWrapper) || handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, jsonObject.getString("qrcodeUrl"));
        result.put("第三方返回",jsonObject.toString()); //保存全部第三方信息，上面的拆开没必要
		List<Map<String, String>> payResultList = Lists.newArrayList();
		payResultList.add(result);
        log.debug("[明捷付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
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
        log.debug("[明捷付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }

}