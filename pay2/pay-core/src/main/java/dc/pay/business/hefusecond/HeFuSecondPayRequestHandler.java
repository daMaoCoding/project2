package dc.pay.business.hefusecond;

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
 * May 26, 2018
 */
@RequestPayHandler("HEFUSECOND")
public final class HeFuSecondPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(HeFuSecondPayRequestHandler.class);

	//notifyUrl          回调地址
	//outOrderNo         外部订单号
	//goodClauses        商品名称
	//tradeAmount        金额
	//payCode            支付方式    wx 微信| alipay 支付宝
	//orderDate          提单时间(格式yyyyMMddHHmmss)
	//code               商户编号
	//sign               签名
	private static final String notifyUrl			="notifyUrl";
	private static final String outOrderNo			="outOrderNo";
	private static final String goodClauses			="goodClauses";
	private static final String tradeAmount			="tradeAmount";
	private static final String payCode				="payCode";
	private static final String orderDate			="orderDate";
	private static final String code				="code";
	
	private static final String merchantKey			="merchantKey";

	//signature	数据签名	32	是	　
	private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
            	put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            	put(outOrderNo,channelWrapper.getAPI_ORDER_ID());
            	put(goodClauses,"name");
            	put(tradeAmount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            	put(payCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            	put(orderDate,HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "yyyyMMddHHmmss"));
                put(code, channelWrapper.getAPI_MEMBERID());
            }
        };
        log.debug("[合付2]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
    	api_response_params.put(merchantKey, channelWrapper.getAPI_KEY());
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!signature.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
        		signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
			}
        }
        String paramsStr = signSrc.toString();
        //去除最后一个&符
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr.substring(0,paramsStr.length()-1)).toLowerCase();
        log.debug("[合付2]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }

	protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
		payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
		payParam.remove(merchantKey);
		HashMap<String, String> result = Maps.newHashMap();
		String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
		if (StringUtils.isBlank(resultStr)) {
			log.error("[合付2]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空"+",参数："+JSON.toJSONString(payParam));
			throw new PayException("返回空"+",参数："+JSON.toJSONString(payParam));
		}
		JSONObject resJson = JSONObject.parseObject(resultStr);
		if (!resJson.containsKey("code") || !"0000".equals(resJson.getString("code"))) {
			log.error("[合付2]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
			throw new PayException(resultStr);
		}
		result.put(handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, resJson.getString("qrUrl"));
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		payResultList.add(result);
		log.debug("[合付2]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
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
        log.debug("[合付2]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}