package dc.pay.business.kuaizhifu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

/**
 * ************************
 * @author tony 3556239829
 */

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
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 
 * @author andrew
 * Feb 5, 2018
 */
@RequestPayHandler("KUAIZHIFU")
public final class KuaiZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(KuaiZhiFuPayRequestHandler.class);
    private static final String MemberID = "MemberID";
    private static final String PayType = "PayType";  //微信，支付宝...
    private static final String OrderMoney = "OrderMoney"; //元，大于等于1
    private static final String TransID = "TransID"; //流水号
    private static final String TradeDate = "TradeDate"; //20140101010101
    private static final String ReturnUrl = "ReturnUrl";  //回掉地址
    private static final String ResultType = "ResultType";  //回掉地址

    private static final String PageUrl = "PageUrl"; //“空”
    private static final String TerminalID = "TerminalID";  //10066008
    private static final String InterfaceVersion = "InterfaceVersion";  //4.0
    private static final String KeyType = "KeyType";  //1
    private static final String bankId = "bankId";  //“空”
    private static final String PayID = "PayID";  //“空”
    private static final String NoticeType = "NoticeType";  //0
    private static final String Amount = "Amount";  //0

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(MemberID, channelWrapper.getAPI_MEMBERID());
                if (channelWrapper.getAPI_CHANNEL_BANK_NAME().contains(PayEumeration.CHANNEL_TYPE._WY_+"") && !channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("KJZF")) {
                	put(PayType, "ONLINE_BANK_PAY");
                	put(bankId, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
				}else {
					put(PayType, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
				}
                put(OrderMoney, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(TransID, channelWrapper.getAPI_ORDER_ID());
                put(ReturnUrl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());  //后台通知
                put(PageUrl, HandlerUtil.getDomain(channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL()));  //前台通知
                put(TradeDate, DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
                put(TerminalID, "10066008");
                put(InterfaceVersion, "4.0");
                put(KeyType, "1");
                put(PayID, "");
                put(NoticeType, "0");
                put(Amount, "1");
                put(ResultType, "JSON");
            }
        };
        log.debug("[快支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map payParam) throws PayException {
        String MARK = "~|~";
        String md5Str =   payParam.get(MemberID) + MARK + MARK+  payParam.get(TradeDate) + MARK +  payParam.get(TransID)  +MARK+  payParam.get(OrderMoney) + MARK +  payParam.get(PageUrl) + MARK +  payParam.get(ReturnUrl) + MARK +  payParam.get(NoticeType) + MARK+ channelWrapper.getAPI_KEY();
        String pay_md5sign = KuaiZhiFuUtils.getMD5ofStr(md5Str);//计算MD5值
        pay_md5sign = HandlerUtil.getMD5UpperCase(md5Str).toLowerCase();
        log.debug("[快支付]-[请求支付]-2.生成加密URL签名参数：" + md5Str);
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        String channel_flag = channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG();
        HashMap<String, String> result = Maps.newHashMap();
		String firstPayresult = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
		if (firstPayresult.length() < 10 || firstPayresult.contains("UNKNOWN_ERROR")) {
			log.error("[快支付]3.1发送支付请求，及获取支付请求结果：" + firstPayresult + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channel_flag);
			throw new PayException(firstPayresult);
		}
		JSONObject parseObject;
		try {
			parseObject = JSON.parseObject(firstPayresult);
		} catch (Exception e) {
			log.error("[快支付]3.2发送支付请求，及获取支付请求结果出错：", e);
			throw new PayException(firstPayresult);
		}
		String qrContent = parseObject.getString("content");
		if(StringUtils.isBlank(qrContent) || !qrContent.contains("//")){
			log.error("[快支付]3.3发送支付请求，及获取支付请求结果：" + firstPayresult + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channel_flag);
			throw new PayException(firstPayresult);
		}
		if (HandlerUtil.isWY(channelWrapper) ||HandlerUtil.isYLKJ(channelWrapper) ||HandlerUtil.isWapOrApp(channelWrapper) ) {
			result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(qrContent, new HashMap<>()).toString());
		}else if (HandlerUtil.isWapOrApp(channelWrapper)) {
			result.put(JUMPURL, qrContent);
		}else {
			result.put(QRCONTEXT, qrContent);
		}
		result.put(PARSEHTML, firstPayresult);
		payResultList.add(result);
		log.debug("[快支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
        return payResultList;
    }

    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (null != resultListMap && !resultListMap.isEmpty()) {
            if (resultListMap.size() == 1) {
                Map<String, String> resultMap = resultListMap.get(0);
                requestPayResult = buildResult(resultMap,channelWrapper,requestPayResult);
            }
            if (ValidateUtil.requestesultValdata(requestPayResult)) {
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            } else {
                throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[快支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}