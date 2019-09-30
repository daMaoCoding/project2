package dc.pay.business.xinbei;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import com.alibaba.fastjson.JSON;
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
 * Apr 17, 2018
 */
@RequestPayHandler("XINBEI")
public final class XinBeiPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XinBeiPayRequestHandler.class);

    //参数				参数名称				长度		参数说明									是否为空
    //Version			网关版本号				4		使用网关的版本号								否
    //MerchantCode		商户编码				6		新贝平台商户编码								否
    //OrderId			商户订单号				30		商户自己业务逻辑的订单号						否
    //Amount			交易金额				7		交易流程中发生的金额							否
    //AsyNotifyUrl		异步通知地址			256		业务完成时异步回发通知的地址						否
    //SynNotifyUrl		同步通知地址			256		业务完成时同步回发通知的地址						否
    //OrderDate			订单交易时间			14		商户产生订单时的交易时间,格式如：20130102030405	否
    //TradeIp			交易IP				19		发起交易的客户IP地址							否
    //PayCode			交易类型编码			6		接入平台时对应的接口编码						否
    //SignValue			加密字符串				32		根据接口文档组合参数加密后的字段，加密方式为MD5大写		否
    private static final String Version			 = "Version";
    private static final String MerchantCode	 = "MerchantCode";
    private static final String OrderId			 = "OrderId";
    private static final String Amount			 = "Amount";
    private static final String AsyNotifyUrl	 = "AsyNotifyUrl";
    private static final String SynNotifyUrl	 = "SynNotifyUrl";
    private static final String OrderDate		 = "OrderDate";
    private static final String TradeIp			 = "TradeIp";
    private static final String PayCode			 = "PayCode";
    private static final String TokenKey		 = "TokenKey";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
            	put(Version,"V1.0");
            	put(MerchantCode, channelWrapper.getAPI_MEMBERID());
            	put(OrderId,channelWrapper.getAPI_ORDER_ID());
            	put(Amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            	put(AsyNotifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            	put(SynNotifyUrl,channelWrapper.getAPI_WEB_URL());
            	put(OrderDate,HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "yyyyMMddHHmmss"));
            	put(TradeIp,channelWrapper.getAPI_WEB_URL());
            	put(PayCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            }
        };
        log.debug("[新贝]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map api_response_params) throws PayException {
		//1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
		//2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
		StringBuffer signSrc= new StringBuffer();
		signSrc.append(Version+"=").append("["+api_response_params.get(Version)+"]");
		signSrc.append(MerchantCode+"=").append("["+api_response_params.get(MerchantCode)+"]");
		signSrc.append(OrderId+"=").append("["+api_response_params.get(OrderId)+"]");
		signSrc.append(Amount+"=").append("["+api_response_params.get(Amount)+"]");
		signSrc.append(AsyNotifyUrl+"=").append("["+api_response_params.get(AsyNotifyUrl)+"]");
		signSrc.append(SynNotifyUrl+"=").append("["+api_response_params.get(SynNotifyUrl)+"]");
		signSrc.append(OrderDate+"=").append("["+api_response_params.get(OrderDate)+"]");
		signSrc.append(TradeIp+"=").append("["+api_response_params.get(TradeIp)+"]");
		signSrc.append(PayCode+"=").append("["+api_response_params.get(PayCode)+"]");
		signSrc.append(TokenKey+"=").append("["+channelWrapper.getAPI_KEY()+"]");
		String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[新贝]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }

	protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
		payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
		HashMap<String, String> result = Maps.newHashMap();
		if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
			result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
		}else{
			String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
			if (StringUtils.isBlank(resultStr)) {
				log.error("[新贝]3.1.发送支付请求，获取支付请求返回值异常:返回空"+",参数："+JSON.toJSONString(payParam));
				throw new PayException("返回空"+",参数："+JSON.toJSONString(payParam));
			}
			if (!resultStr.contains("qrCodeUrl")) {
				log.error("[新贝]3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
				throw new PayException(resultStr);
			}
			result.put(QRCONTEXT, Jsoup.parse(resultStr).select("[id=qrCodeUrl]").first().val());
		}
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		payResultList.add(result);
		log.debug("[新贝]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
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
        log.debug("[新贝]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}