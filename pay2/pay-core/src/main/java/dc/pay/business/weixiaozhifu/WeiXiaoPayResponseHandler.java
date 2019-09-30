package dc.pay.business.weixiaozhifu;

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
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.crypto.hash.Sha256Hash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

@ResponsePayHandler("WEIXIAOZHIFU")
public final class WeiXiaoPayResponseHandler extends PayResponseHandler {
     private final Logger log = LoggerFactory.getLogger(getClass());
     private static final String RESPONSE_PAY_MSG = "SUCCESS";

     private static final String  encodeType   =  "encodeType"  ;    //: "SHA2",
     private static final String  memo   =  "memo"  ;                //: "memo",
     private static final String  merchantId   =  "merchantId"  ;    //: "100310680258",
     private static final String  orderAmount   =  "orderAmount"  ;   //: "10",
     private static final String  orderNo   =  "orderNo"  ;           //: "1522657049556",
     private static final String  payMode   =  "payMode"  ;           //: "QQ",
     private static final String  signSHA2   =  "signSHA2"  ;         //: "FA4767B1E95E8B2E66772CACB62A0D282726C776123263BB991837256063B0D5",
     private static final String  success   =  "success"  ;           //: "Y",
     private static final String  tradeNo   =  "tradeNo"  ;           //: "MD180402161731190658"



    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(orderNo);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[微笑支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if( success.equalsIgnoreCase(paramKeys.get(i).toString()  )   || signSHA2.equalsIgnoreCase(paramKeys.get(i).toString()  )  ||   memo.equalsIgnoreCase(paramKeys.get(i).toString() ) )  //
                continue;
            sb.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i))).append("&");
        }
        sb.append("HashIV=" +channelWrapper.getAPI_MEMBERID().split("&")[1]);
        String signStr = "SHA2Key=".concat(channelWrapper.getAPI_KEY()).concat("&").concat(sb.toString()); //.replaceFirst("&key=","")

        // pay_md5sign = HandlerUtil.getMD5UpperCase(signStr).toLowerCase();

        try {
            signStr =  URLEncoder.encode(signStr,"UTF-8").toLowerCase();
            pay_md5sign = new Sha256Hash(signStr, null).toString().toUpperCase();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return pay_md5sign;
    }



    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean result = false;
        String payStatus = api_response_params.get(success);
        String responseAmount =  HandlerUtil.getFen(api_response_params.get(orderAmount));
        boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        if (checkAmount && payStatus.equalsIgnoreCase("Y")) {
            result = true;
        } else {
            log.error("[微笑支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatus + " ,支付金额：" + responseAmount + " ，应支付金额：" + amountDb);
        }
        log.debug("[微笑支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amountDb + ",第三方响应支付成功标志:" + payStatus + " ,计划成功：Y");
        return result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(signSHA2).equalsIgnoreCase(signMd5);
        log.debug("[微笑支付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[微笑支付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}