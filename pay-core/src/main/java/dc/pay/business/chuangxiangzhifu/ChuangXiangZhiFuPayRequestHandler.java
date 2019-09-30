package dc.pay.business.chuangxiangzhifu;

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
 * @author sunny
 * 05 10, 2019
 */
@RequestPayHandler("CHUANGXIANGZHIFU")
public final class ChuangXiangZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(ChuangXiangZhiFuPayRequestHandler.class);

//    参数名称			参数编码			属性				数据描述			数据类型
//    version			版本号			M				固定值1.0.0		ASC(5)
//    transType			业务类型			M				固定值 SALES		A(4)
//    productId			产品类型			M				0001     		网关消费 0111     QQH50121     微信H5 支付0131     支付宝H5 支付0122     微信收银台扫码	N(4)
//    merNo				商户号			M				商户号			N(15)
//    orderDate			订单日期			M				订单交易日期		 yyyyMMdd	N(8)
//    orderNo			订单号			M				商户平台订单号		ASC(40)
//    clientIp			商户IP			M				真实的客户端IP	
//    notifyUrl			后台通知地址		M				用户完成支付后,服务器后台通知地址	ASC(255)
//    returnUrl			前台跳转地址		M				前台跳转地址	ASC(255)
//    transAmt			交易金额			M				分为单位如 100 代表  1.00元	N(64)
//    bankCode			银行编码			O				跳转目标银行银行编码 参考 附录二网银直连时必填 	ASC(20)
//    connectType		终端类型			C				IOS_WAP   		IOS系统浏览器支付 AND_WAP  ANDROID 系统浏览器支付如果不上送会影响成功率及可能被风控	ASC(40)
//    commodityName		产品名称			C				产品名称	ASC(64)
//    commodityDetail	产品详情			C				产品详情 真实交易的订单详情 如 交易金额 3.50元 commodityDetail 上送可口可乐  如果改参数不上送 会影响成功率及可能被风控	ASC(255)
//    signature			签名字段			M				参考 目录3.3	

  private static final String version               ="version";
  private static final String transType           	="transType";
  private static final String productId           	="productId";
  private static final String merNo           		="merNo";
  private static final String orderDate          	="orderDate";
  private static final String orderNo              	="orderNo";
  private static final String clientIp            	="clientIp";
  private static final String notifyUrl           	="notifyUrl";
  private static final String returnUrl           	="returnUrl";
  private static final String transAmt           	="transAmt";
  
  private static final String signType            ="signType";
  private static final String sign                ="sign";
  private static final String key                 ="key";
  


  @Override
  protected Map<String, String> buildPayParam() throws PayException {
      Map<String, String> payParam = new TreeMap<String, String>() {
          {
              put(merNo, channelWrapper.getAPI_MEMBERID());
              put(orderNo,channelWrapper.getAPI_ORDER_ID());
              put(transAmt,channelWrapper.getAPI_AMOUNT());
              put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
              put(productId,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              put(returnUrl,channelWrapper.getAPI_WEB_URL());
              put(orderDate,DateUtil.formatDateTimeStrByParam("yyyy-MM-dd"));
              put(transType,"SALES");
              put(version,"1.0.0");
              put(clientIp,channelWrapper.getAPI_Client_IP());
          }
      };
      log.debug("[唱响支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
      return payParam;
  }

   protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
      String signSrc="";
      try {
		signSrc = SignUtils.signData2(api_response_params, channelWrapper.getAPI_KEY());
      } catch (Exception e) {
		e.printStackTrace();
      }
      log.debug("[唱响支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signSrc));
      return signSrc;
  }

  protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
      payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
      HashMap<String, String> result = Maps.newHashMap();
      ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
      if (1==2&&(HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)  ||  HandlerUtil.isWapOrApp(channelWrapper))) {
          result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());  //.replace("method='post'","method='get'"));
          payResultList.add(result);
      }else{
      	String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,null);
	        if (StringUtils.isBlank(resultStr)) {
	            log.error("[唱响支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(EMPTYRESPONSE);
	        }
	        if (!resultStr.contains("{") || !resultStr.contains("}")) {
	            log.error("[唱响支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        JSONObject resJson;
	        try {
	            resJson = JSONObject.parseObject(resultStr);
	        } catch (Exception e) {
	            e.printStackTrace();
	            log.error("[唱响支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        if (null != resJson && resJson.containsKey("respCode") && resJson.getString("respCode").equals("P000")) {
	            String code_url = resJson.getString("payInfo");
	            result.put(JUMPURL, code_url);
	        }else {
	            log.error("[唱响支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        payResultList.add(result);
      }
      log.debug("[唱响支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
      log.debug("[唱响支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
      return requestPayResult;
  }
}