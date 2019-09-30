package dc.pay.business.tianchengzhifu;

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
@RequestPayHandler("TIANCHENGZHIFU")
public final class TianChengZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(TianChengZhiFuPayRequestHandler.class);

//    参数名称					参数含义				是否必填				参与签名				参数说明
//    pay_memberid				商户号				是					是					平台分配商户号
//    pay_member_orderid		订单号				是					是					上送订单号唯一, 字符长度大于10位小于30位(支持字母数字下划线)
//    pay_type					支付类型				是					是					2支付宝wap，3微信扫码，5微信h5支付，6支付宝扫码，7银联wap，8网关扫码
//    pay_amount				订单金额				是					是					单位：元(正整数金额，不要保留小数点)
//    pay_notifyurl				异步请求地址			是					是					异步通知地址.（POST返回数据）
//    returnurl					同步通知地址			是					是					支付成功后跳转到的地址
//    sign						MD5签名				是					是					（商户订单号+商户号+商户秘钥）
//    remark					备注					是					否	

  private static final String pay_memberid               	="pay_memberid";
  private static final String pay_member_orderid           	="pay_member_orderid";
  private static final String pay_type           			="pay_type";
  private static final String pay_amount           			="pay_amount";
  private static final String pay_notifyurl          		="pay_notifyurl";
  private static final String returnurl              		="returnurl";
  private static final String remark            			="remark";
  
  private static final String signType            ="signType";
  private static final String sign                ="sign";
  private static final String key                 ="key";

  @Override
  protected Map<String, String> buildPayParam() throws PayException {
      Map<String, String> payParam = new TreeMap<String, String>() {
          {
              put(pay_memberid, channelWrapper.getAPI_MEMBERID());
              put(pay_member_orderid,channelWrapper.getAPI_ORDER_ID());
              put(pay_amount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
              put(pay_notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
              put(pay_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              put(returnurl,channelWrapper.getAPI_WEB_URL());
          }
      };
      log.debug("[天成支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
      return payParam;
  }

   protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
      String signSrc=String.format("%s%s%s",
    		  api_response_params.get(pay_member_orderid),
    		  api_response_params.get(pay_memberid),
    		  channelWrapper.getAPI_KEY()
      );
      String paramsStr = signSrc.toString();
      String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
      log.debug("[天成支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
      return signMD5;
  }

  protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
      payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
      HashMap<String, String> result = Maps.newHashMap();
      ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
      result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());  //.replace("method='post'","method='get'"));
      payResultList.add(result);
      log.debug("[天成支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
      log.debug("[天成支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
      return requestPayResult;
  }
}