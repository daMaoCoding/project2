package dc.pay.business.huitaodaifu;

/**
 * ************************
 * @author tony 3556239829
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

import java.net.URLDecoder;
import java.util.List;
import java.util.Map;

@Slf4j
@ResponseDaifuHandler("HUITAODAIFU")
public final class HuiTaoDaiFuResponseHandler extends DaifuResponseHandler {
     private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("SUCCESS");

     private static final String  tradeTime = "tradeTime";     // "2019-03-01 13:10:08",
     private static final String  orderAmount = "orderAmount";     // "2.00",
     private static final String  orderTime = "orderTime";     // "2019-03-01 13:09:38",
     private static final String  tradeNo = "tradeNo";     // "T1903011309383646616",
     private static final String  merchantId = "merchantId";     // "M60579514",
     private static final String  merOrderNo = "merOrderNo";     // "20190301130937653123",
     private static final String  tradeStatus = "tradeStatus";     // "success",
     private static final String  sign = "sign";     // "056D5758443242FBBEC36C92FA449A35",
     private static final String  signType = "signType";     // "MD5"




    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        String ordernumberR = API_RESPONSE_PARAMS.get(merOrderNo);
        if (StringUtils.isBlank(ordernumberR))  throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("1.[汇淘代付]-[代付回调]-获取回调订单号：{}",ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> payParam, String api_key) throws PayException {
        //生成md5
        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(payParam);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if(org.apache.commons.lang.StringUtils.isBlank(payParam.get(paramKeys.get(i))) || sign.equalsIgnoreCase(paramKeys.get(i).toString())    )  //
                continue;
            sb.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
        }
        sb.append("key=" + channelWrapper.getAPI_KEY());
        String signStr = sb.toString(); //.replaceFirst("&key=","")
        pay_md5sign = HandlerUtil.getMD5UpperCase(signStr);
        log.debug("2.[汇淘代付]-[代付回调]-自建签名：{}",pay_md5sign);
        return pay_md5sign;
    }



    @Override
    protected boolean checkAmount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean checkResult = false;
        String responseAmount =  HandlerUtil.getFen(api_response_params.get(orderAmount));
        boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        if (checkAmount)   checkResult = true;
        log.debug("3.[汇淘代付]-[代付回调]-验证回调金额：{}",checkResult);
        return checkResult;
    }

    //检查回调签名
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("4.[汇淘代付]-[代付回调]-验证第三方签名：{}",result);
        return result;
    }


    //响应回调的内容
    @Override
    protected String responseSuccess() {
        log.debug("5.[汇淘代付]-[代付回调]-响应第三方内容：{}",RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }



    //回调订单状态
    @Override
    protected PayEumeration.DAIFU_RESULT  resOrderStatus(Map<String, String> api_response_params) throws PayException {
        PayEumeration.DAIFU_RESULT  orderStatus = PayEumeration.DAIFU_RESULT.UNKNOW;
        if(api_response_params.containsKey(tradeStatus)){
           if( "failure".equalsIgnoreCase(api_response_params.get(tradeStatus)) ) orderStatus = PayEumeration.DAIFU_RESULT.ERROR;
           if( "pending".equalsIgnoreCase(api_response_params.get(tradeStatus)) ) orderStatus = PayEumeration.DAIFU_RESULT.PAYING;
           if( "success".equalsIgnoreCase(api_response_params.get(tradeStatus)) ) orderStatus = PayEumeration.DAIFU_RESULT.SUCCESS;
        }
        log.debug("6.[汇淘代付]-[代付回调]-订单状态：{}",orderStatus);
        return orderStatus;
    }





}