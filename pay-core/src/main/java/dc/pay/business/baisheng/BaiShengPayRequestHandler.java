package dc.pay.business.baisheng;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
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
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Feb 27, 2018
 */
@RequestPayHandler("BAISHENG")
public final class BaiShengPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(BaiShengPayRequestHandler.class);

    //MerchantId	String	M	商户号
    private static final String MerchantId		  ="MerchantId";
    //Sign		String	M	数据签名
    private static final String Sign		  ="Sign";
    //Timestamp	String	M	发送请求的时间，格式"yyyy-MM-dd HH:mm:ss"
    private static final String Timestamp		  ="Timestamp";
    //PaymentTypeCode	String	M	入款类型，具体请参考附录中的【入款类型】
    private static final String PaymentTypeCode		  ="PaymentTypeCode";
    //OutPaymentNo	String	M	商户的入款流水号
    private static final String OutPaymentNo		  ="OutPaymentNo";
    //PaymentAmount	String	C	入款金额，单位为分，1元 = 100
    private static final String PaymentAmount		  ="PaymentAmount";
    //NotifyUrl	String	M	入款成功异步通知URL，服务器的通知返回是由我司的服务器发起，以 POST 的方式返回到商户的网站上。系统自动判断商户网站的响应 HttpStatusCode，若为200则表示通知成功，否则会在继续通知（通知上限是5次），通知频率会逐渐减弱（1分钟、3分钟、5分钟、10分钟、15分钟）5次通知失败后，请使用入款查询接口，系统不提供任何补发功能。
    private static final String NotifyUrl		  ="NotifyUrl";

    /**
     * 封装第三方所需要的参数
     * 
	 * @return
	 * @throws PayException
	 * @author andrew
	 * Feb 27, 2018
	 */
	@Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
            	put(MerchantId, channelWrapper.getAPI_MEMBERID());
            	put(Timestamp, DateUtil.formatDateTimeStrByParam("yyyy-MM-dd HH:mm:ss"));
            	put(PaymentTypeCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            	put(OutPaymentNo,channelWrapper.getAPI_ORDER_ID());
            	put(PaymentAmount,  channelWrapper.getAPI_AMOUNT());
            	put(NotifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            }
        };
        log.debug("[百盛]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

    
    /**
     * 生成签名
     * 
     * @param api_response_params
     * @return
     * @throws PayException
     * @author andrew
     * Feb 27, 2018
     */
	 protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
			//1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
			//2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
			StringBuffer signSrc= new StringBuffer();
			signSrc.append(MerchantId+"=").append(api_response_params.get(MerchantId)).append("&");
			signSrc.append(NotifyUrl+"=").append(api_response_params.get(NotifyUrl)).append("&");
			signSrc.append(OutPaymentNo+"=").append(api_response_params.get(OutPaymentNo)).append("&");
			signSrc.append(PaymentAmount+"=").append(api_response_params.get(PaymentAmount)).append("&");
			signSrc.append(PaymentTypeCode+"=").append(api_response_params.get(PaymentTypeCode)).append("&");
			signSrc.append(Timestamp+"=").append(api_response_params.get(Timestamp));
			signSrc.append(channelWrapper.getAPI_KEY());
			String paramsStr = signSrc.toString();
			String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
	        log.debug("[百盛]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
	        return signMd5;
	    }
    /**
     * 生成返回给RequestPayResult对象detail字段的值
     * 
     * @param payParam
     * @param pay_md5sign
     * @return
     * @throws PayException
     * @author andrew
     * Feb 27, 2018
     */
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
    	payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        List<Map<String, String>> payResultList = Lists.newArrayList();
        Map<String,String> result = Maps.newHashMap();
		if(HandlerUtil.isWapOrApp(channelWrapper)){
            StringBuffer htmlContent = HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
            //保存第三方返回值
            result.put(HTMLCONTEXT,htmlContent.toString());
            payResultList.add(result);
        }else{
        	String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
        	if (StringUtils.isBlank(resultStr)) {
        		log.error("[百盛]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回null，入参数："+JSON.toJSONString(payParam));
        		throw new PayException("返回null，入参数："+JSON.toJSONString(payParam));
        	}
        	if (!resultStr.contains("Code") || !resultStr.contains("200")) {
                log.error("[百盛]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                try {
					throw new PayException(URLDecoder.decode(resultStr, "UTF-8"));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
        	JSONObject resJson = JSONObject.parseObject(resultStr);
        	if (!resJson.containsKey("Code") || !"200".equals(resJson.getString("Code"))) {
        		log.error("[百盛]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
        		throw new PayException(JSON.toJSONString(resJson));
        	}
        	result.put(QRCONTEXT, resJson.getString("QrCodeUrl"));
        	payResultList.add(result);
        }
    	log.debug("[百盛]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
        return payResultList;
    }

    /**
     * 生成返回给前端使用的值
     * 
	 * @param resultListMap
	 * @return
	 * @throws PayException
	 * @author andrew
	 * Feb 27, 2018
	 */
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
        log.debug("[百盛]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}