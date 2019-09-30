package dc.pay.business.xinma;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Dec 12, 2017
 */
@RequestPayHandler("XINMA")
public final class XinMaPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XinMaPayRequestHandler.class);

    //扫码属性
    private static final String QRCONTEXT = "QrContext";
	private static final String QRURL = "QrUrl";
	private static final String HTMLCONTEXT = "HtmlContext";
	//wap跳转属性
	private static final String JUMPURL = "JUMPURL";	

	//messageid	STR	200001	M	
	private static final String messageid  ="messageid";
	//out_trade_no	STR	商户接入方系统内部订单号，要求32个字符内，建议是数字、大小写字母组合，且在同一个商户号下唯一。	M	
	private static final String out_trade_no  ="out_trade_no";
	//branch_id	STR	服务方平台分配的商户号	M	
	private static final String branch_id  ="branch_id";
	//pay_type	STR	支付渠道编号	10:微信	20:支付宝		40:京东钱包		50:QQ钱包		70:银联二维码（银联钱包）	M	
	private static final String pay_type  ="pay_type";
	//total_fee	INT	订单总金额(单位：分)	M	
	private static final String total_fee  ="total_fee";
	//prod_name	STR	订单标题，这个可能会显示在客户支付的页面，但不同的支付方式显示会有差异	M	
	private static final String prod_name  ="prod_name";
	//prod_desc	STR	产品描述	M	
	private static final String prod_desc  ="prod_desc";
	//back_notify_url	STR	后台通知url，必须为直接可访问的url，不能携带参数。示例：“https://pay.weixin.qq.com/wxpay/pay.action”.		支持http跟https	M	
	private static final String back_notify_url  ="back_notify_url";
	//nonce_str	STR	随机字符串(随机字符串，不长于32位)	M	
	private static final String nonce_str  ="nonce_str";
	//front_notify_url	STR	前端通知url
	private static final String front_notify_url  ="front_notify_url";
	//bank_code	STR	银行简码 如 CCBD:建行，具体见下方银行编码列表
	private static final String bank_code  ="bank_code";
	//bank_flag	STR	0：借记卡 1：贷记卡。客户在支付之前必须选择是借记卡还是贷记卡
	private static final String bank_flag  ="bank_flag";
	//支付发起方IP，如填写错会导致跳转到支付页面后无法支付
	private static final String client_ip  ="client_ip";
	
	private static final String key  ="key";

    /**
     * 封装第三方所需要的参数
     * 
     * @return
     * @throws PayException
     * @author andrew
     * Dec 12, 2017
     */
    @Override
    protected Map<String, String> buildPayParam() throws PayException {
    	String api_CHANNEL_BANK_NAME_FlAG = channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG();
    	if (null == api_CHANNEL_BANK_NAME_FlAG || !api_CHANNEL_BANK_NAME_FlAG.contains(",") || api_CHANNEL_BANK_NAME_FlAG.split(",").length != 3) {
            log.error("[新码]-[请求支付]-1.1.组装请求参数格式：通信类型,支付渠道编号,简码。如：网关支付,银联,工商银行==>200002,30,ICBCD" );
            throw new PayException("[新码]-[请求支付]-1.1.组装请求参数格式：通信类型,支付渠道编号,简码。如：网关支付,银联,工商银行==>200002,30,ICBCD" );
		}
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(messageid, api_CHANNEL_BANK_NAME_FlAG.split(",")[0]);
                put(nonce_str, System.currentTimeMillis()+"");
                put(branch_id, channelWrapper.getAPI_MEMBERID());
                put(out_trade_no,channelWrapper.getAPI_ORDER_ID() );
                put(total_fee,  channelWrapper.getAPI_AMOUNT());
                put(prod_name,"prod_name");
                put(prod_desc,"prod_desc");
                put(back_notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(pay_type,api_CHANNEL_BANK_NAME_FlAG.split(",")[1]);
                //如果是wap
                if (channelWrapper.getAPI_CHANNEL_BANK_NAME().contains(PayEumeration.CHANNEL_TYPE._WAP_.name())) {
                	if (null != channelWrapper.getAPI_WEB_URL() && StringUtils.isNotBlank(channelWrapper.getAPI_WEB_URL())) {
                		put(front_notify_url,channelWrapper.getAPI_WEB_URL());
                	}
                	if (null != channelWrapper.getAPI_Client_IP() && StringUtils.isNotBlank(channelWrapper.getAPI_Client_IP())) {
                		put(client_ip,  channelWrapper.getAPI_Client_IP());
                	}
				}
                //如果是网银
                if (channelWrapper.getAPI_CHANNEL_BANK_NAME().contains(PayEumeration.CHANNEL_TYPE._WY_.name())) {
                	put(front_notify_url,"");
                	put(bank_code,api_CHANNEL_BANK_NAME_FlAG.split(",")[2]);
                	//0：这里只使用借记卡
                	put(bank_flag,"0");
                }
            }
        };
        log.debug("[新码]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

    /**
     * 生成签名
     * 第一步，设所有发送或者接收到的数据为集合M，将集合M内非空参数值的参数按照参数名ASCII码从小到大排序（字典序），使用URL键值对的格式（即key1=value1&key2=value2…）拼接成字符串stringA。
     *  
     * @param api_response_params
     * @return
     * @throws PayException
     * @author andrew
     * Dec 12, 2017
     */
    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
		StringBuffer signSrc= new StringBuffer();
        signSrc.append(back_notify_url+"=").append(api_response_params.get(back_notify_url)).append("&");
        //如果是网银
        if (channelWrapper.getAPI_CHANNEL_BANK_NAME().contains(PayEumeration.CHANNEL_TYPE._WY_.name())) {
        	signSrc.append(bank_code+"=").append(api_response_params.get(bank_code)).append("&");
        	signSrc.append(bank_flag+"=").append(api_response_params.get(bank_flag)).append("&");
        }
        signSrc.append(branch_id+"=").append(api_response_params.get(branch_id)).append("&");
        //如果是wap
        if (channelWrapper.getAPI_CHANNEL_BANK_NAME().contains(PayEumeration.CHANNEL_TYPE._WAP_.name())) {
        	if (null != api_response_params.get(client_ip) && StringUtils.isNotBlank(api_response_params.get(client_ip))) {
        		signSrc.append(client_ip+"=").append(api_response_params.get(client_ip)).append("&");
        	}
        	if (null != api_response_params.get(front_notify_url) && StringUtils.isNotBlank(api_response_params.get(front_notify_url))) {
        		signSrc.append(front_notify_url+"=").append(api_response_params.get(front_notify_url)).append("&");
        	}
        }
        signSrc.append(messageid+"=").append(api_response_params.get(messageid)).append("&");
        signSrc.append(nonce_str+"=").append(api_response_params.get(nonce_str)).append("&");
        signSrc.append(out_trade_no+"=").append(api_response_params.get(out_trade_no)).append("&");
        signSrc.append(pay_type+"=").append(api_response_params.get(pay_type)).append("&");
        signSrc.append(prod_desc+"=").append(api_response_params.get(prod_desc)).append("&");
        signSrc.append(prod_name+"=").append(api_response_params.get(prod_name)).append("&");
        signSrc.append(total_fee+"=").append(api_response_params.get(total_fee)).append("&");
    	signSrc.append(key+"=").append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[通扫]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5)+"，参数："+JSON.toJSONString(paramsStr));
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
     * Dec 12, 2017
     */
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        List<Map<String, String>> payResultList = Lists.newArrayList();
        String api_channel_bank_name = channelWrapper.getAPI_CHANNEL_BANK_NAME();
        String api_CHANNEL_BANK_URL = channelWrapper.getAPI_CHANNEL_BANK_URL();
        Map<String,String> result = Maps.newHashMap();
//        if(api_channel_bank_name.contains(PayEumeration.CHANNEL_TYPE._WY_.name())){
        //14:41:55	新码支付技术服务:	网银不支持
        if(false){
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
        		resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam);
        		if (null == resultStr || StringUtils.isBlank(resultStr)) {
        			log.error("[新码]3.1.发送支付请求，获取支付请求返回值异常:返回空");
        			throw new PayException("返回空");
        		}
        		resultStr = new String(resultStr.getBytes("ISO-8859-1"), "UTF-8");
        	} catch (Exception e) {
        		log.error("[新码]3.3.发送支付请求，及获取支付请求出错，订单号：" + channelWrapper.getAPI_ORDER_ID() + ",通道名称：" + api_channel_bank_name + ",postUrl:" + api_CHANNEL_BANK_URL + ",payForManagerCGIResultJsonObj" + e.getMessage(), e);
        		throw new PayException(e.getMessage(),e);
        	}
        	if (!resultStr.contains(":") || !resultStr.contains("{")) {
        		log.error("[新码]3.2.发送支付请求，获取支付请求返回值异常:"+resultStr);
        		throw new PayException(resultStr);
        	}
        	JSONObject jsonObject = JSONObject.parseObject(resultStr);
        	//resultCode		返回系统编码(00表示成功,其余表示失败). 此字段是通信标识，非交易标识，交易是否成功需要查看resCode来判断	
        	//resCode	STR	返回业务编码(00表示成功,其余表示失败)
        	if ( jsonObject.containsKey("resultCode") && (StringUtils.isBlank(jsonObject.getString("resultCode")) || !"00".equals(jsonObject.getString("resultCode").toString().trim()))) {
        		log.error("[新码]3.3.发送支付请求，获取支付请求返回值异常:"+resultStr);
        		throw new PayException(resultStr);
        	}
        	if ( jsonObject.containsKey("resCode") && (StringUtils.isBlank(jsonObject.getString("resCode")) || !"00".equals(jsonObject.getString("resCode").toString().trim()))) {
        		log.error("[新码]3.4.发送支付请求，获取支付请求返回值异常:"+resultStr);
        		throw new PayException(resultStr);
        	}
        	//按不同的请求接口，向不同的属性设置值
        	if(HandlerUtil.isFS(channelWrapper)|| channelWrapper.getAPI_CHANNEL_BANK_NAME().contains(PayEumeration.CHANNEL_TYPE._WAP_.name()) || channelWrapper.getAPI_CHANNEL_BANK_NAME().contains(PayEumeration.CHANNEL_TYPE._WAPAPP_.name())) {
        		result.put(JUMPURL, jsonObject.getString("payUrl"));
        	}else if (channelWrapper.getAPI_CHANNEL_BANK_NAME().contains(PayEumeration.CHANNEL_TYPE._WEB_.name())) {
        		result.put(HTMLCONTEXT, jsonObject.getString("payUrl"));
        	}else{
        		result.put(QRCONTEXT, jsonObject.getString("payUrl"));
        	}
        	result.put("第三方返回",resultStr); //保存全部第三方信息，上面的拆开没必要
        	payResultList.add(result);
        }
        log.debug("[新码]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
        return payResultList;
    }

    /**
     * 生成返回给前端使用的值
     * 
     * @param resultListMap
     * @return
     * @throws PayException
     * @author andrew
     * Dec 12, 2017
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
        log.debug("[新码]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}