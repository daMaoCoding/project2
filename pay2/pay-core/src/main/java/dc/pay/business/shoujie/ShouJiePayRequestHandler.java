package dc.pay.business.shoujie;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
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
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Apr 25, 2018
 */
@RequestPayHandler("SHOUJIE")
public final class ShouJiePayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(ShouJiePayRequestHandler.class);

	//参数名称			变量名				类型长度		是否可空	加入签名		说明
	//版本号			Ver					varchar(5)		N		Y			默认1.0
	//商户编号			partner				int(8)			N		Y			商户后台获取
	//商户订单号		ordernumber			varchar(20)		N		Y			商户系统订单号，该订单号将作为首捷接口的返回数据。该值需在商户系统内唯一，首捷系统暂时不检查该值是否唯一
	//订单金额			paymoney			decimal(10,2)	N		N			单位元（人民币）,两位小数点
	//支付编号			paytype				varchar(10)		N					详见附录1
	//银行编号			bankcode			varchar(10)		网银直连	N			详见附录2
	//  												不可为空	
	//异步通知URL		notifyurl			varchar(50)		N		Y			http://开头且没有任何参数
	//同步跳转URL		returnurl			varchar(50)		N		Y			不能带有任何参数
	//md5签名串		sign				varchar(32)							参照md5签名说明
	private static final String Ver		  ="Ver";
	private static final String partner	  ="partner";
	private static final String ordernumber	  ="ordernumber";
	private static final String paymoney	  ="paymoney";
	private static final String paytype	  ="paytype";
	private static final String bankcode	  ="bankcode";
	private static final String notifyurl	="notifyurl";
	private static final String returnurl	="returnurl";

		
    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
            	put(Ver, "1.0");
            	put(partner, channelWrapper.getAPI_MEMBERID());
            	put(ordernumber,channelWrapper.getAPI_ORDER_ID());
            	put(paymoney,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            	if (HandlerUtil.isWY(channelWrapper) && !HandlerUtil.isWebWyKjzf(channelWrapper)) {
            		put(paytype,"bank");
            		put(bankcode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
				}else {
					put(paytype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
				}
            	put(notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            	put(returnurl,channelWrapper.getAPI_WEB_URL());
            }
        };
        log.debug("[首捷]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map api_response_params) throws PayException {
		//1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
		//2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
    	//{value}要替换成接收到的值，{apikey}要替换成平台分配的接入密钥，可在商户后台获取
    	//Ver={value}&partner={value}&paymoney={value}&ordernumber={value}&notifyurl={value}&returnurl={value}&{apikey}
		StringBuffer signSrc= new StringBuffer();
		signSrc.append(Ver+"=").append(api_response_params.get(Ver)).append("&");
		signSrc.append(partner+"=").append(api_response_params.get(partner)).append("&");
		signSrc.append(paymoney+"=").append(api_response_params.get(paymoney)).append("&");
		signSrc.append(ordernumber+"=").append(api_response_params.get(ordernumber)).append("&");
		signSrc.append(notifyurl+"=").append(api_response_params.get(notifyurl)).append("&");
		signSrc.append(returnurl+"=").append(api_response_params.get(returnurl)).append("&");
		signSrc.append(channelWrapper.getAPI_KEY());
		String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[首捷]-[请求支付]-2.生成加密URL签名完成："+JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
		payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        Map<String,String> result = Maps.newHashMap();
		if(HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper)){
            StringBuffer htmlContent = HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
            //保存第三方返回值
            result.put(HTMLCONTEXT,htmlContent.toString());
        }else{
        	String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
        	if (StringUtils.isBlank(resultStr)) {
        		log.error("[首捷]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空");
        		throw new PayException("第三方返回异常:返回空");
        	}
			if (!resultStr.contains("body")) {
				log.error("[首捷]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
				throw new PayException(resultStr);
			}
			result.put("第三方返回",resultStr); //保存全部第三方信息，上面的拆开没必要
			Elements imgs = Jsoup.parse(resultStr).select("img");
			if (null == imgs || imgs.size() < 1) {
				log.error("[首捷]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
				throw new PayException(resultStr);
			}
			String attr = imgs.get(1).attr("src");
			if (StringUtils.isBlank(attr)) {
				log.error("[首捷]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
				throw new PayException(resultStr);	
			}
			//按不同的请求接口，向不同的属性设置值
			result.put(QRCONTEXT, attr.startsWith("http") ? QRCodeUtil.decodeByUrl(attr) : QRCodeUtil.decodeByUrl("http://www.cardbankpay.com/qrcode?url="+attr));
        }
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		payResultList.add(result);
        log.debug("[首捷]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
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
        log.debug("[首捷]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}