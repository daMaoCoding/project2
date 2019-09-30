package dc.pay.business.chengyitong2zhifu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

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
import dc.pay.utils.RsaUtil;
import dc.pay.utils.Sha1Util;
import dc.pay.utils.UnicodeUtil;
import dc.pay.utils.ValidateUtil;


/**
 * @author sunny
 */
@RequestPayHandler("CHENGYITONG2ZHIFU")
public final class ChengYiTong2ZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(ChengYiTong2ZhiFuPayRequestHandler.class);

//    字段名				变量名		类型			签名				实例值			说明
//    商户ID				p1_mchtid	int			是				商户ID,由支付分配
//    支付方式			p2_paytype	String(20)	是				UNIONFASTPAY	支付网关(参见附录说明4.3)
//    支付金额			p3_paymoney	decimal		是				0.01	订单金额最小0.01(以元为单位）
//    商户平台唯一订单号	p4_orderno	String(50)	是				商户系统内部订单号，要求50字符以内，同一商户号下订单号唯一
//    商户异步回调通知地址	p5_callbackurl	String(200)	是			商户异步回调通知地址
//    商户同步通知地址		p6_notifyurl	String(200)	是			商户同步通知地址
//    版本号				p7_version	String(4)	是				v2.9	v2.9
//    签名加密方式		p8_signtype	int			是				2	值为2
//    备注信息，上行中attach原样返回	p9_attach	String(128)	是		值为空也参加sign签名
//    备注信息，上行中attach原样返回
//    标识				p10_appname	Strng(25)	是				无任何意义 值为空也参加sign签名
//    是否显示			p11_isshow	int			是				0	0（写死的值，无任何意义
//    商户的用户下单IP		p12_orderip	String(20)	是				192.168.10.1	商户的用户下单IP
//    商户系统用户唯一标识	p13_memberid	String(5-32)	是		123456	传会员ID或代表会员ID唯一的信息（如：加密后的会员ID，混淆的会员ID)

  private static final String p1_mchtid               ="p1_mchtid";
  private static final String p2_paytype              ="p2_paytype";
  private static final String p3_paymoney             ="p3_paymoney";
  private static final String p4_orderno              ="p4_orderno";
  private static final String p5_callbackurl          ="p5_callbackurl";
  private static final String p6_notifyurl            ="p6_notifyurl";
  private static final String p7_version              ="p7_version";
  private static final String p8_signtype             ="p8_signtype";
  private static final String p9_attach               ="p9_attach";
  private static final String p10_appname             ="p10_appname";
  private static final String p11_isshow              ="p11_isshow";
  private static final String p12_orderip             ="p12_orderip";
  private static final String p13_memberid            ="p13_memberid";
  
  private static final String signType            ="signType";
  private static final String sign                ="sign";
  private static final String key                 ="key";
  


  @Override
  protected Map<String, String> buildPayParam() throws PayException {
      Map<String, String> payParam = new TreeMap<String, String>() {
          {
              put(p1_mchtid, channelWrapper.getAPI_MEMBERID());
              put(p4_orderno,channelWrapper.getAPI_ORDER_ID());
              put(p3_paymoney,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
              put(p5_callbackurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
              put(p2_paytype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              put(p6_notifyurl,channelWrapper.getAPI_WEB_URL());
              put(p7_version,"v2.9");
              put(p8_signtype,"2");
              put(p9_attach,channelWrapper.getAPI_ORDER_ID());
              put(p10_appname,channelWrapper.getAPI_ORDER_ID());
              put(p11_isshow,"0");
              put(p12_orderip,channelWrapper.getAPI_Client_IP());
              put(p13_memberid,channelWrapper.getAPI_ORDER_ID());
          }
      };
      log.debug("[诚易通2支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
      return payParam;
  }

   protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
      String signSrc=String.format("%s%s%s%s%s%s%s%s%s%s%s%s%s%s", 
    		  p1_mchtid+"="+api_response_params.get(p1_mchtid)+"&",
    		  p2_paytype+"="+api_response_params.get(p2_paytype)+"&",
    		  p3_paymoney+"="+api_response_params.get(p3_paymoney)+"&",
    		  p4_orderno+"="+api_response_params.get(p4_orderno)+"&",
    		  p5_callbackurl+"="+api_response_params.get(p5_callbackurl)+"&",
    		  p6_notifyurl+"="+api_response_params.get(p6_notifyurl)+"&",
    		  p7_version+"="+api_response_params.get(p7_version)+"&",
    		  p8_signtype+"="+api_response_params.get(p8_signtype)+"&",
    		  p9_attach+"="+api_response_params.get(p9_attach)+"&",
    		  p10_appname+"="+api_response_params.get(p10_appname)+"&",
    		  p11_isshow+"="+api_response_params.get(p11_isshow)+"&",
    		  p12_orderip+"="+api_response_params.get(p12_orderip)+"&",
    		  p13_memberid+"="+api_response_params.get(p13_memberid),
    		  channelWrapper.getAPI_PUBLIC_KEY().split("&")[0]
      );
      String paramsStr = signSrc.toString();
      String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
      log.debug("[诚易通2支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
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
    	HashMap<String, String> postMap = Maps.newHashMap();
    	postMap.put("mchtid", channelWrapper.getAPI_MEMBERID());
    	try {
			postMap.put("reqdata", HandlerUtil.UrlEncode(RsaUtil.encryptToBase64(HandlerUtil.mapToJson(payParam), channelWrapper.getAPI_PUBLIC_KEY().split("&")[1])));
		} catch (Exception e1) {
			e1.printStackTrace();
		}
    	if( HandlerUtil.isYLKJ(channelWrapper) ){
    		result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),postMap).toString());  //.replace("method='post'","method='get'"));
            payResultList.add(result);
    	}else{
    		String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), postMap, String.class, HttpMethod.POST);
	        if (StringUtils.isBlank(resultStr)) {
	            log.error("[诚易通2支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(EMPTYRESPONSE);
	        }
	        if (!resultStr.contains("{") || !resultStr.contains("}")) {
	            log.error("[诚易通2支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        JSONObject resJson;
	        try {
	            resJson = JSONObject.parseObject(resultStr);
	        } catch (Exception e) {
	            e.printStackTrace();
	            log.error("[诚易通2支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        if (null != resJson && resJson.containsKey("rspCode") && resJson.getString("rspCode").equals("1")) {
	        	JSONObject data = resJson.getJSONObject("data");
	            result.put(JUMPURL, data.getString("r6_qrcode"));
	        }else {
	            log.error("[诚易通2支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        payResultList.add(result);
    	}
    	
      }
      log.debug("[诚易通2支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
      log.debug("[诚易通2支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
      return requestPayResult;
  }
}