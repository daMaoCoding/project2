package dc.pay.business.boshi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * May 16, 2018
 */
@RequestPayHandler("BOSHI")
public final class BoShiPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(BoShiPayRequestHandler.class);

	//参数名				可空			说明
	//MerchantCode		否			商户id,由博士商户系统分配
	//KJ				是			默认为0，1:快捷支付，2:手机网银
	//BankCode			否			支付类型或银行类型，具体请参考附录
	//Amount			否			单位元（人民币） ，2位小数
	//OrderId			否			商户系统订单号
	//NotifyUrl			否			异步通知过程的返回地址
	//OrderDate			否			请求时间，时间戳，长度10位，例如：1300088888
	//Remark			否			备注信息，会原样返回
	//Sign				否			32位大写MD5签名值
	private static final String MerchantCode	="MerchantCode";
	private static final String KJ				="KJ";
	private static final String BankCode		="BankCode";
	private static final String Amount			="Amount";
	private static final String OrderId			="OrderId";
	private static final String NotifyUrl		="NotifyUrl";
	private static final String OrderDate		="OrderDate";
	private static final String Remark			="Remark";
	
	private static final String TokenKey		="TokenKey";

	//signature	数据签名	32	是	　
	private static final String signature  ="Sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(MerchantCode, channelWrapper.getAPI_MEMBERID());
                //				是			默认为0，1:快捷支付，2:手机网银
                put(KJ,handlerUtil.isYLKJ(channelWrapper) ? "1" : "0");
                put(BankCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(Amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(OrderId,channelWrapper.getAPI_ORDER_ID());
                put(NotifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(OrderDate,System.currentTimeMillis()+"");
                put(Remark,"remark");
            }
        };
        log.debug("[博士]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map api_response_params) throws PayException {
		//1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
		//2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
		StringBuffer signSrc= new StringBuffer();
		signSrc.append(MerchantCode+"=").append("["+api_response_params.get(MerchantCode)+"]");
		signSrc.append(OrderId+"=").append("["+api_response_params.get(OrderId)+"]");
		signSrc.append(Amount+"=").append("["+api_response_params.get(Amount)+"]");
		signSrc.append(NotifyUrl+"=").append("["+api_response_params.get(NotifyUrl)+"]");
		signSrc.append(OrderDate+"=").append("["+api_response_params.get(OrderDate)+"]");
		signSrc.append(BankCode+"=").append("["+api_response_params.get(BankCode)+"]");
		signSrc.append(TokenKey+"=").append("["+channelWrapper.getAPI_KEY()+"]");
		String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[博士]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }

	protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
		payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
		HashMap<String, String> result = Maps.newHashMap();
		if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
			result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
		}else{
			String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
			if (StringUtils.isBlank(resultStr)) {
				log.error("[博士]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空"+",参数："+JSON.toJSONString(payParam));
				throw new PayException("返回空"+",参数："+JSON.toJSONString(payParam));
			}
			if (channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("BOSHI_BANK_WEBWAPAPP_JD_SM")) {
			    result.put(HTMLCONTEXT, resultStr);
            }else {
                if (!resultStr.contains("form")) {
                    log.error("[博士]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
                Elements elements = Jsoup.parse(resultStr).select("[name=url]");
                if (null == elements) {
                    log.error("[博士]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
                result.put(QRCONTEXT, elements.first().val());
            }
		}
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		payResultList.add(result);
		log.debug("[博士]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
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
        log.debug("[博士]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}