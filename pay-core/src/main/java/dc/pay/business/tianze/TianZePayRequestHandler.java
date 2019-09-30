package dc.pay.business.tianze;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import com.alibaba.fastjson.JSON;
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
import dc.pay.utils.qr.QRCodeUtil;

/**
 * ************************
 * @author beck 2229556569
 */

@RequestPayHandler("TIANZE")
public final class TianZePayRequestHandler extends PayRequestHandler {
	private static final Logger log = LoggerFactory.getLogger(TianZePayRequestHandler.class);

	private static final String p0_Cmd = "p0_Cmd";                    //固定值 “Buy”
	private static final String merchantId = "p1_MerId";              //商户号
	private static final String orderNumber = "p2_Order";             //订单号
	private static final String money = "p3_Amt";                     //支付金额
	private static final String p4_Cur = "p4_Cur";                    //支付币种，默认CNY
    private static final String p5_Pid = "p5_Pid";                    //商品名称
    private static final String p6_Pcat = "p6_Pcat";                  //商品种类
	private static final String p7_Pdesc = "p7_Pdesc";                //商品描述
	private static final String notifyUrl = "p8_Url";                //异步通知url
	private static final String p9_SAF = "p9_SAF";                   //为“1”：需要用户将送货地址留在[API支付平台]系统；为“0”：不需要，默认为“0”
	private static final String pa_MP = "pa_MP";
	private static final String payType = "pd_FrpId";                //支付类型
	private static final String pr_NeedResponse = "pr_NeedResponse"; //固定值为“1”
	private static final String signType = "hmac";                   //签名字段
	


	@Override
	protected Map<String, String> buildPayParam() throws PayException {
		Map<String, String> payParam = Maps.newHashMap();
		
		payParam.put(p0_Cmd, "Buy");
		payParam.put(merchantId, channelWrapper.getAPI_MEMBERID());
		payParam.put(orderNumber, channelWrapper.getAPI_ORDER_ID());
		payParam.put(money, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
		payParam.put(p4_Cur, "CNY");
		payParam.put(p5_Pid, "goodname");
		payParam.put(p6_Pcat, "goodname");
		payParam.put(p7_Pdesc, "goodname");
		payParam.put(notifyUrl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
		payParam.put(p9_SAF, "0");
		payParam.put(pa_MP, "remark");
		payParam.put(payType, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
		payParam.put(pr_NeedResponse, "1");
		
		log.debug("[天泽]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
		return payParam;
	}

	@Override
	protected String buildPaySign(Map<String, String> params) throws PayException {
	    StringBuilder sb = new StringBuilder();
	    sb.append(params.get(p0_Cmd));
	    sb.append(params.get(merchantId));
	    sb.append(params.get(orderNumber));
	    sb.append(params.get(money));
	    sb.append(params.get(p4_Cur));
	    sb.append(params.get(p5_Pid));
	    sb.append(params.get(p6_Pcat));
	    sb.append(params.get(p7_Pdesc));
	    sb.append(params.get(notifyUrl));
	    sb.append(params.get(p9_SAF));
	    sb.append(params.get(pa_MP));
	    sb.append(params.get(payType));
	    sb.append(params.get(pr_NeedResponse));
	    
		String signStr = sb.toString();
		String pay_md5sign = DigestUtil.hmacSign(signStr, channelWrapper.getAPI_KEY());
		log.debug("[天泽]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
		return pay_md5sign;
	}

	@Override
	protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
		payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		Map result = Maps.newHashMap();
		String resultStr;
		        
		try {
			if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLWAP(channelWrapper)
			        || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isWebWyKjzf(channelWrapper)
			        ||HandlerUtil.isFS(channelWrapper)||HandlerUtil.isYLKJ(channelWrapper)) {
				String htmlContent = HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam).toString();
				result.put(HTMLCONTEXT, htmlContent);
			}else {
				resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
				//if(channelWrapper.getAPI_CHANNEL_BANK_NAME().equalsIgnoreCase("TIANZE_BANK_WEBWAPAPP_WX_SM")){ //如果为微信扫码
				    
				    String html = this.firstJump(resultStr);
				    Document document = Jsoup.parse(html);
			        
				    Element qrImg = document.getElementById("img1");
				    if(qrImg == null){
				        String errorMsg = "[天泽]-[请求支付]3.1.发送支付请求，及获取支付请求结果出错："+html;
				        log.error(errorMsg);
	                    throw new PayException(html);
				    }
				    
				    String qrImgUrl = document.getElementById("img1").attr("src");
				    if(StringUtils.isBlank(qrImgUrl)){
				        String errorMsg = "[天泽]-[请求支付]3.2.未获取到二维码地址。";
				        log.error(errorMsg);
				        throw new PayException(html);
				    }
				    String host = "http://101.132.128.241/pay/";
				    qrImgUrl = host + qrImgUrl;
				    String qrCode = QRCodeUtil.decodeByUrl(qrImgUrl);
				    result.put(QRCONTEXT, qrCode);
				//}
			}
			payResultList.add(result);
			
		} catch (Exception e) {
			log.error("[天泽]3.2.发送支付请求，及获取支付请求结果出错：", e);
			throw new PayException(e.getMessage(), e);
		}
		
		log.debug("[天泽]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));
		return payResultList;
	}

	@Override
	protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
		RequestPayResult requestPayResult = new RequestPayResult();
		if (null != resultListMap && !resultListMap.isEmpty()) {
			if (resultListMap.size() == 1) {
				Map<String, String> resultMap = resultListMap.get(0);
				requestPayResult = buildResult(resultMap, channelWrapper, requestPayResult);
			}
			if (ValidateUtil.requestesultValdata(requestPayResult)) {
				requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
			} else {
				throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
			}
		} else {
			throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
		}
		log.debug("[天泽]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
		return requestPayResult;
	}
	
	
	/**
	 * 第一次跳转
	 * */
	private String firstJump(String html){
	    Document document = Jsoup.parse(html);
        
        Element form = document.getElementsByTag("form").first();
        if(form == null)
            return html;
        
        String formAction = form.attr("action");
        
        String Price = document.select("[name='Price']").first().attr("value");
        String num = document.select("[name='num']").first().attr("value");
        String codename = document.select("[name='codename']").first().attr("value");
        String GetStr_wg = document.select("[name='GetStr_wg']").first().attr("value");
        String QrUrl = document.select("[name='QrUrl']").first().attr("value");
        String channelid = document.select("[name='channelid']").first().attr("value");
        String gotostr = document.select("[name='goto']").first().attr("value");
        
        Map<String, String> payParam = Maps.newHashMap();
        payParam.put("Price", Price);
        payParam.put("num", num);
        payParam.put("codename", codename);
        payParam.put("GetStr_wg", GetStr_wg);
        payParam.put("QrUrl", QrUrl);
        payParam.put("channelid", channelid);
        payParam.put("goto", gotostr);
        
        String content = RestTemplateUtil.sendByRestTemplateRedirect(formAction, payParam, String.class, HttpMethod.POST).trim();
        
        return content;
        
	}
}