package dc.pay.business.gaotong;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

/**
 * ************************
 *
 * @author tony 3556239829
 */

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
import dc.pay.utils.qr.QRCodeUtil;

@RequestPayHandler("GAOTONG")
public final class GaoTongPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(GaoTongPayRequestHandler.class);
    private static final String partner = "partner";
    private static final String banktype = "banktype";
    private static final String paymoney = "paymoney";
    private static final String ordernumber = "ordernumber";
    private static final String callbackurl = "callbackurl";
//    private static final String orderstatus = "orderstatus";
//    private static final String sysnumber = "sysnumber";
//    private static final String attach = "attach";
//    private static final String sign = "sign";
//    private static final String RESPCODE = "respCode";
//    private static final String MESSAGE = "message";
//    private static final String BARCODE = "barCode";
    
    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newTreeMap();
        payParam.put(partner, channelWrapper.getAPI_MEMBERID());
        payParam.put(banktype, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
        payParam.put(paymoney, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
        payParam.put(ordernumber, channelWrapper.getAPI_ORDER_ID());
        payParam.put(callbackurl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
        log.debug("[高通]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }
    protected String buildPaySign(Map payParam) throws PayException {
        String pay_md5sign = null;
        String paramsStr = String.format("partner=%s&banktype=%s&paymoney=%s&ordernumber=%s&callbackurl=%s%s",
                payParam.get(partner),
                payParam.get(banktype),
                payParam.get(paymoney),
                payParam.get(ordernumber),
                payParam.get(callbackurl),
                channelWrapper.getAPI_KEY());
        pay_md5sign = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[高通]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }
    
    
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
		payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		HashMap<String, String> result = Maps.newHashMap();
		if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
			result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
		}else{
			String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.GET).trim();
			if (StringUtils.isBlank(resultStr)) {
				log.error("[高通]3.1.发送支付请求，获取支付请求返回值异常:返回空"+",参数："+JSON.toJSONString(payParam));
				throw new PayException("返回空"+",参数："+JSON.toJSONString(payParam));
			}
			if (!resultStr.contains("img")) {
				log.error("[高通]3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
				throw new PayException(resultStr);
			}
			Document document = Jsoup.parse(resultStr);  //Jsoup.parseBodyFragment(html)
			Element bodyEl = document.getElementsByTag("body").first();
			Elements imgs = bodyEl.getElementsByTag("img");
			if (1 != imgs.size()) {
				log.error("[高通]3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
				throw new PayException(resultStr);
			}
			try {
				result.put(QRCONTEXT, QRCodeUtil.decodeByUrl(HandlerUtil.getDomain(channelWrapper.getAPI_CHANNEL_BANK_URL())+imgs.get(0).attr("src")));
			} catch (Exception e) {
				log.error("[高通]3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
				throw new PayException(e.getMessage());
			}
		}
		payResultList.add(result);
		log.debug("[高通]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
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
        log.debug("[高通]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}