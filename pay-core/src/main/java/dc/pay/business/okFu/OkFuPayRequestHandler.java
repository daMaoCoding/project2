package dc.pay.business.okFu;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import dc.pay.utils.RestTemplateUtil;
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
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Nov 29, 2017
 */
@RequestPayHandler("OKFU")
public final class OkFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(OkFuPayRequestHandler.class);

    private static final String QRCONTEXT = "QrContext";
	private static final String QRURL = "QrUrl";
	private static final String HTMLCONTEXT = "HtmlContext";
	private static final String PARSEHTML = "parseHtml";

	private static final String version  ="version";
	private static final String partner  ="partner";
	private static final String orderid  ="orderid";
	private static final String payamount  ="payamount";
	private static final String payip  ="payip";
	private static final String notifyurl  ="notifyurl";
	private static final String returnurl  ="returnurl";
	private static final String paytype  ="paytype";
	private static final String remark  ="remark";

    /**
     * 封装第三方所需要的参数
     * 
     * @return
     * @throws PayException
     * @author andrew
     * Nov 29, 2017
     */
    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(version, "2.0");
                put(remark, "3556239829");
                put(partner, channelWrapper.getAPI_MEMBERID());
                put(orderid,channelWrapper.getAPI_ORDER_ID() );
                put(payamount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(payip,HandlerUtil.getRandomIp(channelWrapper));
                put(notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(returnurl,"http://cn.unionpay.com/");
                put(paytype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            }
        };
        log.debug("[OK付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

    /**
     * 生成签名
     * 
     * @param api_response_params
     * @return
     * @throws PayException
     * @author andrew
     * Nov 29, 2017
     */
    protected String buildPaySign(Map api_response_params) throws PayException {
       //version=1.0&partner=1000&orderid=2016032620152314&payamount=100.00&payip=223.12.45.248&notifyurl=http://www.xxx.com/okfpay1.aspx&returnurl=http://www.xxx.com/okfpay.aspx&paytype=ICBC&remark=qq12345&key=8a0ecdfb1e2d4dbe8ea3f3b99762fc19
        String paramsStr = String.format("version=%s&partner=%s&orderid=%s&payamount=%s&payip=%s&notifyurl=%s&returnurl=%s&paytype=%s&remark=%s&key=%s",
                api_response_params.get(version),
                api_response_params.get(partner),
                api_response_params.get(orderid),
                api_response_params.get(payamount),
                api_response_params.get(payip),
                api_response_params.get(notifyurl),
                api_response_params.get(returnurl),
                api_response_params.get(paytype),
                api_response_params.get(remark),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[OK付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
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
     * Nov 28, 2017
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
        	String tmpStr = null;
        	try {
        		//tmpStr = HttpUtil.doPostRedirect(api_CHANNEL_BANK_URL, payParam);
                tmpStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
                JSONObject jsonObject = StringUtils.isNotBlank(tmpStr)?JSONObject.parseObject(tmpStr):null;
                if (null!=jsonObject &&  jsonObject.containsKey("code_url")  && jsonObject.containsKey("code")  && StringUtils.isNotBlank(jsonObject.getString("code"))) {
                    result.put(QRURL,  jsonObject.getString("code_url"));
                    result.put(QRCONTEXT, jsonObject.getString("code"));
                    result.put("第三方返回",tmpStr); //保存全部第三方信息，上面的拆开没必要
                    payResultList.add(result);
                }else{
                    log.error("[OK付]3.2.发送支付请求，获取支付请求返回值异常:"+tmpStr);
                    throw new PayException(tmpStr);
                }
        	} catch (Exception e) {
        		log.error("[OK付]3.3.发送支付请求，及获取支付请求出错，订单号：" + channelWrapper.getAPI_ORDER_ID() + ",通道名称：" + api_channel_bank_name + ",postUrl:" + api_CHANNEL_BANK_URL + ",payForManagerCGIResultJsonObj" + e.getMessage(), e);
        		throw new PayException(e.getMessage(),e);
        	}

        }
        log.debug("[OK付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
        return payResultList;
    }

    /**
     * 生成返回给前端使用的值
     * 
     * @param resultListMap
     * @return
     * @throws PayException
     * @author andrew
     * Nov 29, 2017
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
        log.debug("[OK付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}