package dc.pay.business.xiaomianyang;

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


/**
 * @author sunny
 * Dec 18, 2018
 */
@RequestPayHandler("XIAOMIANYANG")
public final class XiaoMianYangPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XiaoMianYangPayRequestHandler.class);

//    #		参数名			含义			类型			说明				参与加密			必填
//    1.	uid				商户ID		int			您的商户唯一标识，注册后在基本资料里获得		
//    2.	price			金额			float		单位：元。精确小数点后2位		
//    3.	paytype			支付渠道		int			1：支付宝转账；2：微信转账；4：支付宝转银行卡；5：银行卡转银行卡；6：支付宝红包		
//    4.	notify_url		异步回调地址	string(255)	用户支付成功后，我们服务器会主动发送一个Get消息到这个网址。由您自定义。不要urlencode并且不带任何参数。例：http://www.xxx.com/notify_url		
//    5.	return_url		同步跳转地址	string(255)	用户支付成功后，我们会让用户浏览器自动跳转到这个网址。由您自定义。不要urlencode并且不带任何参数。例：http://www.xxx.com/return_url		
//    6.	user_order_no	商户自定义订单号	string(50)	我们会据此判别是同一笔订单还是新订单。我们回调时，会带上这个参数。例：201010101041		
//    7.	note			附加内容		string(1000)	回调时将会根据传入内容原样返回（为防止乱码情况，请尽量不填写中文）		
//    8.	cuid			商户平台的玩家账号	string(50)	我们会显示在您后台的订单列表中，方便后台对账。强烈建议填写。可以填玩家账号、玩家ID、邮箱		
//    9.	sign			签名	string(32)	将参数1至6按顺序连Token一起，做md5-32位加密，取字符串小写。网址类型的参数值不要urlencode（例：uid + price + paytype + notify_url + return_url + user_order_no + token）

  private static final String uid                 ="uid";
  private static final String price               ="price";
  private static final String paytype             ="paytype";
  private static final String notify_url          ="notify_url";
  private static final String return_url          ="return_url";
  private static final String user_order_no       ="user_order_no";
  private static final String note                ="note";
  private static final String cuid                ="cuid";
  
  private static final String signType            ="signType";
  private static final String sign                ="sign";
  private static final String key                 ="key";

  @Override
  protected Map<String, String> buildPayParam() throws PayException {
      Map<String, String> payParam = new TreeMap<String, String>() {
          {
              put(uid, channelWrapper.getAPI_MEMBERID());
              put(user_order_no,channelWrapper.getAPI_ORDER_ID());
              put(price,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
              put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
              put(paytype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              put(return_url,channelWrapper.getAPI_WEB_URL());
          }
      };
      log.debug("[小绵羊支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
      return payParam;
  }

   protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
      String signSrc=String.format("%s%s%s%s%s%s%s", 
    		  api_response_params.get(uid),
    		  api_response_params.get(price),
    		  api_response_params.get(paytype),
    		  api_response_params.get(notify_url),
    		  api_response_params.get(return_url),
    		  api_response_params.get(user_order_no),
    		  channelWrapper.getAPI_KEY()
      );
      String paramsStr = signSrc.toString();
      String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
      log.debug("[小绵羊支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
      return signMD5;
  }

  protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
      payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
      HashMap<String, String> result = Maps.newHashMap();
      ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
      result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());  //.replace("method='post'","method='get'"));
      payResultList.add(result);
      log.debug("[小绵羊支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
      log.debug("[小绵羊支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
      return requestPayResult;
  }
}