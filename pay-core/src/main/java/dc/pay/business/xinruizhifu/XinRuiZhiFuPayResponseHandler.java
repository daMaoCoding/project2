package dc.pay.business.xinruizhifu;

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
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Feb 28, 2019
 */
@ResponsePayHandler("XINRUIZHIFU")
public final class XinRuiZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //参数名称              参数含义                参数说明
    //memberid          商户ID                 商户id,由新睿支付支付分配
    //orderid           订单号         请求时的订单号
    //amount            订单金额            单位：元
    //datetime          交易时间            
    //returncode        交易状态            “00000” 为成功
    //sign              验证签名                请看验证签名参数顺序，key为商户APPkey
    //transaction_id
    private static final String memberid                        ="memberid";
    private static final String orderid                         ="orderid";
    private static final String amount                          ="amount";
    private static final String datetime                        ="datetime";
    private static final String returncode                      ="returncode";
    private static final String transaction_id                   ="transaction_id";

    //signature 数据签名    32  是   　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "OK";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(memberid);
        String ordernumberR = API_RESPONSE_PARAMS.get(orderid);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[新睿支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        StringBuilder signStr = new StringBuilder();
        signStr.append(amount+"=").append(api_response_params.get(amount)).append("&");
        signStr.append(datetime+"=").append(api_response_params.get(datetime)).append("&");
        signStr.append(memberid+"=").append(api_response_params.get(memberid)).append("&");
        signStr.append(orderid+"=").append(api_response_params.get(orderid)).append("&");
        signStr.append(returncode+"=").append(api_response_params.get(returncode)).append("&");
        signStr.append(transaction_id+"=").append(api_response_params.get(transaction_id)).append("&");
        signStr.append("key=").append(channelWrapper.getAPI_KEY());
        String paramsStr =signStr.toString();
        System.out.println(paramsStr);
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[新睿支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //returncode    交易状态    是   “00” 为成功
        String payStatusCode = api_response_params.get(returncode);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(amount));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        
        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("00")) {
            my_result = true;
        } else {
            log.error("[新睿支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[新睿支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：00");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[新睿支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[新睿支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}