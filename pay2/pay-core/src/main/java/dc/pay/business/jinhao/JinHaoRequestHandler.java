package dc.pay.business.jinhao;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.util.CollectionUtils;

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
 * Dec 26, 2017
 */
@RequestPayHandler("JINHAO")
public final class JinHaoRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(JinHaoRequestHandler.class);

    //扫码属性
    private static final String QRCONTEXT = "QrContext";
	private static final String QRURL = "QrUrl";
	private static final String HTMLCONTEXT = "HtmlContext";
	//wap跳转属性
	private static final String JUMPURL = "JUMPURL";
	
	// payKey          | 商户支付Key                                  | String | 否    | 32   |
	private static final String payKey  ="payKey";
	// orderPrice      | 订单金额，单位：元<br>保留小数点后两位       | String | 否    | 12   |
	private static final String orderPrice  ="orderPrice";
	// outTradeNo      | 商户支付订单号                               | String | 否    | 30   |
	private static final String outTradeNo  ="outTradeNo";
	// productType     | 产品类型<br>==50000101== B2C T1支付<br>==50000103== B2C T0支付 | String | 否    | 8    |
	private static final String productType  ="productType";
	// orderTime       | 下单时间，格式<br>yyyyMMddHHmmss             | String | 否    | 14   |
	private static final String orderTime  ="orderTime";
	// productName     | 支付产品名称                                 | String | 否    | 200  |
	private static final String productName  ="productName";
	// orderIp         | 下单IP                                       | String | 否    | 15   |
	private static final String orderIp  ="orderIp";
	// bankCode        | 银行编码                                     | String | 否    | 10   |
	private static final String bankCode  ="bankCode";
	// bankAccountType | 支付银行卡类型<br>==PRIVATE_DEBIT_ACCOUNT== 对私借记卡<br>==PRIVATE_CREDIT_ACCOUNT== 对私贷记卡 | String | 否    | 10   |
	private static final String bankAccountType  ="bankAccountType";
	// returnUrl       | 页面通知地址                                 | String | 否    | 300  |
	private static final String returnUrl  ="returnUrl";
	// notifyUrl       | 后台异步通知地址                             | String | 否    | 300  |
	private static final String notifyUrl  ="notifyUrl";
	// remark          | 备注                                         | String | 是    | 200  |
	private static final String remark  ="remark";
	// mobile          | 移动端（当为手机端时此参数不为空 值为 1）    | String | 是    | 10   |
	private static final String mobile  ="mobile";
	private static final String paySecret  ="paySecret";

    /**
     * 封装第三方所需要的参数
     * 
     * @return
     * @throws PayException
     * @author andrew
     * Dec 26, 2017
     */
    @Override
    protected Map<String, String> buildPayParam() throws PayException {
    	String api_CHANNEL_BANK_NAME_FlAG = channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG();
		Map<String, String> payParam = new TreeMap<String, String>() {
            {
            	put(payKey, channelWrapper.getAPI_MEMBERID());
                put(productType,HandlerUtil.isWY(channelWrapper) ? "50000103" : api_CHANNEL_BANK_NAME_FlAG);
                put(productName,"productName");
                put(orderTime,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
                put(orderPrice,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
               // put(orderIp,"http://"+HandlerUtil.getRandomIp(channelWrapper));
				put(orderIp,channelWrapper.getAPI_Client_IP());
                put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(returnUrl,channelWrapper.getAPI_WEB_URL());
                put(remark,"remark");
                //| bankAccountType | 支付银行卡类型<br>==PRIVATE_DEBIT_ACCOUNT== 对私借记卡<br>==PRIVATE_CREDIT_ACCOUNT== 对私贷记卡 | String | 否    | 10   |
                if (HandlerUtil.isWY(channelWrapper)) {
                	put(bankAccountType,"PRIVATE_DEBIT_ACCOUNT");
                	put(bankCode,api_CHANNEL_BANK_NAME_FlAG);
					if(HandlerUtil.isOrderFromWapOrApp(channelWrapper.getAPI_ORDER_FROM())){
						put(mobile,"1");
					}
				}
                put(outTradeNo,channelWrapper.getAPI_ORDER_ID());
            }
        };
        log.debug("[金好]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

    /**
     * 生成签名
     * 
     * @param api_response_params
     * @return
     * @throws PayException
     * @author andrew
     * Dec 26, 2017
     */
    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
    	//1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
    	//2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
		StringBuffer signSrc= new StringBuffer();
		//| bankAccountType | 支付银行卡类型<br>==PRIVATE_DEBIT_ACCOUNT== 对私借记卡<br>==PRIVATE_CREDIT_ACCOUNT== 对私贷记卡 | String | 否    | 10   |
		if (channelWrapper.getAPI_CHANNEL_BANK_NAME().contains(PayEumeration.CHANNEL_TYPE._WY_.name())) {
			signSrc.append(bankAccountType+"=").append(api_response_params.get(bankAccountType)).append("&");
			signSrc.append(bankCode+"=").append(api_response_params.get(bankCode)).append("&");
		}
		if (null != api_response_params.get(mobile) && StringUtils.isNotBlank(api_response_params.get(mobile))) {
			signSrc.append(mobile+"=").append(api_response_params.get(mobile)).append("&");
		}
		signSrc.append(notifyUrl+"=").append(api_response_params.get(notifyUrl)).append("&");
		signSrc.append(orderIp+"=").append(api_response_params.get(orderIp)).append("&");
		signSrc.append(orderPrice+"=").append(api_response_params.get(orderPrice)).append("&");
		signSrc.append(orderTime+"=").append(api_response_params.get(orderTime)).append("&");
		signSrc.append(outTradeNo+"=").append(api_response_params.get(outTradeNo)).append("&");
		signSrc.append(payKey+"=").append(api_response_params.get(payKey)).append("&");
		signSrc.append(productName+"=").append(api_response_params.get(productName)).append("&");
		signSrc.append(productType+"=").append(api_response_params.get(productType)).append("&");
		signSrc.append(remark+"=").append(api_response_params.get(remark)).append("&");
		signSrc.append(returnUrl+"=").append(api_response_params.get(returnUrl)).append("&");
        signSrc.append(paySecret+"=").append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[金好]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5)+"，参数："+JSON.toJSONString(paramsStr));
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
     * Dec 26, 2017
     */
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
    	payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        List<Map<String, String>> payResultList = Lists.newArrayList();
        String api_channel_bank_name = channelWrapper.getAPI_CHANNEL_BANK_NAME();
        String api_CHANNEL_BANK_URL = channelWrapper.getAPI_CHANNEL_BANK_URL();
        Map<String,String> result = Maps.newHashMap();
        if(api_channel_bank_name.contains(PayEumeration.CHANNEL_TYPE._WY_.name()) ){
        	StringBuffer sbHtml = new StringBuffer();
        	sbHtml.append("<form id='postForm' name='mobaopaysubmit' action='"+ api_CHANNEL_BANK_URL + "' method='post'>");
        	for (Map.Entry<String, String> entry : payParam.entrySet()) {
        		sbHtml.append("<input type='hidden' name='"+ entry.getKey() + "' value='" + entry.getValue()+ "'/>");
        	}
        	sbHtml.append("</form>");
        	sbHtml.append("<script>document.forms['postForm'].submit();</script>");
        	//保存第三方返回值
        	result.put(HTMLCONTEXT, sbHtml.toString());
        	payResultList.add(result);
        }else{
        	String tmpStr = null;
        	try {
        		tmpStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
                if (null == tmpStr || StringUtils.isBlank(tmpStr)) {
                	log.error("[金好]3.1.发送支付请求，获取支付请求返回值异常:返回空");
                	throw new PayException("返回空");
                }
        	} catch (Exception e) {
        		log.error("[金好]3.3.发送支付请求，及获取支付请求出错，订单号：" + channelWrapper.getAPI_ORDER_ID() + ",通道名称：" + api_channel_bank_name + ",postUrl:" + api_CHANNEL_BANK_URL + ",payForManagerCGIResultJsonObj" + e.getMessage(), e);
        		throw new PayException(e.getMessage(),e);
        	}
            JSONObject jsonObject = JSONObject.parseObject(tmpStr);
            //响应码为‘0000’时非空
            if (null == jsonObject || (jsonObject.containsKey("resultCode") && !"0000".equals(jsonObject.getString("resultCode")))) {
            	 log.error("[金好]3.2.发送支付请求，获取支付请求返回值异常:"+tmpStr);
                 throw new PayException(tmpStr);
            }
            //按不同的请求接口，向不同的属性设置值
            if(channelWrapper.getAPI_CHANNEL_BANK_NAME().contains(PayEumeration.CHANNEL_TYPE._WAP_.name())||channelWrapper.getAPI_CHANNEL_BANK_NAME().contains(PayEumeration.CHANNEL_TYPE._WAPAPP_.name())) {
            	result.put(JUMPURL, jsonObject.getString("payMessage"));
            }else{
            	result.put(QRCONTEXT, jsonObject.getString("payMessage"));
            }
            result.put("第三方返回",tmpStr); //保存全部第三方信息，上面的拆开没必要
            payResultList.add(result);
        }
        log.debug("[金好]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
        return payResultList;
    }

    /**
     * 生成返回给前端使用的值
     * 
     * @param resultListMap
     * @return
     * @throws PayException
     * @author andrew
     * Dec 26, 2017
     */
    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (CollectionUtils.isEmpty(resultListMap) || resultListMap.size() != 1) {
        	throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
		}
        Map<String, String> qrMap = resultListMap.get(0);
        if (null != qrMap && qrMap.containsKey(QRCONTEXT)) {
        	requestPayResult.setRequestPayQRcodeContent(qrMap.get(QRCONTEXT));
        }else if (null != qrMap && qrMap.containsKey(HTMLCONTEXT)) {
        	requestPayResult.setRequestPayHtmlContent(qrMap.get(HTMLCONTEXT));
        }else if (null != qrMap && qrMap.containsKey(JUMPURL)) {
            requestPayResult.setRequestPayJumpToUrl(qrMap.get(JUMPURL));
        }
        requestPayResult.setRequestPayamount(channelWrapper.getAPI_AMOUNT());
        requestPayResult.setRequestPayOrderId(channelWrapper.getAPI_ORDER_ID());
        requestPayResult.setRequestPayOrderCreateTime(HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "yyyy-MM-dd HH:mm:ss"));
        requestPayResult.setRequestPayQRcodeURL(null);
        requestPayResult.setRequestPayChannelBankName(channelWrapper.getAPI_CHANNEL_BANK_NAME());
        if (!ValidateUtil.requestesultValdata(requestPayResult)) {
        	throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
        }
        requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
        log.debug("[金好]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}