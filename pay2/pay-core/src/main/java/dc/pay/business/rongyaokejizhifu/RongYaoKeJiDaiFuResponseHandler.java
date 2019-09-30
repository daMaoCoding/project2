package dc.pay.business.rongyaokejizhifu;



import dc.pay.base.processor.DaifuResponseHandler;
import dc.pay.base.processor.PayException;
import dc.pay.config.annotation.ResponseDaifuHandler;
import dc.pay.constant.PayEumeration;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;

/**
 * @author Mikey
 * Jun 19, 2019
 */
@Slf4j
@ResponseDaifuHandler("RONGYAOKEJIDAIFU")
public final class RongYaoKeJiDaiFuResponseHandler extends DaifuResponseHandler {
    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("success");

     /**
      *
      *
      *
      *
      *
      *  第三方使用自动查询，无回调处理。本类无用。
      *
      *
      *
      *
      *
      *
      *
      *
      *
      *
      */


     @Override
     public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
         throw new PayException("该第三方使用[代付自动查询]，本类无用。");
     }


     @Override
     protected String buildPaySign(Map<String, String> payParam, String api_key) throws PayException {
         String pay_md5sign = null;
         log.debug("2.[荣耀科技代付]-[代付回调]-自建签名：{}",pay_md5sign);
         return pay_md5sign;
     }



     @Override
     protected boolean checkAmount(Map<String, String> api_response_params, String amountDb) throws PayException {
         boolean checkResult = false;
         log.debug("3.[荣耀科技代付]-[代付回调]-验证回调金额：{}",checkResult);
         return checkResult;
     }

     //检查回调签名
     @Override
     protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
         boolean result = false;
         log.debug("4.[荣耀科技代付]-[代付回调]-验证第三方签名：{}",result);
         return result;
     }


     //响应回调的内容
     @Override
     protected String responseSuccess() {
         log.debug("5.[荣耀科技代付]-[代付回调]-响应第三方内容：{}",RESPONSE_PAY_MSG);
         return RESPONSE_PAY_MSG;
     }



     //回调订单状态
     @Override
     protected PayEumeration.DAIFU_RESULT  resOrderStatus(Map<String, String> api_response_params) throws PayException {
         PayEumeration.DAIFU_RESULT  orderStatus = PayEumeration.DAIFU_RESULT.UNKNOW;
         log.debug("6.[荣耀科技代付]-[代付回调]-订单状态：{}",orderStatus);
         return orderStatus;
     }

}