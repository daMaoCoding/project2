package dc.pay.business.huitong;

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
 * Jun 13, 2018
 */
@RequestPayHandler("HUITONG")
public final class HuiTongPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(HuiTongPayRequestHandler.class);

	//参数名称					参数变量名				类型					必填			说明
	//服务器异步通知地址			notify_url			String(200)			是			支付成功后，支付平台会主动通知商家系统，因此商家必须指定接收支付网关商户开发指南通知的地址，例如：http://www.merchant.cn/notify.jsp注意：
	//支付方式					pay_type			String(2)			是			1 为网银支付
	//银行编码					bank_code			String(16)			是			参见附录中的银行编码对照表
	//商户号					merchant_code		String(8)			是			商户注册签约后，支付平台分配的唯一标识号
	//商户订单号				order_no			String(32)			是			由商户系统生成的唯一订单编号，最大长度为32 位
	//商户订单总金额				order_amount		Number(9,2)			是			订单总金额以元为单位，精确到小数点后两位
	//商户订单时间				order_time			Date				是			字符串格式要求为：yyyy-MM-dd HH:mm:ss例如：2015-01-01 12:45:52
	//来路域名					req_referer			String(200)			是
	//消费者IP				customer_ip			String(15)			是
	//签名					sign				String				是			签名数据，签名规则见附录
	private static final String notify_url			="notify_url";
	private static final String pay_type			="pay_type";
//	private static final String bank_code			="bank_code";
	private static final String merchant_code		="merchant_code";
	private static final String order_no			="order_no";
	private static final String order_amount		="order_amount";
	private static final String order_time			="order_time";
	private static final String req_referer			="req_referer";
	private static final String customer_ip			="customer_ip";

	//signature	数据签名	32	是	　
//	private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
            	put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            	put(pay_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(merchant_code, channelWrapper.getAPI_MEMBERID());
                put(order_no,channelWrapper.getAPI_ORDER_ID());
                put(order_amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(order_time,  HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "yyyy-MM-dd HH:mm:ss"));
                put(req_referer,"name");
                put(customer_ip,handlerUtil.getRandomIp(channelWrapper));
            }
        };
        log.debug("[汇通]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
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
        log.debug("[汇通]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }

	protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
		payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
		HashMap<String, String> result = Maps.newHashMap();
		String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
		if (StringUtils.isBlank(resultStr)) {
			log.error("[汇通]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空"+",参数："+JSON.toJSONString(payParam));
			throw new PayException("返回空"+",参数："+JSON.toJSONString(payParam));
		}
		if (!resultStr.contains("\"flag\":\"00\"")) {
			log.error("[汇通]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
			throw new PayException(resultStr);
		}
		JSONObject jsonObject = JSONObject.parseObject(resultStr);
		result.put(handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT,jsonObject.getString("qrCodeUrl"));
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		payResultList.add(result);
		log.debug("[汇通]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
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
        log.debug("[汇通]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}