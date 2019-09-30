package dc.pay.business.renrenfubaba;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;

/**
 * 
 * @author andrew
 * Sep 6, 2019
 */
@ResponsePayHandler("RENRENFUBABA")
public final class RenRenFuBaBaPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    public static final String FLAG              ="RENRENFUBABA"; //商户账号
    
    public static final String Result              ="Result"; 
    
    public static final String MerchantAccount              ="MerchantAccount"; //商户账号
    public static final String Action              ="Action"; //接口名称：业务行为;对应四种业务类型英文名称代表
    public static final String Data              ="Data"; //业务参数:json格式;业务参数json包经过AES机密后的信息
    public static final String sign              ="sign"; //签名:使用MD5加密的签名
    
    //名称 类型 描述 备注
    //MerchantOrderNo string 商户订单号 
    private static final String MerchantOrderNo                ="MerchantOrderNo";
    //OrderState string 订单完成状态 6：完成， 7：部分完成 
    private static final String OrderState                ="OrderState";
    //OrderPrice string 用户订单金额 业务信息 
    private static final String OrderPrice                ="OrderPrice";
    //OrderRealPrice string 用户实际支付金额 业务信息
    private static final String OrderRealPrice                ="OrderRealPrice";
    //Code int 请求返回码 业务信息 Time string 平台传送的当前时间 业务信息 
//    private static final String Code                ="Code";
    //Message string 附加信息 业务信息
//    private static final String Message                ="Message";
    //OrderType int 请求类型 请求类型分为充值和提现，充值为 1，提现为 2 
//    private static final String OrderType                ="OrderType";
    //MerchantFee string 手续费 业务信息 
//    private static final String MerchantFee                ="MerchantFee";
    //CompleteTime string 完成时间 业务信息 
//    private static final String CompleteTime                ="CompleteTime";
    //DealTime string 到账时间 业务信息
//    private static final String DealTime                ="DealTime";
    
//    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="Sign";

    private static final String RESPONSE_PAY_MSG = "SUCCESS";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
       //String api_KEY = channelWrapper.getAPI_KEY();
       //if (null == api_KEY || !api_KEY.contains("-") || api_KEY.split("-").length != 2) {
       //    log.error("[人人付88]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //    throw new PayException("[人人付88]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //}
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty() ||  StringUtils.isBlank(API_RESPONSE_PARAMS.get(MerchantAccount)) ||  StringUtils.isBlank(API_RESPONSE_PARAMS.get(Result))) 
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
//        String partnerR = API_RESPONSE_PARAMS.get(merchno);

        String flag_id = FLAG+":"+API_RESPONSE_PARAMS.get(MerchantAccount);
        String strFromRedis = handlerUtil.getStrFromRedis(flag_id);
        if ( StringUtils.isBlank(strFromRedis))
            throw new PayException("缓存里获取不到键："+flag_id+"的密钥，请检查 ");
        
        String reqResultData = "";
        try {
            //解密result
            reqResultData = AESUtil.decrypt(strFromRedis, API_RESPONSE_PARAMS.get(Result));
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
            log.error("[人人付88]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}",e.getMessage(),e);
            throw new PayException(e.getMessage(),e);
        }
        JSONObject reqJson = JSONObject.parseObject(reqResultData);
        String ordernumberR = reqJson.getString(MerchantOrderNo);
        if ( StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[人人付88]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        //Map<String, String> jsonToMap = handlerUtil.jsonToMap(api_response_params.get(data));
        
        String strFromRedis = handlerUtil.getStrFromRedis(FLAG+":"+api_response_params.get(MerchantAccount));
        String reqResultData = "";
        try {
            //解密result
            reqResultData = AESUtil.decrypt(strFromRedis, api_response_params.get(Result));
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
            log.error("[人人付88]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}",e.getMessage(),e);
            throw new PayException(e.getMessage(),e);
        }

        String paramsStr = MerchantAccount+"="+channelWrapper.getAPI_MEMBERID().split("&")[1]+"&Result="+api_response_params.get(Result)+"&Key="+api_key;

        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[人人付88]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }
    
    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {

        String strFromRedis = handlerUtil.getStrFromRedis(FLAG+":"+api_response_params.get(MerchantAccount));
        if ( StringUtils.isBlank(strFromRedis))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_HANDLER_GETREQPAYINFO_ERROR);
        
        String reqResultData = "";
        try {
            //解密result
            reqResultData = AESUtil.decrypt(strFromRedis, api_response_params.get(Result));
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
            log.error("[人人付88]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}",e.getMessage(),e);
            throw new PayException(e.getMessage(),e);
        }
        JSONObject reqJson = JSONObject.parseObject(reqResultData);
        
        boolean my_result = false;
        //
        String payStatusCode = reqJson.getString(OrderState);
        String responseAmount = HandlerUtil.getFen(reqJson.getString(OrderRealPrice));

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        // 1 代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("6")) {
            my_result = true;
        } else {
            log.error("[人人付88]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[人人付88]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：6");
        return my_result;
    }

    
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[人人付88]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[人人付88]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
    
    public String getReqEncryptData(Map<String, Object> paramMap,String aesKey) throws PayException {
      String encryptData = "";
      try {
          encryptData = AESUtil.encodeHexStr(AESUtil.encrypt(aesKey, JSON.toJSONString(paramMap)), false);
//          System.out.println(("getReqEncryptData.encryptData:" + encryptData));
      } catch (Exception e) {
          e.printStackTrace();
          log.debug("[人人付88]-[响应支付]-2.生成加密出错，签名出错：{}",e.getMessage(),e);
          throw new PayException(e.getMessage(),e);
      }
      return encryptData;
  }
}