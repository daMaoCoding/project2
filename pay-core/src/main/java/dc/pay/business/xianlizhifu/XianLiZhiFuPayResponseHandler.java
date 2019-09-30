package dc.pay.business.xianlizhifu;

/**
 * ************************
 * @author tony 3556239829
 */

import com.alibaba.fastjson.JSON;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import org.apache.commons.lang.StringUtils;
import org.assertj.core.util.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@ResponsePayHandler("XIANLIZHIFU")
public final class XianLiZhiFuPayResponseHandler extends PayResponseHandler {
     private final Logger log = LoggerFactory.getLogger(getClass());
     private static final String RESPONSE_PAY_MSG = "success";

     private static final String   pay_result = "pay_result";    ///: "1",
     private static final String   system_order_no = "system_order_no";    ///: "5b0be1b988456839625b0be1b9884a9",
     private static final String   order_no = "order_no";    ///: "XINLIZHIFU_QQ_SM-KFuzD",
     private static final String   amount = "amount";    ///: "2",
     private static final String   real_amount = "real_amount";    ///: "2",
     private static final String   currency = "currency";    ///: "CNY",
     private static final String   sign = "sign";    ///: "4F7D49DFB433C3E6DDB7DC98F0DB2B49"
     private static final String   key = "key";    ///: "4F7D49DFB433C3E6DDB7DC98F0DB2B49"


    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(order_no);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[先力支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        HashSet<String> spParam = Sets.newHashSet();//不需要引号的值
        spParam.add(pay_result);
        spParam.add(amount);
        spParam.add(real_amount);
        TreeMap<String, String> payParam =new TreeMap<>(params);
        payParam.remove(sign);
        payParam.put(key, channelWrapper.getAPI_KEY());
        String str = HandlerUtil.mapToJsonWithSpParam(payParam,spParam).replaceAll("parms\\[","").replaceAll("\\]","");
        String pay_md5sign = HandlerUtil.md5(str);
        log.debug("[先力支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }



    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean result = false;
        String payStatus = api_response_params.get(pay_result);
        String responseAmount =  HandlerUtil.getFen(api_response_params.get(amount));
        boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        if (checkAmount && payStatus.equalsIgnoreCase("1")) {
            result = true;
        } else {
            log.error("[先力支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatus + " ,支付金额：" + responseAmount + " ，应支付金额：" + amountDb);
        }
        log.debug("[先力支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amountDb + ",第三方响应支付成功标志:" + payStatus + " ,计划成功：1");
        return result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("[先力支付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[先力支付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}