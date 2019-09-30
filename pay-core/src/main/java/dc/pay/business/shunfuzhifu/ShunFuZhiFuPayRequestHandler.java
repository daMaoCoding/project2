package dc.pay.business.shunfuzhifu;

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
@RequestPayHandler("SHUNFUZHIFU")
public final class ShunFuZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(ShunFuZhiFuPayRequestHandler.class);

//    字段名						变量名						必填				类型						示例值										描述
//    商户单号					billNumero				是					String(50)			order201712010001			商户上送订单号，保持唯一值。
//    交易金额					money						是					String(20)			1000										以分为单位，10.00元填入值为1000
//    币种							currency					是					String(10)			CNY										CNY-人民币，默认为CNY
//    商品名称					goodsName			是					String(20)			用于描述该笔交易或商品的主体信息
//    商品描述					goodsDesc			是					String(500)			用于描述该笔交易或商品的主体信息
//    商品备注					goodsRemark		否					String(500)			用于描述该笔交易或商品的主体信息
//    支付类型					payType					是					String(20)			ALI_H5	详见”4.1：交易类型“
//    异步通知地址			notifyUrl					是					String(255)			支付结果后台异步通知地址。(不能含有’字符. 如果含有?&=字符, 必须先对该地址做URL编码)
//    字符集						charset					否					String(2)	00	00表示UTF-8，暂时只支持UTF-8
//    版本号						version					否					String(10)	1.0.0	默认为1.0.0，采用向下兼容原则传入版本为1.1.0时，异步通知密文返回交易类型；明文返回商户单号。
//    平台商户号				merchantId				是					String(32)	PAY10017681024	商户在平台注册与使用的商户编号
//    请求时间					askTime					是					String(14)	20180303161616	暂时只支持RSA，必须大写的RSA
//    签名信息					sign							是					String		使用商户证书对报文签名后值
    
//    字符集						charset					否					String(2)				00	00表示UTF-8，暂时只支持UTF-8
//    版本号						version					否					String(10)	1.0.0	默认为1.0.0，采用向下兼容原则传入版本为1.1.0时，异步通知密文返回交易类型；明文返回商户单号。
//    平台商户号				merchantId				是					String(32)	PAY10017681024	商户在平台注册与使用的商户编号
//    请求时间					askTime					是					String(14)	20180303161616	暂时只支持RSA，必须大写的RSA
//    签名信息					sign							是					String		使用商户证书对报文签名后值

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
      log.debug("[顺付支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
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
       if((HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper))){
    	   resultbody.put("userId", api_response_params.get(merchantId));
    	   resultbody.put("bankNumber", "1000");
    	   resultbody.put("cardType", "SAVINGS");
       }
      
       String signMD5="";
	   try {
		   signMD5 = RSAUtils.verifyAndEncryptionToString(resultbody, resulthead, channelWrapper.getAPI_KEY() , channelWrapper.getAPI_PUBLIC_KEY());
	    } catch (Exception e) {
		e.printStackTrace();
	   }
      log.debug("[顺付支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
      return signMD5;
  }

  protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
       HashMap<String, String> result = Maps.newHashMap();
       ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
       JSONObject jsonParam = new JSONObject();
       jsonParam.put("context", pay_md5sign);
       JSONObject resJson=null;
       if ((HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper))) {
    	   HashMap<String, String> payMap = Maps.newHashMap();
    	   payMap.put("context", pay_md5sign);
           result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payMap).toString().replace("method='post'","method='get'"));  //.replace("method='post'","method='get'"));
           payResultList.add(result);
       }else{
       try {
	        resJson = HttpClients.doPost(channelWrapper.getAPI_CHANNEL_BANK_URL(), jsonParam);
	        if (null != resJson && resJson.containsKey("success") && resJson.getString("success").equals("true")) {
	        	String context=resJson.getString("context");
	        	String decrypt= RSAUtils.decryptByPrivateKey(context,channelWrapper.getAPI_KEY() );
				JSONObject resultJson = JSONObject.fromObject(decrypt);
	            if ( null!=resultJson && resultJson.containsKey("businessContext")  && null!=resultJson.getJSONObject("businessContext")
	                      &&  resultJson.getJSONObject("businessContext").containsKey("payurl")
	                      && StringUtils.isNotBlank(resultJson.getJSONObject("businessContext").getString("payurl"))
	                      && !ValidateUtil.isHaveChinese(resultJson.getJSONObject("businessContext").getString("payurl"))) {
	                      if(HandlerUtil.isWapOrApp(channelWrapper)){
	                          result.put(JUMPURL,  resultJson.getJSONObject("businessContext").getString("payurl"));
	                      }else{
	                          result.put(QRCONTEXT,  resultJson.getJSONObject("businessContext").getString("payurl"));
	                      }
	                      payResultList.add(result);
	              }else{ throw new PayException(resultJson.toString());}
	
	        }else {
	            throw new PayException(JSON.toJSONString(resJson.toString()));
	        }
        } catch (Exception e) {
        	log.error("[顺付支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resJson.toString()) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(JSON.toJSONString(resJson.toString()));
		}
       }
        log.debug("[顺付支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
      log.debug("[顺付支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
      return requestPayResult;
  }
  
}