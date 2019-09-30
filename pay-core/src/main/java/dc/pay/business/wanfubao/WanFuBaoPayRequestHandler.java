package dc.pay.business.wanfubao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

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
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Mar 14, 2018
 */
@RequestPayHandler("WANFUBAO")
public final class WanFuBaoPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(WanFuBaoPayRequestHandler.class);

	//商户 ID		partner		N	Y		商户 ID,由万付宝分配							
    private static final String partner  ="partner";
	//银行类型		banktype	N	Y		银行类型，具体参考 附录一default 为跳转到万付宝进行选择							支付
    private static final String banktype  ="banktype";
	//金额			paymoney	N	Y	单位：元（人民币）商户系统订单号，该订单号将作为
    private static final String paymoney  ="paymoney";
	//商户订单号		ordernumber	N	Y	万付宝的返回数据。该值需在商户	系统内唯一，万付宝暂时不检查该值是否唯一				
    private static final String ordernumber  ="ordernumber";
	//下行异步通知地址	callbackurl	N	Y	下行异步通知的地址，需要以	http://开头且没有任何参数
    private static final String callbackurl  ="callbackurl";
//	//下行同步通知地址	hrefbackurl	Y	N	
//    private static final String hrefbackurl  ="hrefbackurl";
//	//备注信息		attach		Y	N	建议回传一个会员账号，方便订单				
//    private static final String attach  ="attach";
    //是否返回二维码链接	isshow		Y	N	0：返回二维码链接	1：跳转收银台
    private static final String isshow  ="isshow";
    //MD5 签名		sign		N	N	32 位小写MD5 签名值，GB2312 编码				
	private static final String signature  ="sign";

    /**
     * 封装第三方所需要的参数
     * 
     * @return
     * @throws PayException
     * @author andrew
     * Dec 5, 2017
     */
    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(partner, channelWrapper.getAPI_MEMBERID());
                put(banktype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(paymoney,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(ordernumber,channelWrapper.getAPI_ORDER_ID());
                put(callbackurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(isshow,"0");
            }
        };
        log.debug("[万付宝]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map api_response_params) throws PayException {
		//1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
		//2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
		StringBuffer signSrc= new StringBuffer();
		signSrc.append(partner+"=").append(api_response_params.get(partner)).append("&");
		signSrc.append(banktype+"=").append(api_response_params.get(banktype)).append("&");
		signSrc.append(paymoney+"=").append(api_response_params.get(paymoney)).append("&");
		signSrc.append(ordernumber+"=").append(api_response_params.get(ordernumber)).append("&");
		signSrc.append(callbackurl+"=").append(api_response_params.get(callbackurl));
		signSrc.append(channelWrapper.getAPI_KEY());
		String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[万付宝]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }

	protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
		payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		HashMap<String, String> result = Maps.newHashMap();
		if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper)) {
			String html = HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString();
			result.put(HTMLCONTEXT, html.replace("method='post'", "method='get'"));
		}else{
			String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.GET).trim();
			if (StringUtils.isBlank(resultStr)) {
				log.error("[万付宝]3.1.发送支付请求，获取支付请求返回值异常:返回空");
				throw new PayException("返回空");
			}
			JSONObject resJson = JSONObject.parseObject(resultStr);
			if (resJson == null || !resJson.containsKey("code") || !"0000".equals(resJson.getString("code"))) {
				log.error("[万付宝]3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
				throw new PayException(resultStr);
			}
			result.put(QRCONTEXT, resJson.getString("message"));
		}
		payResultList.add(result);
		log.debug("[万付宝]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
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
        log.debug("[万付宝]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}