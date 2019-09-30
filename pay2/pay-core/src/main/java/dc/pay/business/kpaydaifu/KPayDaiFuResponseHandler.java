package dc.pay.business.kpaydaifu;


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
 * @author Cobby
 * June 20, 2019
 */
@Slf4j
@ResponseDaifuHandler("KPAYDAIFU")
public final class KPayDaiFuResponseHandler extends DaifuResponseHandler {
    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("success");

    private static final String retcode = "retcode";           //    结果    string(5)    Y    0为请求成功，非0发起支付失败，非0需将通过回调或者查询确认代付交易结果
    //    private static final String retmsg           = "retmsg";          //    结果描述    string(255)    Y    错误信息描述
//    private static final String version          = "version";         //    版本号    string(3)    Y    默认填写2.0
//    private static final String charset          = "charset";         //    字符集    string(10)    Y    默认填写UTF-8
//    private static final String spid             = "spid";            //    商户号    string(10)    Y    我司分配的商户号
    private static final String spbillno = "spbillno";          //    商户代付单号    string(32)    Y    商户系统内部的订单号
    //    private static final String transactionId    = "transactionId";   //    k-pay 单号    string(32)    N    k-pay 单号
//    private static final String outTransactionId = "outTransactionId";//    第三方单号    string(32)    N    第三方单号
//    private static final String tranAmt          = "tranAmt";         //    代付金额    string(18)    Y    交易金额，单位为分
    private static final String payAmt = "payAmt";            //    实付金额    string(18)    N    实付金额，单位为分,交易成功返回
    private static final String result = "result";            //    代付状态    string(10)    N    代付结果
    //    private static final String msg              = "msg";             //    该笔代付状态描述    string(255)    Y    状态描述
//    private static final String attach           = "attach";          //    保留字段    string(255)    N    原样返回
    private static final String signType = "signType";          //    签名类型    string(10)    Y    目前只支持MD5
    private static final String sign = "sign";              //    关键参数签名    string(32)    Y    签名（MD5）


    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        String ordernumberR = API_RESPONSE_PARAMS.get(spbillno);
        if (StringUtils.isBlank(ordernumberR)) throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("1.[Kpay代付]-[代付回调]-获取回调订单号：{}", ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> payParam, String api_key) throws PayException {
        List          paramKeys = MapUtils.sortMapByKeyAsc(payParam);
        StringBuilder signSrc   = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!signType.equals(paramKeys.get(i)) && !sign.equals(paramKeys.get(i)) && StringUtils.isNotBlank(payParam.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
            }
        }
        signSrc.append("key=" + channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5   = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("2.[Kpay代付]-[代付回调]-生成md5签名：{}", signMd5);
        return signMd5;
    }

    @Override
    protected boolean checkAmount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean checkResult    = false;
        String  responseAmount = api_response_params.get(payAmt);
        boolean checkAmount    = amountDb.equalsIgnoreCase(responseAmount);
        if (checkAmount) checkResult = true;
        log.debug("3.[Kpay代付]-[代付回调]-验证回调金额：{}", checkResult);
        return checkResult;
    }

    //检查回调签名
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("4.[Kpay代付]-[代付回调]-验证第三方签名：{}", result);
        return result;
    }


    //响应回调的内容
    @Override
    protected String responseSuccess() {
        log.debug("5.[Kpay代付]-[代付回调]-响应第三方内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }

    //回调订单状态
    @Override
    protected PayEumeration.DAIFU_RESULT resOrderStatus(Map<String, String> api_response_params) throws PayException {
        //    processreview-等待复核    processsuccess-转账成功    processing-转账处理中    processfailed-转账失败    processreject-复核驳回
        PayEumeration.DAIFU_RESULT orderStatus = PayEumeration.DAIFU_RESULT.UNKNOW;
        if (api_response_params.containsKey(retcode)) {
            if (api_response_params.get(retcode).equalsIgnoreCase("0") &&
                    api_response_params.get(result).equalsIgnoreCase("processreview") ||
                    api_response_params.get(result).equalsIgnoreCase("processing")
                    ) orderStatus = PayEumeration.DAIFU_RESULT.PAYING;
            if (api_response_params.get(retcode).equalsIgnoreCase("0") &&
                    api_response_params.get(result).equalsIgnoreCase("processsuccess")
                    ) orderStatus = PayEumeration.DAIFU_RESULT.SUCCESS;
            else orderStatus = PayEumeration.DAIFU_RESULT.ERROR;
        }
        log.debug("6.[Kpay代付]-[代付回调]-订单状态：{}", orderStatus);
        return orderStatus;
    }

}