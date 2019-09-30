package dc.pay.business.yihaodaifu;


import dc.pay.base.processor.DaifuResponseHandler;
import dc.pay.base.processor.PayException;
import dc.pay.config.annotation.ResponseDaifuHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.util.Map;

/**
 * @author Cobby
 * July 16, 2019
 */
@Slf4j
@ResponseDaifuHandler("YIHAODAIFU")
public final class YiHaoDaiFuResponseHandler extends DaifuResponseHandler {
    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("success");

    private static final String result       = "result";      //代付状态        10    0 成功，1处理中，2失败
    private static final String mer_id       = "mer_id";      //商户号        20    商户号
    private static final String out_trade_no = "out_trade_no";//商户订单号        32
    private static final String pay_trade_no = "pay_trade_no";//平台订单号        32
    private static final String total_fee    = "total_fee";   //订单金额        8    单位：分
    private static final String real_fee     = "real_fee";    //实际代付金额        8    单位：分
    private static final String sign         = "sign";        //签名        32    签名，详情查看2.2备注


    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        String ordernumberR = API_RESPONSE_PARAMS.get(out_trade_no);
        if (StringUtils.isBlank(ordernumberR)) throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("1.[壹号代付]-[代付回调]-获取回调订单号：{}", ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> payParam, String api_key) throws PayException {
//        mer_id=XXX&out_trade_no=XXX&pay_trade_no=XXX&real_fee=XXX&result=0&total_fee=XXX&key= XXX
        String paramsStr = String.format("mer_id=%s&out_trade_no=%s&pay_trade_no=%s&real_fee=%s&result=%s&total_fee=%s&key=%s",
                payParam.get(mer_id),
                payParam.get(out_trade_no),
                payParam.get(pay_trade_no),
                payParam.get(real_fee),
                payParam.get(result),
                payParam.get(total_fee),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("2.[壹号代付]-[代付回调]-生成md5签名：{}", signMd5);
        return signMd5;
    }

    @Override
    protected boolean checkAmount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean checkResult    = false;
        String  responseAmount = api_response_params.get(total_fee);
        boolean checkAmount    = amountDb.equalsIgnoreCase(responseAmount);
        if (checkAmount) checkResult = true;
        log.debug("3.[壹号代付]-[代付回调]-验证回调金额：{}", checkResult);
        return checkResult;
    }

    //检查回调签名
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("4.[壹号代付]-[代付回调]-验证第三方签名：{}", result);
        return result;
    }


    //响应回调的内容
    @Override
    protected String responseSuccess() {
        log.debug("5.[壹号代付]-[代付回调]-响应第三方内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }

    //回调订单状态
    @Override
    protected PayEumeration.DAIFU_RESULT resOrderStatus(Map<String, String> api_response_params) throws PayException {
        //  "result"   代付状态           0 成功，1处理中，2失败
        PayEumeration.DAIFU_RESULT orderStatus = PayEumeration.DAIFU_RESULT.UNKNOW;
        if (api_response_params.containsKey(result)) {
            if (api_response_params.get(result).equalsIgnoreCase("1")) orderStatus = PayEumeration.DAIFU_RESULT.PAYING;
            if (api_response_params.get(result).equalsIgnoreCase("0")) orderStatus = PayEumeration.DAIFU_RESULT.SUCCESS;
            if (api_response_params.get(result).equalsIgnoreCase("2")) orderStatus = PayEumeration.DAIFU_RESULT.ERROR;
        }
        log.debug("6.[壹号代付]-[代付回调]-订单状态：{}", orderStatus);
        return orderStatus;
    }

}