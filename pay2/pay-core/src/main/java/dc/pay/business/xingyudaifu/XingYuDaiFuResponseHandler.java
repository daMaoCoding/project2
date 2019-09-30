package dc.pay.business.xingyudaifu;


import dc.pay.base.processor.DaifuResponseHandler;
import dc.pay.base.processor.PayException;
import dc.pay.business.bsdaifu.SecurityUtils;
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
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author sunny
 * @date 10 Aug 2019
 */
@Slf4j
@ResponseDaifuHandler("XINGYUDAIFU")
public final class XingYuDaiFuResponseHandler extends DaifuResponseHandler {
    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("SUCCESS");


    private static final String  mch_id   					="mch_id";//   商户订单号	String	是	商户订单号
    private static final String  out_trade_no            	="out_trade_no";         //   商户编号	String	是	
    private static final String  withdraw_status            ="withdraw_status";         //   参数列表	String	是	使用RSA加密
    private static final String  total_fee            		="total_fee";         //   参数列表	String	是	使用RSA加密


    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        String ordernumberR = API_RESPONSE_PARAMS.get(out_trade_no);
        if (StringUtils.isBlank(ordernumberR))  throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("1.[星宇代付]-[代付回调]-获取回调订单号：{}",ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> payParam, String api_key) throws PayException {
    	List paramKeys = MapUtils.sortMapByKeyAsc(payParam);
    	paramKeys.remove("error_msg");
    	paramKeys.remove("error_code");
    	paramKeys.remove("sign");
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
        	signSrc.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
        }
        signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "");
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("2.[星宇代付]-[代付回调]-生成md5签名：{}",signMD5);
        return signMD5;
    }

    @Override
    protected boolean checkAmount(Map<String, String> api_response_params, String amountDb) throws PayException {
        String responseAmount =  HandlerUtil.getFen(api_response_params.get(total_fee));
        boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        log.debug("3.[星宇代付]-[代付回调]-验证回调金额：{}",checkAmount);
        return checkAmount;
    }

    //检查回调签名
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get("sign").equalsIgnoreCase(signMd5);
        log.debug("4.[星宇代付]-[代付回调]-验证第三方签名：{}",result);
        return result;
    }


    //响应回调的内容
    @Override
    protected String responseSuccess() {
        log.debug("5.[星宇代付]-[代付回调]-响应第三方内容：{}",RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }

    //回调订单状态
    @Override
    protected PayEumeration.DAIFU_RESULT  resOrderStatus(Map<String, String> api_response_params) throws PayException {
    	PayEumeration.DAIFU_RESULT  orderStatus = PayEumeration.DAIFU_RESULT.UNKNOW;
        if(api_response_params.containsKey(withdraw_status)){
           if("2".indexOf(api_response_params.get(withdraw_status).toString())!=-1) orderStatus = PayEumeration.DAIFU_RESULT.ERROR;
           if("0".indexOf(api_response_params.get(withdraw_status).toString())!=-1) orderStatus = PayEumeration.DAIFU_RESULT.PAYING;
           if("1".indexOf(api_response_params.get(withdraw_status).toString())!=-1) orderStatus = PayEumeration.DAIFU_RESULT.SUCCESS;
        }
        log.debug("6.[星宇代付]-[代付回调]-订单状态：{}",orderStatus);
        return orderStatus;
    }

}