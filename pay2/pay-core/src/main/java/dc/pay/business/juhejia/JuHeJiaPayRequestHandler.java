package dc.pay.business.juhejia;

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
 * Jun 22, 2018
 */
@RequestPayHandler("JUHEJIA")
public final class JuHeJiaPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(JuHeJiaPayRequestHandler.class);

	//字段名			变量名				必填			类型				示例值	描述
	//商户ID			mchId				是			String(32)			10000000 分配的商户号
	//商户订单号		mchOrderNo			是			String(32)			20160427210604000490 商户生成的订单号
	//渠道ID			channelId			是			String(24)			YZPAY_ALIPAY_QR 见支付渠道参数
	//支付金额			amount				是			int 100				支付金额,单位分
	//支付结果回调URL	notifyUrl			是			String(200)			http://mock.jianasi.com 支付结果回调URL
	//商品主题			subject				是			String(64)			测试商品1 商品主题
	//商品描述信息		body				是			String(256)			测试商品描述 商品描述信息
	//客户端IP		clientIp			是			String(32)			114.114.114.114 支付用户的IP
	//扩展参数1		param1				否			String(64)			回调时会原样返回
	//扩展参数2		param2				否			String(64)			回调时会原样返回
	//签名			sign				是			String(32)			签名值，详见签名规则
	private static final String mchId						="mchId";
	private static final String mchOrderNo					="mchOrderNo";
	private static final String channelId					="channelId";
	private static final String amount						="amount";
	private static final String notifyUrl					="notifyUrl";
	private static final String subject						="subject";
	private static final String body						="body";
	private static final String clientIp					="clientIp";
//	private static final String param1						="param1";
//	private static final String param2						="param2";

	//signature	数据签名	32	是	　
//	private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(mchId, channelWrapper.getAPI_MEMBERID());
                put(mchOrderNo,channelWrapper.getAPI_ORDER_ID());
                put(channelId,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(amount,channelWrapper.getAPI_AMOUNT());
                put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(subject,"name");
                put(body,"name");
                put(clientIp,channelWrapper.getAPI_Client_IP());
            }
        };
        log.debug("[聚合家]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
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
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toUpperCase();
        log.debug("[聚合家]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

	protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
		payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
		HashMap<String, String> result = Maps.newHashMap();
		String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
		if (StringUtils.isBlank(resultStr)) {
			log.error("[聚合家]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(payParam));
			throw new PayException("返回空,参数："+JSON.toJSONString(payParam));
		}
		JSONObject resJson = JSONObject.parseObject(resultStr);
		if (!resJson.containsKey("resultCode") || !"SUCCESS".equals(resJson.getString("resultCode"))) {
			log.error("[聚合家]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
			throw new PayException(resultStr);
		}
		result.put(handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, resJson.getString("payUrl"));
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		payResultList.add(result);
		log.debug("[聚合家]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[聚合家]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}