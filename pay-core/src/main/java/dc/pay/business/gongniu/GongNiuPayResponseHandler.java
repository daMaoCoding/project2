package dc.pay.business.gongniu;

import java.util.Base64;
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
 * Nov 8, 2018
 */
@ResponsePayHandler("GONGNIU")
public final class GongNiuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //2.2.2. 通知接口参数定义
    //参数                        格式                   必选            说明
    //merchant_code               String(10)              √              参数名称：商家号 商户签约时，公牛支付分配给商家的唯一身份标识 例如：100000001或者100000002
    //merchant_order_no           String(25)              √              参数名称：商户订单号 由商户网站或APP生成的订单号，即支付发起时传输到公牛支付的订单号
    //merchant_amount             String(10)              √              参数名：支付实际金额 实际入账以该金额为准（为提高支付成功率，公牛支付的支付宝接口会在商户提交的支付金额进行1元内的小数随机减少）
    //merchant_amount_orig        String(10)              √              参数名称：商户提交的原始支付金额 在支付发起时提交的支付金额，实际到账金额以request_amount为准
    //merchant_sign               String(200)             √              参数名称：数据加密印鉴  生成规则：base64_encode(md5('merchant_code='.$merchant_code.'&merchant_order_no='.$merchant_order_no.'&merchant_amount='.$ merchant_amount.'&merchant_amount_orig='.$ merchant_amount_orig.'&merchant_md5='.$merchant_md5))生成规则中的“+”表示字符连接，两个参数中间实际不加任何额外字符。merchant_md5是公牛支付分配给商户的商户MD5，请不要泄露
    private static final String merchant_code                                                 ="merchant_code";
    private static final String merchant_order_no                                             ="merchant_order_no";
    private static final String merchant_amount                                               ="merchant_amount";
    private static final String merchant_amount_orig                                          ="merchant_amount_orig";

    private static final String status_code                                                   ="status_code";
    
    private static final String key        ="merchant_md5";
    //signature    数据签名    32    是    　
    private static final String signature  ="merchant_sign";

    private static final String RESPONSE_PAY_MSG = "SUCCESS";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(merchant_code);
        String ordernumberR = API_RESPONSE_PARAMS.get(merchant_order_no);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[公牛]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        StringBuilder signStr = new StringBuilder();
        signStr.append(merchant_code+"=").append(api_response_params.get(merchant_code)).append("&");
        signStr.append(merchant_order_no+"=").append(api_response_params.get(merchant_order_no)).append("&");
        signStr.append(merchant_amount+"=").append(api_response_params.get(merchant_amount)).append("&");
        signStr.append(merchant_amount_orig+"=").append(api_response_params.get(merchant_amount_orig)).append("&");
        signStr.append(key+"=").append(channelWrapper.getAPI_KEY());
        String paramsStr =signStr.toString();
        String signMd5 = new String(Base64.getEncoder().encode(HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase().getBytes()));
        log.debug("[公牛]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //Andy 2018/11/8 10:48:41
        //加了一個參數status_code，00的時候表示成功
        String payStatusCode = api_response_params.get(status_code);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(merchant_amount));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        boolean checkAmount = handlerUtil.isAllowAmountt(db_amount,responseAmount,"100");
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("00")) {
            my_result = true;
        } else {
            log.error("[公牛]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[公牛]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：00");
        return my_result;
    }

     @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[公牛]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[公牛]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}