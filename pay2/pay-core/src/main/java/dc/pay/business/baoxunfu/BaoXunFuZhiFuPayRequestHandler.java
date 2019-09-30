package dc.pay.business.baoxunfu;

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
 * @author sunny
 * 26 04, 2019
 */
@RequestPayHandler("BAOXUNFUZHIFU")
public final class BaoXunFuZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(BaoXunFuZhiFuPayRequestHandler.class);

//    参数				类型				必填			说明
//    parter			String			M			商户号
//    type				String			M			银行类型，具体请参考附录1
//    value				String			M			单位元（人民币），2位小数，最小支付金额为2.00
//    orderid			String			M			商户系统订单号，该订单号将作为接口的返回数据。该值需在商户系统内唯一，系统暂时不检查该值是否唯一，最长位数３２位
//    callbackurl		String			M			下行异步通知过程的返回地址，需要以http://开头且没有任何参数
//    payerIp			String			C			用户在下单时的真实IP，接口将会判断玩家支付时的ip和该值是否相同。若不相同，接口将提示用户支付风险
//    attach			String			C			备注信息，下行中会原样返回。若该值包含中文，请注意编码
//    sign				tring			M			32位小写MD5签名值，GB2312编码

  private static final String parter               	="parter";
  private static final String type           		="type";
  private static final String value           		="value";
  private static final String orderid           	="orderid";
  private static final String callbackurl           ="callbackurl";
  private static final String payerIp               ="payerIp";
  private static final String attach            	="attach";
  
  private static final String signType            ="signType";
  private static final String sign                ="sign";
  private static final String key                 ="key";

  @Override
  protected Map<String, String> buildPayParam() throws PayException {
      Map<String, String> payParam = new TreeMap<String, String>() {
          {
              put(parter, channelWrapper.getAPI_MEMBERID());
              put(orderid,channelWrapper.getAPI_ORDER_ID());
              put(value,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
              put(callbackurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
              put(type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              put(payerIp,channelWrapper.getAPI_Client_IP());
          }
      };
      log.debug("[宝迅支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
      return payParam;
  }

   protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
      String signSrc=String.format("%s%s%s%s%s%s",
    		  parter+"="+ api_response_params.get(parter)+"&",
    		  type+"="+ api_response_params.get(type)+"&",
    		  value+"="+ api_response_params.get(value)+"&",
    		  orderid +"="+ api_response_params.get(orderid )+"&",
    		  callbackurl +"="+ api_response_params.get(callbackurl ),
    		  channelWrapper.getAPI_KEY()
      );
      String paramsStr = signSrc.toString();
      String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
      log.debug("[宝迅支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
      return signMD5;
  }

  protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
      payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
      HashMap<String, String> result = Maps.newHashMap();
      ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
      result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());  //.replace("method='post'","method='get'"));
      payResultList.add(result);
      log.debug("[宝迅支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
      log.debug("[宝迅支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
      return requestPayResult;
  }
}