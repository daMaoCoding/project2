package dc.pay.business.dayinzhifu;

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
 * Aug 16, 2019
 */
@ResponsePayHandler("DAYINZHIFU")
public final class DaYinZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

//    参数项           类型          说明
//    no            String      系统订单号
//    outTradeNo    String      商户订单号
//    merchantNo    String      商户号
//    productId     String      商品 ID
//    money         long        支付金额(分)
//    body          String      订单内容
//    detail        String      订单详情
//    tradeType     String      结算类型(如 T0、T1)
//    date          Date        支付时间
//    nonce         String      随机字符
//    timestamp     Long        时间戳
//    sign          String      签名
//    success       Boolean     支付装态（付款成功：true,付款失败：false, 未付款:null）
    private static final String no                   ="no";
    private static final String outTradeNo           ="outTradeNo";
    private static final String merchantNo           ="merchantNo";
//    private static final String productId            ="productId";
    private static final String money                ="money";
//    private static final String body                 ="body";
//    private static final String detail               ="detail";
//    private static final String tradeType            ="tradeType";
//    private static final String date                 ="date";
    private static final String nonce                ="nonce";
    private static final String timestamp            ="timestamp";
//    private static final String sign                 ="sign";
    private static final String success              ="success";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("SUCCESS");

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(merchantNo);
        String ordernumberR = API_RESPONSE_PARAMS.get(outTradeNo);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[大银支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        String signSrc=String.format("%s%s%s%s%s", 
                merchantNo+"="+api_response_params.get(merchantNo)+"&",
                no+"="+api_response_params.get(no)+"&",
                nonce+"="+api_response_params.get(nonce)+"&",
                timestamp+"="+api_response_params.get(timestamp)+"&",
                key+"="+channelWrapper.getAPI_KEY()
                );
        String paramsStr = signSrc.toString();
        String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[大银支付]-[响应支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return signMD5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        String payStatusCode = api_response_params.get(success);
        String responseAmount = api_response_params.get(money);
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差
        
        //1代表第三方支付成功
        if (checkAmount&&payStatusCode.equalsIgnoreCase("true")) {
            my_result = true;
        } else {
            log.error("[大银支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[大银支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：true");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[大银支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[大银支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return  RESPONSE_PAY_MSG;
    }
}