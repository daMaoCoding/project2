package dc.pay.business.yishanfuzhifu;

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
 * 05 16, 2019
 */
@RequestPayHandler("YISHANFUZHIFU")
public final class YiShanFuZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(YiShanFuZhiFuPayRequestHandler.class);

//    参数			必选			类型			说明			示例值
//    appid			是			String		必填。		您的商户唯一标识	20180033
//    pay_type		是			String		wechat:微信、alipay:支付宝、alipay_red:支付宝红包、bank:支付宝转银行卡、alipay_rec:支付宝加好友转、alipayVariableTransfer:支付宝个人转账（金额浮动）、solidCode:微信跑分固码、alipaySolidCode: 支付宝跑分固码	alipay
//    amount		是			float		交易金额,必填。单位：元。精确小数点后2位	10.00
//    return_type	否			String		请求支付标识,app、PC、mobile	app
//    callback_url	是			String		回调地址	
//    success_url	否			String		支付成功后网页自动跳转地址	
//    error_url		否			String		支付失败时，或支付超时后网页自动跳转地址	
//    out_uid		否			String		用户网站的请求支付用户信息，可以是帐号也可以是数据库的ID	15017391234
//    out_trade_no	是			String		商户订单号	C20142222231234
//    version		是			String		接口版本号	v1.0
//    sign			是			String		签名字符串	

  private static final String appid               ="appid";
  private static final String pay_type            ="pay_type";
  private static final String amount           	  ="amount";
  private static final String return_type         ="return_type";
  private static final String callback_url        ="callback_url";
  private static final String success_url         ="success_url";
  private static final String error_url           ="error_url";
  private static final String out_uid             ="out_uid";
  private static final String out_trade_no        ="out_trade_no";
  private static final String version        	  ="version";
  
  private static final String signType            ="signType";
  private static final String sign                ="sign";
  private static final String key                 ="key";

  @Override
  protected Map<String, String> buildPayParam() throws PayException {
      Map<String, String> payParam = new TreeMap<String, String>() {
          {
              put(appid, channelWrapper.getAPI_MEMBERID().split("&")[0]);
              put(out_trade_no,channelWrapper.getAPI_ORDER_ID());
              put(amount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
              put(callback_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
              if(HandlerUtil.isZfbSM(channelWrapper)){
            	  put(pay_type,channelWrapper.getAPI_MEMBERID().split("&")[1]);
              }else{
            	  put(pay_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              }
              put(success_url,channelWrapper.getAPI_WEB_URL());
              put(version,"v1.0");
          }
      };
      log.debug("[易闪付支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
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
      String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr);
      log.debug("[易闪付支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
      return signMD5;
  }

  protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
      payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
      HashMap<String, String> result = Maps.newHashMap();
      ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
      result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());  //.replace("method='post'","method='get'"));
      payResultList.add(result);
      log.debug("[易闪付支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
      log.debug("[易闪付支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
      return requestPayResult;
  }
}