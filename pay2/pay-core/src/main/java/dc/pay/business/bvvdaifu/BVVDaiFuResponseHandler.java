package dc.pay.business.bvvdaifu;

import java.util.Map;

import org.apache.commons.lang.StringUtils;

import dc.pay.base.processor.DaifuResponseHandler;
import dc.pay.base.processor.PayException;
import dc.pay.config.annotation.ResponseDaifuHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author andrew
 * Sep 10, 2019
 */
@Slf4j
@ResponseDaifuHandler("BVVDAIFU")
public final class BVVDaiFuResponseHandler extends DaifuResponseHandler {
    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("ok");

    //返回结果和通知中的参数一致,包含如下内容：
    //字段名 变量名 类型  说明
    //订单号 orderno String  订单号
    private static final String  orderno            = "orderno";   
    //用户编号    usercode    String  用户编号
    private static final String  usercode            = "usercode";   
    //商户订单号   customno    String  商户订单号
    private static final String  customno            = "customno";   
    //银行名称    bankcode    String  银行编号
    private static final String  bankcode            = "bankcode";   
    //银行卡号    cardno  String  
    private static final String  cardno            = "cardno";   
    //真实姓名    realname    String  
    private static final String  realname            = "realname";   
    //身份证号    idcard  String  
    private static final String  idcard            = "idcard";   
    //提交支付金额  tjmoney String  提交支付金额
    private static final String  tjmoney            = "tjmoney";   
    //结算金额    money   String  结算金额
    private static final String  money            = "money";   
    //付款订单状态  status  int 订单状态: 3付款成功，  4付款失败
    private static final String  status            = "status";   
    //返回码 resultcode  String  返回码
//    private static final String  resultcode            = "resultcode";   
    //返回信息    resultmsg   String  返回信息
//    private static final String  resultmsg            = "resultmsg";   
    //数字签名    sign    String  数字签名
    private static final String  sign            = "sign";


    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        String ordernumberR = API_RESPONSE_PARAMS.get(customno);
        if (StringUtils.isBlank(ordernumberR))  throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("1.[BVV代付]-[代付回调]-获取回调订单号：{}",ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> payParam, String api_key) throws PayException {
        //生成md5
        
//        origin＝+“|”+ +“|”++“|”++“|”++“|”+ +“|”++ “|”++“|”++“|”++ “|”+md5key；
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(payParam.get(usercode)).append("|");
        signSrc.append(payParam.get(orderno)).append("|");
        signSrc.append(payParam.get(customno)).append("|");
        signSrc.append(payParam.get(bankcode)).append("|");
        signSrc.append(payParam.get(cardno)).append("|");
        signSrc.append(payParam.get(realname)).append("|");
        signSrc.append(payParam.get(idcard)).append("|");
        signSrc.append(payParam.get(tjmoney)).append("|");
        signSrc.append(payParam.get(money)).append("|");
        signSrc.append(payParam.get(status)).append("|");
        signSrc.append(channelWrapper.getAPI_KEY());
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        String paramsStr = signSrc.toString();
//        System.out.println("签名源串=========>"+paramsStr);
        String pay_md5sign = HandlerUtil.getMD5UpperCase(paramsStr);
        
        log.debug("2.[BVV代付]-[代付回调]-自建签名：{}",pay_md5sign);
        return pay_md5sign;
    }



    @Override
    protected boolean checkAmount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean checkResult = false;
        String responseAmount =  HandlerUtil.getFen(api_response_params.get(tjmoney));
        boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        if (checkAmount)   checkResult = true;
        log.debug("3.[BVV代付]-[代付回调]-验证回调金额：{}",checkResult);
        return checkResult;
    }

    //检查回调签名
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("4.[BVV代付]-[代付回调]-验证第三方签名：{}",result);
        return result;
    }


    //响应回调的内容
    @Override
    protected String responseSuccess() {
        log.debug("5.[BVV代付]-[代付回调]-响应第三方内容：{}",RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }



    //回调订单状态
    //***给我平台业务系统返回ERROR状态需要谨慎：返回不恰当，会造成业务系统后台可解锁订单而造成多次出款-->需要卖肾赔钱
    //***因此，当不确定的情况下，哪怕返回UNKNOW，也不要返回ERROR
    @Override
    protected PayEumeration.DAIFU_RESULT  resOrderStatus(Map<String, String> api_response_params) throws PayException {
        PayEumeration.DAIFU_RESULT  orderStatus = PayEumeration.DAIFU_RESULT.UNKNOW;
        //付款订单状态  status  int 订单状态: 3付款成功，  4付款失败
        if(api_response_params.containsKey(status)){
           if( "4".equalsIgnoreCase(api_response_params.get(status)) ) orderStatus = PayEumeration.DAIFU_RESULT.ERROR;
           if( "3".equalsIgnoreCase(api_response_params.get(status)) ) orderStatus = PayEumeration.DAIFU_RESULT.SUCCESS;
//           if(true) orderStatus = PayEumeration.DAIFU_RESULT.PAYING;
        }
        log.debug("6.[BVV代付]-[代付回调]-订单状态：{}",orderStatus);
        return orderStatus;
    }





}