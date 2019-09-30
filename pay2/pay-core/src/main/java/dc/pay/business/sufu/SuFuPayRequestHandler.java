package dc.pay.business.sufu;

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
 * May 11, 2018
 */
@RequestPayHandler("SUFU")
public final class SuFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(SuFuPayRequestHandler.class);

	//参数名称			参数变量名				类型					必填			说明
	//异步通知地址		notify_url			String(200)			是			支付成功后，支付平台会主动通知页 面 同 步 跳 转通知地址	
	//支付方式			pay_type			String(2)			是			
	//银行编码			bank_code			String(16)			是			参见附录中的银行编码对照表
	//商户号			merchant_code		String(8)			是			商户注册签约后，支付平台分配的唯一标识号
	//商户订单号		order_no			String(32)			是			由商户系统生成的唯一订单编号，最大长度为 32 位
	//商户订单总金额		order_amount		String				是			订单总金额以元为单位，精确到小数点后两位
	//商户订单时间		order_time			String				是			字符串格式要求为：yyyy-MM-dd HH:mm:ss例如：2015-01-01 12:45:52
	//来路域名			req_referer			String(200)			是			
	//消费者 IP		customer_ip			String(15)			是			如果商户支付请求时传递了该参
	//签名			sign				String				是			签名数据，签名规则见附录
	private static final String notify_url		="notify_url";
	private static final String pay_type		="pay_type";
	private static final String bank_code		="bank_code";
	private static final String merchant_code	="merchant_code";
	private static final String order_no		="order_no";
	private static final String order_amount	="order_amount";
	private static final String order_time		="order_time";
	private static final String req_referer		="req_referer";
	private static final String customer_ip		="customer_ip";
	
	//signature	数据签名	32	是	　
	private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
            	put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            	put(pay_type,handlerUtil.isWY(channelWrapper) ? "1" : channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            	if (handlerUtil.isWY(channelWrapper)) {
            		put(bank_code, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
				}
            	put(merchant_code, channelWrapper.getAPI_MEMBERID());
            	put(order_no,channelWrapper.getAPI_ORDER_ID());
            	put(order_amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            	put(order_time,  HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "yyyy-MM-dd HH:mm:ss"));
            	//随便填写	不为 空就行
            	put(req_referer,"name");
            	put(customer_ip,  HandlerUtil.getRandomIp(channelWrapper));
            }
        };
        log.debug("[速付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

	 protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!signature.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
        		signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
			}
        }
        signSrc.append("key=" + channelWrapper.getAPI_KEY());
		String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[速付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }

	protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
		payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
		HashMap<String, String> result = Maps.newHashMap();
		if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper)) {
			result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
		}else{
			String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8").trim();
			if (StringUtils.isBlank(resultStr)) {
				log.error("[速付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空"+",参数："+JSON.toJSONString(payParam));
				throw new PayException("第三方返回异常:返回空"+",参数："+JSON.toJSONString(payParam));
			}
			JSONObject resJson = JSONObject.parseObject(resultStr);
			if (!resJson.containsKey("flag") || !"00".equals(resJson.getString("flag"))) {
				log.error("[速付]-[请求支付]-3.2.发送支付请求，及获取支付请求出错，订单号：" + channelWrapper.getAPI_ORDER_ID() + ",通道名称：" + channelWrapper.getAPI_CHANNEL_BANK_NAME() + ",postUrl:" + channelWrapper.getAPI_CHANNEL_BANK_URL());
				throw new PayException(resultStr);
			}
			result.put(QRCONTEXT, resJson.getString("qrCodeUrl"));
			result.put("第三方返回",resultStr); //保存全部第三方信息，上面的拆开没必要
        }
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		payResultList.add(result);
		log.debug("[速付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
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
        log.debug("[速付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}