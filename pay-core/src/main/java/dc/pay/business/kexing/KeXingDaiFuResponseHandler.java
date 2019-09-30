package dc.pay.business.kexing;

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
@ResponseDaifuHandler("KEXINGDAIFU")
public final class KeXingDaiFuResponseHandler extends DaifuResponseHandler {
    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("success");


     private static final String  respDesc="respDesc";    // "交易成功",
     private static final String  orderNo="orderNo";    // "20190228163028858983",
     private static final String  merNo="merNo";    // "850460059325377",
     private static final String  productId="productId";    // "8002",
     private static final String  transType="transType";    // "PROXY_PAY",
     private static final String  serialId="serialId";    // "O270003672005",
     private static final String  signature="signature";    // "CoY0X443GOf3kD/fuodONVj3xpesUN2e+3epmVX+XPTJ8XKQAYAg9BugeGBDHslneGOSQnZjZSBGffrhIRq/0uXxMPBgkkfo4xgbyVjZ2E+HJGFhIEA7Z7jJq+QPDD+IlQRzcb83idHPb0gjlgAg3pSaDMM/KJNFWG7Ts8bgKYC8T3RFd2vaGLUUTaTpdz2dv+xIQWR8+GAS8E50i4SY56DQPr9ew5dTt5qpIqL5qAe29PfDFBcsRR3PmG3mm+sbFeQEVHyLi4NV87vw0WQABTbckCZUDlDR54S5WwbxKDwkslvJIT6vsqCt1M3uAyP9rmQiNlFh1tsW1OiRt3gLpQ==",
     private static final String  transAmt="transAmt";    // "100",
     private static final String  orderDate="orderDate";    // "20190228",
     private static final String  respCode="respCode";    // "0000"



    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        String ordernumberR = API_RESPONSE_PARAMS.get(orderNo);
        if (StringUtils.isBlank(ordernumberR))  throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("1.[科星代付]-[代付回调]-获取回调订单号：{}",ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> payParam, String api_key) throws PayException {
        String signMd5=FALSE;
        boolean verifyed = Rsa.verify(payParam, channelWrapper.getAPI_PUBLIC_KEY());
        if(verifyed) signMd5= TRUE;
        log.debug("2.[科星代付]-[代付回调]-生成md5签名：{}",signMd5);
        return signMd5;
    }



    @Override
    protected boolean checkAmount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean checkResult = false;
        String responseAmount =  api_response_params.get(transAmt);
        boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        if (checkAmount)   checkResult = true;
        log.debug("3.[科星代付]-[代付回调]-验证回调金额：{}",checkResult);
        return checkResult;
    }

    //检查回调签名
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean checkResult =false;
        checkResult = signMd5.equalsIgnoreCase(TRUE);
        log.debug("3.[科星代付]-[代付回调]-验证回调金额：{}",checkResult);
        return checkResult;
    }


    //响应回调的内容
    @Override
    protected String responseSuccess() {
        log.debug("5.[科星代付]-[代付回调]-响应第三方内容：{}",RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }



    //回调订单状态
    @Override
    protected PayEumeration.DAIFU_RESULT  resOrderStatus(Map<String, String> api_response_params) throws PayException {
        PayEumeration.DAIFU_RESULT  orderStatus = PayEumeration.DAIFU_RESULT.UNKNOW;
        if(api_response_params.containsKey(respCode)){
           if( api_response_params.get(respCode).startsWith("P") ) orderStatus = PayEumeration.DAIFU_RESULT.PAYING;
           if( api_response_params.get(respCode).equalsIgnoreCase("0000")  ) orderStatus = PayEumeration.DAIFU_RESULT.SUCCESS;
           if( api_response_params.get(respCode).equalsIgnoreCase("9998")  ) orderStatus = PayEumeration.DAIFU_RESULT.ERROR;
        }
        log.debug("6.[科星代付]-[代付回调]-订单状态：{}",orderStatus);
        return orderStatus;
    }





}