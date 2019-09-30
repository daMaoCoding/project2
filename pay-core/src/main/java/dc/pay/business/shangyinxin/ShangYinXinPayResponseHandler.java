package dc.pay.business.shangyinxin;

/**
 * ************************
 * @author tony 3556239829
 */

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RsaUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@ResponsePayHandler("SHANGYINXIN")
public final class ShangYinXinPayResponseHandler extends PayResponseHandler {
     private final Logger log = LoggerFactory.getLogger(getClass());
     private static final String RESPONSE_PAY_MSG = "success";



    private static final String  sign  =  "sign";                 // "cd8S9uFGTUdCRz1IfJ3MrX4qzXxGP723Sb0nJabwMEMtmA8YehEZoj61x3hhMaft*Abu8k8rD8r2K6ciLwF0oGrnDkTztlgAAprOsnBUP*eq4CfFVHbMPZN6qIOFPmjpVPacxwS0KTzqUrFOQaJZnuheKM2*GNU-GETE-ZNtZks=",
    private static final String  body  =  "body";                  // "body",
    private static final String  notifyTime  =  "notifyTime";      // "20180416181116",
    private static final String  tradeStatus  =  "tradeStatus";     // "2",
    private static final String  inputCharset  =  "inputCharset";    // "UTF-8",
    private static final String  subject  =  "subject";              // "subject",
    private static final String  transTime  =  "transTime";          // "20180416180755",
    private static final String  notifyId  =  "notifyId";            // "165257248",
    private static final String  merchantId  =  "merchantId";        // "001016051700644",
    private static final String  transAmt  =  "transAmt";            // "0.1",
    private static final String  localOrderId  =  "localOrderId";     // "237960299",
    private static final String  outOrderId  =  "outOrderId";         // "SHANGYINXIN_WY_JSYH-GkEvW"
    private static final String  signType  =  "signType";


    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(outOrderId);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[商银信支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if(StringUtils.isBlank(params.get(paramKeys.get(i))) || sign.equalsIgnoreCase(paramKeys.get(i).toString())   || signType.equalsIgnoreCase(paramKeys.get(i).toString())   )  //
                continue;
            sb.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i)));
            if(i!=paramKeys.size()-1) sb.append("&");
        }
        boolean checkSign = RsaUtil.validateSignByPublicKey2(sb.toString(), channelWrapper.getAPI_PUBLIC_KEY(), params.get(sign).toString(),"UTF-8");
        log.debug("[商银信支付]-[响应支付]-2.验证签名结果,{}" ,checkSign);
        return String.valueOf(checkSign);
    }



    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean result = false;
        String payStatus = api_response_params.get(tradeStatus);
        String responseAmount =  HandlerUtil.getFen(api_response_params.get(transAmt));
        boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        if (checkAmount && payStatus.equalsIgnoreCase("2")) {
            result = true;
        } else {
            log.error("[商银信支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatus + " ,支付金额：" + responseAmount + " ，应支付金额：" + amountDb);
        }
        log.debug("[商银信支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amountDb + ",第三方响应支付成功标志:" + payStatus + " ,计划成功：1");
        return result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = "true".equalsIgnoreCase(signMd5);
        log.debug("[商银信支付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[商银信支付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}