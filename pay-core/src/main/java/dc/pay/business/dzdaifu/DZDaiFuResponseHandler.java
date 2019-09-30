package dc.pay.business.dzdaifu;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import dc.pay.base.processor.DaifuResponseHandler;
import dc.pay.base.processor.PayException;
import dc.pay.config.annotation.ResponseDaifuHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RsaUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author andrew
 * Aug 5, 2019
 */
@Slf4j
@ResponseDaifuHandler("DZDAIFU")
public final class DZDaiFuResponseHandler extends DaifuResponseHandler {
    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("success");

    //顺序  参数名 参数描述    非空  长度  示例
    //1   retCode 处理结果码   是       10000处理成功
    private static final String  retCode            = "retCode";   
    //2   retMsg  处理结果描述  否       
//    private static final String  retMsg            = "retMsg";   
    //3   merchantId  商户编号    是       商户号
//    private static final String  merchantId            = "merchantId";   
    //4   orderId 平台订单号   是       
//    private static final String  orderId            = "orderId";   
    //5   outOrderId  商户订单号   是       
    private static final String  outOrderId            = "outOrderId";   
    //6   amount  订单金额    是       
    private static final String  amount            = "amount";   
    //7   payAmount   到账金额    是       
//    private static final String  payAmount            = "payAmount";   
    //8   totalFee    手续费 是       
//    private static final String  totalFee            = "totalFee";   
    //9   status  订单状态    是       （状态说明：1处理中，2成功，3失败或拒绝）
    private static final String  status            = "status";   
    //10  transTime   系统订单完成时间    是       
//    private static final String  transTime            = "transTime";   
    //11  sign    签名  是       
    private static final String  sign            = "sign";


    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        String ordernumberR = API_RESPONSE_PARAMS.get(outOrderId);
        if (StringUtils.isBlank(ordernumberR))  throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("1.[Dz代付]-[代付回调]-获取回调订单号：{}",ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        //生成md5
        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!sign.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {                
                sb.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        sb.append("key=" + channelWrapper.getAPI_KEY().split("-")[0]);
        String signStr = sb.toString(); //.replaceFirst("&key=","")
        pay_md5sign = HandlerUtil.getMD5UpperCase(signStr, "UTF-8").toUpperCase();

        boolean my_result = RsaUtil.validateSignByPublicKey2(pay_md5sign,channelWrapper.getAPI_PUBLIC_KEY(),api_response_params.get(sign));
        log.debug("2.[Dz代付]-[代付回调]-自建签名：{}",String.valueOf(my_result));
        return String.valueOf(my_result);
    }



    @Override
    protected boolean checkAmount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean checkResult = false;
        String responseAmount =  HandlerUtil.getFen(api_response_params.get(amount));
        boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        if (checkAmount)   checkResult = true;
        log.debug("3.[Dz代付]-[代付回调]-验证回调金额：{}",checkResult);
        return checkResult;
    }

    //检查回调签名
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        Boolean my_result = new Boolean(signMd5);
        
//        boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("4.[Dz代付]-[代付回调]-验证第三方签名：{}",my_result.booleanValue());
        return my_result.booleanValue();
    }


    //响应回调的内容
    @Override
    protected String responseSuccess() {
        log.debug("5.[Dz代付]-[代付回调]-响应第三方内容：{}",RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }



    //回调订单状态
    //***给我平台业务系统返回ERROR状态需要谨慎：返回不恰当，会造成业务系统后台可解锁订单而造成多次出款-->需要卖肾赔钱
    //***因此，当不确定的情况下，哪怕返回UNKNOW，也不要返回ERROR
    @Override
    protected PayEumeration.DAIFU_RESULT  resOrderStatus(Map<String, String> api_response_params) throws PayException {
        PayEumeration.DAIFU_RESULT  orderStatus = PayEumeration.DAIFU_RESULT.UNKNOW;
        //1   retCode 处理结果码   是       10000处理成功
        if(api_response_params.containsKey(retCode)){
            //9   status  订单状态    是       （状态说明：1处理中，2成功，3失败或拒绝）
           if( "3".equalsIgnoreCase(api_response_params.get(status)) ) orderStatus = PayEumeration.DAIFU_RESULT.ERROR;
           if( "1".equalsIgnoreCase(api_response_params.get(status)) ) orderStatus = PayEumeration.DAIFU_RESULT.PAYING;
           if( "2".equalsIgnoreCase(api_response_params.get(status)) ) orderStatus = PayEumeration.DAIFU_RESULT.SUCCESS;
        }
        log.debug("6.[Dz代付]-[代付回调]-订单状态：{}",orderStatus);
        return orderStatus;
    }





}