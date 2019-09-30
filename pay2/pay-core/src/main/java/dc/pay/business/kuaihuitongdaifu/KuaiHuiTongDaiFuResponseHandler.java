package dc.pay.business.kuaihuitongdaifu;

import dc.pay.base.processor.DaifuResponseHandler;
import dc.pay.base.processor.PayException;
import dc.pay.config.annotation.ResponseDaifuHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.util.List;
import java.util.Map;

/**
 * 
 * @author andrew
 * Jul 24, 2019
 */
@Slf4j
@ResponseDaifuHandler("KUAIHUITONGDAIFU")
public final class KuaiHuiTongDaiFuResponseHandler extends DaifuResponseHandler {
    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("success");

    //4.4代付结果通知 1020
    //数据元名称   数据元标识   数据元格式   请求  响应  数据元取值说明
    //交易金额    transAmt    数字(1,20)    M       整数，以分为单位
    private static final String  transAmt            = "transAmt";   
    //结算金额    actualAmount    数字(1,20)    C       整数，以分为单位，扣除费率部分之后的金额
//    private static final String  actualAmount            = "actualAmount";   
    //交易日期    transDate   日期(8,8) M       yyyyMMdd
//    private static final String  transDate            = "transDate";   
    //订单号 orderNo 任意(1,20)    M       商户号+订单号+商户日期唯一标示一笔交易
    private static final String  orderNo            = "orderNo";   
    //订单状态    orderStatus 数字(1)   M       0-待支付 1-支付成功 2-支付失败其它-待确认
//    private static final String  orderStatus            = "orderStatus";   
    //交易流水    tranSerno   任意(1,30)    M       交易流水号
//    private static final String  tranSerno            = "tranSerno";   
    //签名  sign    任意(1,1024)  M       签名
    private static final String  sign            = "sign";   
    //通讯应答码   ret_code    任意(1,20)    M       0000-成功，非0000-失败，失败原因看通讯描述。
//    private static final String  ret_code            = "ret_code";   
    //通讯描述    ret_msg 任意(1,1024)  M       通讯描述
//    private static final String  ret_msg            = "ret_msg";   



    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        String ordernumberR = API_RESPONSE_PARAMS.get(orderNo);
        if (StringUtils.isBlank(ordernumberR))  throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("1.[快汇通代付]-[代付回调]-获取回调订单号：{}",ordernumberR);
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
        log.debug("2.[快汇通代付]-[代付回调]-自建签名：{}",pay_md5sign);
        return pay_md5sign;
    }



    @Override
    protected boolean checkAmount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean checkResult = false;
        String responseAmount =  api_response_params.get(transAmt);
        boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        if (checkAmount)   checkResult = true;
        log.debug("3.[快汇通代付]-[代付回调]-验证回调金额：{}",checkResult);
        return checkResult;
    }

    //检查回调签名
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("4.[快汇通代付]-[代付回调]-验证第三方签名：{}",result);
        return result;
    }


    //响应回调的内容
    @Override
    protected String responseSuccess() {
        log.debug("5.[快汇通代付]-[代付回调]-响应第三方内容：{}",RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }



    //回调订单状态
    //***给我平台业务系统返回ERROR状态需要谨慎：返回不恰当，会造成业务系统后台可解锁订单而造成多次出款-->需要卖肾赔钱
    //***因此，当不确定的情况下，哪怕返回UNKNOW，也不要返回ERROR
    @Override
    protected PayEumeration.DAIFU_RESULT  resOrderStatus(Map<String, String> api_response_params) throws PayException {
        JSONObject jsonObj = (JSONObject) JSON.toJSON(api_response_params);
        PayEumeration.DAIFU_RESULT  orderStatus = PayEumeration.DAIFU_RESULT.UNKNOW;
        //订单状态    orderStatus 数字(1)   M       0-待支付 1-支付成功 2-支付失败 其它-待确认
        //通讯应答码   ret_code    任意(1,20)    M       0000-成功，非0000-失败，失败原因看通讯描述。
        if( HandlerUtil.valJsonObj(jsonObj,"ret_code","0000")){
            if( HandlerUtil.valJsonObj(jsonObj,"orderStatus","0")) return PayEumeration.DAIFU_RESULT.PAYING;
            if( HandlerUtil.valJsonObj(jsonObj,"orderStatus","1")) return PayEumeration.DAIFU_RESULT.SUCCESS;
            if( HandlerUtil.valJsonObj(jsonObj,"orderStatus","2")) return PayEumeration.DAIFU_RESULT.ERROR;
            //其它-待确认
            if( !HandlerUtil.valJsonObj(jsonObj,"orderStatus","0") && !HandlerUtil.valJsonObj(jsonObj,"orderStatus","1") && !HandlerUtil.valJsonObj(jsonObj,"orderStatus","2")) return PayEumeration.DAIFU_RESULT.PAYING;
        }else if ( !HandlerUtil.valJsonObj(jsonObj,"ret_code","0000")) {
            return PayEumeration.DAIFU_RESULT.ERROR;
        }
        log.debug("6.[快汇通代付]-[代付回调]-订单状态：{}",orderStatus);
        return orderStatus;
    }





}