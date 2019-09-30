package dc.pay.business.tongsao;

import java.util.LinkedHashMap;
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
 * Dec 5, 2017
 */
@RequestPayHandler("TONGSAO")
public final class TongSaoRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(TongSaoRequestHandler.class);

	//wap跳转属性
	private static final String JUMPURL = "JUMPURL";
	
	//merchno	商户号	15	是	　
	private static final String merchno  ="merchno";
	//amount	交易金额	12	是	单位/元
	private static final String amount  ="amount";
	//traceno	商户流水号	32	是	由商户网站系统产生的订单号，订单号不可重复。
	private static final String traceno  ="traceno";
	//payType	支付方式	1	是	1-支付宝 	2-微信	3-百度钱包 （待开发）	4-QQ钱包	5-京东钱包
	private static final String paytype  ="payType";
	//goodsName	商品名称	30	是	默认取商户名称
	private static final String goodsname  ="goodsName";
	//notifyUrl	通知地址	50	否	交易成功，则给商户发送异步通知。
	private static final String notifyurl  ="notifyUrl";
	//默认T+1
	private static final String settletype  ="settleType";
	//signature	数据签名	32	是	　
	private static final String signature  ="signature";

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
                put(merchno, channelWrapper.getAPI_MEMBERID());
                put(goodsname,"goodsName");
                put(amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(paytype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(traceno,channelWrapper.getAPI_ORDER_ID());
                put(settletype,"1");
            }
        };
        log.debug("[通扫]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

    /**
     * 生成签名
     * 
     * @param api_response_params
     * @return
     * @throws PayException
     * @author andrew
     * Dec 5, 2017
     */
    protected String buildPaySign(Map api_response_params) throws PayException {
    	//1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
    	//2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
        String paramsStr = String.format(amount+"=%s&"+goodsname+"=%s&"+merchno+"=%s&"+notifyurl+"=%s&"+paytype+"=%s&"+settletype+"=%s&"+traceno+"=%s&%s",
                api_response_params.get(amount),
                api_response_params.get(goodsname),
                api_response_params.get(merchno),
                api_response_params.get(notifyurl),
                api_response_params.get(paytype),
                api_response_params.get(settletype),
                api_response_params.get(traceno),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
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
     * Dec 5, 2017
     */
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
    	Map<String,String> map = new LinkedHashMap<>();
    	map.put(amount, payParam.get(amount));
    	map.put(goodsname, payParam.get(goodsname));
    	map.put(merchno, payParam.get(merchno));
    	map.put(notifyurl, payParam.get(notifyurl));
    	map.put(paytype, payParam.get(paytype));
    	map.put(traceno, payParam.get(traceno));
    	map.put(settletype, payParam.get(settletype));
    	//signature接在交易串的最后面
    	map.put(signature, pay_md5sign);
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
                tmpStr = RestTemplateUtil.postForm(api_CHANNEL_BANK_URL, map,"UTF-8").trim();
                if (null == tmpStr || StringUtils.isBlank(tmpStr)) {
                	log.error("[通扫]3.1.发送支付请求，获取支付请求返回值异常:返回空");
                	throw new PayException("返回空");
                }
                tmpStr = new String(tmpStr.getBytes("ISO-8859-1"), "GBK");
        	} catch (Exception e) {
        		log.error("[通扫]3.3.发送支付请求，及获取支付请求出错，订单号：" + channelWrapper.getAPI_ORDER_ID() + ",通道名称：" + api_channel_bank_name + ",postUrl:" + api_CHANNEL_BANK_URL + ",payForManagerCGIResultJsonObj" + e.getMessage(), e);
        		throw new PayException(e.getMessage(),e);
        	}
            JSONObject jsonObject = StringUtils.isNotBlank(tmpStr) ? JSONObject.parseObject(tmpStr) : null;
            //支付成功返回：00；支付失败则返回其它
            if (null == jsonObject || (jsonObject.containsKey("respCode")  && !"00".equals(jsonObject.getString("respCode")))) {
            	 log.error("[通扫]3.2.发送支付请求，获取支付请求返回值异常:"+jsonObject.getString("message"));
                 throw new PayException(jsonObject.getString("message"));
            }
            //按不同的请求接口，向不同的属性设置值
            if(channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("_WAP_")||channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("_WAPAPP_")) {
            	result.put(JUMPURL, jsonObject.getString("barCode"));
            }else{
            	result.put(QRCONTEXT, jsonObject.getString("barCode"));
            }
            result.put("第三方返回",tmpStr); //保存全部第三方信息，上面的拆开没必要
            payResultList.add(result);
        }
        log.debug("[通扫]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
        return payResultList;
    }

    /**
     * 生成返回给前端使用的值
     * 
     * @param resultListMap
     * @return
     * @throws PayException
     * @author andrew
     * Dec 5, 2017
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
        log.debug("[通扫]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}