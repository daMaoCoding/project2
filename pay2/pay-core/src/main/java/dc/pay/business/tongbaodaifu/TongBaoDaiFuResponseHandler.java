package dc.pay.business.tongbaodaifu;

import java.util.Map;

import org.apache.commons.lang.StringUtils;

import dc.pay.base.processor.DaifuResponseHandler;
import dc.pay.base.processor.PayException;
import dc.pay.config.annotation.ResponseDaifuHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author andrew
 * Aug 6, 2019
 */
@Slf4j
@ResponseDaifuHandler("TONGBAODAIFU")
public final class TongBaoDaiFuResponseHandler extends DaifuResponseHandler {
    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("success");

    //以下是有订单回调时的信息字段
    //字段名 变量名 类型  描述
    //商户ID    appId   String  商户唯一标识
//    private static final String  appId            = "appId";   
    //订单号 tradeNo String  通宝平台订单号
//    private static final String  tradeNo            = "tradeNo";   
    //商户订单号   apiTradeNo  String  商户系统内部订单号，要求32个字符内，只能是数字、大小写字母_-|*且在同一个商户号下唯一。
    private static final String  apiTradeNo            = "apiTradeNo";   
    //回调地址    notifyUrl   String  订单回调地址
//    private static final String  notifyUrl            = "notifyUrl";   
    //代付金额    orderAmount String  订单代付金额（单位：元）
//    private static final String  orderAmount            = "orderAmount";
    //订单状态    status  String  订单状态码
    private static final String  status            = "status";   
    //状态说明    result  String  订单状态说明
//    private static final String  result            = "result";   
    //创建时间    createTime  String  订单创建时间
//    private static final String  createTime            = "createTime";   
    //修改时间    updateTime  String  订单修改时间
//    private static final String  updateTime            = "updateTime";   
    //订单备注    remark  String  订单备注
//    private static final String  remark            = "remark";   
//    //订单号 tradeNo String  平台代付订单号
//    private static final String  tradeNo            = "tradeNo";
    private static final String  money            = "money";   
    
//    private static final String  respCode            = "respCode";  
    
//    private static final String  sign            = "sign";


    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        String ordernumberR = API_RESPONSE_PARAMS.get(apiTradeNo);
        if (StringUtils.isBlank(ordernumberR))  throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("1.[通宝代付]-[代付回调]-获取回调订单号：{}",ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> payParam, String api_key) throws PayException {
        //生成md5
//        List paramKeys = MapUtils.sortMapByKeyAsc(payParam);
//        StringBuilder sb = new StringBuilder();
//        for (int i = 0; i < paramKeys.size(); i++) {
//            if(org.apache.commons.lang3.StringUtils.isBlank(payParam.get(paramKeys.get(i))) || sign.equalsIgnoreCase(paramKeys.get(i).toString())    )  //
//                continue;
//            sb.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
//        }
//      //删除最后一个字符
//        sb.deleteCharAt(sb.length()-1);
////        sb.append("key=" + channelWrapper.getAPI_KEY());
//        String signStr = sb.toString(); //.replaceFirst("&key=","")
        
        boolean my_result = false;
        try {
            my_result = RSAUtil.rsaCheck(payParam,  channelWrapper.getAPI_PUBLIC_KEY());

//            my_result=  RsaUtil.validateSignByPublicKey(signStr, channelWrapper.getAPI_PUBLIC_KEY(), payParam.get(sign), "SHA256WithRSA");
        } catch (Exception e) {
            e.printStackTrace();
            log.debug("2.[通宝代付]-[代付回调]-自建签名异常：{}",String.valueOf(my_result));
        }
//        System.out.println("my_result====>"+my_result);
//        System.out.println("tils.pairListToMap(params)====>"+Utils.pairListToMap(params));
        log.debug("2.[通宝代付]-[代付回调]-自建签名：{}",String.valueOf(my_result));
        return String.valueOf(my_result);
    }



    @Override
    protected boolean checkAmount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean checkResult = false;

//      //第三方返回：100.00  100时，去除小数转换成分的处理方法
//        double s = Double.valueOf(api_response_params.get(money));
//        int num1 = (int) s;//整数部分
//        String responseAmount = Integer.toString(num1);
        
        String responseAmount =  handlerUtil.getFen(api_response_params.get(money));

        boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        if (checkAmount)   checkResult = true;
        log.debug("3.[通宝代付]-[代付回调]-验证回调金额：{}",checkResult);
        return checkResult;
    }

    //检查回调签名
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        Boolean my_result = new Boolean(signMd5);
        
//        boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("4.[通宝代付]-[代付回调]-验证第三方签名：{}",my_result.booleanValue());
        return my_result.booleanValue();
    }


    //响应回调的内容
    @Override
    protected String responseSuccess() {
        log.debug("5.[通宝代付]-[代付回调]-响应第三方内容：{}",RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }



    //回调订单状态
    //***给我平台业务系统返回ERROR状态需要谨慎：返回不恰当，会造成业务系统后台可解锁订单而造成多次出款-->需要卖肾赔钱
    //***因此，当不确定的情况下，哪怕返回UNKNOW，也不要返回ERROR
    @Override
    protected PayEumeration.DAIFU_RESULT  resOrderStatus(Map<String, String> api_response_params) throws PayException {
        PayEumeration.DAIFU_RESULT  orderStatus = PayEumeration.DAIFU_RESULT.UNKNOW;
        //灰太狼 2019/8/2 11:29:16
        //1 进行中        2 成功        3 失败        4 异常        5 等待中        6 代付中        7 失败        8 成功        9 申请失败        10 处理中
        if(api_response_params.containsKey(status)){
           if( "3".equalsIgnoreCase(api_response_params.get(status)) ||
                   "4".equalsIgnoreCase(api_response_params.get(status)) ||
                   "7".equalsIgnoreCase(api_response_params.get(status)) ||
                   "9".equalsIgnoreCase(api_response_params.get(status))) orderStatus = PayEumeration.DAIFU_RESULT.ERROR;
           if( "1".equalsIgnoreCase(api_response_params.get(status)) ||
                   "5".equalsIgnoreCase(api_response_params.get(status)) ||
                   "6".equalsIgnoreCase(api_response_params.get(status)) ||
                   "10".equalsIgnoreCase(api_response_params.get(status))) orderStatus = PayEumeration.DAIFU_RESULT.PAYING;
           if( "2".equalsIgnoreCase(api_response_params.get(status)) ||
                   "8".equalsIgnoreCase(api_response_params.get(status))) orderStatus = PayEumeration.DAIFU_RESULT.SUCCESS;
        }
        log.debug("6.[通宝代付]-[代付回调]-订单状态：{}",orderStatus);
        return orderStatus;
    }





}