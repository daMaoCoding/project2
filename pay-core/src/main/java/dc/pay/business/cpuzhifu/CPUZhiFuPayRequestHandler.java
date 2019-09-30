package dc.pay.business.cpuzhifu;

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
@RequestPayHandler("CPUZHIFU")
public final class CPUZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(CPUZhiFuPayRequestHandler.class);

//    参数名称				参数变量名				类型			必填			说明
//    参数字符集编码			inputCharset			String		是	商户系统与支付平台间交互信息时使用的字符集编码
//    异步通知地址			notifyUrl				String(200)	是	支付成功后，支付平台会主动通知商家系统
//    同步跳转通知地址			pageUrl					String(200) 否	支付成功后，通过页面跳转的方式跳转到商户指定的网站页面s
//    支付方式				payType					String(2)	是	1：网银支付、2：微信3：支付宝、4：QQ5：快捷支付（无卡支付）
//    商户号					merchantId				String(8)	是	商户注册签约后，支付平台分配的唯一标识号
//    商户订单号				orderId					String(32)	是	由商户系统生成的唯一订单编号，最大长度为32位
//    订单金额				transAmt				Number(9,2)	是	订单总金额以元为单位，精确到小数点后两位
//    商户订单时间			orderTime				Date		是	字符串格式要求为： yyyy-MM-dd HH:mm:ss 例如：2017-06-14 12:45:52
//    签名					sign					String		是	签名数据，签名规则见附录

  private static final String inputCharset               	="inputCharset";
  private static final String notifyUrl           			="notifyUrl";
  private static final String pageUrl           			="pageUrl";
  private static final String payType           			="payType";
  private static final String bankCode          			="bankCode";
  private static final String merchantId              		="merchantId";
  private static final String orderId            			="orderId";
  private static final String transAmt           			="transAmt";
  private static final String orderTime            			="orderTime";
  private static final String isPhone            			="isPhone";
  
  private static final String sign                ="sign";
  private static final String key                 ="key";
  
  @Override
  protected Map<String, String> buildPayParam() throws PayException {
      Map<String, String> payParam = new TreeMap<String, String>() {
          {
              put(merchantId, channelWrapper.getAPI_MEMBERID());
              put(orderId,channelWrapper.getAPI_ORDER_ID());
              put(transAmt,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
              put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
              put(payType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              put(pageUrl,channelWrapper.getAPI_WEB_URL());
              put(orderTime,DateUtil.formatDateTimeStrByParam("yyyy-MM-dd HH:mm:ss"));
              put(inputCharset,"UTF-8");
              if(HandlerUtil.isWapOrApp(channelWrapper)){
            	  put(isPhone,"1");
              }
          }
      };
      log.debug("[CPU支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
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
      signSrc.append(key+"="+channelWrapper.getAPI_KEY());
      String paramsStr = signSrc.toString();
      String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
      log.debug("[CPU支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
      return signMD5;
  }

  protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
      payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
      HashMap<String, String> result = Maps.newHashMap();
      ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
      result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());  //.replace("method='post'","method='get'"));
      payResultList.add(result);
      log.debug("[CPU支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
      log.debug("[CPU支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
      return requestPayResult;
  }
}