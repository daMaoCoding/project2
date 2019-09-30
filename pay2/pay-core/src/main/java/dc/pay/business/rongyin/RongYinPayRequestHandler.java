package dc.pay.business.rongyin;

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
import dc.pay.utils.UnicodeUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Jun 21, 2018
 */
@RequestPayHandler("RONGYIN")
public final class RongYinPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(RongYinPayRequestHandler.class);

	//参数名				参数					类型			可空			说明
	//下行异步通知地址		notifyUrl			String			N			下行异步通知的地址，需要以http://开头且没有任何参数
	//外部订单号			outOrderNo			String			N			商户系统的订单编号
	//商品名称				goodsClauses		String			N			商户系统的商品名称
	//交易金额				tradeAmount			double			N			商户 商品价格（元）支持小数
	//商户code			code				String			N			点击头像，查看code
	//支付类型				payCode				String			N			alipay/wxpay
	//MD5签名				sign				String			N			32位MD5签名值
	private static final String notifyUrl				="notifyUrl";
	private static final String outOrderNo				="outOrderNo";
	private static final String goodsClauses			="goodsClauses";
	private static final String tradeAmount				="tradeAmount";
	private static final String code					="code";
	private static final String payCode					="payCode";

	//signature	数据签名	32	是	　
//	private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
            	put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            	put(outOrderNo,channelWrapper.getAPI_ORDER_ID());
            	put(goodsClauses,"name");
            	put(tradeAmount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            	put(code, channelWrapper.getAPI_MEMBERID());
            	put(payCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            }
        };
        log.debug("[蓉银]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

	 protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
        		signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
			}
        }
        signSrc.append("key=" + channelWrapper.getAPI_KEY());
		String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[蓉银]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

	protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
		payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
		HashMap<String, String> result = Maps.newHashMap();
		String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
		//String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
		if (StringUtils.isBlank(resultStr)) {
			log.error("[蓉银]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(payParam));
			throw new PayException("返回空,参数："+JSON.toJSONString(payParam));
		}
		JSONObject resJson = JSONObject.parseObject(resultStr);
		if (!resJson.containsKey("payState") || !"success".equals(resJson.getString("payState"))) {
			log.error("[蓉银]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(UnicodeUtil.unicodeToString(resultStr)) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
			throw new PayException(UnicodeUtil.unicodeToString(resultStr));
		}
		result.put(QRCONTEXT, resJson.getString("url"));
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		payResultList.add(result);
		log.debug("[蓉银]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[蓉银]-[请求支付]-4.处理请求响应成功：{}" ,JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}