package dc.pay.business.kk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * May 31, 2018
 */
@RequestPayHandler("KK")
public final class KKPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(KKPayRequestHandler.class);

	//字段				输入项名称			长度			属性			注释	
    //5.2.微信扫码/支付宝扫码支付申请
	//versionId			服]务版本号			1			必输			 1.0当前				
	//orderAmount		订单金额			25			必输			以分为单位				
	//orderDate			订单日期			14			必输			yyyyMMddHHmmss				
	//currency			货币类型			8			必输			RMB：人民币  其他币种代号另行提供	
	//transType			交易类别			4			必输			默认填写 0008				
	//asynNotifyUrl		异步通知URL		200			必输			结果返回URL，1.7接口用到。支付系统处理完请求后，将处理结果返回给这个URL	
	//synNotifyUrl		同步返回URL		120			必输			针对该交易的交易状态同步通知接收URL			
	//signType			加密方式			4			必输			MD5							
	//merId				商户编号			30			必输										
	//prdOrdNo			商户订单号			30			必输										
	//payMode			支付方式			50			必输			00021-支付宝扫码 00022-微信扫码00024-QQ扫码		
	//receivableType	到账类型			10			必输			D00,T01;(说明： D00为D+0,T01为T+1）			
	//prdAmt			商品价格			13			必输			以分为单位						
	//prdName			商品名称			50			必输										
	//signData			加密数据			500			必输			signTgpe为MD5时：将把所有参数按名称a-z排序,并且按key=value格式用“&”符号拼接起来,遇到key为空值的参数不参加签名，在字符串的最后还需拼接上MD5加密key，如果字符串中有中文在MD5加密时还需用UTF-8编码。	500
	private static final String versionId	  ="versionId";
	private static final String orderAmount	  ="orderAmount";
	private static final String orderDate	  ="orderDate";
	private static final String currency	  ="currency";
	private static final String transType	  ="transType";
	private static final String asynNotifyUrl ="asynNotifyUrl";
	private static final String synNotifyUrl  ="synNotifyUrl";
	private static final String signType	  ="signType";
	private static final String merId	  ="merId";
	private static final String prdOrdNo	  ="prdOrdNo";
	private static final String payMode	  ="payMode";
	private static final String receivableType  ="receivableType";
	private static final String prdAmt	  ="prdAmt";
	private static final String prdName	  ="prdName";
	//5.5.支付宝H5微信H5 及网页版
	//pnum			商品数量	必输			
	//prdDesc		商品描述	必输	
	private static final String pnum	  ="pnum";
	private static final String prdDesc	  ="prdDesc";
	
//	private static final String signature	  ="signData";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
            	put(versionId,"1.0");
            	put(orderAmount,  channelWrapper.getAPI_AMOUNT());
            	put(orderDate, HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "yyyyMMddHHmmss"));
            	put(currency,"RMB");
            	put(transType,"0008");
            	put(asynNotifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            	put(synNotifyUrl,channelWrapper.getAPI_WEB_URL());
            	put(signType,"MD5");
            	put(merId, channelWrapper.getAPI_MEMBERID());
            	put(prdOrdNo,channelWrapper.getAPI_ORDER_ID());
            	put(payMode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            	//	到账类型			10			必输			D00,T01;(说明： D00为D+0,T01为T+1）			
            	put(receivableType,"D00");
            	//			商品价格			13			必输			以分为单位
            	put(prdAmt,"1");
            	put(prdName,"name");
            	if (handlerUtil.isWapOrApp(channelWrapper)) {
            		put(pnum,"name");
            		put(prdDesc,"name");
				}
            }
        };
        log.debug("[kk]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

	 protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
        		signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
			}
        }
        signSrc.append("key=" + channelWrapper.getAPI_KEY());
		String paramsStr = signSrc.toString();
        String pay_md5sign = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[kk]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }


	protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
		payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
		HashMap<String, String> result = Maps.newHashMap();
//		if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
		if (false) {
			result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
		}else{
			String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
			if (StringUtils.isBlank(resultStr)) {
				log.error("[kk]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空"+",参数："+JSON.toJSONString(payParam));
				throw new PayException("返回空"+",参数："+JSON.toJSONString(payParam));
			}
			JSONObject resJson = JSONObject.parseObject(resultStr);
			if (resJson ==null || !resJson.containsKey("retCode") || !"1".equals(resJson.getString("retCode"))) {
				log.error("[kk]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
				throw new PayException(resultStr);
			}
			if (handlerUtil.isWapOrApp(channelWrapper)) {
			    result.put(HTMLCONTEXT, resJson.getString("htmlText"));
            }else {
                result.put(QRCONTEXT, resJson.getString("qrcode"));
            }
		}
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		payResultList.add(result);
		log.debug("[kk]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
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
        log.debug("[kk]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}