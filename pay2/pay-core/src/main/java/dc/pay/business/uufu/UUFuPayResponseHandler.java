package dc.pay.business.uufu;

/**
 * ************************
 * @author tony 3556239829
 */

import com.alibaba.fastjson.JSON;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RsaUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@ResponsePayHandler("UUFU")
public final class UUFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String RESPONSE_PAY_MSG = "SUCCEED";

    private static final String  payType    =  "payType";  //	String	是	支付方式
    private static final String  tradeNo    =  "tradeNo";  //	String	是	平台交易号
    private static final String  outTradeNo    =  "outTradeNo";  //	String	是	商户交易号，请确保本商户内唯一
    private static final String  outContext    =  "outContext";  //	String	否	创建交易时的outContext参数，外部上下文，当创建交易时有传入的时候才会返回
    private static final String  merchantNo    =  "merchantNo";  //	String	是	商户号
    private static final String  currency    =  "currency";  //	String	是	货币类型 CNY：人民币, USD:美元, 目前仅支持CNY
    private static final String  amount    =  "amount";  //	Long	是	交易金额, 单位分
    private static final String  payedAmount    =  "payedAmount";  //	Long	是	用户支付金额, 单位分, payedAmount == amount
    private static final String  status    =  "status";  //	String	是	交易状态，WAITING_PAY：待支付, PAYED: 支付成功，PAYED_FAILED：支付失败, WAITING_SETTLE: 待结算，SETTLEING: 结算中， SETTLED: 结算成功, SETTLED_FAILED: 结算失败
    private static final String  sign    =  "sign";  //	String	是	RSA签名


    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(outTradeNo);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[UU付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if( sign.equalsIgnoreCase(paramKeys.get(i).toString()))  //StringUtils.isBlank(params.get(paramKeys.get(i))) ||
                continue;
            sb.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i))).append("&");
        }
        String signInfo = sb.toString().substring(0, sb.toString().length() - 1);
        boolean result = false;
        String wpay_public_key = channelWrapper.getAPI_PUBLIC_KEY();
        //String wpay_public_key = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCGosEaDEGG9VaZbJ0NOxevFLd9xGEI0/mXcy1EOfHaI0/NZgFbysS0SDf1M1vRCBLXL3dmoiUW8cLWNf0askCtQanxz5kWXXKrGmJpsL5a8dTu6PCl0wD4OB+9B0zCoe/SquACJLBGjsHNGeYS8FmitdYnDjfrTDClimkUUuRthQIDAQAB";
        result = RsaUtil.validateSignByPublicKey(signInfo, wpay_public_key, params.get(sign),"SHA1withRSA");	// 验签   signInfoUU付返回的签名参数排序， wpay_public_keyUU付公钥， wpaySignUU付返回的签名
        log.debug("[UU付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(result));
        return String.valueOf(result);
    }




    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amount) throws PayException {
        boolean result = false;
        String payStatusCode = api_response_params.get(status);
        String responseAmount = api_response_params.get("amount");
        boolean checkAmount = amount.equalsIgnoreCase(responseAmount);
        if (checkAmount &&( payStatusCode.equalsIgnoreCase("PAYED") ||  payStatusCode.equalsIgnoreCase("SETTLED"))) {
            result = true;
        } else {
            log.error("[UU付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + amount);
        }
        log.debug("[UU付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：PAYED 或者 SETTLED");
        return result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        Boolean signMd5Boolean = Boolean.valueOf(signMd5);

        //boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("[UU付]-[响应支付]-4.验证MD5签名：" + signMd5Boolean.booleanValue());
        return signMd5Boolean.booleanValue();
    }

    @Override
    protected String responseSuccess() {
        log.debug("[UU付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}