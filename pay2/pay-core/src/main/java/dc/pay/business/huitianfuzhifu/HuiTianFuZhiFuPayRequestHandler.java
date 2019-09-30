package dc.pay.business.huitianfuzhifu;

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
 * 05 22, 2019
 */
@RequestPayHandler("HUITIANFUZHIFU")
public final class HuiTianFuZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(HuiTianFuZhiFuPayRequestHandler.class);

//    字段名 							填写类型 													说明 
//    P_UserID 					必填 														商户编号如 1000001
//    P_OrderID 					必填 														商户定单号（要保证唯一），长度最长 32 字符
//    P_FaceValue 				必填 														申明交易金额，单位：元
//    P_ChannelID 				必填 														支付方式，支付方式编码：参照附录 6.1
//    P_Price 						必填 														商品售价
//    P_Result_URL				必填 														支付后异步通知地址，URL 参数是以 http://或 https://开头的 完整 URL 地址(后台处理) ᨀ交的 url 地址必须外网能访问到,否则无法通知商户
//    P_Notify_URL				非必填 													支付后返回的商户显示页面，URL 参数是以 http:// 或https://开头的完整 URL 地址(前台显示)
//    P_PostKey 					必填															MD5 签名结果

  private static final String P_UserID               			="P_UserID";
  private static final String P_OrderID           				="P_OrderID";
  private static final String P_FaceValue           			="P_FaceValue";
  private static final String P_ChannelID           			="P_ChannelID";
  private static final String P_Price          					="P_Price";
  private static final String P_Result_URL              		="P_Result_URL";
  private static final String P_Notify_URL           			="P_Notify_URL";
  private static final String P_CardId           				="P_CardId";
  private static final String P_CardPass           				="P_CardPass";
  private static final String P_Description           			="P_Description";
  
  private static final String signType            ="signType";
  private static final String sign                ="sign";
  private static final String key                 ="key";
  


  @Override
  protected Map<String, String> buildPayParam() throws PayException {
      Map<String, String> payParam = new TreeMap<String, String>() {
          {
              put(P_UserID, channelWrapper.getAPI_MEMBERID());
              put(P_OrderID,channelWrapper.getAPI_ORDER_ID());
              put(P_FaceValue,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
              put(P_Result_URL,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
              put(P_ChannelID,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
              put(P_Notify_URL,channelWrapper.getAPI_WEB_URL());
              put(P_Price,"1");
              put(P_CardId,"");
              put(P_CardPass,"");
              if(HandlerUtil.isWY(channelWrapper)){
            	  put(P_Description,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
              }
          }
      };
      log.debug("[汇天付支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
      return payParam;
  }

   protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
      String signSrc=String.format("%s|%s|%s|%s|%s|%s|%s", 
    		  api_response_params.get(P_UserID),
    		  api_response_params.get(P_OrderID),
    		  api_response_params.get(P_CardId),
    		  api_response_params.get(P_CardPass),
    		  api_response_params.get(P_FaceValue),
    		  api_response_params.get(P_ChannelID),
    		  channelWrapper.getAPI_KEY()
      );
      String paramsStr = signSrc.toString();
      String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
      log.debug("[汇天付支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
      return signMD5;
  }

  protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
      payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
      HashMap<String, String> result = Maps.newHashMap();
      ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
      result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());  //.replace("method='post'","method='get'"));
      payResultList.add(result);
      log.debug("[汇天付支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
      log.debug("[汇天付支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
      return requestPayResult;
  }
}