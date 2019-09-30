package dc.pay.business.dijia2zhifu;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayRequestHandler;
import dc.pay.business.RequestPayResult;
import dc.pay.business.kuaitongbaozhifu.RSAUtils;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.ValidateUtil;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;



/**
 * @author sunny
 * 05 21, 2019
 */
@RequestPayHandler("DIJIA2ZHIFU")
public final class DiJia2ZhiFuPayRequestHandler extends PayRequestHandler {
  private static final Logger log = LoggerFactory.getLogger(DiJia2ZhiFuPayRequestHandler.class);

  private static final String billNumero               ="billNumero";
  private static final String money           			 ="money";
  private static final String currency           		 ="currency";
  private static final String goodsName            ="goodsName";
  private static final String goodsDesc             ="goodsDesc";
  private static final String payType            		="payType";
  private static final String notifyUrl           		="notifyUrl";
  
  private static final String merchantId           		="merchantId";
  private static final String askTime           		="askTime";
  
  private static final String signType            		="signType";
  private static final String sign               		 	="sign";
  private static final String key                 			="key";

  @Override
  protected Map<String, String> buildPayParam() throws PayException {
      Map<String, String> payParam = new TreeMap<String, String>() {
          {
              put(merchantId, channelWrapper.getAPI_MEMBERID());
              put(billNumero,channelWrapper.getAPI_ORDER_ID());
              put(money,channelWrapper.getAPI_AMOUNT());
              put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
              put(payType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              put(askTime,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
              put(currency,"CNY");
              put(goodsName,channelWrapper.getAPI_ORDER_ID());
              put(goodsDesc,channelWrapper.getAPI_ORDER_ID());
          }
      };
      log.debug("[迪迦支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
      return payParam;
  }

   protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
	   JSONObject resulthead = new JSONObject();
	   resulthead.put("merchantId", api_response_params.get(merchantId));
       resulthead.put("version", "V1.1.0");
       resulthead.put("askTime", api_response_params.get(askTime));
       
       JSONObject resultbody = new JSONObject();
       resultbody.put("money", api_response_params.get(money)); //单位分
       resultbody.put("billNumero", api_response_params.get(billNumero));//商户单号 需要确保唯一
       resultbody.put("currency", "CNY");
       resultbody.put("goodsDesc",api_response_params.get(goodsDesc));//商品描述
       resultbody.put("goodsName", api_response_params.get(goodsName));//商品名称
       resultbody.put("notifyUrl", api_response_params.get(notifyUrl));
       resultbody.put("orderCreateIp", channelWrapper.getAPI_Client_IP());//商户发起支付请求的IP
       resultbody.put("payType", api_response_params.get(payType));// 1.微信扫码 WECHAT_NATIVE 2.银联扫码 UNION_NATIVE 3.QQ钱包扫码 QQ_NAIVE
       resultbody.put("userId", api_response_params.get(merchantId));
	   resultbody.put("bankNumber", "1000");
	   resultbody.put("cardType", "SAVINGS");
      
       String signMD5="";
	   try {
		   signMD5 = RSAUtils.verifyAndEncryptionToString(resultbody, resulthead, channelWrapper.getAPI_KEY() , channelWrapper.getAPI_PUBLIC_KEY());
	    } catch (Exception e) {
		e.printStackTrace();
	   }
      log.debug("[迪迦支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
      return signMD5;
  }

  protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
       HashMap<String, String> result = Maps.newHashMap();
       ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
       JSONObject jsonParam = new JSONObject();
       jsonParam.put("context", pay_md5sign);
       JSONObject resJson=null;
       HashMap<String, String> payMap = Maps.newHashMap();
	   payMap.put("context", pay_md5sign);
       result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payMap).toString().replace("method='post'","method='get'"));  //.replace("method='post'","method='get'"));
       payResultList.add(result);
       log.debug("[迪迦支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
      log.debug("[迪迦支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
      return requestPayResult;
  }
  
}