package dc.pay.business.kuyou;

import java.util.ArrayList;
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
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Jun 7, 2018
 */
@RequestPayHandler("KUYOU")
public final class KuYouPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(KuYouPayRequestHandler.class);

	//参数名称						参数含义			是否必填			参数长度			参数说明													签名顺序
    //bankCode					银行编码			否				String(36)			详见附录4.4银行通道编码	 如果使用网银支付，该参数为必传！				1
	//memberGoods				商品信息			是				String(36)			必须使用商户订单号！！！										4
	//noticeSysaddress			异步通知地址		是				String(128)			支付订单处理完成后我方系统会向该地址发送支付结果通知						5
	//productNo					产品编码			是				String(36)			该字段请参考附录4.1：支付方式编码列表								7
	//requestAmount				订单金额			是				Double				单位:元，精确到分。订单金额最大金额10000。最小值0。例如10元=10.00		8
	//trxMerchantNo				商户编号			是				String(36)			商户在系统的唯一身份标识。获取方式见“如何获得商户编号”					9
	//trxMerchantOrderno		商户订单号			是				String(36)			提交的订单号必须在自身账户交易中唯一，只能传递数字和字母，禁止传递特殊字符		10
	//extend					扩展信息			否				JSON				详见2.2.3扩展信息参数说明	-
	//hmac						签名数据			是				按键ascii值升序，参数为空的不参与加密，最后附上&key=密钥做32位md5小写加密，示例：
    private static final String bankCode				="bankCode";
	private static final String memberGoods				="memberGoods";
	private static final String noticeSysaddress		="noticeSysaddress";
	private static final String productNo				="productNo";
	private static final String requestAmount			="requestAmount";
	private static final String trxMerchantNo			="trxMerchantNo";
	private static final String trxMerchantOrderno		="trxMerchantOrderno";
	private static final String extend					="extend";

	//signature	数据签名	32	是	　
//	private static final String signature  ="hmac";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
            	put(memberGoods,channelWrapper.getAPI_ORDER_ID());
            	put(noticeSysaddress,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            	put(requestAmount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            	put(trxMerchantNo, channelWrapper.getAPI_MEMBERID());
            	put(trxMerchantOrderno,channelWrapper.getAPI_ORDER_ID());
            	if (handlerUtil.isWY(channelWrapper)) {
            		put(bankCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            		put(productNo,"EBANK-JS");
				}else {
					put(productNo,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
				}
            	if (handlerUtil.isWebYlKjzf(channelWrapper)) {
            		put(extend,"{\"cerdNo\":\""+handlerUtil.getRandomStr(8)+"\"}");
				}
            }
        };
        log.debug("[酷游]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

	 protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!extend.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
        		signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
			}
        }
        signSrc.append("key=" + channelWrapper.getAPI_KEY());
		String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[酷游]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }

	protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
		payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
		HashMap<String, String> result = Maps.newHashMap();
		String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
		if (StringUtils.isBlank(resultStr)) {
			log.error("[酷游]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
			throw new PayException(resultStr);
		}
        if (!resultStr.contains("{") || !resultStr.contains("}")) {
            log.error("[酷游]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
         }
		JSONObject resJson = JSONObject.parseObject(resultStr);
		if (!resJson.containsKey("code") || !"00000".equals(resJson.getString("code"))) {
			log.error("[酷游]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
			throw new PayException(resultStr);
		}
		result.put((handlerUtil.isWapOrApp(channelWrapper) || handlerUtil.isFS(channelWrapper)) ? JUMPURL : QRCONTEXT, resJson.getString("payUrl"));
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		payResultList.add(result);
		log.debug("[酷游]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
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
        log.debug("[酷游]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}