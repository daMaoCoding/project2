package dc.pay.business.hongwangzhifu;

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
 * 
 * @author andrew
 * Sep 10, 2019
 */
@ResponsePayHandler("HONGWANGZHIFU")
public final class HongWangZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

     //
     //"amount":"10000.00",//订单金额
    private static final String amount                ="amount";
     //"order_no":"X2019051411100573",//平台订单号
//    private static final String order_no                ="order_no";
     //"plat_num":"155780340562",//商户订单号
    private static final String plat_num                ="plat_num";
     //"app_id":"201900046",//商户APP_ID
    private static final String app_id                ="app_id";
     //"sign_type":"MD5",//签名方式
//    private static final String sign_type                ="sign_type";
     //"pay_type":"wechat",//支付方式
//    private static final String pay_type                ="pay_type";
     //"status":"SUCCESS",//SUCCESS 成功； FAIL 失败
    private static final String status                ="status";
     //"back_status":"WAIT_BACK",//返单状态
//    private static final String back_status                ="back_status";
     //"complete_time":"1557803462",//完成时间
//    private static final String complete_time                ="complete_time";
     //"sign":"Nwj5iIhB477Uh\/WaBbc282AObT11auRTJAc462mEmxhXWkOOjkvRkipL1HkIsO5Zm8nODpTxSmu3Gj\/pxCWg\/JMlYUQD+40H2VDskhBGGCdkbR4xgkVN4fDHHqvx8ArhtZkl0OwOJ2V7UiCO23tqBkp+tWYrkOrcv6jeKKbIvCBN6qw+ec5QJQ0VVZop3z4IIIdrK66G49Z1j90vozO0MyxRVSsClTVJ+dfMz7AUPWqhOF\/cktJwCpWUA4aCxYkC7+9V7rUES0REjthUsAnGjggDya0I5KTN2fXYQFCF1bKaM+peBWhPV+fsaB8a2NjipA+a3wIqKPGEJNUFAgCtLg=="
//    private static final String sign                ="sign";
    
//    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
       //String api_KEY = channelWrapper.getAPI_KEY();
       //if (null == api_KEY || !api_KEY.contains("-") || api_KEY.split("-").length != 2) {
       //    log.error("[红网支付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //    throw new PayException("[红网支付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //}
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(app_id);
        String ordernumberR = API_RESPONSE_PARAMS.get(plat_num);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[红网支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        StringBuilder signStr = new StringBuilder();
        signStr.append(api_response_params.get(app_id));
        signStr.append(channelWrapper.getAPI_KEY());
        String paramsStr =signStr.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[红网支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }


    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //"status":"SUCCESS",//SUCCESS 成功； FAIL 失败
        String payStatusCode = api_response_params.get(status);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(amount));

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        // 1 代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("SUCCESS")) {
            my_result = true;
        } else {
            log.error("[红网支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[红网支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：SUCCESS");
        return my_result;
    }

    
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[红网支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[红网支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}