package dc.pay.business.jufengzhifu;

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
 * 
 * @author andrew
 * Aug 21, 2019
 */
@ResponsePayHandler("JUFENGZHIFU")
public final class JuFengZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //回调参数
    //字段名 描述  必填  描述
    //merId   商户号 是   您在我方平台的商户号
    private static final String merId                ="merId";
    //orderId 订单号 是   商户订单号
    private static final String orderId                ="orderId";
    //sysOrderId  平台单号    是   平台订单号
//    private static final String sysOrderId                ="sysOrderId";
    //desc    描述  是   订单描述
//    private static final String desc                ="desc";
    //orderAmt    订单金额    是   订单金额
    private static final String orderAmt                ="orderAmt";
    //status  订单状态    是   订单状态 1为成功
    private static final String status                ="status";
    //attch   附加信息    否   请求时传入的附加数据
//    private static final String attch                ="attch";
    //nonceStr    随机字符串   是   随机字符串，最长不超过32位
//    private static final String nonceStr                ="nonceStr";
    //sign    签名  是   请参考签名算法章节
    private static final String sign                ="sign";
    
    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
       //String api_KEY = channelWrapper.getAPI_KEY();
       //if (null == api_KEY || !api_KEY.contains("-") || api_KEY.split("-").length != 2) {
       //    log.error("[巨丰支付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //    throw new PayException("[巨丰支付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //}
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(merId);
        String ordernumberR = API_RESPONSE_PARAMS.get(orderId);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[巨丰支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        //Map<String, String> jsonToMap = handlerUtil.jsonToMap(api_response_params.get(data));
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!signature.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        signSrc.append(key +"="+ channelWrapper.getAPI_KEY().split("-")[0]);
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        
        boolean result = false;
        //String wpay_public_key = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCGosEaDEGG9VaZbJ0NOxevFLd9xGEI0/mXcy1EOfHaI0/NZgFbysS0SDf1M1vRCBLXL3dmoiUW8cLWNf0askCtQanxz5kWXXKrGmJpsL5a8dTu6PCl0wD4OB+9B0zCoe/SquACJLBGjsHNGeYS8FmitdYnDjfrTDClimkUUuRthQIDAQAB";
        result = RsaUtil.validateSignByPublicKey(signMd5, channelWrapper.getAPI_PUBLIC_KEY(), api_response_params.get(sign),"SHA256withRSA");  // 验签   signInfo安全付返回的签名参数排序， wpay_public_key安全付公钥， wpaySign安全付返回的签名
        log.debug("[巨丰支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(result));
        return String.valueOf(result);
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //status    订单状态    是   订单状态 1为成功
        String payStatusCode = api_response_params.get(status);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(orderAmt));

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        // 1 代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("1")) {
            my_result = true;
        } else {
            log.error("[巨丰支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[巨丰支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        Boolean signMd5Boolean = Boolean.valueOf(signMd5);

        //boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("[巨丰支付]-[响应支付]-4.验证MD5签名：" + signMd5Boolean.booleanValue());
        return signMd5Boolean.booleanValue();
    }

    @Override
    protected String responseSuccess() {
        log.debug("[巨丰支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}