package dc.pay.business.caifuzhifu;

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
import dc.pay.business.sihaiyun.DigestUtil1;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
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
@RequestPayHandler("CAIFUZHIFU")
public final class CaiFuZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(CaiFuZhiFuPayRequestHandler.class);

//  字段 			说明 			类型 			必填 			备注
//  appId 		商户标 识		String 		是 			您的商户唯一标识，登陆后台系统 在个人中心获得。
//  payMethod 	支付方 式		String 		是			支付方式，固定枚举值。 2001：支付宝 1001:微信
//  notifyUrl 	支付成功回调地址String 	是			用户支付成功后，会做异步通知， 我们服务器会主动发送 post 消 息到这个地址。由调用者自定义。 
//  returnUrl		支付成功跳转地址String 	是			用户支付成功后，可以让用户浏览器自动跳转到这个网址。由调用者 自定义。
//  outTradeNo	商户支付单标识String 		是			系统会据此判别是同一笔订单还是新订单。回调时，会返回这个参数。
//  signType 		签名方 式		String 		是		       使用 MD5 签名。默认值MD5sign 签名后 数据String 是把使用到的所有参数，按参数名字 母升序排序。
//  amount 		订单金 额		String 		是 			单位：元。精确小数点后 2 位。
//  nonceStr 		随机字 符串	String 		是			 随机字符串。
//  timestamp 	时间戳		 String 	是 			当前时间戳。

  private static final String p0_Cmd               	="p0_Cmd";
  private static final String p1_MerId           	="p1_MerId";
  private static final String p2_Order           	="p2_Order";
  private static final String p3_Cur           		="p3_Cur";
  private static final String p4_Amt          		="p4_Amt";
  private static final String p5_Pid              	="p5_Pid";
  private static final String p8_Url            	="p8_Url";
  private static final String pa_FrpId           	="pa_FrpId";
  private static final String pc_CardNo           	="pc_CardNo";
  private static final String ph_Ip           		="ph_Ip";
  private static final String pi_Url           		="pi_Url";
  
  
  private static final String signType            ="signType";
  private static final String sign                ="sign";
  private static final String key                 ="key";

  @Override
  protected Map<String, String> buildPayParam() throws PayException {
      Map<String, String> payParam = new TreeMap<String, String>() {
          {
              put(p1_MerId, channelWrapper.getAPI_MEMBERID());
              put(p2_Order,channelWrapper.getAPI_ORDER_ID());
              put(p4_Amt,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
              put(p8_Url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
              put(pa_FrpId,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              put(pi_Url,channelWrapper.getAPI_WEB_URL());
              put(p5_Pid,channelWrapper.getAPI_ORDER_ID());
              put(p3_Cur,"CNY");
              put(p0_Cmd,"Buy");
              put(pc_CardNo,"");
              put(ph_Ip,channelWrapper.getAPI_Client_IP());
          }
      };
      log.debug("[彩付支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
      return payParam;
  }

   protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
	   List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
       StringBuilder signSrc = new StringBuilder();
       for (int i = 0; i < paramKeys.size(); i++) {
//           if (!pay_productname.equals(paramKeys.get(i)) && !pay_attach.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
           if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
           	signSrc.append(api_response_params.get(paramKeys.get(i)));
           }
       }
       //最后一个&转换成#
       String paramsStr = signSrc.toString();
       String signMD5 = DigestUtil1.hmacSign(paramsStr, channelWrapper.getAPI_KEY());
      log.debug("[彩付支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
      return signMD5;
  }

  protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
      payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
      HashMap<String, String> result = Maps.newHashMap();
      ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
      if (!HandlerUtil.isWEBWAPAPP_SM(channelWrapper)) {
          result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());  //.replace("method='post'","method='get'"));
          payResultList.add(result);
      }else{
      	String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,null);
	        if (StringUtils.isBlank(resultStr)) {
	            log.error("[彩付支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(EMPTYRESPONSE);
	        }
	        if (!resultStr.contains("{") || !resultStr.contains("}")) {
	            log.error("[彩付支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        JSONObject resJson;
	        try {
	            resJson = JSONObject.parseObject(resultStr);
	        } catch (Exception e) {
	            e.printStackTrace();
	            log.error("[彩付支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        if (null != resJson && resJson.containsKey("r1_Code") && resJson.getString("r1_Code").equals("1")) {
	            String code_url = resJson.getString("r3_PayInfo");
	            result.put(QRCONTEXT, code_url);
	        }else {
	            log.error("[彩付支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        payResultList.add(result);
      }
      log.debug("[彩付支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
      log.debug("[彩付支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
      return requestPayResult;
  }
}