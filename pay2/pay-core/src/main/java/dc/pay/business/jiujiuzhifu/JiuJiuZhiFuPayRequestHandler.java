package dc.pay.business.jiujiuzhifu;

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
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Jul 25, 2018
 */
@RequestPayHandler("JIUJIUZHIFU")
public final class JiuJiuZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(JiuJiuZhiFuPayRequestHandler.class);


    private static final String cus_orderno		="cus_orderno";
    private static final String merchant_no		="merchant_no";
    private static final String msg				="msg";
    private static final String order_type		="order_type";
    private static final String trans_amount	="trans_amount";
    private static final String ip				="ip";
	private static final String key				="key";
	
    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merchant_no, channelWrapper.getAPI_MEMBERID());
                put(msg,channelWrapper.getAPI_ORDER_ID());
                put(trans_amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(ip,HandlerUtil.getRandomIp(channelWrapper));
                put(order_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(cus_orderno,channelWrapper.getAPI_ORDER_ID());
            }
        };
        log.debug("[99支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
    	//使用新的对象来接收数据
    	Map<String,String> map = new TreeMap<>();
    	map = api_response_params;
    	//1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
    	//2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
        List paramKeys = MapUtils.sortMapByKeyAsc(map);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
        	if (handlerUtil.isWebYlKjzf(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper)) {
        		if (StringUtils.isNotBlank(map.get(paramKeys.get(i)))) {
        			signSrc.append(paramKeys.get(i)).append("=").append(map.get(paramKeys.get(i))).append("&");
        		}
			}else {
				if (StringUtils.isNotBlank(map.get(paramKeys.get(i)))  && !ip.equals(paramKeys.get(i))) {
					signSrc.append(paramKeys.get(i)).append("=").append(map.get(paramKeys.get(i))).append("&");
				}
			}
        }
        signSrc.append(key+"=" + channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[99支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        TreeMap<String, String> map = new TreeMap<>();
        map.put(cus_orderno, payParam.get(cus_orderno));
        map.put(merchant_no, payParam.get(merchant_no));
        map.put(msg, payParam.get(msg));
        map.put(order_type, payParam.get(order_type));
        map.put(trans_amount, payParam.get(trans_amount));
        map.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        map.put(ip, payParam.get(ip));
        Map<String,String> result = Maps.newHashMap();
        if (handlerUtil.isWebYlKjzf(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper)) {
			result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),map).toString());
		}else{
			String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), map,"UTF-8");
			if (StringUtils.isBlank(resultStr)) {
                log.error("[99支付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
                throw new PayException("返回空,参数："+JSON.toJSONString(map));
			}
			JSONObject jsonObject = null;
			try {
				jsonObject = JSONObject.parseObject(resultStr);
			} catch (Exception e) {
				log.error("[99支付]-[请求支付]-3.2.发送支付请求，获取支付请求返回值异常:"+resultStr);
				throw new PayException(resultStr);
			}
			if (null == jsonObject || !jsonObject.containsKey("qrcode") || StringUtils.isBlank(jsonObject.getString("qrcode"))) {
				log.error("[99支付]-[请求支付]-3.3.发送支付请求，获取支付请求返回值异常:"+jsonObject);
				throw new PayException(jsonObject.toJSONString());
			}
			if (null == jsonObject || !jsonObject.containsKey("trans_amount") || StringUtils.isBlank(jsonObject.getString("trans_amount"))) {
				log.error("[99支付]-[请求支付]-3.4.发送支付请求，获取支付请求返回值异常:"+jsonObject);
				throw new PayException(jsonObject.toJSONString());
			}
			result.put(QRCONTEXT, jsonObject.getString("qrcode"));
			result.put("第三方返回",resultStr); //保存全部第三方信息，上面的拆开没必要
		}
		List<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[99支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
        return payResultList;
    }

    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (null != resultListMap && !resultListMap.isEmpty()) {
            if (resultListMap.size() == 1) {
                Map<String, String> resultMap = resultListMap.get(0);
                requestPayResult = buildResult(resultMap, channelWrapper, requestPayResult);
            }
            if (ValidateUtil.requestesultValdata(requestPayResult)) {
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            } else {
                throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[99支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}