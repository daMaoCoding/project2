package dc.pay.business.tiantiandaifu;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.alibaba.fastjson.JSON;

import dc.pay.base.processor.DaifuResponseHandler;
import dc.pay.base.processor.PayException;
import dc.pay.config.annotation.ResponseDaifuHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author andrew
 * Sep 19, 2019
 */
@Slf4j
@ResponseDaifuHandler("TIANTIANDAIFU")
public final class TianTianDaiFuResponseHandler extends DaifuResponseHandler {
    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("SUCCESS");

    private static final String  FLAG               = "TIANTIANDAIFU:";
    
    //请求参数
    //参数名 参数类型    参数说明    是否必填
    //orderNum    String  合作商订单号  必填
    private static final String  orderNum            = "orderNum";   
    //pl_orderNum String  平台订单号   必填
//    private static final String  pl_orderNum            = "pl_orderNum";   
    //pl_transState   String  交易状态（1-成功，2-失败，3-未明）    必填
    private static final String  pl_transState            = "pl_transState";   
    //pl_transMessage String  交易说明    必填
//    private static final String  pl_transMessage            = "pl_transMessage";
    private static final String  transMoney            = "transMoney";
    
    private static final String  pl_groupId            = "pl_groupId";   
    private static final String  pl_code            = "pl_code";   
    private static final String  pl_sign            = "pl_sign";   
    


    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        String strFromRedis = handlerUtil.getStrFromRedis(FLAG+API_RESPONSE_PARAMS.get(pl_groupId));
        if (StringUtils.isBlank(strFromRedis))  throw new PayException("1.[天天代付]-[代付回调]-获取缓存公钥异常");

        Map<String, Object> decrypt_map;
        try {
            decrypt_map = ApiUtil.decrypt(API_RESPONSE_PARAMS.get(pl_sign), strFromRedis);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            log.error("[天天代付]-[代付回调]-解密出错：{}",e.getMessage(),e);
            throw new PayException(e.getMessage(),e);
        }
        String ordernumberR = decrypt_map.get(orderNum)+"";
        if (StringUtils.isBlank(ordernumberR))  throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("1.[天天代付]-[代付回调]-获取回调订单号：{}",ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> payParam, String api_key) throws PayException {
        
        boolean my_result = false;
        
        Map<String, Object> decrypt_map;
        try {
            decrypt_map = ApiUtil.decrypt(payParam.get(pl_sign), channelWrapper.getAPI_PUBLIC_KEY());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            my_result = false;
            log.error("[天天代付]-[代付回调]-解密出错：{}",e.getMessage(),e);
            throw new PayException(e.getMessage(),e);
        }
        if (null != decrypt_map.get("pl_transState") && StringUtils.isNotBlank(decrypt_map.get("pl_transState")+"")) {
            my_result = true;
        }
        log.debug("2.[天天代付]-[代付回调]-自建签名：{}",JSON.toJSONString(my_result));
        return String.valueOf(my_result);
    }



    @Override
    protected boolean checkAmount(Map<String, String> api_response_params, String amountDb) throws PayException {
        Map<String, Object> decrypt_map;
        try {
            decrypt_map = ApiUtil.decrypt(api_response_params.get(pl_sign), channelWrapper.getAPI_PUBLIC_KEY());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            log.error("3.[天天代付]-[代付回调]-验证回调金额：{}-解密出错：{}",e.getMessage(),e);
            throw new PayException(e.getMessage(),e);
        }

        boolean checkResult = false;
        
//        mc 2019/9/19 15:58:45
//        pl_code为0000且pl_transState为1代表代付成功
//        🙈 2019/9/19 15:58:49
//        好的        
        String responseAmount =  decrypt_map.get(transMoney)+"";
        boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        if (checkAmount)   checkResult = true;
        log.debug("3.[天天代付]-[代付回调]-验证回调金额：{}",checkResult);
        return checkResult;
    }

    //检查回调签名
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        Boolean my_result = new Boolean(signMd5);
        log.debug("4.[天天代付]-[代付回调]-验证第三方签名：{}",my_result.booleanValue());
        return my_result.booleanValue();
    }


    //响应回调的内容
    @Override
    protected String responseSuccess() {
        log.debug("5.[天天代付]-[代付回调]-响应第三方内容：{}",RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }



    //回调订单状态
    //***给我平台业务系统返回ERROR状态需要谨慎：返回不恰当，会造成业务系统后台可解锁订单而造成多次出款-->需要卖肾赔钱
    //***因此，当不确定的情况下，哪怕返回UNKNOW，也不要返回ERROR
    @Override
    protected PayEumeration.DAIFU_RESULT  resOrderStatus(Map<String, String> api_response_params) throws PayException {
        PayEumeration.DAIFU_RESULT  orderStatus = PayEumeration.DAIFU_RESULT.UNKNOW;
        
        //回调时pl_code为0000时    pl_transState只会123，对吧？
//        如果你们回调不返回金额，我是否可只判断你们下发状态来决定是否下发成功？@mc 
//        mc 2019/9/19 15:58:45
//        pl_code为0000且pl_transState为1代表代付成功
//        🙈 2019/9/19 15:58:49
//        好的
        if(api_response_params.containsKey(pl_code) && "0000".equals(api_response_params.get(pl_code))){
            Map<String, Object> decrypt_map;
            try {
                decrypt_map = ApiUtil.decrypt(api_response_params.get(pl_sign), channelWrapper.getAPI_PUBLIC_KEY());
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                log.error("3.[天天代付]-[代付回调]-验证回调金额：{}-解密出错：{}",e.getMessage(),e);
                throw new PayException(e.getMessage(),e);
            }
            //pl_transState String  交易状态（1-成功，2-失败，3-未明）
            if( "2".equalsIgnoreCase(decrypt_map.get(pl_transState)+"") ) orderStatus = PayEumeration.DAIFU_RESULT.ERROR;
            if( "3".equalsIgnoreCase(decrypt_map.get(pl_transState)+"") ) orderStatus = PayEumeration.DAIFU_RESULT.PAYING;
            if( "1".equalsIgnoreCase(decrypt_map.get(pl_transState)+"") ) orderStatus = PayEumeration.DAIFU_RESULT.SUCCESS;
        }
        log.debug("6.[天天代付]-[代付回调]-订单状态：{}",orderStatus);
        return orderStatus;
    }





}