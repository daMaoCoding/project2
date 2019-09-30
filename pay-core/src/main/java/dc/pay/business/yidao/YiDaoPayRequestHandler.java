package dc.pay.business.yidao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.codec.binary.Base64;
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
import dc.pay.utils.kspay.AESUtil;
import dc.pay.utils.kspay.BASEUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * May 1, 2018
 */
@RequestPayHandler("YIDAO")
public final class YiDaoPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(YiDaoPayRequestHandler.class);

	//输入项					输入项名称			属性			注释
	//merchantCode			商户号			M			必填，商户号
	//version				版本号			M			固定值 : 1.0.1
	//subject				商品标题			M			
	//amount				订单总金额			M			单位元
	//notifyUrl				回调通知地址		M			
	//orgOrderNo			机构订单号			M			
	//expireTime			有效期			M			分钟数1代表一分钟
	//source				订单付款方式		M			WXZF:微信；
	//tranTp				通道类型			M			固定值：0
	//sign					签名				M			转换为大写
//	private static final String merchantCode  ="merchantCode";
	private static final String version	  ="version";
	private static final String subject	  ="subject";
	private static final String amount	  ="amount";
	private static final String notifyUrl	  ="notifyUrl";
	private static final String orgOrderNo	  ="orgOrderNo";
//	private static final String expireTime	  ="expireTime";
	private static final String source	  ="source";
	//来用回传订单：第三方不能处理本字段 
	private static final String extra_para	  ="extra_para";
	private static final String tranTp	  ="tranTp";

	//signature	数据签名	32	是	　
//	private static final String signature  ="sign";
	
    @Override
    protected Map<String, String> buildPayParam() throws PayException {
		String api_KEY = channelWrapper.getAPI_KEY();
    	if (StringUtils.isBlank(api_KEY) || !api_KEY.contains("-")) {
            log.error("[易到]-[请求支付]-1.1.组装请求参数私钥填写格式：商户密钥-md5私钥" );
            throw new PayException("[易到]-[请求支付]-1.1.组装请求参数私钥填写格式：商户密钥-md5私钥" );
		}
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
            	put(version,"1.0.1");
            	put(orgOrderNo,channelWrapper.getAPI_ORDER_ID());
            	put(subject,"name");
            	put(amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            	put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
//            	put(expireTime,"30");
            	put(extra_para,channelWrapper.getAPI_ORDER_ID());
            	put(source,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            	put(tranTp,"0");
            }
        };
        log.debug("[易到]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
		String sort = null;
		try {
			sort = YiDao2Util.sort(api_response_params);
		} catch (Exception e) {
			e.printStackTrace();
    		log.error("[易到]-[请求支付]-2.1.发送支付请求，及获取支付请求出错，订单号：" + channelWrapper.getAPI_ORDER_ID() + ",通道名称：" + channelWrapper.getAPI_CHANNEL_BANK_NAME() + ",postUrl:" + channelWrapper.getAPI_CHANNEL_BANK_URL() + ",payForManagerCGIResultJsonObj" + e.getMessage(), e);
    		throw new PayException(e.getMessage(),e);
		}
		String sortPrivage = Base64.encodeBase64String(sort.getBytes());
		String aesPrivage = AESUtil.encrypt(sortPrivage, channelWrapper.getAPI_KEY().split("-")[0]);
		String signMd5 = HandlerUtil.getMD5UpperCase(aesPrivage+channelWrapper.getAPI_KEY().split("-")[1]).toUpperCase();//以md5加密 加上TradeCode转换大写
        log.debug("[易到]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
    	payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
		JSONObject jsonObject = null;
    	try {
    		String Keyprivage = BASEUtil.encode(YiDao2Util.sort(payParam));
    		Map<String,String> reqMap = new HashMap<String,String>();
    		reqMap.put("merchantCode", channelWrapper.getAPI_MEMBERID());
    		reqMap.put("transData",  AESUtil.encrypt(Keyprivage, channelWrapper.getAPI_KEY().split("-")[0]));
    		String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), "reqJson="+JsonUtil.stringify(reqMap),MediaType.APPLICATION_FORM_URLENCODED_VALUE,"Keep-Alive",MediaType.APPLICATION_JSON_UTF8_VALUE.toString());
            if (StringUtils.isBlank(resultStr)) {
                log.error("[易到]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
//            	log.error("[易到]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空"+",参数："+JSON.toJSONString(payParam));
//            	throw new PayException("第三方返回异常:返回空"+",参数："+JSON.toJSONString(payParam));
            }
			jsonObject = JSONObject.parseObject(resultStr);
    	} catch (Exception e) {
    		log.error("[易到]-[请求支付]-3.2.发送支付请求，及获取支付请求出错，订单号：" + channelWrapper.getAPI_ORDER_ID() + ",通道名称：" + channelWrapper.getAPI_CHANNEL_BANK_NAME() + ",postUrl:" + channelWrapper.getAPI_CHANNEL_BANK_URL() + ",payForManagerCGIResultJsonObj" + e.getMessage(), e);
    		throw new PayException(e.getMessage(),e);
    	}          
    	//{"responseMessage":"SUCCESS","responseCode":"200","responseObj":{"sign":"98EF94C16EFCB440B996B1709ECECCE5","amount":"5.00","respCode":"0000","orgOrderNo":"YIDAO_WEBWAPAPP_QQ_SM-V1XoL","tradeNo":"HGDG9911566216504279042","subject":"name","qrCode":"https://qpay.qq.com/qr/6cd3fbb0","respMsg":"下单成功","txnTime":"20180501112528"}}
        if (!jsonObject.containsKey("responseCode")  || !"200".equals(jsonObject.getString("responseCode"))) {
            log.error("[易到]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(jsonObject) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
             throw new PayException(JSON.toJSONString(jsonObject));
        }
        jsonObject = JSONObject.parseObject(jsonObject.getString("responseObj"));
        Map<String,String> result = Maps.newHashMap();
        //按不同的请求接口，向不同的属性设置值
        result.put((handlerUtil.isWapOrApp(channelWrapper) || handlerUtil.isWebWyKjzf(channelWrapper)) ? JUMPURL : QRCONTEXT, jsonObject.getString("qrCode"));
        result.put("第三方返回",jsonObject.toString()); //保存全部第三方信息，上面的拆开没必要
		List<Map<String, String>> payResultList = Lists.newArrayList();
		payResultList.add(result);
        log.debug("[易到]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
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
        log.debug("[易到]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }

}