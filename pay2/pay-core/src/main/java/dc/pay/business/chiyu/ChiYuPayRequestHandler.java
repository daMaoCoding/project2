package dc.pay.business.chiyu;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSON;
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
import dc.pay.utils.qr.QRCodeUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Dec 21, 2017
 */
@RequestPayHandler("CHIYU")
public final class ChiYuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(ChiYuPayRequestHandler.class);

    private static final String QRCONTEXT = "QrContext";
	private static final String QRURL = "QrUrl";
	private static final String HTMLCONTEXT = "HtmlContext";
	private static final String PARSEHTML = "parseHtml";

	//pay_memberid	商户ID	是	 
	private static final String pay_memberid  ="pay_memberid";
	//pay_orderid	订单号	是	可以为空，为空时系统自动生成订单号，如果不为空请保证订单号不重复，此字段可以为空，但必须参加加密
	private static final String pay_orderid  ="pay_orderid";
	//pay_amount	金额	是	订单金额，单位：元，精确到分
	private static final String pay_amount  ="pay_amount";
	//pay_applydate	订单提交时间	是	订单提交的时间: 如： 2014-12-26 18:18:18
	private static final String pay_applydate  ="pay_applydate";
	//pay_bankcode	银行编号	是	银行编码
	private static final String pay_bankcode  ="pay_bankcode";
	//pay_notifyurl	服务端返回地址	是	服务端返回地址.（POST返回数据）
	private static final String pay_notifyurl  ="pay_notifyurl";
	//pay_callbackurl	 页面返回地址	是	页面跳转返回地址（POST返回数据）
	private static final String pay_callbackurl  ="pay_callbackurl";
	private static final String tongdao  ="tongdao";
	
	private static final String key  ="key";
    /**
     * 封装第三方所需要的参数
     * 
     * @return
     * @throws PayException
     * @author andrew
     * Dec 21, 2017
     */
    @Override
    protected Map<String, String> buildPayParam() throws PayException {
    	String api_CHANNEL_BANK_NAME_FlAG = channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG();
    	if (null == api_CHANNEL_BANK_NAME_FlAG || !api_CHANNEL_BANK_NAME_FlAG.contains(",") || api_CHANNEL_BANK_NAME_FlAG.split(",").length != 2) {
            log.error("[驰誉]-[请求支付]-1.1.组装请求参数格式：通道编码,支付渠道。如：通道编码,微信==>ShangYinXinWxSm,WXZF" );
            throw new PayException("[驰誉]-[请求支付]-1.1.组装请求参数格式：通道编码,支付渠道。如：通道编码,微信==>ShangYinXinWxSm,WXZF" );
		}
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(pay_memberid, channelWrapper.getAPI_MEMBERID());
                put(pay_orderid,channelWrapper.getAPI_ORDER_ID() );
                put(pay_amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(pay_notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(pay_applydate,DateUtil.formatDateTimeStrByParam("yyyy-MM-dd HH:mm:ss"));
                put(pay_callbackurl,"http://api.968cn.com/api/page.php");
                put(pay_bankcode,api_CHANNEL_BANK_NAME_FlAG.split(",")[1]);
                put(tongdao,api_CHANNEL_BANK_NAME_FlAG.split(",")[0]);
            }
        };
        log.debug("[驰誉]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

    /**
     * 生成签名
     * 
     * @param api_response_params
     * @return
     * @throws PayException
     * @author andrew
     * Dec 21, 2017
     */
    protected String buildPaySign(Map api_response_params) throws PayException {
    	//1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
    	//2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
		StringBuffer signSrc= new StringBuffer();
		signSrc.append(pay_amount+"=>").append(api_response_params.get(pay_amount)).append("&");
		signSrc.append(pay_applydate+"=>").append(api_response_params.get(pay_applydate)).append("&");
		signSrc.append(pay_bankcode+"=>").append(api_response_params.get(pay_bankcode)).append("&");
		signSrc.append(pay_callbackurl+"=>").append(api_response_params.get(pay_callbackurl)).append("&");
		signSrc.append(pay_memberid+"=>").append(api_response_params.get(pay_memberid)).append("&");
		signSrc.append(pay_notifyurl+"=>").append(api_response_params.get(pay_notifyurl)).append("&");
        signSrc.append(pay_orderid+"=>").append(api_response_params.get(pay_orderid)).append("&");
    	signSrc.append(key+"=").append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[驰誉]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
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
     * Dec 21, 2017
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
                	log.error("[驰誉]3.1.发送支付请求，获取支付请求返回值异常:返回空");
                	throw new PayException("第三方返回异常:返回空");
                }
                //如果返回的文档没有script属性，表明有异常
                if (tmpStr.contains("body")) {
                	Document document = Jsoup.parse(tmpStr);
                	Element bodyEl = document.getElementsByTag("body").first();
                	String decode = QRCodeUtil.decodeByUrl("http://api.968cn.com/"+bodyEl.select("img").attr("src"));
                	result.put(QRCONTEXT, decode);
                	result.put("第三方返回",tmpStr); //保存全部第三方信息，上面的拆开没必要
                	payResultList.add(result);
				}else {
					tmpStr = replaceBlank(tmpStr);
					//如果包含代码为FAIL，表明失败
					if (tmpStr.contains("FAIL") && tmpStr.contains("reCode")) {
						if (tmpStr.contains("message")) {
							String[] messages = tmpStr.split("message");
							String[] myMsgs = messages[1].split("\"");
							log.error("[驰誉]3.2.发送支付请求，获取支付请求返回值异常:"+HandlerUtil.UrlDecode(myMsgs[2]));
							throw new PayException(HandlerUtil.UrlDecode(myMsgs[2]));
						}
						log.error("[驰誉]3.3.发送支付请求，获取支付请求返回值异常:无描述");
						throw new PayException("无描述");
					}
					if (tmpStr.contains("br")) {
						log.error("[驰誉]3.4.发送支付请求，获取支付请求返回值异常:"+tmpStr);
						throw new PayException(tmpStr);
					}
				}
        	} catch (Exception e) {
        		log.error("[驰誉]3.发送支付请求，及获取支付请求出错，订单号：" + channelWrapper.getAPI_ORDER_ID() + ",通道名称：" + api_channel_bank_name + ",postUrl:" + api_CHANNEL_BANK_URL + ",payForManagerCGIResultJsonObj" + e.getMessage(), e);
        		throw new PayException(e.getMessage(),e);
        	}
        }
        log.debug("[驰誉]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
        return payResultList;
    }
    
    public static String replaceBlank(String str) {
		String dest = "";
		if (str!=null) {
			Pattern p = Pattern.compile("\\s*|\t|\r|\n");
			Matcher m = p.matcher(str);
			dest = m.replaceAll("");
		}
		return dest;
	}
    
    /**
     * 生成返回给前端使用的值
     * 
     * @param resultListMap
     * @return
     * @throws PayException
     * @author andrew
     * Dec 21, 2017
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
        log.debug("[驰誉]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}