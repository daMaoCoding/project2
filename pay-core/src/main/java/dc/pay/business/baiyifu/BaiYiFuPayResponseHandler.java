package dc.pay.business.baiyifu;

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
@ResponsePayHandler("BAIYIFU")
public final class BaiYiFuPayResponseHandler extends PayResponseHandler {
      //private final Logger log = LoggerFactory.getLogger(getClass());
      private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("{\"status\":true}");

     private static final String  out_trade_no = "out_trade_no";    // "20181213143626136651",
     private static final String  out_channel_no = "out_channel_no";    // "P3301KJ1302Q174Q1544683175194",
     private static final String  respCode = "respCode";    // "00000",
     private static final String  respMsg = "respMsg";    // "付款成功",
     private static final String  sign = "sign";    // "d8e3452343d018713a7092b10205705b",
     private static final String  attach = "attach";    // "20181213143626136651",
     private static final String  notifytime = "notifytime";    // "2018-12-13 14:39:59.0",
     private static final String  amount = "amount";    // "1000"


    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(out_trade_no);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[百益付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if(StringUtils.isBlank(params.get(paramKeys.get(i))) || sign.equalsIgnoreCase(paramKeys.get(i).toString())  ||respCode.equalsIgnoreCase(paramKeys.get(i).toString())     || respMsg.equalsIgnoreCase(paramKeys.get(i).toString())  )  //
                continue;
            sb.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i))).append("&");
        }
        sb.append("key=" + channelWrapper.getAPI_KEY());
        String signStr = sb.toString(); //.replaceFirst("&key=","")
        pay_md5sign = HandlerUtil.getMD5UpperCase(signStr);
        log.debug("[百益付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }



    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean checkResult = false;
        String payStatus = api_response_params.get(respCode);
        String responseAmount =  api_response_params.get(amount);
        //boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        boolean checkAmount =  HandlerUtil.isRightAmount(amountDb,responseAmount,"100");//第三方回调金额差额1元内
        if (checkAmount && payStatus.equalsIgnoreCase("00000")) {
            checkResult = true;
        } else {
            log.error("[百益付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatus + " ,支付金额：" + responseAmount + " ，应支付金额：" + amountDb);
        }
        log.debug("[百益付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + checkResult + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amountDb + ",第三方响应支付成功标志:" + payStatus + " ,计划成功：1");
        return checkResult;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("[百益付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[百益付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}