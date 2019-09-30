package dc.pay.business.hefu;

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
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Mar 31, 2018
 */
@RequestPayHandler("HEFU")
public final class HeFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(HeFuPayRequestHandler.class);

	//下行异步通知地址		notifyUrl			String			N			N				下行异步通知的地址，需要以http://开头且没有任何参数
	//MD5签名				sign				String			N			N				32位大写MD5签名值
	//外部订单号			outOrderNo			String			N			Y				商户系统的订单编号
	//商品名称				goodsClauses			String			N			Y				商户系统的商品名称
	//交易金额				tradeAmount			double			N			Y				商户 商品价格（元）支持小数
	//商户code			code				String			N			N				点击头像，查看code
	//支付类型				payCode				String			N			N				请求的支付类型weixinpay,alipay,weixinh5
	//H5支付场景地址		returnUrl			String			Y			N				H5支付必传例如：http://dsdasda.dada.com
    //提单时间(格式yyyyMMddHHmmss)	orderDate
    //密钥	merchantKey
    private static final String notifyUrl	  ="notifyUrl";
    private static final String outOrderNo	  ="outOrderNo";
    private static final String goodsClauses  ="goodsClauses";
    private static final String tradeAmount	  ="tradeAmount";
    private static final String code	  ="code";
    private static final String payCode	  ="payCode";
    private static final String returnUrl	  ="returnUrl";
    private static final String orderDate	  ="orderDate";
    private static final String merchantKey	  ="merchantKey";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
            	put(orderDate, HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "yyyyMMddHHmmss"));
                put(code, channelWrapper.getAPI_MEMBERID());
                put(tradeAmount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(payCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(outOrderNo,channelWrapper.getAPI_ORDER_ID());
                put(goodsClauses,"name");
                if (HandlerUtil.isWapOrApp(channelWrapper)) {
                	put(returnUrl,channelWrapper.getAPI_WEB_URL());
				}
            }
        };
        log.debug("[合付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

	 protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
		//1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
		//2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
		StringBuffer signSrc= new StringBuffer();
		signSrc.append(goodsClauses+"=").append(api_response_params.get(goodsClauses)).append("&");
		signSrc.append(outOrderNo+"=").append(api_response_params.get(outOrderNo)).append("&");
		signSrc.append(tradeAmount+"=").append(api_response_params.get(tradeAmount)).append("&");
		signSrc.append("key=").append(channelWrapper.getAPI_KEY());
		String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[合付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }


//	 protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
//		 payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
//		 ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
//		 HashMap<String, String> result = Maps.newHashMap();
//		 if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
//			 result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
//		 }else{
//			 payParam.remove(merchantKey);
//			 String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
//			 if (StringUtils.isBlank(resultStr)) {
//				 log.error("[合付]3.1.发送支付请求，获取支付请求返回值异常:返回空"+",参数："+JSON.toJSONString(payParam));
//				 throw new PayException("返回空"+",参数："+JSON.toJSONString(payParam));
//			 }
//			 JSONObject resJson ;
//			 try {
//				 resJson = JSONObject.parseObject(resultStr);
//			 } catch (Exception e) {
//				 log.error("[合付]3.2.发送支付请求，及获取支付请求结果出错：", e);
//				 throw new PayException(e.getMessage(), e);
//			 }
//			 if (resJson!=null && resJson.containsKey("code") && !"0000".equals(resJson.getString("code"))) {
//				 log.error("[合付]3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//				 throw new PayException(resultStr);
//			 }
//			 result.put(QRCONTEXT, resJson.getString("qrUrl"));
//		 }
//		 payResultList.add(result);
//		 log.debug("[合付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
//		 return payResultList;
//	 }
	protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
		payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);

		payParam.remove(merchantKey);
		String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
		if (StringUtils.isBlank(resultStr)) {
			log.error("[合付]3.1.发送支付请求，获取支付请求返回值异常:返回空"+",参数："+JSON.toJSONString(payParam));
			throw new PayException("返回空"+",参数："+JSON.toJSONString(payParam));
		}
		JSONObject resJson ;
		try {
			resJson = JSONObject.parseObject(resultStr);
		} catch (Exception e) {
			log.error("[合付]3.2.发送支付请求，及获取支付请求结果出错：", e);
			throw new PayException(e.getMessage(), e);
		}
		if (resJson!=null && resJson.containsKey("payState") && !"success".equals(resJson.getString("payState"))) {
			log.error("[合付]3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
			throw new PayException(resultStr);
		}
		HashMap<String, String> result = Maps.newHashMap();
		result.put(handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, resJson.getString("url"));
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		payResultList.add(result);
		log.debug("[合付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
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
        log.debug("[合付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}