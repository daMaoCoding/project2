package dc.pay.business.rongjinzhifu;

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
@RequestPayHandler("RONGJINFUZHIFU")
public final class RongJinFuZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(RongJinFuZhiFuPayRequestHandler.class);
//    参数名称			参数名			类型				可否为空				说明
//    应用编号			app_id			Number			必填	合作商户的应用编号（支付平台分配）
//    支付方式			pay_type		Number			必填	支付方式 参考附录1
//    银行卡类型			card_type		Number			选填	银行卡类型。1：借记卡2：贷记卡 默认1
//    银行缩写			bank_code		String			选填，网银支付必填	银行缩写参考附录4的支持银行的英文缩写例农业银行缩写为ABC
//    银行卡号			bank_account	String			选填	有些快捷通道需要上传银行卡号
//    商户订单号			order_id		String			必填	必须唯一，不超过30字符（商户系统生成）
//    订单金额			order_amt		Number			填	订单金额，保留两位小数 单位：元
//    支付结果异步通知URL	notify_url		String			必填	用于异步返回支付处理结果的接口
//    支付返回URL			return_url		String			必填	支付成功跳转地址
//    商品名称			goods_name		String			必填	商品名称,长度最长50字符，不能为空（不参加签名）
//    扩展参数			extends			String			选填	商户自定义参数，原样返回
//    时间戳				time_stamp		String			必填	提交时间戳(格式为yyyyMMddHHmmss 4位年+2位月+2位日+2位时+2位分+2位秒)
//    用户ip				user_ip			String			必填	客户端真实ip
//    签名				sign			String			必填	参数机制（参见2.4	HTTP参数签名机制） 参数组成（参见下面的签名参数说明）

  private static final String app_id               ="app_id";
  private static final String pay_type             ="pay_type";
  private static final String card_type            ="card_type";
  private static final String bank_code            ="bank_code";
  private static final String user_ip         		="user_ip";
  private static final String order_id             ="order_id";
  private static final String order_amt            ="order_amt";
  private static final String time_stamp           ="time_stamp";
  private static final String notify_url           ="notify_url";
  private static final String return_url           ="return_url";
  private static final String goods_name           ="goods_name";
  
  private static final String signType            ="signType";
  private static final String sign                ="sign";
  private static final String key                 ="key";
  


  @Override
  protected Map<String, String> buildPayParam() throws PayException {
      Map<String, String> payParam = new TreeMap<String, String>() {
          {
              put(app_id, channelWrapper.getAPI_MEMBERID());
              put(order_id,channelWrapper.getAPI_ORDER_ID());
              put(order_amt,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
              put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
              put(pay_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
              put(return_url,channelWrapper.getAPI_WEB_URL());
              put(time_stamp,DateUtil.formatDateTimeStrByParam("yyyyMMddHH:mm:ss"));
              put(goods_name,channelWrapper.getAPI_ORDER_ID());
              put(user_ip,channelWrapper.getAPI_Client_IP());
              if(HandlerUtil.isWY(channelWrapper)){
            	  put(bank_code,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
              }
          }
      };
      log.debug("[融金付支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
      return payParam;
  }

   protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
      String signSrc=String.format("%s%s%s%s%s%s%s%s", 
    		  app_id+"=" + api_response_params.get(app_id)+"&",
    		  pay_type+"=" + api_response_params.get(pay_type)+"&",
    		  order_id+"=" + api_response_params.get(order_id)+"&",
    		  order_amt+"=" + api_response_params.get(order_amt)+"&",
    		  notify_url+"=" + api_response_params.get(notify_url)+"&",
    		  return_url+"=" + api_response_params.get(return_url)+"&",
    		  time_stamp+"=" + api_response_params.get(time_stamp)+"&",
    		  key+"=" +HandlerUtil.getMD5UpperCase(channelWrapper.getAPI_KEY()).toLowerCase()
      );
      String paramsStr = signSrc.toString();
      String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
      log.debug("[融金付支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
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
      	String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,null);
	        if (StringUtils.isBlank(resultStr)) {
	            log.error("[融金付支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(EMPTYRESPONSE);
	        }
	        resultStr = UnicodeUtil.unicodeToString(resultStr);
	        resultStr=resultStr.replaceAll("\\\\", "");
	        if (!resultStr.contains("{") || !resultStr.contains("}")) {
	            log.error("[融金付支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        JSONObject resJson;
	        try {
	            resJson = JSONObject.parseObject(resultStr);
	        } catch (Exception e) {
	            e.printStackTrace();
	            log.error("[融金付支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        if (null != resJson && resJson.containsKey("status_code") && resJson.getString("status_code").equals("0")) {
	            String code_url = resJson.getString("pay_data");
	            if(HandlerUtil.isZFB(channelWrapper)&&HandlerUtil.isWapOrApp(channelWrapper)){
	            	result.put(JUMPURL, code_url.replace("订单编号", "%e8%ae%a2%e5%8d%95%e7%bc%96%e5%8f%b7"));
	            }else{
	            	result.put(JUMPURL, code_url);
	            }
	            
	        }else {
	            log.error("[融金付支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        payResultList.add(result);
      }
      log.debug("[融金付支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
      log.debug("[融金付支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
      return requestPayResult;
  }
}