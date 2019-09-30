package dc.pay.business.haiyirongzhifu;

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
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.Sha1Util;
import dc.pay.utils.UnicodeUtil;
import dc.pay.utils.ValidateUtil;


/**
 * 
 * 
 * @author sunny
 * Dec 18, 2018
 */
@RequestPayHandler("HAIYIRONGZHIFU")
public final class HaiYiRongZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(HaiYiRongZhiFuPayRequestHandler.class);

//    请求字段名称			说明
//    merNo					商户号
//    money					订单金额
//    orderId				商户支付订单号（长度50以内）
//    payId					对照附录支付编码表
//    orderTime				下单时间，格式(yyyyMMddHHmmss)
//    items					支付产品名称(超出部分将会被截断)
//    ip					下单IP(必须符合IPv4地址规范)
//    syncRedirectUrl		页面通知地址
//    asyncNotifyUrl		后台异步通知地址
//    remark				备注
//    sign					MD5签名

  private static final String merNo               	="merNo";
  private static final String money           	  	="money";
  private static final String orderId           	="orderId";
  private static final String payId           		="payId";
  private static final String orderTime          	="orderTime";
  private static final String items              	="items";
  private static final String ip            		="ip";
  private static final String syncRedirectUrl       ="syncRedirectUrl";
  private static final String asyncNotifyUrl        ="asyncNotifyUrl";
  private static final String remark        		="remark";
  private static final String bankCode        		="bankCode";
  
  private static final String signType            ="signType";
  private static final String sign                ="sign";
  private static final String key                 ="key";
  


  @Override
  protected Map<String, String> buildPayParam() throws PayException {
      Map<String, String> payParam = new TreeMap<String, String>() {
          {
              put(merNo, channelWrapper.getAPI_MEMBERID());
              put(orderId,channelWrapper.getAPI_ORDER_ID());
              put(money,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
              put(asyncNotifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
              put(payId,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
              put(items,channelWrapper.getAPI_ORDER_ID());
              put(orderTime,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
              put(ip,channelWrapper.getAPI_Client_IP());
              put(syncRedirectUrl,channelWrapper.getAPI_WEB_URL());
              put(remark,channelWrapper.getAPI_ORDER_ID());
              if(HandlerUtil.isWY(channelWrapper)){
            	  put(bankCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
              }
          }
      };
      log.debug("[海易融支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
      return payParam;
  }

   protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
      List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
      StringBuilder signSrc = new StringBuilder();
      for (int i = 0; i < paramKeys.size(); i++) {
//          if (!pay_productname.equals(paramKeys.get(i)) && !pay_attach.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
          if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
          	signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
          }
      }
      //最后一个&转换成#
      signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "" );
      signSrc.append(channelWrapper.getAPI_KEY());
      String paramsStr = signSrc.toString();
      String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr);
      log.debug("[海易融支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
      return signMD5;
  }

  protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
      payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
      HashMap<String, String> result = Maps.newHashMap();
      ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
      if (1==2&HandlerUtil.isWY(channelWrapper)) {
          result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());  //.replace("method='post'","method='get'"));
          payResultList.add(result);
      }else{
      	String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,null);
	        if (StringUtils.isBlank(resultStr)) {
	            log.error("[海易融支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(EMPTYRESPONSE);
	        }
	        if (!resultStr.contains("{") || !resultStr.contains("}")) {
	            log.error("[海易融支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        JSONObject resJson;
	        try {
	            resJson = JSONObject.parseObject(resultStr);
	        } catch (Exception e) {
	            e.printStackTrace();
	            log.error("[海易融支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        if (null != resJson && resJson.containsKey("returnCode") && resJson.getString("returnCode").equals("0")) {
	        	if(HandlerUtil.isWEBWAPAPP_SM(channelWrapper)){
	        		String code_url = resJson.getString("content");
	 	            result.put(QRCONTEXT, code_url);
	        	}
	        	if(HandlerUtil.isWapOrApp(channelWrapper)||HandlerUtil.isWY(channelWrapper)||HandlerUtil.isYLKJ(channelWrapper)){
	        		String code_url = resJson.getString("content");
		            result.put(JUMPURL, code_url);
	        	}
	            
	        }else {
	            log.error("[海易融支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        payResultList.add(result);
      }
      log.debug("[海易融支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
      log.debug("[海易融支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
      return requestPayResult;
  }
}