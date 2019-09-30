package dc.pay.business.dongfangkuaifu;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;
import dc.pay.utils.qr.QRCodeUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Dec 5, 2017
 */
@RequestPayHandler("DONGFANGKUAIFU")
public final class DongFangKuaiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(DongFangKuaiFuPayRequestHandler.class);

    //扫码属性
    private static final String QRCONTEXT = "QrContext";
	private static final String QRURL = "QrUrl";
	private static final String HTMLCONTEXT = "HtmlContext";
	//wap跳转属性
	private static final String JUMPURL = "JUMPURL";
	
	//商户ID	pay_memberid		String	必填	是	商户后台可查看商户ID
	private static final String pay_memberid  ="pay_memberid";
	//订单号	pay_orderid			String	必填	是	唯一性
	private static final String pay_orderid  ="pay_orderid";
	//支付金额	pay_amount			String	必填	是	单位：元
	private static final String pay_amount  ="pay_amount";
	//支付时间	pay_applydate		String	必填	是	时间格式:2017-11-17 00:00:00
	private static final String pay_applydate  ="pay_applydate";
	//异步通知地址pay_notifyurl		String	必填	是	将按填写的地址发送通知
	private static final String pay_notifyurl  ="pay_notifyurl";
	//跳转地址	pay_callbackurl		String	必填	是	支付成功跳转地址
	private static final String pay_callbackurl  ="pay_callbackurl";
	//银行编码	pay_bankcode		String	必填	是	参考附录
	private static final String pay_bankcode  ="pay_bankcode";
	private static final String pay_bankname  ="pay_bankname";
	//产品名称	pay_productname		String	必填	否	商品名称
	private static final String pay_productname  ="pay_productname";
	private static final String client_ip  ="client_ip";
	private static final String key  ="key";

    /**
     * 封装第三方所需要的参数
     * 
     * @return
     * @throws PayException
     * @author andrew
     * Jan 5, 2018
     */
    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(pay_memberid, channelWrapper.getAPI_MEMBERID());
                put(pay_orderid,channelWrapper.getAPI_ORDER_ID());
                put(pay_amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(pay_applydate,  HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "yyyy-MM-dd HH:mm:ss"));
                put(pay_notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(pay_callbackurl,channelWrapper.getAPI_WEB_URL());
                if (channelWrapper.getAPI_CHANNEL_BANK_NAME().contains(PayEumeration.CHANNEL_TYPE._WY_+"")) {
                	put(pay_bankcode,"907");
                	put(pay_bankname,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
				}else{
					put(pay_bankcode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
				}
                put(pay_productname,"pay_productname");
                //支付方式为微信H5时，必填。其他支付方式不填，为用户真实IP地址
                if ("901".equalsIgnoreCase(channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG())) {
                	put(client_ip,HandlerUtil.getRandomIp(channelWrapper));
				}
            }
        };
        log.debug("[东方快付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

    /**
     * 生成签名
     * 
	 * @param api_response_params
	 * @return
	 * @throws PayException
	 * @author andrew
	 * Jan 5, 2018
	 */
	protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
	        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
	        StringBuilder signSrc = new StringBuilder();
	        for (int i = 0; i < paramKeys.size(); i++) {
	        	if (null != api_response_params.get(paramKeys.get(i)) && pay_productname != paramKeys.get(i) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i))) && client_ip != paramKeys.get(i) && pay_bankname != paramKeys.get(i)) {
	        		signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
				}
	        }
	        signSrc.append(key+"=" + channelWrapper.getAPI_KEY());
			String paramsStr = signSrc.toString();
	        String pay_md5sign = HandlerUtil.getMD5UpperCase(paramsStr);
	        log.debug("[东方快付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign)+"参数："+paramsStr);
	        return pay_md5sign;
	}

    /**
     * 生成返回给RequestPayResult对象detail字段的值
     * 
     * @param payParam
     * @param pay_md5sign
     * @return
     * @throws PayException
     * @author andrew
     * Jan 5, 2018
     */
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        List<Map<String, String>> payResultList = Lists.newArrayList();
        String api_channel_bank_name = channelWrapper.getAPI_CHANNEL_BANK_NAME();
        String api_CHANNEL_BANK_URL = channelWrapper.getAPI_CHANNEL_BANK_URL();
        Map<String,String> result = Maps.newHashMap();
        if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper)) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            payResultList.add(result);
        }else{
    		String tmpStr ;
        	Element bodyEl ;
        	try {
        		tmpStr = RestTemplateUtil.postForm(api_CHANNEL_BANK_URL, payParam,"UTF-8");
                if (null == tmpStr || StringUtils.isBlank(tmpStr)) {
                	log.error("[东方快付]3.1.发送支付请求，获取支付请求返回值异常:返回空");
                	throw new PayException("返回空");
                }
                Document document = Jsoup.parse(tmpStr);
                bodyEl = document.getElementsByTag("body").first();
        	} catch (Exception e) {
        		log.error("[东方快付]3.2.发送支付请求，及获取支付请求出错，订单号：" + channelWrapper.getAPI_ORDER_ID() + ",通道名称：" + api_channel_bank_name + ",postUrl:" + api_CHANNEL_BANK_URL + ",payForManagerCGIResultJsonObj" + e.getMessage(), e);
        		throw new PayException(e.getMessage(),e);
        	}
            if (bodyEl.html().contains("error")) {
                log.error("[东方快付]3.3.发送支付请求，及获取支付请求出错，订单号：" + channelWrapper.getAPI_ORDER_ID() + ",通道名称：" + api_channel_bank_name + ",postUrl:" + api_CHANNEL_BANK_URL + ",payForManagerCGIResultJsonObj" + bodyEl.html());
                throw new PayException(bodyEl.html());
            }
            Elements selects = bodyEl.select("form img ");
            if (null == selects || selects.size() < 2) {
            	log.error("[东方快付]3.4.发送支付请求，及获取支付请求出错，订单号：" + channelWrapper.getAPI_ORDER_ID() + ",通道名称：" + api_channel_bank_name + ",postUrl:" + api_CHANNEL_BANK_URL + ",payForManagerCGIResultJsonObj" + bodyEl.html());
            	throw new PayException(bodyEl.html());
			}
            //第二个标签才有需要的值
            String attr = selects.get(1).attr("src");
            if (null == attr || StringUtils.isBlank(attr)){
            	log.error("[东方快付]3.4.发送支付请求，获取支付请求返回值异常:src返回空");
            	throw new PayException("src返回空");
			}
            //按不同的请求接口，向不同的属性设置值
            if(HandlerUtil.isWapOrApp(channelWrapper)) {
            	result.put(JUMPURL, attr);
            }else{
            	result.put(QRCONTEXT, QRCodeUtil.decodeByUrl(attr));
            }
            result.put("第三方返回",tmpStr); //保存全部第三方信息，上面的拆开没必要
            payResultList.add(result);
        }
        log.debug("[东方快付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
        return payResultList;
    }

    /**
     * 生成返回给前端使用的值
     * 
     * @param resultListMap
     * @return
     * @throws PayException
     * @author andrew
     * Jan 5, 2018
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
        log.debug("[东方快付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}