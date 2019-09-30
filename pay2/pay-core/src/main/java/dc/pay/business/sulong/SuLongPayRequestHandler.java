package dc.pay.business.sulong;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import dc.pay.utils.*;
import org.apache.commons.lang.StringUtils;
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

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 *  Jun 6, 2018
 */
@RequestPayHandler("SULONG")
public final class SuLongPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(SuLongPayRequestHandler.class);

	//参数						参数名称				类型（长度）			使用			说明
	//基本参数
	//merchant_code				商家号				String(12)			必填			商户签约时，速龙支付平台分配的唯一商家号。举例：100100100999。
	//service_type				业务类型				String(10)			必选			固定值：alipay_scan 或 weixin_scan或tenpay_scan或ylpay_scan或jdpay_scan
	//notify_url				服务器异步通知地址		String(200)			必选			支付成功后，速龙支付平台会主动通知商家系统，商家系统必须指定接收通知的地址。举例：http://www.islpay.com/Notify_Url.jsp
	//interface_version			接口版本				String(10)			必选			接口版本，固定值：V3.1(必须大写)
	//client_ip					客户端IP				String(15)			必选			消费者创建交易时所使用机器的IP或者终端ip，最大长度为15个字符。举例：192.168.1.25
	//sign_type					签名方式				String(10)			必选			RSA或RSA-S，不参与签名
	//sign						签名					String				必选			签名数据，具体请见附录的签名规则定义。
	//业务参数
	//order_no					商户网站唯一订单号		String(64)			必选			商户系统订单号，由商户系统生成,保证其唯一性，最长100位,由字母、数字组成.举例：1000201666。
	//order_time				商户订单时间			Date				必选			商户订单时间，格式：yyyy-MM-dd HH:mm:ss，举例：2013-11-01 12:34:58
	//order_amount				商户订单总金额			Number(13,2)		必选			该笔订单的总金额，以元为单位，精确到小数点后两位。举例：12.01。
	//product_name				商品名称				String(100)			必选			商品名称，不超过100个字符。举例：华硕G750Y47JX-BL。
	//extra_return_param		公用回传参数			String(100)			可选			商户如果支付请求时传递了该参数，则通知商户支付成功时会回传该参数。
	private static final String merchant_code			="merchant_code";
	private static final String service_type			="service_type";
	private static final String notify_url				="notify_url";
	private static final String interface_version		="interface_version";
	private static final String client_ip				="client_ip";
	private static final String sign_type				="sign_type";
	private static final String order_no				="order_no";
	private static final String order_time				="order_time";
	private static final String order_amount			="order_amount";
	private static final String product_name			="product_name";
//	private static final String extra_return_param		="extra_return_param";
	
	//wap
	private static final String input_charset			="input_charset";
	
	//网银
	//参数					格式					必填			说明
	//service_type			String(10)			√			参数名称：服务类型固定值：direct_pay
	//pay_type				String(10)			×			参数名称：支付类型	取值如下（必须小写，多选时请用逗号隔开）	b2c(网银支付),weixin（微信扫码）,alipay_scan（支付宝扫码）,tenpay_scan（qq钱包扫码）, yl_scan (银联扫码), jd_scan (京东扫码)
	//client_ip_check		Int(1)				×			参数名称：客户端IP是否校验标识	当值为1校验客户端IP；	当值为0不校验客户端IP；
	//bank_code				String(10)			×			参数名称：网银直连银行代码	参见附录中的银行代码对照表，当该参数为空或与对照表中银行编码不一致时，直接跳转到速龙支付收银台选择银行页面
	//extend_param			String				√			参数名称：业务扩展参数	格式:参数名1^参数值1|参数名2^参数值2...，多个参数使用“|”进行分割	例如：name ^Zhang San|sex^Male	跨境商家必选，非跨境商家可选
//	private static final String pay_type				="pay_type";
	private static final String client_ip_check			="client_ip_check";
	private static final String bank_code				="bank_code";
	private static final String extend_param			="extend_param";
	private static final String redo_flag				="redo_flag";
	
	//signature	数据签名	32	是	　
//	private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merchant_code, channelWrapper.getAPI_MEMBERID());
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(client_ip,handlerUtil.getRandomIp(channelWrapper));
                put(sign_type,"RSA-S");
//                put(order_no,System.currentTimeMillis()+"");
                put(order_no,channelWrapper.getAPI_ORDER_ID());
                put(order_time,HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "yyyy-MM-dd HH:mm:ss"));
                put(order_amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(product_name,"name");
                if (handlerUtil.isWY(channelWrapper)) {
                	put(interface_version, "V3.0");
                	put(redo_flag, "1");
                	put(client_ip_check, "0");
                	put(service_type,"direct_pay");
                	if (!handlerUtil.isWebWyKjzf(channelWrapper)) {
                		put(bank_code,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
					}
                	put(extend_param,"");
                	put(input_charset, "UTF-8");
				}else if (handlerUtil.isWapOrApp(channelWrapper)) {
                	put(interface_version, "V3.0");
                	put(input_charset, "UTF-8");
                	put(service_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
				}else {
					put(service_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
					put(interface_version,"V3.1");
				}
            }
        };
        log.debug("[速龙]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

	 protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        List<String> paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
			if (!sign_type.equalsIgnoreCase(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
				signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
			}
        }
		String signInfo = signSrc.toString();
		String signMd5="";
		try {
			signMd5 = RsaUtil.signByPrivateKey(signInfo.substring(0,signInfo.length()-1),channelWrapper.getAPI_KEY());	// 签名
		} catch (Exception e) {
			log.error("[速龙]-[请求支付]-2.生成加密URL签名出错，签名出错：{}",e.getMessage(),e);
			throw new PayException(e.getMessage(),e);
		}
		log.debug("[速龙]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
		return signMd5;
	}

	protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
		payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
		HashMap<String, String> result = Maps.newHashMap();
		if (HandlerUtil.isWY(channelWrapper) ||  HandlerUtil.isWapOrApp(channelWrapper)) {
			result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
		}else{
			String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
			if (StringUtils.isBlank(resultStr)) {
				log.error("[速龙]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空"+",参数："+JSON.toJSONString(payParam));
				throw new PayException("返回空"+",参数："+JSON.toJSONString(payParam));
			}
			if (!resultStr.contains("<resp_code>SUCCESS</resp_code>")) {
				log.error("[速龙]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
				throw new PayException(resultStr);
			}
			resultStr = resultStr.replaceAll("<dinpay>", "").replaceAll("</dinpay>", "");
			Map<String, String> mapBodys = null ;
            try {
            	mapBodys = XmlUtil.toMap(resultStr.getBytes(), "utf-8");
	            if (null == mapBodys || !mapBodys.containsKey("result_code") || !"0".equals(mapBodys.get("result_code"))) {
	            	log.error("[速龙]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            	throw new PayException(resultStr);
	            }
	            result.put(QRCONTEXT, mapBodys.get("qrcode"));
			} catch (Exception e) {
				e.printStackTrace();
				log.error("[速龙]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
				throw new PayException(resultStr);
			}
		}
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		payResultList.add(result);
		log.debug("[速龙]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
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
        log.debug("[速龙]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}