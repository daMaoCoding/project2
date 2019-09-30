package dc.pay.business.zhangfubao;

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
 * 
 * 
 * @author sunny
 * Dec 18, 2018
 */
@RequestPayHandler("ZHANGFUBAOZHIFU")
public final class ZhangFuBaoZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(ZhangFuBaoZhiFuPayRequestHandler.class);

//    参数名称				参数含义				必填				说明
//    fxid					商务号				是				唯一号，由掌付宝提供
//    fxddh					商户订单号			是				仅允许字母或数字类型,不超过22个字符，不要有中文
//    fxdesc				商品名称				是				utf-8编码
//    fxfee					支付金额				是				请求的价格(单位：元) 可以0.01元
//    fxnotifyurl			异步通知地址			是				异步接收支付结果通知的回调地址，通知url必须为外网可访问的url，不能携带参数。
//    fxbackurl				同步通知地址			是				支付成功后跳转到的地址，不参与签名。
//    fxpay				请求类型 【微信扫码：wxewm】	是				请求支付的接口类型。
//    fxnotifystyle			异步数据类型			否				异步返回数据的类型，默认1 返回数据为表单数据（Content-Type: multipart/form-data），2 返回post json数据。
//    fxattch				附加信息				否				原样返回，utf-8编码
//    fxsmstyle				扫码模式				否				用于扫码模式（sm），仅带sm接口可用，默认0返回扫码图片，为1则返回扫码跳转地址。
//    fxbankcode			银行类型				否				用于网银直连模式，请求的银行编号，参考银行附录,仅网银接口可用。
//    fxfs					反扫付款码数字			否				用于用户被扫，用户的付款码数字,仅反扫接口可用。
//    fxuserid				快捷模式绑定商户id		否				用于识别用户绑卡信息，仅快捷接口可用。
//    fxsign				签名【md5(商务号+商户订单号+支付金额+异步通知地址+商户秘钥)】	是	通过签名算法计算得出的签名值。
//    fxip					支付用户IP地址		是				用户支付时设备的IP地址

  private static final String fxid               		="fxid";
  private static final String fxddh           			="fxddh";
  private static final String fxdesc           			="fxdesc";
  private static final String fxfee           			="fxfee";
  private static final String fxnotifyurl          		="fxnotifyurl";
  private static final String fxbackurl              	="fxbackurl";
  private static final String fxpay            			="fxpay";
  private static final String fxnotifystyle             ="fxnotifystyle";
  private static final String fxip             			="fxip";
  
  private static final String signType            ="signType";
  private static final String sign                ="sign";
  private static final String key                 ="key";
  


  @Override
  protected Map<String, String> buildPayParam() throws PayException {
      Map<String, String> payParam = new TreeMap<String, String>() {
          {
              put(fxid, channelWrapper.getAPI_MEMBERID());
              put(fxddh,channelWrapper.getAPI_ORDER_ID());
              put(fxfee,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
              put(fxnotifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
              put(fxpay,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              put(fxbackurl,channelWrapper.getAPI_WEB_URL());
              put(fxip,channelWrapper.getAPI_Client_IP());
              put(fxnotifystyle,"2");
              put(fxdesc,channelWrapper.getAPI_ORDER_ID());
          }
      };
      log.debug("[掌付宝支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
      return payParam;
  }

   protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
      String signSrc=String.format("%s%s%s%s%s", 
    		  api_response_params.get(fxid),
    		  api_response_params.get(fxddh),
    		  api_response_params.get(fxfee),
    		  api_response_params.get(fxnotifyurl),
    		  channelWrapper.getAPI_KEY()
       );
      String paramsStr = signSrc.toString();
      String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
      log.debug("[掌付宝支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
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
	            log.error("[掌付宝支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(EMPTYRESPONSE);
	        }
	        resultStr = UnicodeUtil.unicodeToString(resultStr);
	        resultStr=resultStr.replaceAll("\\\\", "");
	        if (!resultStr.contains("{") || !resultStr.contains("}")) {
	            log.error("[掌付宝支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        JSONObject resJson;
	        try {
	            resJson = JSONObject.parseObject(resultStr);
	        } catch (Exception e) {
	            e.printStackTrace();
	            log.error("[掌付宝支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        if (null != resJson && resJson.containsKey("status") && resJson.getString("status").equals("1")) {
	            String code_url = resJson.getString("payurl");
	            result.put(JUMPURL, code_url);
	        }else {
	            log.error("[掌付宝支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            throw new PayException(resultStr);
	        }
	        payResultList.add(result);
      }
      log.debug("[掌付宝支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
      log.debug("[掌付宝支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
      return requestPayResult;
  }
}