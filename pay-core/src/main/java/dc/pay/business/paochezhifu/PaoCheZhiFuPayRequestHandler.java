package dc.pay.business.paochezhifu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

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
import dc.pay.utils.Sha1Util;
import dc.pay.utils.UnicodeUtil;
import dc.pay.utils.ValidateUtil;
import dc.pay.utils.qr.QRCodeUtil;


/**
 * @author sunny
 * 05 16, 2019
 */
@RequestPayHandler("PAOCHEZHIFU")
public final class PaoCheZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(PaoCheZhiFuPayRequestHandler.class);

//    字段名				变量名			类型				最大长度			说明			可空			是否签名
//    签名				sign			String			32				签名 			N	
//    商户号				merchantCode	String			32				商户号：平台提供给商户	N	Y
//    商户订单号			merchantOrderNo	String			32				商户订单号：必传，订单唯一标示，作为订单结果通知的标识，可按照你们业务生成	N	Y
//    付款类型			payType			int				2				付款类型（1-支付宝、2-微信）	N	Y
//    付款金额			payPrice		int				付款金额（整数，以分为单位）：payPrice，如1元传100	N	Y
//    商户名称			merchantName	String			32				商户名称：必传，作为付款码图片上的商户名称	N	Y
//    异步通知地址		callbackUrl		String			255				异步通知地址：必传，作为订单结果通知地址，具体回调接口可参考回调示例	N	
//    商户备注			merchantRemark	String			255				商户备注：可为空，作为付款说明，回调时会传递过去		

  private static final String merchantCode               ="merchantCode";
  private static final String merchantOrderNo            ="merchantOrderNo";
  private static final String payType           		 ="payType";
  private static final String payPrice           		 ="payPrice";
  private static final String merchantName          	 ="merchantName";
  private static final String callbackUrl              	 ="callbackUrl";
  
  private static final String signType            ="signType";
  private static final String sign                ="sign";
  private static final String key                 ="key";
  


  @Override
  protected Map<String, String> buildPayParam() throws PayException {
      Map<String, String> payParam = new TreeMap<String, String>() {
          {
              put(merchantCode, channelWrapper.getAPI_MEMBERID());
              put(merchantOrderNo,channelWrapper.getAPI_ORDER_ID());
              put(payPrice,channelWrapper.getAPI_AMOUNT());
              put(callbackUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
              put(payType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              put(merchantName,channelWrapper.getAPI_ORDER_ID());
          }
      };
      log.debug("[跑车支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
      return payParam;
  }

   protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
      String signSrc=String.format("%s%s%s%s%s%s",
    		  merchantName+"="+api_response_params.get(merchantName)+"&",
    		  merchantCode+"="+api_response_params.get(merchantCode)+"&",
    		  merchantOrderNo+"="+api_response_params.get(merchantOrderNo)+"&",
    		  payPrice+"="+api_response_params.get(payPrice)+"&",
    		  payType+"="+api_response_params.get(payType)+"&",
    		  channelWrapper.getAPI_KEY()
    		  );
      String paramsStr = signSrc.toString();
      String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
      log.debug("[跑车支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
      return signMD5;
  }

  protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
      payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
      HashMap<String, String> result = Maps.newHashMap();
      ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
      if (1==2&&(HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)  ||  HandlerUtil.isWapOrApp(channelWrapper))) {
          result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());  //.replace("method='post'","method='get'"));
          payResultList.add(result);
      }else{
      	String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
	        if (StringUtils.isBlank(resultStr)) {
	            log.error("[跑车支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(EMPTYRESPONSE);
	        }
	        if (!resultStr.contains("{") || !resultStr.contains("}")) {
	            log.error("[跑车支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        JSONObject resJson;
	        try {
	            resJson = JSONObject.parseObject(resultStr);
	        } catch (Exception e) {
	            e.printStackTrace();
	            log.error("[跑车支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        if (null != resJson && resJson.containsKey("result") && resJson.getString("result").equals("0")) {
	        	JSONObject 	datas=resJson.getJSONObject("datas");
	            String code_url = datas.getString("payPic");
	        	if(HandlerUtil.isWxSM(channelWrapper)){
		            result.put(QRCONTEXT, QRCodeUtil.decodeByUrl(code_url));
	        	}else{
	        		result.put(JUMPURL, code_url);
	        	}
	        	
	        }else {
	            log.error("[跑车支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        payResultList.add(result);
      }
      log.debug("[跑车支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
      log.debug("[跑车支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
      return requestPayResult;
  }
}