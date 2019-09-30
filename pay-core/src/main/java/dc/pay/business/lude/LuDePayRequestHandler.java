package dc.pay.business.lude;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayRequestHandler;
import dc.pay.business.RequestPayResult;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.XmlUtil;
import dc.pay.utils.*;
import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.util.*;

/**
 * 
 * @author andrew
 * Feb 5, 2018
 */
@RequestPayHandler("LUDEZHIFU")
public final class LuDePayRequestHandler extends PayRequestHandler {
	private static final Logger log = LoggerFactory.getLogger(LuDePayRequestHandler.class);
    private static final String  service        = "service";        //B2C方式：“TRADE.B2C”,B2B方式：“TRADE.B2B”  | “TRADE.SCANPAY”  |“TRADE.H5PAY”
    private static final String  version        = "version";        //1.0.0.0
    private static final String  merId          = "merId";          //商户在支付平台的唯一标识
    private static final String  tradeNo        = "tradeNo";        //商户系统产生的唯一订单号
    private static final String  tradeDate      = "tradeDate";      //商户系统生成的订单日期 格式：YYYYMMDD（年[4位]月[2位]日[2位]）
    private static final String  amount         = "amount";         //以“元”为单位
    private static final String  notifyUrl      = "notifyUrl";      //页面通知和服务器通知地址二合一,
    private static final String  summary        = "summary";        //交易摘要//交易摘要
    private static final String  expireTime        = "expireTime";
    private static final String  extra        = "extra";
    private static final String  clientIp       = "clientIp";       //付款人IP 地址
    private static final String  bankId         = "bankId";         //银行代码
    private static final String  typeId         = "typeId";         //1；支付宝 2；微信 3；QQ钱包 4；银联扫码


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<>();
        if(HandlerUtil.isWapOrApp(channelWrapper)){
            payParam.put(service , "TRADE.H5PAY");
            payParam.put(typeId,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
        }else if(channelWrapper.getAPI_CHANNEL_BANK_NAME().endsWith("_SM")){
             payParam.put(service, "TRADE.SCANPAY");
             payParam.put(typeId,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
        }else {
            payParam.put(service , "TRADE.B2C");
            payParam.put(bankId , channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
        }
        payParam.put(version , "1.0.0.0");
        payParam.put(merId , channelWrapper.getAPI_MEMBERID());
        payParam.put(tradeNo ,channelWrapper.getAPI_ORDER_ID() );
//        payParam.put(tradeNo ,System.currentTimeMillis()+"");
        payParam.put(tradeDate , DateUtil.formatDateTimeStrByParam("yyyyMMdd"));
        payParam.put(amount , HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
        payParam.put(notifyUrl ,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL() );
        payParam.put(summary , "PAY");
        payParam.put(expireTime , "");
        payParam.put(extra , "PAY");
        //payParam.put(clientIp , HandlerUtil.getRandomIp());
        payParam.put(clientIp , channelWrapper.getAPI_Client_IP());

        log.debug("[路德]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

    @Override
	 protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
	        StringBuilder signSrc = new StringBuilder();
	        if(channelWrapper.getAPI_CHANNEL_BANK_NAME().endsWith("_SM")){
	        	signSrc.append(service).append("=").append(api_response_params.get(service)).append("&");
	        	signSrc.append(version).append("=").append(api_response_params.get(version)).append("&");
	        	signSrc.append(merId).append("=").append(api_response_params.get(merId)).append("&");
	        	signSrc.append(typeId).append("=").append(api_response_params.get(typeId)).append("&");
	        	signSrc.append(tradeNo).append("=").append(api_response_params.get(tradeNo)).append("&");
	        	signSrc.append(tradeDate).append("=").append(api_response_params.get(tradeDate)).append("&");
	        	signSrc.append(amount).append("=").append(api_response_params.get(amount)).append("&");
	        	signSrc.append(notifyUrl).append("=").append(api_response_params.get(notifyUrl)).append("&");
	        	signSrc.append(extra).append("=").append(api_response_params.get(extra)).append("&");
	        	signSrc.append(summary).append("=").append(api_response_params.get(summary)).append("&");
	        	signSrc.append(expireTime).append("=").append(api_response_params.get(expireTime)).append("&");
	        	if (StringUtils.isNotBlank(api_response_params.get(clientIp))) {
	        		signSrc.append(clientIp).append("=").append(api_response_params.get(clientIp));
				}
			}else {
				signSrc.append(service).append("=").append(api_response_params.get(service)).append("&");
				signSrc.append(version).append("=").append(api_response_params.get(version)).append("&");
				signSrc.append(merId).append("=").append(api_response_params.get(merId)).append("&");
				signSrc.append(tradeNo).append("=").append(api_response_params.get(tradeNo)).append("&");
				signSrc.append(tradeDate).append("=").append(api_response_params.get(tradeDate)).append("&");
				signSrc.append(amount).append("=").append(api_response_params.get(amount)).append("&");
				signSrc.append(notifyUrl).append("=").append(api_response_params.get(notifyUrl)).append("&");
				signSrc.append(extra).append("=").append(api_response_params.get(extra)).append("&");
				signSrc.append(summary).append("=").append(api_response_params.get(summary)).append("&");
				signSrc.append(expireTime).append("=").append(api_response_params.get(expireTime)).append("&");
	        	if (StringUtils.isNotBlank(api_response_params.get(clientIp))) {
	        		signSrc.append(clientIp).append("=").append(api_response_params.get(clientIp)).append("&");
				}
				signSrc.append(bankId).append("=").append(api_response_params.get(bankId));
			}
	        signSrc.append(channelWrapper.getAPI_KEY());
			String paramsStr = signSrc.toString();
	        String pay_md5sign = HandlerUtil.getMD5UpperCase(paramsStr);
	        log.debug("[路德]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
	        return pay_md5sign;
	}
    
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
    	HashMap<String, String> result = Maps.newHashMap();
    	if(HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper)){
			StringBuffer htmlContent = HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
			//保存第三方返回值
			result.put(HTMLCONTEXT,htmlContent.toString());
        }else{
			String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
            if (null == resultStr || StringUtils.isBlank(resultStr)) {
            	log.error("[路德]3.1.发送支付请求，获取支付请求返回值异常:返回空");
            	throw new PayException("第三方返回异常:返回空");
            }
			if (resultStr.contains("icon-error1")) {
			    log.error("发送支付请求，及获取支付请求结果错误："+resultStr);
			    throw new PayException(resultStr );
			}
			if(resultStr.contains("html") && channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("_WAP_")||channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("_WAPAPP_")) {
				Document document = Jsoup.parse(resultStr);
				Element aEl = document.getElementsByTag("a").first();
				if(null == aEl || StringUtils.isBlank(aEl.attr("href"))){
				    log.error("发送支付请求，及获取支付请求结果错误："+resultStr);
				    throw new PayException(resultStr );
				}
				result.put(JUMPURL, aEl.attr("href"));
			}else{
			    Map<String, String> mapBodys = Maps.newHashMap();
			    try {
			    	XmlUtil.parse(resultStr, mapBodys);
				} catch (Exception e) {
		            log.error("[路德]3.发送支付请求，及获取支付请求结果出错：", e);
		            throw new PayException(e.getMessage(), e);
				}
				String qrCode = mapBodys.get("/message/detail/qrCode");
				if(!"00".equalsIgnoreCase(mapBodys.get("/message/detail/code"))){
	            	 log.error("[路德]3.2.发送支付请求，获取支付请求返回值异常:"+resultStr);
	                 throw new PayException(resultStr);
			    }
				result.put(QRCONTEXT, new String(Base64.getDecoder().decode(qrCode)));
			}
			result.put(PARSEHTML, resultStr);
        }
    	payResultList.add(result);
        log.debug("[路德]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
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
        log.debug("[路德]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}