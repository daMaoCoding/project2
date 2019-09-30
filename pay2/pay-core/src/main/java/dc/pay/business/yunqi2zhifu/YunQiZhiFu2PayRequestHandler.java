package dc.pay.business.yunqi2zhifu;

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
@RequestPayHandler("YUNQIZHIFU2")
public final class YunQiZhiFu2PayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(YunQiZhiFu2PayRequestHandler.class);

//    字段名称							内容							是否必输						备注
//    branchId							商户编号					是									1027
//    bizType								交易类型					是									银联快捷：100003;
//    bankCode						银行编码					否									网银支付时直连银行的编码
//    orderId								订单号						是									20位长度唯一订单标识
//    transDate							交易日期					是									yyyyMMdd
//    transTime							交易时间					是									HHmmss
//    transAmt							交易金额					是									分为单位
//    commodityName				商品名称					是	
//    returnUrl							后台回调通知地址	是									后台回调通知地址
//    notifyUrl								页面通知地址			是									页面通知地址
//    signature							验签字段					是									MD5加密

  private static final String branchId               				="branchId";
  private static final String bizType           						="bizType";
  private static final String bankCode           				="bankCode";
  private static final String orderId           						="orderId";
  private static final String transDate          					="transDate";
  private static final String transTime              				="transTime";
  private static final String transAmt            					="transAmt";
  private static final String commodityName           	="commodityName";
  private static final String returnUrl           					="returnUrl";
  private static final String notifyUrl           					="notifyUrl";
  private static final String data           							="data";
  
  private static final String signType            ="signType";
  private static final String signature                ="signature";
  private static final String key                 ="key";

  @Override
  protected Map<String, String> buildPayParam() throws PayException {
      if (null == channelWrapper.getAPI_MEMBERID() || !channelWrapper.getAPI_MEMBERID().contains("&")) {
          throw new PayException("商户号输入数据格式为【中间使用&分隔】：平台商户号 &交易类型" );
      }
      Map<String, String> payParam = new TreeMap<String, String>() {
          {
              put(branchId, channelWrapper.getAPI_MEMBERID().split("&")[0]);
              put(orderId,channelWrapper.getAPI_ORDER_ID());
              put(transAmt,channelWrapper.getAPI_AMOUNT());
              put(returnUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
              put(bizType,channelWrapper.getAPI_MEMBERID().split("&")[1]);
              put(notifyUrl,channelWrapper.getAPI_WEB_URL());
              put(commodityName,channelWrapper.getAPI_ORDER_ID());
              put(transDate,DateUtil.formatDateTimeStrByParam("yyyyMMdd"));
              put(transTime,DateUtil.formatDateTimeStrByParam("HHmmss"));
          }
      };
      log.debug("[云起支付2]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
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
      signSrc.append(channelWrapper.getAPI_KEY().split("&")[0]);
      String paramsStr = signSrc.toString();
      String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr);
      log.debug("[云起支付2]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
      return signMD5;
  }

  protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
      payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
      HashMap<String, String> result = Maps.newHashMap();
      ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
      Map<String, String> postParam=Maps.newHashMap();
      String DESsign = Des.encode(HandlerUtil.mapToJson(payParam), channelWrapper.getAPI_KEY().split("&")[1]);
      postParam.put("branchId", channelWrapper.getAPI_MEMBERID().split("&")[0]);
      postParam.put("data",DESsign);
      result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),postParam).toString());  //.replace("method='post'","method='get'"));
      payResultList.add(result);
      log.debug("[云起支付2]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
      log.debug("[云起支付2]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
      return requestPayResult;
  }
}