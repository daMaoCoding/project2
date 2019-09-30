package dc.pay.business.gmifu;

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
 * Jun 28, 2018
 */
@RequestPayHandler("GMIFU")
public final class GMiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(GMiFuPayRequestHandler.class);

    //字段名称              类型            可否为空            最大长度            说明
    //payKey                 String            否                  32                  商户支付Key
    //orderPrice             Float             否                  12                  订单金额，单位：元,保留小数点后两位
    //outTradeNo             String            否                  30                  商户支付订单号（长度30以内）
    //productType            String            否                  8                   请参考支付方式编码
    //orderTime              String            否                  14                  下单时间，格式(yyyyMMddHHmmss)
    //productName            String            否                  200                 支付产品名称
    //orderIp                String            否                  15                  下单IP
    //returnUrl              String            否                  300                 页面通知地址
    //notifyUrl              String            否                  300                 后台异步通知地址
    //subPayKey              String            是                  32                  子商户支付Key，大商户时必填
    //remark                 String            是                  200                 备注
    //sign                   String            否                  50                  MD5大写签名
    private static final String payKey            ="payKey";
    private static final String orderPrice        ="orderPrice";
    private static final String outTradeNo        ="outTradeNo";
    private static final String productType       ="productType";
    private static final String orderTime         ="orderTime";
    private static final String productName       ="productName";
    private static final String orderIp           ="orderIp";
    private static final String returnUrl         ="returnUrl";
    private static final String notifyUrl         ="notifyUrl";
//    private static final String subPayKey         ="subPayKey";
//    private static final String sign              ="sign";


	//网银
	//bankCode	String	否	10	银行编码点击查看银行编码
	//bankAccountType	String	否	10	支付银行卡类型,对私借记卡:PRIVATE_DEBIT_ACCOUNT;对私贷记卡:PRIVATE_CREDIT_ACCOUNT
	private static final String bankCode	  ="bankCode";
	private static final String bankAccountType	  ="bankAccountType";
//	private static final String remark	  ="remark";
	
	private static final String paySecret  ="paySecret";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
            	put(payKey, channelWrapper.getAPI_MEMBERID());
            	put(orderPrice,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            	put(outTradeNo,channelWrapper.getAPI_ORDER_ID());
            	if (handlerUtil.isWY(channelWrapper)) {
            		//产品类型,B2C T0:50000103,B2C T1:50000101
            		put(productType,"50000103");
            		//支付银行卡类型,对私借记卡:PRIVATE_DEBIT_ACCOUNT;对私贷记卡:PRIVATE_CREDIT_ACCOUNT
            		put(bankAccountType,"PRIVATE_DEBIT_ACCOUNT");
            		put(bankCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//            		put(remark,"");
				}else {
					put(productType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
				}
            	put(orderTime,HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "yyyyMMddHHmmss"));
            	put(productName,"name");
            	put(orderIp,channelWrapper.getAPI_Client_IP());
            	put(returnUrl,channelWrapper.getAPI_WEB_URL());
            	put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            }
        };
        log.debug("[G米付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

	 protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
		 //平台默认使用MD5签名方式进行数据验签，保证数据完整性。请求方在请求数据是将请求数据按照键值对的,方式通过'&'符号进行拼接，获取到签名源文。将源文进行MD5(大写)签名后，作为sign字段放在请求报文中。源文拼接方式为：按照参数名称进行ASCII编码排序，如果参数值为空，则不参与签名
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
        	if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
        		signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
			}
        }
        signSrc.append(paySecret+"=" + channelWrapper.getAPI_KEY());
		String paramsStr = signSrc.toString();
        String pay_md5sign = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[G米付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }

	protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
		payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
		HashMap<String, String> result = Maps.newHashMap();
		if (HandlerUtil.isWY(channelWrapper)) {
			result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
		}else{
			String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
			if (StringUtils.isBlank(resultStr)) {
				log.error("[G米付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空"+",参数："+JSON.toJSONString(payParam));
				throw new PayException("返回空"+",参数："+JSON.toJSONString(payParam));
			}
			JSONObject resJson = JSONObject.parseObject(resultStr);
			if (!resJson.containsKey("resultCode") || !"0000".equals(resJson.getString("resultCode"))) {
				log.error("[G米付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
				throw new PayException(resultStr);
			}
			if (handlerUtil.isWebYlKjzf(channelWrapper) || handlerUtil.isWapOrApp(channelWrapper)) {
				result.put(channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("JD_SM") ? HTMLCONTEXT : JUMPURL, resJson.getString("payMessage"));
//			}else if (channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("GMIFU_BANK_WEBWAPAPP_JD_SM")) {
//				result.put(HTMLCONTEXT, resJson.getString("payMessage"));
			}else {
				result.put(QRCONTEXT, resJson.getString("payMessage"));
			}
//			result.put((handlerUtil.isFS(channelWrapper) || channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("_WAP_JD_")) ? HTMLCONTEXT : (HandlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT), resJson.getString("payMessage"));
		}
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		payResultList.add(result);
		log.debug("[G米付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
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
        log.debug("[G米付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}