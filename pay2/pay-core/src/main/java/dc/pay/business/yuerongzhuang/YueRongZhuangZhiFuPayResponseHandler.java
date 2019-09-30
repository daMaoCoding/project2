package dc.pay.business.yuerongzhuang;

/**
 * ************************
 * @author tony 3556239829
 */

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@ResponsePayHandler("YUERONGZHUANG")
public final class YueRongZhuangZhiFuPayResponseHandler extends PayResponseHandler {
     private final Logger log = LoggerFactory.getLogger(getClass());
     private static final String RESPONSE_PAY_MSG = "success";


     private static final String   userOrderId = "userOrderId";  // "20180919165954",
     private static final String   money = "money";  // "1000",
     private static final String   agentUserName = "agentUserName";  // "165B30C6AE1",
     private static final String   payTime = "payTime";  // "1537347644",
     private static final String   createTime = "createTime";  // "1537347599",
     private static final String   sign = "sign";  // "K60FbhKqqPybCHZh/06W3AuRMxaH7L2uNNT+Z1ip50g2ahNtmbZGdT5K/4MPwTj6R2LOD6zu3fZM/k/vEWK4uAPFhJ9KbguWcdqRkxytgz+7cOnl7x7qzSX6Q7ZojlXCiusqPy96ntJ+RRM/8bmVWA6qdV7AdhSc8sZMM8yVSb8=",
     private static final String   guid = "guid";  // "69f88c86395e409baf8083ec49f5fe14",
     private static final String   comment = "comment";  // "",
     private static final String   userName = "userName";  // "165CCE2E93A",
     private static final String   productName = "productName";  // "20180919165954",
     private static final String   status = "status";  // "5"


    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(userOrderId);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[悦榕庄支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if(StringUtils.isBlank(params.get(paramKeys.get(i))) || sign.equalsIgnoreCase(paramKeys.get(i).toString())    )  //
                continue;
            sb.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i)));
            if(i<paramKeys.size()-1) sb.append("&");
        }
        boolean b = false;
        try {
            b = EncryptUtil.rsaVerifyByPublicKey(sb.toString(), channelWrapper.getAPI_KEY(), params.get(sign));
        } catch (Exception e) {
           throw new PayException("回调密钥验签失败");
        }
        log.debug("[悦榕庄支付]-[请求支付]-2.生成加密URL签名完成：" + b);
        return String.valueOf(b);
    }



    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean checkResult = false;
        String payStatus = api_response_params.get(status);
        String responseAmount = api_response_params.get(money);
        boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        if (checkAmount && payStatus.equalsIgnoreCase("5")) {
            checkResult = true;
        } else {
            log.error("[悦榕庄支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatus + " ,支付金额：" + responseAmount + " ，应支付金额：" + amountDb);
        }
        log.debug("[悦榕庄支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + checkResult + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amountDb + ",第三方响应支付成功标志:" + payStatus + " ,计划成功：1");
        return checkResult;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = "true".equalsIgnoreCase(signMd5);
        log.debug("[悦榕庄支付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[悦榕庄支付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}