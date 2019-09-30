package dc.pay.business.youchuangdaifu;


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
 * July 06, 2019
 */
@Slf4j
@ResponseDaifuHandler("YOUCHUANGDAIFU")
public final class YouChuangDaiFuResponseHandler extends DaifuResponseHandler {
    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("success");


    private static final String businessnumber = "businessnumber";  //    业务订单号
    private static final String amount         = "amount";          //    交易金额     单位 分
    private static final String sign           = "sign";            //    签名
    private static final String result         = "result";          //    返回码      请求结果标志    success：请求授理成功，不代表交易成功    error：业务失败    exception：网络异常失败， 不代表交易失败
    private static final String status         = "status";          //    交易状态     成功/处理中/失败   如果报文头status为’成功’则表明接口返回成功，可以进行下面步骤的操作的，如果返回结果不为成功表示失败，具体的失败原因在remark中表明
    private static final String sign_type      = "sign_type";       //    签名算法类型
//  private static final String  code            = "code";            //    返回码描述   错误编码
//  private static final String  msg             = "msg";             //    错误描述     例:账户余额不足
//  private static final String  mer_id          = "mer_id";          //    商户号       支付分配给商户的mer_id
//  private static final String  transactiondate = "transactiondate"; //    交易时间     格式 yyyy-MM-dd HH:mm:ss
//  private static final String  transactiontype = "transactiontype"; //    交易类型     例:代付
//  private static final String  inputdate       = "inputdate";       //    交易创建时间 格式 yyyy-MM-dd HH:mm:ss
//  private static final String  remark          = "remark";          //    结果说明


    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        String ordernumberR = API_RESPONSE_PARAMS.get(businessnumber);
        if (StringUtils.isBlank(ordernumberR)) throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("1.[优创代付]-[代付回调]-获取回调订单号：{}", ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> payParam, String api_key) throws PayException {
        //生成md5
        String        pay_md5sign = null;
        List          paramKeys   = MapUtils.sortMapByKeyAsc(payParam);
        StringBuilder sb          = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (StringUtils.isBlank(payParam.get(paramKeys.get(i))) || sign.equalsIgnoreCase(paramKeys.get(i).toString()) || sign_type.equalsIgnoreCase(paramKeys.get(i).toString()))  //
                continue;
            sb.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
        }
        sb.append(channelWrapper.getAPI_KEY());
        String signStr = sb.toString(); //.replaceFirst("&key=","")
        pay_md5sign = HandlerUtil.getMD5UpperCase(signStr);
        log.debug("2.[优创代付]-[代付回调]-自建签名：{}", pay_md5sign);
        return pay_md5sign;
    }


    @Override
    protected boolean checkAmount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean checkResult    = false;
        String  responseAmount = api_response_params.get(amount);
        boolean checkAmount    = amountDb.equalsIgnoreCase(responseAmount);
        if (checkAmount) checkResult = true;
        log.debug("3.[优创代付]-[代付回调]-验证回调金额：{}", checkResult);
        return checkResult;
    }

    //检查回调签名
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("4.[优创代付]-[代付回调]-验证第三方签名：{}", result);
        return result;
    }


    //响应回调的内容
    @Override
    protected String responseSuccess() {
        log.debug("5.[优创代付]-[代付回调]-响应第三方内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }


    //回调订单状态
    @Override
    protected PayEumeration.DAIFU_RESULT resOrderStatus(Map<String, String> api_response_params) throws PayException {
//        result  返回码   请求结果标志    success：请求授理成功，不代表交易成功    error：业务失败    exception：网络异常失败， 不代表交易失败
        PayEumeration.DAIFU_RESULT orderStatus = PayEumeration.DAIFU_RESULT.UNKNOW;
        if (api_response_params.containsKey(result)) {
            if ("error".equalsIgnoreCase(api_response_params.get(result)))
                orderStatus = PayEumeration.DAIFU_RESULT.ERROR;
            if ("exception".equalsIgnoreCase(api_response_params.get(result)))
                orderStatus = PayEumeration.DAIFU_RESULT.PAYING;
//       status  交易状态     成功/处理中/失败   如果报文头status为’成功’则表明接口返回成功，可以进行下面步骤的操作的，如果返回结果不为成功表示失败，具体的失败原因在remark中表明
            if ("success".equalsIgnoreCase(api_response_params.get(result)) && "失败".equalsIgnoreCase(api_response_params.get(status)))
                orderStatus = PayEumeration.DAIFU_RESULT.ERROR;
            if ("success".equalsIgnoreCase(api_response_params.get(result)) && "处理中".equalsIgnoreCase(api_response_params.get(status)))
                orderStatus = PayEumeration.DAIFU_RESULT.PAYING;
            if ("success".equalsIgnoreCase(api_response_params.get(result)) && "成功".equalsIgnoreCase(api_response_params.get(status)))
                orderStatus = PayEumeration.DAIFU_RESULT.SUCCESS;
        }
        log.debug("6.[优创代付]-[代付回调]-订单状态：{}", orderStatus);
        return orderStatus;
    }


}