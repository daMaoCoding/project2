package dc.pay.business.fanchaodaifu;

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
import java.util.TreeMap;

/**
 * 
 * @author andrew
 * May 17, 2019
 */
@Slf4j
@ResponseDaifuHandler("FANCHAODAIFU")
public final class FanChaoDaiFuResponseHandler extends DaifuResponseHandler {
    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("success");

    //2.9.2参数
    //参数名称    参数含义    长度  是否必填    备注
    //merchantId  商户ID    10  是   商户在中心的唯一标识
    private static final String  merchantId            = "merchantId";   
    //orderNo 商户订单号   40  是   商户提交的订单号
    private static final String  orderNo            = "orderNo";   
    //amount  订单金额    10  是   订单金额 （单位：分）
    private static final String  amount            = "amount";   
    //totalCount  总笔数 10  是   订单拆分笔数
    private static final String  totalCount            = "totalCount";   
    //outAmount   汇出金额    10  是   成功汇出金额（单位：分）
    private static final String  outAmount            = "outAmount";   
    //outCount    汇出笔数    10  是   成功汇出笔数
    private static final String  outCount            = "outCount";   
    //resultCode  交易结果    5   是   0：未处理    1：处理中    2：已处理    4：汇出退回   5：订单不存在
    private static final String  resultCode            = "resultCode";   
    //sign    签名数据    32  是   32位小写的组合加密验证串
    private static final String  sign            = "sign";   


    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        String ordernumberR = API_RESPONSE_PARAMS.get(orderNo);
        if (StringUtils.isBlank(ordernumberR))  throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("1.[凡超代付]-[代付回调]-获取回调订单号：{}",ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> payParam, String api_key) throws PayException {
        Map<String,String> map = new TreeMap<>(payParam);
        map.put("key", channelWrapper.getAPI_KEY());
        //生成md5
        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(map);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if(org.apache.commons.lang3.StringUtils.isBlank(payParam.get(paramKeys.get(i))) || sign.equalsIgnoreCase(paramKeys.get(i).toString())    )  //
                continue;
            sb.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
        }
        //删除最后一个字符
        sb.deleteCharAt(sb.length()-1);
        String signStr = sb.toString(); //.replaceFirst("&key=","")
        pay_md5sign = HandlerUtil.getMD5UpperCase(signStr).toLowerCase();
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);
        log.debug("2.[凡超代付]-[代付回调]-自建签名：{}",pay_md5sign);
        return pay_md5sign;
    }



    @Override
    protected boolean checkAmount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean checkResult = false;
        String responseAmount =  api_response_params.get(outAmount);
        boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        if (checkAmount)   checkResult = true;
        log.debug("3.[凡超代付]-[代付回调]-验证回调金额：{}",checkResult);
        return checkResult;
    }

    //检查回调签名
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("4.[凡超代付]-[代付回调]-验证第三方签名：{}",result);
        return result;
    }


    //响应回调的内容
    @Override
    protected String responseSuccess() {
        log.debug("5.[凡超代付]-[代付回调]-响应第三方内容：{}",RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }



    //回调订单状态
    //***给我平台业务系统返回ERROR状态需要谨慎：返回不恰当，会造成业务系统后台可解锁订单而造成多次出款-->需要卖肾赔钱
    //***因此，当不确定的情况下，哪怕返回UNKNOW，也不要返回ERROR
    @Override
    protected PayEumeration.DAIFU_RESULT  resOrderStatus(Map<String, String> api_response_params) throws PayException {
        PayEumeration.DAIFU_RESULT  orderStatus = PayEumeration.DAIFU_RESULT.UNKNOW;
        if(api_response_params.containsKey(resultCode)){
           if( "0".equalsIgnoreCase(api_response_params.get(resultCode)) ) orderStatus = PayEumeration.DAIFU_RESULT.UNKNOW;
           if( "4".equalsIgnoreCase(api_response_params.get(resultCode)) || "5".equalsIgnoreCase(api_response_params.get(resultCode)) ) orderStatus = PayEumeration.DAIFU_RESULT.ERROR;
           if( "1".equalsIgnoreCase(api_response_params.get(resultCode)) ) orderStatus = PayEumeration.DAIFU_RESULT.PAYING;
           if( "2".equalsIgnoreCase(api_response_params.get(resultCode)) ){
               if ("0".equalsIgnoreCase(api_response_params.get(outCount))) {
                   orderStatus = PayEumeration.DAIFU_RESULT.ERROR;                
                } else if (api_response_params.get(amount).equalsIgnoreCase(api_response_params.get(outAmount)) && api_response_params.get(totalCount).equalsIgnoreCase(api_response_params.get(outCount))) {
                    orderStatus = PayEumeration.DAIFU_RESULT.SUCCESS;
                }
           }
        }
        log.debug("6.[凡超代付]-[代付回调]-订单状态：{}",orderStatus);
        return orderStatus;
    }





}