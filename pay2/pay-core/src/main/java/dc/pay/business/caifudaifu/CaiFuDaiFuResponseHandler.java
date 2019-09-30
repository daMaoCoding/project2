package dc.pay.business.caifudaifu;


import dc.pay.base.processor.DaifuResponseHandler;
import dc.pay.base.processor.PayException;
import dc.pay.business.sihaiyun.DigestUtil1;
import dc.pay.config.annotation.ResponseDaifuHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Cobby
 * Apr 20, 2019
 */
@Slf4j
@ResponseDaifuHandler("CAIFUDAIFU")
public final class CaiFuDaiFuResponseHandler extends DaifuResponseHandler {
	private static  String RESPONSE_PAY_MSG = stringResponsePayMsg("success");

//	参数名称 			参数含义 			参数长度 			参数说明 			验签顺序
//	r0_Cmd 			业务类型 			Max(20) 		固定值“TransPay” . 1
//	p1_MerId 		商户编号 			Max(18) 		商户在彩付系统的唯一身份标识2
//	p2_Order 		商户订单号	 	Max(20) 		下单接口时，商户传递的订单号3
//	r1_Code 		返回码 			Max(18) 		返回码，非空 4
//	r2_TrxId 		交易号，			非空
//	Max(20) 		交易号，			非空 5
//	r7_Desc 		返回信息			 Max(50) 		返回信息 6
	
	  private static final String r0_Cmd               	="r0_Cmd";
	  private static final String p1_MerId           	="p1_MerId";
	  private static final String p2_Order           	="p2_Order";
	  private static final String r1_Code           	="r1_Code";
	  private static final String r2_TrxId          	="r2_TrxId";
	  private static final String r7_Desc              	="r7_Desc";
	  private static final String hmac              	="hmac";

   @Override
   public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
       String ordernumberR = API_RESPONSE_PARAMS.get(p2_Order);
       if (StringUtils.isBlank(ordernumberR))  throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
       log.debug("1.[彩付代付]-[代付回调]-获取回调订单号：{}",ordernumberR);
       return ordernumberR;
   }


   @Override
   protected String buildPaySign(Map<String, String> payParam, String api_key) throws PayException {
       //生成md5
       String signStr=String.format("%s%s%s%s%s%s", 
    		   payParam.get(r0_Cmd),
    		   payParam.get(p1_MerId),
    		   payParam.get(p2_Order),
    		   payParam.get(r1_Code),
    		   payParam.get(r2_TrxId),
    		   payParam.get(r7_Desc)
    	);
       String paramsStr = signStr.toString();
       String pay_md5sign = DigestUtil1.hmacSign(paramsStr, channelWrapper.getAPI_KEY());
       log.debug("2.[彩付代付]-[代付回调]-自建签名：{}",pay_md5sign);
       return pay_md5sign;
   }



   @Override
   protected boolean checkAmount(Map<String, String> api_response_params, String amountDb) throws PayException {
       boolean checkResult = true;
       /*String responseAmount =  HandlerUtil.getFen(api_response_params.get(amount_str));
       boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
       if (checkAmount)   checkResult = true;*/
       log.debug("3.[彩付代付]-[代付回调]-验证回调金额：{}",checkResult);
       return checkResult;
   }

   //检查回调签名
   @Override
   protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
       boolean result = api_response_params.get(hmac).equalsIgnoreCase(signMd5);
       log.debug("4.[彩付代付]-[代付回调]-验证第三方签名：{}",result);
       return result;
   }


   //响应回调的内容
   @Override
   protected String responseSuccess() {
       log.debug("5.[彩付代付]-[代付回调]-响应第三方内容：{}",RESPONSE_PAY_MSG);
       return RESPONSE_PAY_MSG;
   }



   //回调订单状态
   @Override
   protected PayEumeration.DAIFU_RESULT  resOrderStatus(Map<String, String> api_response_params) throws PayException {
       PayEumeration.DAIFU_RESULT  orderStatus = PayEumeration.DAIFU_RESULT.UNKNOW;
       if(api_response_params.containsKey(r1_Code)){
          if(!("0000,3003,3004".indexOf(api_response_params.get(r1_Code))!=-1)) orderStatus = PayEumeration.DAIFU_RESULT.ERROR;
          if( "3003,3004".indexOf(api_response_params.get(r1_Code))!=-1) orderStatus = PayEumeration.DAIFU_RESULT.PAYING;
          if( "0000".indexOf(api_response_params.get(r1_Code))!=-1) orderStatus = PayEumeration.DAIFU_RESULT.SUCCESS;
       }
       log.debug("6.[彩付代付]-[代付回调]-订单状态：{}",orderStatus);
       return orderStatus;
   }
}