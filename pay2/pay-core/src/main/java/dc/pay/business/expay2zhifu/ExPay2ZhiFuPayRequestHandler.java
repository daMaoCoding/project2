package dc.pay.business.expay2zhifu;

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
 * @date 15 Jul 2019
 */
@RequestPayHandler("EXPAY2ZHIFU")
public final class ExPay2ZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(ExPay2ZhiFuPayRequestHandler.class);

//    名称			说明			类型			可否为空	  最大长度
//    payKey		商户支付Key	String		否		 32
//    orderPrice	订单金额，单位：元保留小数点后两位	String	否	12
//    outTradeNo	商户支付订单号	String		否		 30
//    productType	产品类型，请查阅本文档2.6 	String	否	8
//    orderTime		下单时间，格式：yyyyMMddHHmmss	String	否	14
//    productName	支付产品名称	String	否	200
//    orderIp		下单IP	String	否	15
//    returnUrl		页面通知地址	String	否	300
//    notifyUrl		后台异步通知地址	String	否	300
//    sign 			签名	String	否	50
//    bankCode		银行编码直连银行必填,详情查看2.7 网关(B2C)支付,必填	String	是	10
//    payBankAccountNo	银行卡号(云闪付和快捷必填)	String	是	200
//    remark		备注	String	是	200

  private static final String payKey               ="payKey";
  private static final String orderPrice           ="orderPrice";
  private static final String outTradeNo           ="outTradeNo";
  private static final String productType          ="productType";
  private static final String orderTime            ="orderTime";
  private static final String payBankAccountNo     ="payBankAccountNo";
  private static final String payPhoneNo           ="payPhoneNo";
  private static final String payBankAccountName   ="payBankAccountName";
  private static final String payCertNo            ="payCertNo";
  private static final String productName          ="productName";
  private static final String orderIp          	   ="orderIp";
  private static final String returnUrl            ="returnUrl";
  private static final String notifyUrl            ="notifyUrl";
  
  private static final String signType            ="signType";
  private static final String sign                ="sign";
  private static final String paySecret                 ="paySecret";
  


  @Override
  protected Map<String, String> buildPayParam() throws PayException {
      Map<String, String> payParam = new TreeMap<String, String>() {
          {
              put(payKey, channelWrapper.getAPI_MEMBERID());
              put(outTradeNo,channelWrapper.getAPI_ORDER_ID());
              put(orderPrice,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
              put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
              put(productType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              put(returnUrl,channelWrapper.getAPI_WEB_URL());
              put(orderTime,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
              put(payBankAccountNo,"123321");
              put(payPhoneNo,"13232155456");
              put(payBankAccountName,"中国银行");
              put(payCertNo,"522652265226569");
              put(productName,channelWrapper.getAPI_ORDER_ID());
              put(orderIp,channelWrapper.getAPI_Client_IP());
          }
      };
      log.debug("[expay2支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
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
      //signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "#" );
      signSrc.append(paySecret+"="+channelWrapper.getAPI_KEY());
      String paramsStr = signSrc.toString();
      String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr);
      log.debug("[expay2支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
      return signMD5;
  }

  protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
      payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
      HashMap<String, String> result = Maps.newHashMap();
      ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
      result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());  //.replace("method='post'","method='get'"));
      payResultList.add(result);
      log.debug("[expay2支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
      log.debug("[expay2支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
      return requestPayResult;
  }
}