package dc.pay.business.qianduoduo;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;


/**
 * ************************
 * @author beck 2229556569
 */

@ResponsePayHandler("QIANDUODUO")
public final class QianDuoDuoPayResponseHandler extends PayResponseHandler {
     private final Logger log = LoggerFactory.getLogger(getClass());
     private static final String RESPONSE_PAY_MSG = "success";

//     private static final String  pay_MerchantNo = "pay_MerchantNo";
     private static final String  pay_OrderNo = "pay_OrderNo";  
     private static final String  pay_Amount = "pay_Amount";
//     private static final String  pay_Cur = "pay_Cur";     
     private static final String  pay_Status = "pay_Status";  
//     private static final String  pay_PayTime = "pay_PayTime";
//     private static final String  pay_DealTime = "pay_DealTime";
     private static final String  sign = "sign";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(pay_OrderNo);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[钱多多]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        String paramsStr = this.getJiGouNo() + params.get(pay_OrderNo) + params.get(pay_Amount) + channelWrapper.getAPI_KEY();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[钱多多]-[响应支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }
    

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean checkResult = false;
        String resultStr = api_response_params.get(pay_Status);
        String responseAmount =  HandlerUtil.getFen(api_response_params.get(pay_Amount));
        boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        if (checkAmount && resultStr.equalsIgnoreCase("100")) {
            checkResult = true;
        } else {
            log.error("[钱多多]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + resultStr + " ,支付金额：" + responseAmount + " ，应支付金额：" + amountDb);
        }
        log.debug("[钱多多]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + checkResult + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amountDb + ",第三方响应支付成功标志:" + resultStr + " ,计划成功：100");
        return checkResult;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("[钱多多]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[钱多多]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
    

    /**
     * 获取商户号
     * */
    private String getMerchantNo() throws PayException{
        String memberInfos[] = this.splitMemberID();
        
        return memberInfos[0];
    }
    
    
    /**
     * 获取机构号
     * */
    private String getJiGouNo() throws PayException{
        String memberInfos[] = this.splitMemberID();
        
        return memberInfos[1];
    }
    
    /**
     * 分割商户ID
     * */
    private String[] splitMemberID() throws PayException {
        String memberInfos[] =  this.channelWrapper.getAPI_MEMBERID().split("&");
        if(memberInfos.length < 2){
            String errorMsg = "[钱多多]-[请求支付]-5. 商户信息填写错误，填写格式：商户号&机构号。";
            log.error(errorMsg);
            throw new PayException(errorMsg);
        }
        return memberInfos;
    }
}