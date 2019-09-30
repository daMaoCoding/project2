package dc.pay.business.xunfeifu;

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
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Mar 21, 2018
 */
@RequestPayHandler("XUNFEIFU")
public final class XunFeiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XunFeiFuPayRequestHandler.class);

	//商户号			merId			10			是		系统给定的商户号
	//交易订单号		outOrderId		40			是		订单号
	//支付类型			payWay			无			是		长度与传入固定参数有关	见附件1
	//交易金额			amount			12			是		0.01单位：元，个别测试最低金额为1元
	//异步回调			notifyUrl		300			是		请给出有效的地址 地址请使用url编码,
    //商品描述			tradeName		20			否		无可不填写
	private static final String merId			="merId";
	private static final String outOrderId		="outOrderId";
	private static final String payWay			="payWay";
	private static final String amount			="amount";
	private static final String notifyUrl		="notifyUrl";
	private static final String tradeName		="tradeName";

	private static final String key  ="key";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merId, channelWrapper.getAPI_MEMBERID());
                put(outOrderId,channelWrapper.getAPI_ORDER_ID());
                put(payWay,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(tradeName,  "name");
                put(amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            }
        };
        log.debug("[讯飞付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
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
        signSrc.append(key+"=" + channelWrapper.getAPI_KEY());
		String paramsStr = signSrc.toString();
        String pay_md5sign = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[讯飞付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }

	protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
		payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
		String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
		if (StringUtils.isBlank(resultStr)) {
			log.error("[讯飞付]3.1.发送支付请求，获取支付请求返回值异常:返回空"+",参数："+JSON.toJSONString(payParam));
			throw new PayException("返回空"+",参数："+JSON.toJSONString(payParam));
		}
		JSONObject resJson = JSONObject.parseObject(resultStr);
		if (!resJson.containsKey("payState") || !"success".equals(resJson.getString("payState")) || !resJson.containsKey("code") || !"0".equals(resJson.getString("code"))) {
			log.error("[讯飞付]3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
			throw new PayException(JSON.toJSONString(resJson));
		}
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		HashMap<String, String> result = Maps.newHashMap();
		result.put(HandlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, resJson.getString("payUrl"));
		payResultList.add(result);
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
        log.debug("[讯飞付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}