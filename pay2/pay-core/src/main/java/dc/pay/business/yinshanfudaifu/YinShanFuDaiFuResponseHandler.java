package dc.pay.business.yinshanfudaifu;

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

/**
 * 
 * @author andrew
 * May 17, 2019
 */
@Slf4j
@ResponseDaifuHandler("YINSHANFUDAIFU")
public final class YinShanFuDaiFuResponseHandler extends DaifuResponseHandler {
    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("SUCCESS");

     private static final String  orderAmount = "orderAmount";   // "1.01",
     private static final String  orderTime = "orderTime";   // "2019-02-02 15:37:13",
     private static final String  transferStatus = "transferStatus";   // "SUCCESS",
     private static final String  transferTime = "transferTime";   // "2019-02-02 15:37:21",
     private static final String  merchantId = "merchantId";   // "MD63309719",
     private static final String  orderSn = "orderSn";   // "T1902021537134481651",
     private static final String  merOrderNo = "merOrderNo";   // "20190202153712950693",
     private static final String  sign = "sign";   // "B1AC8B93DA989BD06291D8C02E481BBA",
     private static final String  signType = "signType";   // "MD5"


    /**
     *
     *
     *
     *
     * 该第三方使用自动查询处理，本类无用。
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
     *
     *
     *
     *
     *
     *
     */




    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        String ordernumberR = API_RESPONSE_PARAMS.get(merOrderNo);
        if (StringUtils.isBlank(ordernumberR))  throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("1.[银闪付代付]-[代付回调]-获取回调订单号：{}",ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> payParam, String api_key) throws PayException {
        //生成md5
        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(payParam);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if(org.apache.commons.lang3.StringUtils.isBlank(payParam.get(paramKeys.get(i))) || sign.equalsIgnoreCase(paramKeys.get(i).toString())    )  //
                continue;
            sb.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
        }
        sb.append("key=" + channelWrapper.getAPI_KEY());
        String signStr = sb.toString(); //.replaceFirst("&key=","")
        pay_md5sign = HandlerUtil.getMD5UpperCase(signStr);
        log.debug("2.[银闪付代付]-[代付回调]-自建签名：{}",pay_md5sign);
        return pay_md5sign;
    }



    @Override
    protected boolean checkAmount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean checkResult = false;
        String responseAmount =  HandlerUtil.getFen(api_response_params.get(orderAmount));
        boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        if (checkAmount)   checkResult = true;
        log.debug("3.[银闪付代付]-[代付回调]-验证回调金额：{}",checkResult);
        return checkResult;
    }

    //检查回调签名
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("4.[银闪付代付]-[代付回调]-验证第三方签名：{}",result);
        return result;
    }


    //响应回调的内容
    @Override
    protected String responseSuccess() {
        log.debug("5.[银闪付代付]-[代付回调]-响应第三方内容：{}",RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }



    //回调订单状态
    @Override
    protected PayEumeration.DAIFU_RESULT  resOrderStatus(Map<String, String> api_response_params) throws PayException {
        PayEumeration.DAIFU_RESULT  orderStatus = PayEumeration.DAIFU_RESULT.UNKNOW;
        if(api_response_params.containsKey(transferStatus)){
           if( "FAILURE".equalsIgnoreCase(api_response_params.get(transferStatus)) ) orderStatus = PayEumeration.DAIFU_RESULT.ERROR;
           if( "PENDING".equalsIgnoreCase(api_response_params.get(transferStatus)) ) orderStatus = PayEumeration.DAIFU_RESULT.PAYING;
           if( "SUCCESS".equalsIgnoreCase(api_response_params.get(transferStatus)) ) orderStatus = PayEumeration.DAIFU_RESULT.SUCCESS;
        }
        log.debug("6.[银闪付代付]-[代付回调]-订单状态：{}",orderStatus);
        return orderStatus;
    }





}