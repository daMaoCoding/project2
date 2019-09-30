package dc.pay.business.huofenghuangdaifu;


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
 * June 19, 2019
 */
@Slf4j
@ResponseDaifuHandler("HUOFENGHUANGDAIFU")
public final class HuoFengHuangDaiFuResponseHandler extends DaifuResponseHandler {
    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("success");


    /**
     * 第三方使用自动查询，无回调处理。本类无用。
     */

    private static final String tenantOrderNo = "tenantOrderNo";//   商户订单号
    private static final String sign          = "sign";         //   签名


    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        String ordernumberR = API_RESPONSE_PARAMS.get(tenantOrderNo);
        if (StringUtils.isBlank(ordernumberR)) throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("1.[火凤凰代付]-[代付回调]-获取回调订单号：{}", ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> payParam, String api_key) throws PayException {
        String preStr  = "key=" + channelWrapper.getAPI_KEY();
        String signMd5 = HandlerUtil.getMD5UpperCase(preStr).toUpperCase();
        log.debug("2.[火凤凰代付]-[代付回调]-生成md5签名：{}", signMd5);
        return signMd5;
    }

    @Override
    protected boolean checkAmount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean checkResult = true;
        log.debug("3.[火凤凰代付]-[代付回调]-验证回调金额：{}", checkResult);
        return checkResult;
    }

    //检查回调签名
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("4.[火凤凰代付]-[代付回调]-验证第三方签名：{}", result);
        return result;
    }


    //响应回调的内容
    @Override
    protected String responseSuccess() {
        log.debug("5.[火凤凰代付]-[代付回调]-响应第三方内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }

    //回调订单状态
    @Override
    protected PayEumeration.DAIFU_RESULT resOrderStatus(Map<String, String> api_response_params) throws PayException {
        //    交易状态(200:支付成功，201:处理中，其他:失败)
        PayEumeration.DAIFU_RESULT orderStatus = PayEumeration.DAIFU_RESULT.UNKNOW;
        log.debug("6.[火凤凰代付]-[代付回调]-订单状态：{}", orderStatus);
        return orderStatus;
    }

}