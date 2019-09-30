package dc.pay.business.jisufu;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.Sha1Util;

/**
 * 
 * @author sunny
 * Dec 14, 2018
 */
@ResponsePayHandler("JISUFU")
public final class JiSuFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());
//  参数 			类型 				说明 							允许为空
//  order_no 		字符串 			订单号，订单唯⼀标识，⻓度20； 		否
//  user_id 		字符串 			商家⽤户ID（shopUserId） 		否
//  shop_no 		字符串 			商家订单号 (shopNo) 			是
//  money 		字符串 			订单⾦额 (amountInString) 		否
//  type 			字符串 			⽀付宝：alipay,⽀付宝转银⾏：bank (payChannel)否
//  date 			字符串 			订单⽇期 						否
//  trade_no 		字符串 			交易流⽔号，⻓度不超过50；(shopNo) 否
//  status 		整型 				⽀付状态，0表示⽀付成功；			 否
//  sign 			字符串 			验签字符串，MD5（shopAccountId + shopUserId +  trade_no +KEY+money+type）；字符串 相加再计算MD5⼀次，32位⼩写；

  private static final String order_no                   ="order_no";
  private static final String user_id                    ="user_id";
  private static final String shop_no                    ="shop_no";
  private static final String money                		 ="money";
  private static final String type             			 ="type";
  private static final String date                 		 ="date";
  private static final String trade_no              	 ="trade_no";
  private static final String status              		 ="status";
  //signature    数据签名    32    是    　
  private static final String signature  ="sign";

  private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("success");

  @Override
  public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
      if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
          throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
      //String partnerR = API_RESPONSE_PARAMS.get(user_id);
      String ordernumberR = API_RESPONSE_PARAMS.get(shop_no);
      if (StringUtils.isBlank(ordernumberR))
          throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
      log.debug("[极速付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
      return ordernumberR;
  }

  @Override
  protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
  	String signSrc=String.format("%s", 
  			channelWrapper.getAPI_MEMBERID()+
      		"0"+
      		api_response_params.get(trade_no)+
      		channelWrapper.getAPI_KEY()+
      		api_response_params.get(money)+
      		api_response_params.get(type)
      );
      String paramsStr = signSrc.toString();
      String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
      log.debug("[极速付]-[响应支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
      return signMD5;
  }

  @Override
  protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
      boolean my_result = false;
      String payStatusCode = api_response_params.get(status);
      String responseAmount = HandlerUtil.getFen(api_response_params.get(money));
      //db_amount数据库存入的是分     第三方返回的responseAmount是元
//      boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
      boolean checkAmount = HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");
      //1代表第三方支付成功
      if (checkAmount&&payStatusCode.equalsIgnoreCase("0")) {
          my_result = true;
      } else {
          log.error("[极速付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
      }
      log.debug("[极速付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：0");
      return my_result;
  }

  @Override
  protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
      boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
      log.debug("[极速付]-[响应支付]-4.验证MD5签名：{}", my_result);
      return my_result;
  }

  @Override
  protected String responseSuccess() {
      log.debug("[极速付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
      return  RESPONSE_PAY_MSG;
  }
}