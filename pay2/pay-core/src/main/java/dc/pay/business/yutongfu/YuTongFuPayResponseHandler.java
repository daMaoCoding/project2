package dc.pay.business.yutongfu;

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
import dc.pay.utils.RsaUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 *  May 28, 2019
 */
@ResponsePayHandler("YUTONGFU")
public final class YuTongFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //参数名称    参数编码    属性  数据描述    数据类型
    //transType   业务类型    M   固定值 PROXY_PAY   A(4)
//    private static final String transType                                        ="transType";
    //productId   产品类型    M   8001   代付   N(4)
//    private static final String productId                                        ="productId";
    //merNo   商户号 M   商户号 N(15)
    private static final String merNo                                        ="merNo";
    //orderDate   订单日期    M   订单交易日期 yyyyMMdd N(8)
//    private static final String orderDate                                        ="orderDate";
    //orderNo 订单号 M   商户平台订单号 ASC(40)
    private static final String orderNo                                        ="orderNo";
    //transAmt    交易金额    M   分为单位如 100 代表  1.00元 N(64)
    private static final String transAmt                                        ="transAmt";
    //serialId    流水号 M   平台产生的交易流水号  ASC(32)
//    private static final String serialId                                        ="serialId";
    //respCode    返回码 M   见附录 一   ASC(4)
    private static final String respCode                                        ="respCode";
    //respDesc    返回描述    C   见附录 一   ASC(1,64)
//    private static final String respDesc                                        ="respDesc";
    //signature   签名数据    M   通过 RSA 公钥验证签名   ASC(1024)
//    private static final String signature                                        ="signature";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="signature";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(merNo);
        String ordernumberR = API_RESPONSE_PARAMS.get(orderNo);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[宇通付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!signature.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        String paramsStr = signSrc.toString().substring(0, signSrc.toString().length() - 1);
        boolean result = false;
        result = RsaUtil.validateSignByPublicKey(paramsStr, channelWrapper.getAPI_PUBLIC_KEY(), api_response_params.get(signature),"SHA1withRSA");    // 验签   signInfoUU付返回的签名参数排序， wpay_public_keyUU付公钥， wpaySignUU付返回的签名
//        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[宇通付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(result) );
        return String.valueOf(result);
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //respCode            返回码                M               见附录 一          ASC(4)  0000        交易成功
        String payStatusCode = api_response_params.get(respCode);
        String responseAmount = api_response_params.get(transAmt);
//        //db_amount数据库存入的是分     第三方返回的responseAmount是元
//        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);

        //tony(Tony) 01-17 15:09:28
        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("0000")) {
            my_result = true;
        } else {
            log.error("[宇通付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[宇通付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：0000");
        return my_result;
    }

     @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
//        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
//        log.debug("[宇通付]-[响应支付]-4.验证MD5签名：{}", my_result);
//        return my_result;
         
         Boolean signMd5Boolean = Boolean.valueOf(signMd5);
         //boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
         log.debug("[宇通付]-[响应支付]-4.验证MD5签名：" + signMd5Boolean.booleanValue());
         return signMd5Boolean.booleanValue();
    }

    @Override
    protected String responseSuccess() {
        log.debug("[宇通付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}