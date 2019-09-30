package dc.pay.business.baijiezhifu;

import java.io.UnsupportedEncodingException;
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
@RequestPayHandler("BAIJIEZHIFU")
public final class BaiJieZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(BaiJieZhiFuPayRequestHandler.class);

//    参数名				必选				类型			说明											示例
//    account_id		是				string		商户ID、在平台首页右边获取商户ID	10000
//    content_type		是				string		请求过程中返回的网页类型，text	text
//    thoroughfare		是				string		初始化支付通道，目前通道：wechat_auto（商户版微信）、alipay_auto（商户版支付宝）、 unionpay_auto（云闪付）、alipay_redpacket_auto（红包助手）、payments_auto（农商银行/银盛通）	wechat_auto
//    type				是				string		支付类型，该参数在thoroughfare为payments_auto时，农银E管家为1，湖南农信为2，银盛通为3，福建农信为4，贵州农信为5，星POS为6，拼多多为7；该参数在thoroughfare为其他值时，无效	1
//    out_trade_no		是				string		订单信息，在发起订单时附加的信息，如用户名，充值订单号等字段参数	2018062668945
//    robin				是				string		轮训，2：开启轮训，1：进入单通道模式	2
//    keyId				是				string		设备KEY，在商户版列表里面Important参数下的DEVICE Key一项，如果该请求为轮训模式，则本参数无效，本参数为单通道模式	785D239777C4DE7739
//    amount			是				string		支付金额，在发起时用户填写的支付金额，精确到分	1.00
//    callback_url		是				string		异步通知地址，在支付完成时，本平台服务器系统会自动向该地址发起一条支付成功的回调请求, 对接方接收到回调后，必须返回 success ,否则默认为回调失败,回调信息会补发3次。	http://www.baidu.com
//    success_url		是				string		支付成功后网页自动跳转地址	http://www.baidu.com
//    error_url			是				string		支付失败时，或支付超时后网页自动跳转地址	http://www.baidu.com
//    sign				是				string		签名算法，在支付时进行签名算法，详见《支付签名算法》	d92eff67b3be05f5e61502e96278d01b

  private static final String account_id               ="account_id";
  private static final String content_type             ="content_type";
  private static final String thoroughfare             ="thoroughfare";
  private static final String type           		   ="type";
  private static final String out_trade_no             ="out_trade_no";
  private static final String robin              	   ="robin";
  private static final String keyId            		   ="keyId";
  private static final String amount           		   ="amount";
  private static final String callback_url             ="callback_url";
  private static final String success_url              ="success_url";
  private static final String error_url                ="error_url";
  
  private static final String signType            ="signType";
  private static final String sign                ="sign";
  private static final String key                 ="key";
  


  @Override
  protected Map<String, String> buildPayParam() throws PayException {
      Map<String, String> payParam = new TreeMap<String, String>() {
          {
              put(account_id, channelWrapper.getAPI_MEMBERID());
              put(out_trade_no,channelWrapper.getAPI_ORDER_ID());
              put(amount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
              put(callback_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
              put(thoroughfare,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              put(success_url,channelWrapper.getAPI_WEB_URL());
              put(error_url,channelWrapper.getAPI_WEB_URL());
              put(robin,"2");
              put(type,"");
              put(content_type,"text");
              put(keyId," ");
          }
      };
      log.debug("[百捷支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
      return payParam;
  }

   protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
      String signSrc=String.format("%s%s", 
    		  api_response_params.get(amount),
    		  api_response_params.get(out_trade_no)
      );
      String paramsStr = signSrc.toString();
      String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
      byte[] signByte=null;
	  try {
		signByte = Sign.encry_RC4_byte(signMD5.getBytes("UTF-8"), channelWrapper.getAPI_KEY());
	  } catch (UnsupportedEncodingException e) {
		e.printStackTrace();
	  }
      String signStr=HandlerUtil.getMD5UpperCase(signByte).toLowerCase();
      log.debug("[百捷支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signStr));
      return signStr;
  }

  protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
      payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
      HashMap<String, String> result = Maps.newHashMap();
      ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
      result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());  //.replace("method='post'","method='get'"));
      payResultList.add(result);
      log.debug("[百捷支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
      log.debug("[百捷支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
      return requestPayResult;
  }
}