package dc.pay.business.sepdaifu;

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
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author andrew
 * Sep 20, 2019
 */
@Slf4j
@ResponseDaifuHandler("SEPDAIFU")
public final class SepDaiFuResponseHandler extends DaifuResponseHandler {
    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("success");

    //报文域 变量命名    数据长度    处理要求    说明
    //签名方法    signMethod  8   M   取值：MD5
    private static final String  signMethod            = "signMethod";   
    //状态码 success 8   M   success=1 代表交易成功，success=0  代表交易失败。
    private static final String  success            = "success";   
    //应答码 respCode    8   M   详情应答码
    private static final String  respCode            = "respCode";   
    //应答信息    respMsg 128 M   
//    private static final String  respMsg            = "respMsg";   
    //商户号 merchantId  32  M   
//    private static final String  merchantId            = "merchantId";   
    //商户订单号   merOrderId  32  M   
    private static final String  merOrderId            = "merOrderId";   
    //交易金额    txnAmt  10  M   单位分
    private static final String  txnAmt            = "txnAmt"; 
    
    //签名信息    signature   64  M   
    private static final String  sign            = "signature";   
    

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        String ordernumberR = API_RESPONSE_PARAMS.get(merOrderId);
        if (StringUtils.isBlank(ordernumberR))  throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("1.[sep代付]-[代付回调]-获取回调订单号：{}",ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> payParam, String api_key) throws PayException {
        //生成md5
        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(payParam);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!signMethod.equals(paramKeys.get(i)) && !sign.equals(paramKeys.get(i))) {
                sb.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
            }
        }
        //删除最后一个字符
        sb.deleteCharAt(sb.length()-1);
        sb.append(channelWrapper.getAPI_KEY());
        String signStr = sb.toString(); //.replaceFirst("&key=","")
//        pay_md5sign = Base64Utils.encryptBASE64(HandlerUtil.getMD5UpperCase(signStr).toLowerCase().getBytes());//BASE64加密;
        pay_md5sign = new String(java.util.Base64.getEncoder().encode(HandlerUtil.getMD5UpperCase(signStr).toLowerCase().getBytes()));//BASE64加密

        log.debug("2.[sep代付]-[代付回调]-自建签名：{}",pay_md5sign);
        return pay_md5sign;
    }



    @Override
    protected boolean checkAmount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean checkResult = false;
        String responseAmount =  api_response_params.get(txnAmt);
        boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        if (checkAmount)   checkResult = true;
        log.debug("3.[sep代付]-[代付回调]-验证回调金额：{}",checkResult);
        return checkResult;
    }

    //检查回调签名
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("4.[sep代付]-[代付回调]-验证第三方签名：{}",result);
        return result;
    }


    //响应回调的内容
    @Override
    protected String responseSuccess() {
        log.debug("5.[sep代付]-[代付回调]-响应第三方内容：{}",RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }



    //回调订单状态
    //***给我平台业务系统返回ERROR状态需要谨慎：返回不恰当，会造成业务系统后台可解锁订单而造成多次出款-->需要卖肾赔钱
    //***因此，当不确定的情况下，哪怕返回UNKNOW，也不要返回ERROR
    @Override
    protected PayEumeration.DAIFU_RESULT  resOrderStatus(Map<String, String> api_response_params) throws PayException {
        PayEumeration.DAIFU_RESULT  orderStatus = PayEumeration.DAIFU_RESULT.UNKNOW;
        //状态码 success 8   M   success=1 代表交易成功，success=0  代表交易失败。
        if(api_response_params.containsKey(success)){
            if("0".equalsIgnoreCase(api_response_params.get(success)) && 
               ( "1002".equalsIgnoreCase(api_response_params.get(respCode)) ||
               "0001".equalsIgnoreCase(api_response_params.get(respCode)) ||
               "0002".equalsIgnoreCase(api_response_params.get(respCode)) ||
               "0201".equalsIgnoreCase(api_response_params.get(respCode)) ||
               "2000".equalsIgnoreCase(api_response_params.get(respCode)) ||
               "2002".equalsIgnoreCase(api_response_params.get(respCode)) ||
               "2003".equalsIgnoreCase(api_response_params.get(respCode)) ||
               "2004".equalsIgnoreCase(api_response_params.get(respCode)) ||
               "2005".equalsIgnoreCase(api_response_params.get(respCode)) ||
               "2006".equalsIgnoreCase(api_response_params.get(respCode)) ||
               "2007".equalsIgnoreCase(api_response_params.get(respCode)) ||
               "2008".equalsIgnoreCase(api_response_params.get(respCode)) ||
               "2009".equalsIgnoreCase(api_response_params.get(respCode)) ||
               "2010".equalsIgnoreCase(api_response_params.get(respCode)) ||
               "2011".equalsIgnoreCase(api_response_params.get(respCode)) ||
               "2012".equalsIgnoreCase(api_response_params.get(respCode)) ||
               "2013".equalsIgnoreCase(api_response_params.get(respCode)) ||
               "2014".equalsIgnoreCase(api_response_params.get(respCode)) ||
               "2015".equalsIgnoreCase(api_response_params.get(respCode)) ||
               "2016".equalsIgnoreCase(api_response_params.get(respCode)) ||
               "2017".equalsIgnoreCase(api_response_params.get(respCode)) ||
               "2018".equalsIgnoreCase(api_response_params.get(respCode)) ||
               "2019".equalsIgnoreCase(api_response_params.get(respCode)) ||
               "2020".equalsIgnoreCase(api_response_params.get(respCode)) ||
               "2021".equalsIgnoreCase(api_response_params.get(respCode)) ||
               "2022".equalsIgnoreCase(api_response_params.get(respCode)) ||
               "2023".equalsIgnoreCase(api_response_params.get(respCode)) ||
               "2024".equalsIgnoreCase(api_response_params.get(respCode)) ||
               "2025".equalsIgnoreCase(api_response_params.get(respCode)) ||
               "2026".equalsIgnoreCase(api_response_params.get(respCode)) ||
               "2039".equalsIgnoreCase(api_response_params.get(respCode)) ||
               "5001".equalsIgnoreCase(api_response_params.get(respCode))))
               orderStatus = PayEumeration.DAIFU_RESULT.ERROR;           
           if( "1".equalsIgnoreCase(api_response_params.get(success)) && ("0000".equalsIgnoreCase(api_response_params.get(respCode)) || "1111".equalsIgnoreCase(api_response_params.get(respCode)) || "9999".equalsIgnoreCase(api_response_params.get(respCode)))) orderStatus = PayEumeration.DAIFU_RESULT.PAYING;
           if( "1".equalsIgnoreCase(api_response_params.get(success)) && "1001".equalsIgnoreCase(api_response_params.get(respCode))) orderStatus = PayEumeration.DAIFU_RESULT.SUCCESS;
        }
        log.debug("6.[sep代付]-[代付回调]-订单状态：{}",orderStatus);
        return orderStatus;
    }





}