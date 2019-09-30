package dc.pay.business.juhua;

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
import dc.pay.utils.RsaUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Feb 23, 2019
 */
@ResponsePayHandler("JUHUA")
public final class JuHuaPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //参数  类型  参数说明
    //RECODE  String  返回状态码
//    private static final String RECODE                ="RECODE";
    //REMSG   String  返回说明信息
//    private static final String REMSG                ="REMSG";
    //ORDERNO String  商户订单号
    private static final String ORDERNO                ="ORDERNO";
    //TXNAMT  String  订单金额（分）
    private static final String TXNAMT                ="TXNAMT";
    //ORDSTATUS   String  订单状态 00待处理; 01 成功; 02 失败 03 订单失效 04:处理中
    private static final String ORDSTATUS                ="ORDSTATUS";
    //PAYORDNO    String  平台订单号
    private static final String PAYORDNO                ="PAYORDNO";
    //SIGN    String  签名字段和顺序ORDERNO，TXNAMT，ORDSTATUS，PAYORDNO
//    private static final String SIGN                 ="SIGN";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="SIGN";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
//        String partnerR = API_RESPONSE_PARAMS.get(merchno);
        String ordernumberR = API_RESPONSE_PARAMS.get(ORDERNO);
        if ( StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[聚华]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        StringBuilder signSrc = new StringBuilder();
        signSrc.append(ORDERNO+"=").append(api_response_params.get(ORDERNO)).append("&");
        signSrc.append(TXNAMT+"=").append(api_response_params.get(TXNAMT)).append("&");
        signSrc.append(ORDSTATUS+"=").append(api_response_params.get(ORDSTATUS)).append("&");
        signSrc.append(PAYORDNO+"=").append(api_response_params.get(PAYORDNO));
        String paramsStr = signSrc.toString();
        boolean signMd5 = RsaUtil.validateSignByPublicKey(paramsStr,  channelWrapper.getAPI_PUBLIC_KEY(), api_response_params.get(signature), "SHA1WithRSA");
        log.debug("[聚华]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return String.valueOf(signMd5);
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //ORDSTATUS String  订单状态        00待处理; 01 成功; 02 失败 03 订单失效 04:处理中
        String payStatusCode = api_response_params.get(ORDSTATUS);
        String responseAmount = api_response_params.get(TXNAMT);

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        //boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("01")) {
            my_result = true;
        } else {
            log.error("[聚华]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[聚华]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：01");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
//        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        Boolean my_result = new Boolean(signMd5);
        log.debug("[聚华]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[聚华]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}