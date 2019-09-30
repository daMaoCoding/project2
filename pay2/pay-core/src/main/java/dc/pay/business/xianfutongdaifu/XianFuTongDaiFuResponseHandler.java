package dc.pay.business.xianfutongdaifu;


import dc.pay.base.processor.DaifuResponseHandler;
import dc.pay.base.processor.PayException;
import dc.pay.config.annotation.ResponseDaifuHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Cobby
 * May 14, 2019
 */
@Slf4j
@ResponseDaifuHandler("XIANFUTONGDAIFU")
public final class XianFuTongDaiFuResponseHandler extends DaifuResponseHandler {
    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("success");


    private static final String  order_status          ="order_status";     //   单状态
    private static final String  amount                ="amount";           //   金额(元)
    private static final String  merchant_order_no     ="merchant_order_no";//   商户订单号
    private static final String  sign                  ="sign";             //   签名


    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        String ordernumberR = API_RESPONSE_PARAMS.get(merchant_order_no);
        if (StringUtils.isBlank(ordernumberR))  throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("1.[先付通代付]-[代付回调]-获取回调订单号：{}",ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> payParam, String api_key) throws PayException {
        StringBuilder sb = new StringBuilder((payParam.size() + 1) * 10);
        List<String> keys = new ArrayList<>(payParam.keySet());
        Collections.sort(keys);
        for (String key : keys) {
            if (!sign.equals(key)){
                sb.append(key).append("=");
                sb.append(payParam.get(key));
                sb.append("&");
            }
        }
//            sb.setLength(sb.length() - 1);
        String preStr = sb.toString()+"key=" +channelWrapper.getAPI_KEY();
        String signMd5 =  HandlerUtil.getMD5UpperCase(preStr).toUpperCase();
        log.debug("2.[先付通代付]-[代付回调]-生成md5签名：{}",signMd5);
        return signMd5;
    }

    @Override
    protected boolean checkAmount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean checkResult = false;
        String responseAmount = HandlerUtil.getFen(api_response_params.get(amount)) ;
        boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        if (checkAmount)   checkResult = true;
        log.debug("3.[先付通代付]-[代付回调]-验证回调金额：{}",checkResult);
        return checkResult;
    }

    //检查回调签名
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("4.[先付通代付]-[代付回调]-验证第三方签名：{}",result);
        return result;
    }


    //响应回调的内容
    @Override
    protected String responseSuccess() {
        log.debug("5.[先付通代付]-[代付回调]-响应第三方内容：{}",RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }

    //回调订单状态
    @Override
    protected PayEumeration.DAIFU_RESULT  resOrderStatus(Map<String, String> api_response_params) throws PayException {
        //  PROCESSING:处理中    COMPLETED:已完成    PAID_FAIL:代付失败
        PayEumeration.DAIFU_RESULT  orderStatus = PayEumeration.DAIFU_RESULT.UNKNOW;
        if(api_response_params.containsKey(order_status)){
           if( api_response_params.get(order_status).equalsIgnoreCase("PROCESSING") ) orderStatus = PayEumeration.DAIFU_RESULT.PAYING;
           if( api_response_params.get(order_status).equalsIgnoreCase("COMPLETED") ) orderStatus = PayEumeration.DAIFU_RESULT.SUCCESS;
           else orderStatus = PayEumeration.DAIFU_RESULT.ERROR;
        }
        log.debug("6.[先付通代付]-[代付回调]-订单状态：{}",orderStatus);
        return orderStatus;
    }

}