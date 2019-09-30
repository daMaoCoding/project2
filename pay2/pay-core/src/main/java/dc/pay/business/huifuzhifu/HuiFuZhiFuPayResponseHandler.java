package dc.pay.business.huifuzhifu;

/**
 * ************************
 * @author tony 3556239829
 */

import com.alibaba.fastjson.JSON;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@ResponsePayHandler("HUIFUZHIFU")
public final class HuiFuZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private static final String RESPONSE_PAY_MSG = "success";

    private static final String   errcode  = "errcode";    //: "0",
    private static final String   no  = "no";    //: "142397",
    private static final String   merchant_no  = "merchant_no";    //: "5211",
    private static final String   total  = "total";    //: "100",
    private static final String   out_trade_no  = "out_trade_no";    //: "20180616115544",
    private static final String   fee  = "fee";    //: "2",
    private static final String   sign  = "sign";    //: "27631FE52ADF76CA9FBF4BDE7F304186",
    private static final String   trade_type  = "trade_type";    //: "1",
    private static final String   message  = "message";    //: "支付成功",
    private static final String   sign_type  = "sign_type";    //: "MD5"



    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(out_trade_no);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[慧富支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        String pay_md5sign;
        Set<String> set = params.keySet().parallelStream().collect(Collectors.toCollection(TreeSet::new));
        StringBuilder str = new StringBuilder("");
        for(String key:set) {
            if(StringUtils.equalsIgnoreCase("sign",key)) {
                continue;
            }
            if(Objects.isNull(params.get(key)) || StringUtils.isBlank(params.get(key))) {
                continue;
            }
            str = str.append(String.format("%s=%s",key,params.get(key)));
            str = str.append("&");
        }
        str = str.append("key=").append(channelWrapper.getAPI_KEY());
        pay_md5sign =  DigestUtils.md5Hex(str.toString()).toUpperCase();
        log.debug("[慧富支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }



    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean checkResult = false;
        String payStatus = api_response_params.get(errcode);
        String responseAmount =   api_response_params.get(total);
        boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        if (checkAmount && payStatus.equalsIgnoreCase("0")) {
            checkResult = true;
        } else {
            log.error("[慧富支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatus + " ,支付金额：" + responseAmount + " ，应支付金额：" + amountDb);
        }
        log.debug("[慧富支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + checkResult + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amountDb + ",第三方响应支付成功标志:" + payStatus + " ,计划成功：0");
        return checkResult;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("[慧富支付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[慧富支付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}