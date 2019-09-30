package dc.pay.business.haitian;

import java.util.List;
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
import dc.pay.utils.MapUtils;

/**
 * 
 * 
 * @author kevin
 * Aug 21, 20008
 */
@ResponsePayHandler("HAITIAN")
public final class HaiTianPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //00.  code    支付状态    Y   String(2)   00：交易成功
    private static final String code  		="code";
    //3.    externalId  订单号 Y   String(32)  
    private static final String externalId  		="externalId";
    //5.    amount  实际支付金额  Y   String  正整数，分为单位
    private static final String amount    	="amount";
    //6.    customerNo  商户编号    N   String(30)  
    private static final String customerNo    		="customerNo";
    
    //支付宝
    //merchant string(32) 商户名
    private static final String merchant          ="merchant";
    //orderNo string(32) 商户订单
    private static final String orderNo          ="orderNo";
    //isFinished int(2) 订单支付状态，1为已支付，其他值均为未
    private static final String isFinished          ="isFinished";
    //reciveAmount decimal(11,2)    实收金额    此金额为实际到账的金额，请以此金额做到账或其他相关的处理
    private static final String reciveAmount          ="reciveAmount";
    
    private static final String      key         = "key";
    
    private static final String sign    		="sign";
    
    private static final String RESPONSE_PAY_MSG = "SUCCESS";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(customerNo);
        String ordernumberR = API_RESPONSE_PARAMS.get(externalId);
        if (StringUtils.isBlank(partnerR)) {
            partnerR = API_RESPONSE_PARAMS.get(merchant);
        }
        if (StringUtils.isBlank(ordernumberR)) {
            ordernumberR = API_RESPONSE_PARAMS.get(orderNo);
        }
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[海天]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" , ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
//            if(StringUtils.isBlank(api_response_params.get(paramKeys.get(i))) || sign.equalsIgnoreCase(paramKeys.get(i).toString()))  
            if(sign.equalsIgnoreCase(paramKeys.get(i).toString()))  
                continue;
            sb.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            
        }

        if (handlerUtil.isZFB(channelWrapper)) {
            //删除最后一个字符
            sb.deleteCharAt(sb.length()-1);
            sb.append(api_key);
        }else {
            sb.append(key +"="+ channelWrapper.getAPI_KEY());
        }
        
//        sb.deleteCharAt(sb.length()-1);
//        sb.append(key+"="+channelWrapper.getAPI_KEY());
        String signStr = sb.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(signStr).toLowerCase();
        log.debug("[海天]-[响应支付]-2.生成加密URL签名完成：{}" , JSON.toJSONString(signMd5));
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        
        if (handlerUtil.isZFB(channelWrapper)) {
            //isFinished int(2) 订单支付状态，1为已支付，其他值均为未
            String payStatusCode = api_response_params.get(isFinished );
            String responseAmount = handlerUtil.getFen(api_response_params.get(reciveAmount ));
            
            //偏差大于00元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
            boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差
            
            if (checkAmount && payStatusCode.equalsIgnoreCase("1")) {
                my_result = true;
            } else {
                log.error("[海天]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
            }
            log.debug("[海天]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
       
        } else {
            //00.    code    支付状态    Y   String(2)   00：交易成功
            String payStatusCode = api_response_params.get(code);
            String responseAmount = api_response_params.get(amount);
            
            //偏差大于00元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
            boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差
            
            if (checkAmount && payStatusCode.equalsIgnoreCase("00")) {
                my_result = true;
            } else {
                log.error("[海天]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
            }
            log.debug("[海天]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：00");
        }

        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("[海天]-[响应支付]-4.验证MD5签名：{}" , my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[海天]-[响应支付]-5.第三方支付确认收到消息返回内容：{}" , RESPONSE_PAY_MSG);
//        return RESPONSE_PAY_MSG;
        
        return (handlerUtil.isZFB(channelWrapper) ? "success" : "SUCCESS");
    }
}