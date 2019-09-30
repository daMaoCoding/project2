package dc.pay.business.quantongyun;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@ResponsePayHandler("QUANTONGYUN")
public final class QuanTongYunPayResponseHandler extends PayResponseHandler {
     private final Logger log = LoggerFactory.getLogger(getClass());
     private static final String RESPONSE_PAY_MSG = "success";

     private static final String   p1_MerId  = "p1_MerId";                //: "60",
     private static final String   r0_Cmd  = "r0_Cmd";                    //: "Buy",
     private static final String   r1_Code  = "r1_Code";                  //: "1",
     private static final String   r2_TrxId  = "r2_TrxId";                //: "O20180523170616149360",
     private static final String   r3_Amt  = "r3_Amt";                    //: "30.000",
     private static final String   r4_Cur  = "r4_Cur";                    //: "CNY",
     private static final String   r5_Pid  = "r5_Pid";                    //: "PAY",
     private static final String   r6_Order  = "r6_Order";                //: "QUANTONGYUN_ZFB_SM-RFkQk",
     private static final String   r7_Uid  = "r7_Uid";                    //: "",
     private static final String   r8_MP  = "r8_MP";                      //: "pa_MP",
     private static final String   r9_BType  = "r9_BType";                //: "2",
     private static final String   rb_BankId  = "rb_BankId";              //: "alipay",
     private static final String   ro_BankOrderId  = "ro_BankOrderId";    //: "",
     private static final String   rp_PayDate  = "rp_PayDate";            //: "2018-05-23",
     private static final String   rq_CardNo  = "rq_CardNo";              //: "",
     private static final String   ru_Trxtime  = "ru_Trxtime";            //: "2018-05-23",
     private static final String   hmac  = "hmac";                        //: "37e15e865b9279c0f40a563b7280aa01"



    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(r6_Order);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[全通云付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if(StringUtils.isBlank(params.get(paramKeys.get(i)))
                    || hmac.equalsIgnoreCase(paramKeys.get(i).toString())
                    || rb_BankId.equalsIgnoreCase(paramKeys.get(i).toString())
                    || ro_BankOrderId.equalsIgnoreCase(paramKeys.get(i).toString())
                    || rp_PayDate.equalsIgnoreCase(paramKeys.get(i).toString())
                    || rq_CardNo.equalsIgnoreCase(paramKeys.get(i).toString())
                    || ru_Trxtime.equalsIgnoreCase(paramKeys.get(i).toString())
                    )  //
                continue;
            sb.append(params.get(paramKeys.get(i)));
        }
        String signStr = sb.toString();
        pay_md5sign = QuanTongYunDigestUtil.hmacSign(signStr, channelWrapper.getAPI_KEY());
        log.debug("[全通云付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }



    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean result = false;
        String payStatus = api_response_params.get(r1_Code);
        String responseAmount =  HandlerUtil.getFen(api_response_params.get(r3_Amt));
        boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        if (checkAmount && payStatus.equalsIgnoreCase("1")) {
            result = true;
        } else {
            log.error("[全通云付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatus + " ,支付金额：" + responseAmount + " ，应支付金额：" + amountDb);
        }
        log.debug("[全通云付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amountDb + ",第三方响应支付成功标志:" + payStatus + " ,计划成功：1");
        return result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(hmac).equalsIgnoreCase(signMd5);
        log.debug("[全通云付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[全通云付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}