package dc.pay.business.huifushidai;

import java.util.ArrayList;
import java.util.HashMap;
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
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * May 28, 2018
 */
@RequestPayHandler("HUIFUSHIDAI")
public final class HuiFuShiDaiPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(HuiFuShiDaiPayRequestHandler.class);

	//参数名称				参数描述					说明							是否必录			是否签名
	//userId			商户号					平台提供给商户的ID					是				是
	//orderNo			商户交易号					在商户系统中保持唯一					是				是
	//tradeType			交易类型					01：微信扫码						是				是
	//payAmt			支付金额					单位：元							是				是
	//bankId			银行id					交易类型41的网银支付					否				否
	//goodsName			商品名称					商品名称							是				否
	//returnUrl			同步通知地址				成功后网页跳转地址					是				是
	//notifyUrl			通知状态异步回调接收地址		异步通知地址，						是				是
	//sign				签名字符串					MD5签名结果：按照参数名ASCII码从小到大	是				否
	private static final String userId			="userId";
	private static final String orderNo			="orderNo";
	private static final String tradeType		="tradeType";
	private static final String payAmt			="payAmt";
	private static final String bankId			="bankId";
	private static final String goodsName		="goodsName";
	private static final String returnUrl		="returnUrl";
	private static final String notifyUrl		="notifyUrl";

	//signature	数据签名	32	是	　
	private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(userId, channelWrapper.getAPI_MEMBERID());
                put(orderNo,channelWrapper.getAPI_ORDER_ID());
                put(payAmt,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                if (handlerUtil.isWY(channelWrapper)) {
                	put(bankId,handlerUtil.isWebWyKjzf(channelWrapper) ? "" : channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                	put(tradeType,"41");
                }else if (handlerUtil.isWebYlKjzf(channelWrapper)) {
                	put(tradeType,handlerUtil.isOrderFromWapOrApp(channelWrapper.getAPI_ORDER_FROM()) ? "51" : "61");
				}else {
					put(tradeType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
				}
                put(goodsName,"name");
                put(returnUrl,channelWrapper.getAPI_WEB_URL());
                put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            }
        };
        log.debug("[汇付时代]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

	 protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
		//1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
		//2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
		StringBuffer signSrc= new StringBuffer();
		signSrc.append(notifyUrl+"=").append(api_response_params.get(notifyUrl)).append("&");
		signSrc.append(orderNo+"=").append(api_response_params.get(orderNo)).append("&");
		signSrc.append(payAmt+"=").append(api_response_params.get(payAmt)).append("&");
		signSrc.append(returnUrl+"=").append(api_response_params.get(returnUrl)).append("&");
		signSrc.append(tradeType+"=").append(api_response_params.get(tradeType)).append("&");
		signSrc.append(userId+"=").append(api_response_params.get(userId)).append("&");
		signSrc.append("key=").append(channelWrapper.getAPI_KEY());
		String paramsStr = signSrc.toString();
		String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[汇付时代]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }

	protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
		payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
		String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
		if (StringUtils.isBlank(resultStr)) {
			log.error("[汇付时代]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空"+",参数："+JSON.toJSONString(payParam));
			throw new PayException("返回空"+",参数："+JSON.toJSONString(payParam));
		}
		JSONObject resJson = JSONObject.parseObject(resultStr);
		if (!resJson.containsKey("retCode") || !"0".equals(resJson.getString("retCode"))) {
			log.error("[汇付时代]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
			throw new PayException(resultStr);
		}
		HashMap<String, String> result = Maps.newHashMap();
		result.put((channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("_WEBWAPAPP_") && !handlerUtil.isWebYlKjzf(channelWrapper)) ? QRCONTEXT : HTMLCONTEXT , resJson.getString("payUrl"));
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		payResultList.add(result);
		log.debug("[汇付时代]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
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
        log.debug("[汇付时代]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}