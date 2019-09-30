package dc.pay.business.houjiezhifu;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import dc.pay.utils.*;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayRequestHandler;
import dc.pay.business.RequestPayResult;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;

/**
 * @author Cobby
 * Jan 31, 2019
 */
@RequestPayHandler("HOUJIEZHIFU")
public final class HouJieZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(HouJieZhiFuPayRequestHandler.class);

    private static final String appId                       ="appId";//  商家的唯⼀ID （必填）
    private static final String apiKey                      ="apiKey";//  API key, 在JRDiDi服务器后台设置
    private static final String inputCharset                ="inputCharset";//  传参内容的字符编码（⾮必填，默认为UTF-8。某些系统内部是⾮UTF-8编码，需特	殊指定，防⽌乱码）
    private static final String apiVersion                  ="apiVersion";//  API版本号（必填，默认为1.0）
    private static final String appSignType                 ="appSignType";//  签名算法名称(必填，默认为 HMAC-SHA256 )
    private static final String appSignContent              ="appSignContent";//  签名之后的签名内容
    private static final String appUserId                   ="appUserId";
    private static final String appOrderId                  ="appOrderId";//由商家系统内部⽣成的订单ID
    private static final String orderAmount                 ="orderAmount";  //本次订单中下单的⾦额或币的数量
    private static final String orderCoinSymbol             ="orderCoinSymbol";//订单⾦额单位符号，必须提供和平台约定好的币种代码，否则下单会失败 （必	填，默认为CNY，⼈⺠币）
    private static final String orderPayTypeId              ="orderPayTypeId";  //本次订单中⽤户主动选择使⽤的付款⽅式
    private static final String appServerNotifyUrl          ="appServerNotifyUrl";//必填。由商家提供的服务器端负责接收订单信息更新异步通知的接⼝
    private static final String appReturnPageUrl            ="appReturnPageUrl";  //必填。在⽤户⽀付完成之后，JRDiDi会根据商家传⼊的appReturnPageUrl参
    private static final String orderRemark                 ="orderRemark";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
        if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 2) {
            log.error("[后捷支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：appId&apiKey" );
            throw new PayException("[后捷支付]-[请求支付]-“支付通道商号”输入数据格式为【appId&apiKey】：appId&apiKey" );
        }
	    String uID = HandlerUtil.randomStr(6);
	    String oRemark = HandlerUtil.randomStr(6);
	    Map<String, String> payParam = new TreeMap<String, String>() {
            {
	            put(appId,aPI_MEMBERID.split("&")[0]);
	            put(apiKey, aPI_MEMBERID.split("&")[1]);
                put(inputCharset, "UTF-8");
	            put(apiVersion,"1.1");
	            put(appSignType,"HMAC-SHA256");
	            put(appUserId,uID);
	            put(appOrderId,channelWrapper.getAPI_ORDER_ID());
	            put(orderAmount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
	            put(orderCoinSymbol,"CNY");
	            put(orderPayTypeId,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            put(appServerNotifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
	            put(appReturnPageUrl,channelWrapper.getAPI_WEB_URL());
	            put(orderRemark,oRemark);
            }
        };

	    Map<String, String> param = new TreeMap<String, String>() {
		    {
			    put(appUserId,uID);
			    put(appOrderId,channelWrapper.getAPI_ORDER_ID());
			    put(orderAmount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
			    put(orderCoinSymbol,"CNY");
			    put(orderPayTypeId,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
			    put(appServerNotifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
			    put(appReturnPageUrl,channelWrapper.getAPI_WEB_URL());
			    put(orderRemark,oRemark);
		    }
	    };
	    payParam.put("param",JSON.toJSONString(param));
        log.debug("[后捷支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> params) throws PayException {
	     List paramKeys = MapUtils.sortMapByKeyAsc(params);
	     StringBuilder signSrc = new StringBuilder();
	     for (int i = 0; i < paramKeys.size(); i++) {
		     if (!"param".equals(paramKeys.get(i)) && StringUtils.isNotBlank(params.get(paramKeys.get(i)))) {
//			     signSrc.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i))).append("&");
			     try {
				     String encode="";
				     if (StringUtils.isNotBlank(String.valueOf(params.get(paramKeys.get(i))))){
					     encode = URLEncoder.encode(String.valueOf(params.get(paramKeys.get(i))), "UTF-8");
				     }
				     signSrc.append(paramKeys.get(i)).append("=").append(encode).append("&");
			     } catch (UnsupportedEncodingException e) {
				     e.printStackTrace();
			     }
		     }
	     }
        //删除最后一个字符
        signSrc.deleteCharAt(signSrc.length()-1);
        String paramsStr = signSrc.toString();
        String signMd5 = HmacSha256Util.digest(paramsStr, channelWrapper.getAPI_KEY()).toLowerCase();
        log.debug("[后捷支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
//	    https://jrdidi.com/order/deposit/create?appId=[由JRDiDi平台分配的appId]&apiKey=[由JRDiDi平台分配的apiKey]
//&inputCharset=UTF-8&apiVersion=1.0&appSignType=HMAC-SHA256&appSignContent=[签名内容]
	    String url = channelWrapper.getAPI_CHANNEL_BANK_URL();
	    String paramsStr = String.format("?appId=%s&apiKey=%s&inputCharset=%s&apiVersion=%s&appSignType=%s&appSignContent=%s",
			    payParam.get(appId),
			    payParam.get(apiKey),
			    payParam.get(inputCharset),
			    payParam.get(apiVersion),
			    payParam.get(appSignType),
			    pay_md5sign
	    );
	    url = url+paramsStr;
	    Map param = (Map)JSON.parse(payParam.get("param"));
	    HashMap<String, String> result = Maps.newHashMap();

        try {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(url,param).toString());
        } catch (Exception e) {
            log.error("[后捷支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }

        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[后捷支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[后捷支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}