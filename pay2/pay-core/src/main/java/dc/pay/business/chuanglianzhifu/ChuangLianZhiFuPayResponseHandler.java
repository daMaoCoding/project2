package dc.pay.business.chuanglianzhifu;

import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.alibaba.fastjson.JSON;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.business.yifubaozhifu.MD5Utils;
import dc.pay.business.yifubaozhifu.RC4;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author andrew
 * Mar 26, 2019
 */
@Slf4j
@ResponsePayHandler("CHUANGLIANZHIFU")
public final class ChuangLianZhiFuPayResponseHandler extends PayResponseHandler {
      //private final Logger log = LoggerFactory.getLogger(getClass());
      private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("success");

//     private static final String  account_name = "account_name";   //  "9wcaipiao",
//     private static final String  pay_time = "pay_time";   //  "1543216612",
     private static final String  status = "status";   //  "success",
     private static final String  amount = "amount";   //  "1.00",
     private static final String  out_trade_no = "out_trade_no";   //  "20181126151610157112",
//     private static final String  trade_no = "trade_no";   //  "827162018112697575256",
//     private static final String  thoroughfare = "thoroughfare";   //  "0.025",
     
     private static final String  sign = "sign";   //  "fceaf11fdcab8c94caac37243b67385c",
//     private static final String  callback_time = "callback_time";   //  "1543216612",
//     private static final String  type = "type";   //  "2"
//     private static final String  key = "key";   //  "2"

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(out_trade_no);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[创联支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        StringBuilder signSrc = new StringBuilder();
        signSrc.append(api_response_params.get(amount));
        signSrc.append(api_response_params.get(out_trade_no));
        String paramsStr = signSrc.toString();
        String md5Crypt = MD5Utils.md5(paramsStr.getBytes());
        byte[] rc4_string = RC4.encry_RC4_byte(md5Crypt, channelWrapper.getAPI_KEY());
        String signMd5 = MD5Utils.md5(rc4_string);
        log.debug("[创联支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean checkResult = false;
        String payStatus = api_response_params.get(status);
        String responseAmount =  HandlerUtil.getFen(api_response_params.get(amount));
        
        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        //boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
//        boolean checkAmount =  HandlerUtil.isRightAmount(amountDb,responseAmount,"20");//第三方回调金额差额1元内
        if (checkAmount && payStatus.equalsIgnoreCase("success")) {
            checkResult = true;
        } else {
            log.error("[创联支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatus + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[创联支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + checkResult + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatus + " ,计划成功：success");
        return checkResult;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("[创联支付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[创联支付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}