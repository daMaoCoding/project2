package dc.pay.business.hefubao;


import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
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
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 
 * @author andrew
 * Dec 12, 2017
 */
@RequestPayHandler("HEFUBAO")
public final class HeFuBaoPayRequestHandler extends PayRequestHandler {
	private static final Logger log = LoggerFactory.getLogger(HeFuBaoPayRequestHandler.class);

    //扫码属性
    private static final String QRCONTEXT = "QrContext";
	private static final String QRURL = "QrUrl";
	private static final String HTMLCONTEXT = "HtmlContext";
	//wap跳转属性
	private static final String JUMPURL = "JUMPURL";
	
	private static final String trxType = "trxType";
	private static final String r1_merchantNo = "r1_merchantNo";
	private static final String r2_orderNumber = "r2_orderNumber";
	private static final String r3_payType = "r3_payType";
	private static final String r4_amount = "r4_amount";
	private static final String r5_currency = "r5_currency";
	private static final String r6_authcode = "r6_authcode";
	private static final String r7_appPayType = "r7_appPayType";
	private static final String r8_callbackUrl = "r8_callbackUrl";
	private static final String r10_orderIp = "r10_orderIp";
	private static final String r11_itemname = "r11_itemname";
	private static final String r16_desc = "r16_desc";
	private static final String sign = "sign";
	

    /**
     * 封装第三方所需要的参数
     * 
     * @return
     * @throws PayException
     * @throws UnsupportedEncodingException
     * @author andrew
     * Dec 13, 2017
     */
    @Override
    protected Map<String, String> buildPayParam() throws PayException, UnsupportedEncodingException {
    	String api_CHANNEL_BANK_NAME_FlAG = channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG();
    	if (null == api_CHANNEL_BANK_NAME_FlAG || !api_CHANNEL_BANK_NAME_FlAG.contains(",") || api_CHANNEL_BANK_NAME_FlAG.split(",").length != 2) {
            log.error("[合付宝]-[请求支付]-1.1.组装请求参数格式：支付类型,客户端类型。如：扫码支付,支付宝==>SCAN,ALIPAY" );
            throw new PayException("[合付宝]-[请求支付]-1.1.组装请求参数格式：支付类型,客户端类型。如：扫码支付,支付宝==>SCAN,ALIPAY" );
		}
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
            	put(trxType, "AppPay");
                put(r1_merchantNo, channelWrapper.getAPI_MEMBERID());
                put(r2_orderNumber,channelWrapper.getAPI_ORDER_ID());
                //配置文件里截取
                put(r3_payType,api_CHANNEL_BANK_NAME_FlAG.split(",")[0]);
//                put(r16_desc,"r16_desc");
                put(r4_amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(r5_currency,"CNY");
                put(r6_authcode,"0");
                put(r7_appPayType,api_CHANNEL_BANK_NAME_FlAG.split(",")[1]);
                put(r8_callbackUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(r10_orderIp,HandlerUtil.getRandomIp(channelWrapper));
                put(r11_itemname,URLDecoder.decode("r11_itemname", "UTF-8"));
            }
        };
        log.debug("[合付宝]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

    /**
     * 生成签名
     * 
     * @param api_response_params
     * @return
     * @throws PayException
     * @author andrew
     * Dec 13, 2017
     */
    protected String buildPaySign(Map api_response_params) throws PayException {
		// 注意参数需要trim，避免一个空格排查问题耽误时间
    	StringBuffer signSrc= new StringBuffer();
    	signSrc.append("#").append(api_response_params.get(trxType)).append("#");
    	signSrc.append(api_response_params.get(r1_merchantNo)).append("#");
    	signSrc.append(api_response_params.get(r2_orderNumber)).append("#");
    	signSrc.append(api_response_params.get(r3_payType)).append("#");
    	signSrc.append(api_response_params.get(r4_amount)).append("#");
    	signSrc.append(api_response_params.get(r5_currency)).append("#");
    	signSrc.append(api_response_params.get(r7_appPayType)).append("#");
    	signSrc.append(api_response_params.get(r8_callbackUrl)).append("#");
    	signSrc.append(api_response_params.get(r10_orderIp)).append("#");
    	signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[合付宝]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
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
     * Dec 13, 2017
     */
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
    	payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        List<Map<String, String>> payResultList = Lists.newArrayList();
        String api_channel_bank_name = channelWrapper.getAPI_CHANNEL_BANK_NAME();
        String api_CHANNEL_BANK_URL = channelWrapper.getAPI_CHANNEL_BANK_URL();
        Map<String,String> result = Maps.newHashMap();
        if(api_channel_bank_name.contains("_WY_") ){
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
        	String resultStr = null;
        	try {
                resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
                if (null == resultStr || StringUtils.isBlank(resultStr)) {
                	log.error("[合付宝]3.1.发送支付请求，获取支付请求返回值异常:返回空");
                	throw new PayException("返回空");
                }
        	} catch (Exception e) {
        		log.error("[合付宝]3.3.发送支付请求，及获取支付请求出错，订单号：" + channelWrapper.getAPI_ORDER_ID() + ",通道名称：" + api_channel_bank_name + ",postUrl:" + api_CHANNEL_BANK_URL + ",payForManagerCGIResultJsonObj" + e.getMessage(), e);
        		throw new PayException(e.getMessage(),e);
        	}
        	if (!resultStr.contains(":") || !resultStr.contains("{")) {
        		log.error("[合付宝]3.2.发送支付请求，获取支付请求返回值异常:"+resultStr);
        		throw new PayException(resultStr);
        	}
            JSONObject jsonObject = JSONObject.parseObject(resultStr);
            //retCode	0000成功
            if (!jsonObject.containsKey("retCode") || !"0000".equals(jsonObject.getString("retCode"))) {
            	 log.error("[合付宝]3.2.发送支付请求，获取支付请求返回值异常:"+resultStr);
                 throw new PayException(resultStr);
            }
            //按不同的请求接口，向不同的属性设置值
            if(channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("_WAP_")||channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("_WAPAPP_")) {
//            	result.put(JUMPURL, jsonObject.getString("r6_wapurl"));
            	result.put(JUMPURL, jsonObject.getString("r5_qrcode"));
            }else{
            	result.put(QRCONTEXT, jsonObject.getString("r5_qrcode"));
            }
            result.put("第三方返回",resultStr); //保存全部第三方信息，上面的拆开没必要
            payResultList.add(result);
        }
        log.debug("[合付宝]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
        return payResultList;
    }

    /**
     * 生成返回给前端使用的值
     * 
     * @param resultListMap
     * @return
     * @throws PayException
     * @author andrew
     * Dec 13, 2017
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
        log.debug("[合付宝]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}