package dc.pay.business.yifutong;

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
 * Aug 21, 2018
 */
@ResponsePayHandler("YIFUTONG")
public final class YiFuTongPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //输出项                  输出项名称                  属性         注释                                                           数据类型
    //respType                 应答类型                    M         S：标识成功；  E：标识错误；   R：标识未知；                      String
    //merchantId               商户号                      M         合作商户的商户号                                                  String
    //outOrderId               商户请求订单号              M         该交易在合作伙伴系统的流水号                                      String
    //transAmt                 交易金额                    M         交易的总金额，单位为元                                            String
    //respMsg                  应答描述                    M                                                                           String
    //localOrderId             一付通系统订单号            M         一付通系统订单的交易号                                            String
    //respCode                 应答码                      M                                                                           String
    //sign                     签名                        M         数据的加密校验字符串，使用RSA签名算法对待签名数据进行签名         String
//    private static final String respType                        ="respType";
    private static final String merchantId                      ="merchantId";
    private static final String outOrderId                      ="outOrderId";
    private static final String transAmt                        ="transAmt";
//    private static final String respMsg                         ="respMsg";
//    private static final String localOrderId                    ="localOrderId";
    private static final String respCode                        ="respCode";

    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(merchantId);
        String ordernumberR = API_RESPONSE_PARAMS.get(outOrderId);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[一付通]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
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
        String paramsStr = signSrc.toString();
        boolean signMd5 = RsaUtil.validateSignByPublicKey(paramsStr.substring(0,paramsStr.length()-1),  channelWrapper.getAPI_PUBLIC_KEY(), api_response_params.get(signature), "SHA1WithRSA");
        log.debug("[一付通]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return String.valueOf(signMd5);
    }
    
    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //00    交易成功
        String payStatusCode = api_response_params.get(respCode);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(transAmt));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("00")) {
            my_result = true;
        } else {
            log.error("[一付通]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[一付通]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：00");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = new Boolean(signMd5);
        log.debug("[一付通]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[一付通]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}