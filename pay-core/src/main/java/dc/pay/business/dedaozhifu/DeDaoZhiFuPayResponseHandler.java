package dc.pay.business.dedaozhifu;

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
import dc.pay.utils.MapUtils;
import org.apache.commons.lang.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
@ResponsePayHandler("DEDAOZHIFU")
public final class DeDaoZhiFuPayResponseHandler extends PayResponseHandler {
      //private final Logger log = LoggerFactory.getLogger(getClass());
      private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("SUCCESS");


    private static final String   result_code  = "result_code";       // "SUCCESS",
    private static final String   mch_id  = "mch_id";       // "5241353241862",
    private static final String   trade_type  = "trade_type";       // "ALIH5",
    private static final String   nonce  = "nonce";       // "0aae48a47e79ad8525923d0247169e0c",
    private static final String   timestamp  = "timestamp";       // "1540445608",
    private static final String   out_trade_no  = "out_trade_no";       // "20181025133224",
    private static final String   total_fee  = "total_fee";       // "100",
    private static final String   trade_no  = "trade_no";       // "JH1540445544456",
    private static final String   platform_trade_no  = "platform_trade_no";       // "5241556832254",
    private static final String   pay_time  = "pay_time";       // "20181025133328",
    private static final String   sign_type  = "sign_type";       // "RSA",
    private static final String   sign  = "sign";       // "WHW47yf1R2viSODxBQBSLbyYq1hQFMPdpH1twighJtaokdx+YTtkGURSA4dzGEw4zdPMybt3dxl7NPAGAgSXixKskDXSRqKs6l8emPMnot1nI+P4YKJfeuVmAE8d4ol9M3aTixvwMFNvOS/I4dBQXuyV8hsTyW2akwvvH5R0uQs="


    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);

      //  if(!API_RESPONSE_PARAMS.containsKey(sign_type)  || !"MD5".equalsIgnoreCase(API_RESPONSE_PARAMS.get(sign_type)) )  兼容第三方回调中没这个数据的
       //     throw new PayException(SERVER_MSG.RESPONSE_PAY_SIGN_METHOD_ERROR);

        String ordernumberR = API_RESPONSE_PARAMS.get(out_trade_no);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[得到支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {

        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if(StringUtils.isBlank(params.get(paramKeys.get(i))) || sign.equalsIgnoreCase(paramKeys.get(i).toString())    )  //
                continue;
            sb.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i))).append("&");
        }
        sb.append("key=" + channelWrapper.getAPI_KEY());
        String signStr = sb.toString(); //.replaceFirst("&key=","")
        pay_md5sign = HandlerUtil.getMD5UpperCase(signStr).toLowerCase();
        log.debug("[得到支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }



    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean checkResult = false;
        String payStatus = api_response_params.get(result_code);
        String responseAmount =   api_response_params.get(total_fee);
        //boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        boolean checkAmount =  HandlerUtil.isRightAmount(amountDb,responseAmount,"100");//第三方回调金额差额1元内
        if (checkAmount && payStatus.equalsIgnoreCase("SUCCESS")) {
            checkResult = true;
        } else {
            log.error("[得到支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatus + " ,支付金额：" + responseAmount + " ，应支付金额：" + amountDb);
        }
        log.debug("[得到支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + checkResult + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amountDb + ",第三方响应支付成功标志:" + payStatus + " ,计划成功：1");
        return checkResult;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("[得到支付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[得到支付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}