package dc.pay.business.baili1daifu;

/**
 * ************************
 * @author sunny
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
@ResponseDaifuHandler("BAILI1DAIFU")
public final class BaiLi1DaiFuResponseHandler extends DaifuResponseHandler {
    private static  String RESPONSE_PAY_MSG = stringResponsePayMsg("SUCCESS");

     private static final String  service = "service";   // 服务名，固定为agent_distribution
     private static final String  input_charset = "input_charset";   // 通信使用的字符编码集，一般与该笔订单请求时传入的字符集相同
     private static final String  partner = "partner";   // 商户号，由平台分配
     private static final String  sign_type = "sign_type";   // 签名算法，目前只有MD5
     private static final String  notify_id = "notify_id";   // 本次通知的ID
     private static final String  notify_time = "notify_time";   // 通知时间，格式yyyyMMddHHmmss
     private static final String  out_trade_no = "out_trade_no";   // 商户代付结算订单号
     private static final String  trade_status = "trade_status";   // 代付订单状态
     private static final String  error_code = "error_code";   // 错误码
     private static final String  error_message = "error_message";   // 错误消息
     private static final String  amount_str = "amount_str";   //代付金额，单位元，保留两位小数
     private static final String  charges = "charges";   //手续费，单位元，保留两位小数，手续费将从账户余额扣除
     private static final String  trade_no = "trade_no";   //手续费，单位元，保留两位小数，手续费将从账户余额扣除
     private static final String  sign = "sign";   //签名

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        String ordernumberR = API_RESPONSE_PARAMS.get(out_trade_no);
        if (StringUtils.isBlank(ordernumberR))  throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        RESPONSE_PAY_MSG=stringResponsePayMsg(API_RESPONSE_PARAMS.get(notify_id));
        log.debug("1.[百利1]-[代付回调]-获取回调订单号：{}",ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> payParam, String api_key) throws PayException {
        //生成md5
        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(payParam);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if(sign.equalsIgnoreCase(paramKeys.get(i).toString()))  //
                continue;
            sb.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
        }
        sb.replace(sb.lastIndexOf("&"), sb.lastIndexOf("&") + 1, "" );	
        sb.append(channelWrapper.getAPI_KEY());
        String signStr = sb.toString(); //.replaceFirst("&key=","")
        pay_md5sign = HandlerUtil.getMD5UpperCase(signStr).toLowerCase();
        log.debug("2.[百利1]-[代付回调]-自建签名：{}",pay_md5sign);
        return pay_md5sign;
    }



    @Override
    protected boolean checkAmount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean checkResult = false;
        String responseAmount =  HandlerUtil.getFen(api_response_params.get(amount_str));
        boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        if (checkAmount)   checkResult = true;
        log.debug("3.[百利1]-[代付回调]-验证回调金额：{}",checkResult);
        return checkResult;
    }

    //检查回调签名
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("4.[百利1]-[代付回调]-验证第三方签名：{}",result);
        return result;
    }


    //响应回调的内容
    @Override
    protected String responseSuccess() {
        log.debug("5.[百利1]-[代付回调]-响应第三方内容：{}",RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }



    //回调订单状态
    @Override
    protected PayEumeration.DAIFU_RESULT  resOrderStatus(Map<String, String> api_response_params) throws PayException {
        PayEumeration.DAIFU_RESULT  orderStatus = PayEumeration.DAIFU_RESULT.UNKNOW;
        if(api_response_params.containsKey(trade_status)){
           if( "4,2,-1".indexOf(api_response_params.get(trade_status))!=-1) orderStatus = PayEumeration.DAIFU_RESULT.ERROR;
           if( "0,1".indexOf(api_response_params.get(trade_status))!=-1) orderStatus = PayEumeration.DAIFU_RESULT.PAYING;
           if( "3".indexOf(api_response_params.get(trade_status))!=-1) orderStatus = PayEumeration.DAIFU_RESULT.SUCCESS;
        }
        log.debug("6.[百利1]-[代付回调]-订单状态：{}",orderStatus);
        return orderStatus;
    }





}