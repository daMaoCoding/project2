package dc.pay.business.qishengzhifu2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
import dc.pay.utils.UnicodeUtil;
import dc.pay.utils.ValidateUtil;


/**
 * 
 * @author andrew
 * Aug 17, 2019
 */
@RequestPayHandler("QISHENGZHIFU2")
public final class QiShengZhiFu2PayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(QiShengZhiFu2PayRequestHandler.class);

//    参数名字          类型          是否必须            默认值         其他          说明
//    channel_id        数值整型        必填              21051           -           渠道ID
//    pay_trench        数值整型        必填              1               -           支付渠道
//    out_bill_num      字符串     必填              -               -           商户订单号
//    amount            字符串     必填              1               -           订单金额
//    timestamp         字符串     必填              2017-03-05      13:51:31    -   时间戳
//    sign              字符串     必填              0               -           签名
  private static final String channel_id                ="channel_id";
  private static final String pay_trench                ="pay_trench";
  private static final String out_bill_num              ="out_bill_num";
  private static final String amount                    ="amount";
  private static final String timestamp                 ="timestamp";
  
//  private static final String signType            ="signType";
//  private static final String sign                ="sign";
//  private static final String key                 ="key";

  @Override
  protected Map<String, String> buildPayParam() throws PayException {
      String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
      if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 2) {
          log.error("[旗胜支付2]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&通道类型编码值（向第三方获取当前使用通道编码值）" );
          throw new PayException("[旗胜支付2]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&通道类型编码值（向第三方获取当前使用通道编码值）" );
      }
      
      Map<String, String> payParam = new TreeMap<String, String>() {
          {
              put(channel_id, channelWrapper.getAPI_MEMBERID().split("&")[0]);
              put(out_bill_num,channelWrapper.getAPI_ORDER_ID());
              put(amount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
//              put(pay_trench,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              put(pay_trench,channelWrapper.getAPI_MEMBERID().split("&")[1]);
              put(timestamp,DateUtil.formatDateTimeStrByParam("yyyy-MM-dd HH:mm:ss"));
          }
      };
      log.debug("[旗胜支付2]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
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
      String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
      log.debug("[旗胜支付2]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
      return signMD5;
  }

  protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
      payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
      HashMap<String, String> result = Maps.newHashMap();
      ArrayList<Map<String, String>> payResultList = Lists.newArrayList();

      String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,null);
          if (StringUtils.isBlank(resultStr)) {
              log.error("[旗胜支付2]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              throw new PayException(EMPTYRESPONSE);
          }
          resultStr = UnicodeUtil.unicodeToString(resultStr);
          resultStr=resultStr.replaceAll("\\\\", "");
          if (!resultStr.contains("{") || !resultStr.contains("}")) {
              log.error("[旗胜支付2]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              throw new PayException(resultStr);
          }
          JSONObject resJson;
          try {
              resJson = JSONObject.parseObject(resultStr);
          } catch (Exception e) {
              e.printStackTrace();
              log.error("[旗胜支付2]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              throw new PayException(resultStr);
          }
          if (null != resJson && resJson.containsKey("response_code") && resJson.getString("response_code").equals("1001")) {
              JSONObject code_url = resJson.getJSONObject("response_data");
              result.put(JUMPURL, code_url.getString("pay_url"));
          }else {
              log.error("[旗胜支付2]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              throw new PayException(resultStr);
          }
          payResultList.add(result);
      log.debug("[旗胜支付2]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
      log.debug("[旗胜支付2]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
      return requestPayResult;
  }
}