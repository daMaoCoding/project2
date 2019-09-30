package dc.pay.business.wuxingdaifu;

/**
 * ************************
 * @author tony 3556239829
 */

import dc.pay.base.processor.DaifuResponseHandler;
import dc.pay.base.processor.PayException;
import dc.pay.config.annotation.ResponseDaifuHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.Map;

@Slf4j
@ResponseDaifuHandler("WUXINGDAIFU")
public final class WuXingDaiFuResponseHandler extends DaifuResponseHandler {
    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("SUCCESS");

     private static final String  orderType = "orderType";    // "pay",
     private static final String  amount = "amount";    // "1.00",
     private static final String  signTime = "signTime";    // "20190314162312",
     private static final String  createTime = "createTime";    // "2019-03-14 16:22:57",
     private static final String  merchantId = "merchantId";    // "15099035",
     private static final String  orderId = "orderId";    // "476636",
     private static final String  sign = "sign";    // "4dae1212bdadac69842f6795d0c6337859fbe9409106c8576f4728970396da7b",
     private static final String  signType = "signType";    // "",
     private static final String  merchantOrderId = "merchantOrderId";    // "20190314162255998512",
     private static final String  status = "status";    // "S"



    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        String ordernumberR = API_RESPONSE_PARAMS.get(merchantOrderId);
        if (StringUtils.isBlank(ordernumberR))  throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("1.[五星代付]-[代付回调]-获取回调订单号：{}",ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> payParam, String api_key) throws PayException {
        if(channelWrapper!=null && !channelWrapper.getAPI_KEY().contains("&")){
            throw new PayException("密钥填写错误，格式：[秘钥]&[Token],如：ABCD&1234");
        }
        //生成md5
        String pay_md5sign = null;
        try {
            pay_md5sign = WuXingDaiFuUtil.sign(payParam,channelWrapper.getAPI_KEY().split("&")[0]);
        }catch (Exception e){
           throw new PayException("生成自签名出错，请检查密钥");
        }
        log.debug("2.[五星代付]-[代付回调]-自建签名：{}",pay_md5sign);
        return pay_md5sign;
    }



    @Override
    protected boolean checkAmount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean checkResult = false;
        String responseAmount =  HandlerUtil.getFen(api_response_params.get(amount));
        boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        if (checkAmount)   checkResult = true;
        log.debug("3.[五星代付]-[代付回调]-验证回调金额：{}",checkResult);
        return checkResult;
    }

    //检查回调签名
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("4.[五星代付]-[代付回调]-验证第三方签名：{}",result);
        return result;
    }


    //响应回调的内容
    @Override
    protected String responseSuccess() {
        log.debug("5.[五星代付]-[代付回调]-响应第三方内容：{}",RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }



    //回调订单状态
    @Override
    protected PayEumeration.DAIFU_RESULT  resOrderStatus(Map<String, String> api_response_params) throws PayException {
        PayEumeration.DAIFU_RESULT  orderStatus = PayEumeration.DAIFU_RESULT.UNKNOW;
        if(api_response_params.containsKey(status)){
           if( "F".equalsIgnoreCase(api_response_params.get(status)) ) orderStatus = PayEumeration.DAIFU_RESULT.ERROR;
           if( "P".equalsIgnoreCase(api_response_params.get(status)) ) orderStatus = PayEumeration.DAIFU_RESULT.PAYING;
           if( "S".equalsIgnoreCase(api_response_params.get(status)) ) orderStatus = PayEumeration.DAIFU_RESULT.SUCCESS;
        }
        log.debug("6.[五星代付]-[代付回调]-订单状态：{}",orderStatus);
        return orderStatus;
    }





}