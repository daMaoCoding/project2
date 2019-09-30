package dc.pay.business.sssfu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
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
import dc.pay.utils.UnicodeUtil;
import dc.pay.utils.ValidateUtil;
import dc.pay.utils.qr.QRCodeUtil;


/**
 * 
 * 
 * @author sunny
 * Dec 25, 2018
 */
@RequestPayHandler("SSSFU")
public final class SssFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(SssFuPayRequestHandler.class);

//    键名				必填			描述							最大长度
//    version			真			协议版本						4
//    chennelID			真			渠道id						8
//    money				真			申请付款金额					8
//    orderNum			真			C方提供的订单号				16
//    time				真	C方发起时间,不做验证,防止重复提交,可为空	24
//    back_url			真	C方提供的通知地址，用于接收订单状态改变时的通知	100
//    sign				真	数据签名 md5(具体签名算法参照下方的签名算法)	32

  private static final String version                 ="version";
  private static final String agentId                 ="agentId";
  private static final String agentOrderId            ="agentOrderId";
  private static final String payType           	  ="payType";
  private static final String bankCode                ="bankCode";
  private static final String payAmt             	  ="payAmt";
  private static final String orderTime               ="orderTime";
  private static final String payIp               	  ="payIp";
  private static final String notifyUrl               ="notifyUrl";
  private static final String noticePage              ="noticePage";
  private static final String remark              	  ="remark";
  
  
  private static final String sign              	  ="sign";
  private static final String key             		  ="key";
  
  private static final String errorCode               ="E100,E101,E102,E200,E201,E202,E203,E204,E205,U999,U998,U997,U996";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
    	Map<String, String> payParam = new LinkedHashMap<String, String>();
    	payParam.put(version,"1.0");
    	payParam.put(agentId,channelWrapper.getAPI_MEMBERID());
    	payParam.put(agentOrderId,channelWrapper.getAPI_ORDER_ID());
    	payParam.put(payType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
        //网银快捷支付
        if(HandlerUtil.isWebWyKjzf(channelWrapper)){
        	/*if(Double.parseDouble(HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()))<0){
        		throw new PayException(SERVER_MSG.REQUEST_PAY_AMOUNT__ERROR);
        	}else if(Double.parseDouble(HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()))<1000){
        		payParam.put(bankCode,"QUICKPAYMIN");
        	}else{
        		payParam.put(bankCode,"QUICKPAYMAX");
        	}*/
        	payParam.put(bankCode,"QUICKPAY");
        }
        //银联扫码
        if(HandlerUtil.isYLSM(channelWrapper)){
        	if(Double.parseDouble(HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()))<0){
        		throw new PayException(SERVER_MSG.REQUEST_PAY_AMOUNT__ERROR);
        	}else if(Double.parseDouble(HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()))<1000){
        		payParam.put(bankCode,"UNIONPAYMIN");
        	}else{
        		payParam.put(bankCode,"UNIONPAYMAX");
        	}
        }
        payParam.put(payAmt,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
        payParam.put(orderTime,HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "yyyyMMddHHmmss"));
        payParam.put(payIp,channelWrapper.getAPI_Client_IP());
        payParam.put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
        log.debug("[sss支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
    	//签名规则
        StringBuilder signSrc = new StringBuilder();
        for (Map.Entry<String, String> entry : api_response_params.entrySet()) 
 		{
        	if(entry.getKey().equals(bankCode)){
        		
        		continue;
        	}
        	signSrc=signSrc.append(entry.getValue()).append("|");
 		}
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr =signSrc.toString();
        String signMD5 = HandlerUtil.getPhpMD5(paramsStr);
        log.debug("[sss支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return signMD5;
    }

     protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
        }else{
        	String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.GET);
	        if (StringUtils.isBlank(resultStr)) {
	            log.error("[sss支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + resultStr + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        String[] codeArr=errorCode.split(",");
	        for(String code:codeArr){
	        	if(resultStr.contains(code)){
	        		log.error("[sss支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + resultStr + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	 	            throw new PayException(resultStr);
		        }
	        }
	        if(resultStr.contains("<td height=\"57\" align=\"right\" class=\"font9\">")){
	        	log.error("[sss支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + resultStr + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
 	            throw new PayException(resultStr);
	        }
	        resultStr = UnicodeUtil.unicodeToString(resultStr);
	        Document document = Jsoup.parse(resultStr);
	        Elements payUrlInputs = document.getElementsByClass("qrcode-img-area");
	        Elements img=payUrlInputs.select("img");
	        result.put(QRCONTEXT, QRCodeUtil.decodeByUrl(img.attr("src")));
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[sss支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[sss支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
    
}