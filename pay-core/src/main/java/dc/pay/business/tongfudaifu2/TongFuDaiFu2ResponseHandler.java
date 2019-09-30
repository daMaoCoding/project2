package dc.pay.business.tongfudaifu2;



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
 * May 23, 2019
 */
@Slf4j
@ResponseDaifuHandler("TONGFUDAIFU2")
public final class TongFuDaiFu2ResponseHandler extends DaifuResponseHandler {
    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("success");

     private static final String  fxid       = "fxid";       //    商务号    是    唯一号，由通付提供
     private static final String  fxddh      = "fxddh";      //    商户订单号    是    平台返回商户提交的订单号
     private static final String  fxorder    = "fxorder";    //    平台订单号    是    平台内部生成的订单号
     private static final String  fxfee      = "fxfee";      //    金额    是    商户请求代付金额 单位0.01元
     private static final String  fxdffee    = "fxdffee";    //    手续费    是    商户请求代付扣除手续费金额，单位元
     private static final String  fxstatus   = "fxstatus";   //    订单状态    是    订单处理结果【0代付失败】【1代付成功】【2代付进行中】
     private static final String  fxbody     = "fxbody";     //    收款账户    是    收款人的账户
     private static final String  fxname     = "fxname";     //    开户名    是    收款人的开户名
     private static final String  fxaddress  = "fxaddress";  //    开户行    是    收款人的开户行，例如中国银行
//   private static final String  fxzhihang  = "fxzhihang";  //    开户行所在支行    否    收款人的开户地址所在支行
//   private static final String  fxsheng    = "fxsheng";    //    开户行所在省    否    收款人的开户地址所在省
//   private static final String  fxshi      = "fxshi";      //    开户行所在市    否    收款人的开户地址所在市
//   private static final String  fxlhh      = "fxlhh";      //    开户卡的联行号    否    开户卡对应银行联行号
     private static final String  fxsign     = "fxsign";     //    签名【md5(商务号+商户订单号+平台订单号+订单状态+金额+手续费+收款账户+开户名+开户行+商户秘钥)】    是    通过签名算法计算得出的签名值。



    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        String ordernumberR = API_RESPONSE_PARAMS.get(fxddh);
        if (StringUtils.isBlank(ordernumberR))  throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("1.[通付代付2]-[代付回调]-获取回调订单号：{}",ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> payParam, String api_key) throws PayException {
        //生成md5【md5(商务号+商户订单号+平台订单号+订单状态+金额+手续费+收款账户+开户名+开户行+商户秘钥)】
        String paramsStr = String.format("%s%s%s%s%s%s%s%s%s",
                payParam.get(fxid),
                payParam.get(fxddh),
                payParam.get(fxorder),
                payParam.get(fxstatus),
                payParam.get(fxfee),
                payParam.get(fxdffee),
                payParam.get(fxbody),
                payParam.get(fxname),
                payParam.get(fxaddress),
                channelWrapper.getAPI_KEY());
        String pay_md5sign =  HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);
        log.debug("2.[通付代付2]-[代付回调]-自建签名：{}",pay_md5sign);
        return pay_md5sign;
    }



    @Override
    protected boolean checkAmount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean checkResult = false;
        String responseAmount =  HandlerUtil.getFen(api_response_params.get(fxfee));
        boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        if (checkAmount)   checkResult = true;
        log.debug("3.[通付代付2]-[代付回调]-验证回调金额：{}",checkResult);
        return checkResult;
    }

    //检查回调签名
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(fxsign).equalsIgnoreCase(signMd5);
        log.debug("4.[通付代付2]-[代付回调]-验证第三方签名：{}",result);
        return result;
    }


    //响应回调的内容
    @Override
    protected String responseSuccess() {
        log.debug("5.[通付代付2]-[代付回调]-响应第三方内容：{}",RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }



    //回调订单状态
    @Override
    protected PayEumeration.DAIFU_RESULT  resOrderStatus(Map<String, String> api_response_params) throws PayException {
        //= "fxstatus"; //    订单状态    是    订单处理结果【0代付失败】【1代付成功】【2代付进行中】
        PayEumeration.DAIFU_RESULT  orderStatus = PayEumeration.DAIFU_RESULT.UNKNOW;
        if(api_response_params.containsKey(fxstatus)){
           if( "0".equalsIgnoreCase(api_response_params.get(fxstatus)) ) orderStatus = PayEumeration.DAIFU_RESULT.ERROR;
           if( "2".equalsIgnoreCase(api_response_params.get(fxstatus)) ) orderStatus = PayEumeration.DAIFU_RESULT.PAYING;
           if( "1".equalsIgnoreCase(api_response_params.get(fxstatus)) ) orderStatus = PayEumeration.DAIFU_RESULT.SUCCESS;
        }
        log.debug("6.[通付代付2]-[代付回调]-订单状态：{}",orderStatus);
        return orderStatus;
    }





}