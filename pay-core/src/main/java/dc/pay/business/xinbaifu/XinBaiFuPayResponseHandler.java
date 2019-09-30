package dc.pay.business.xinbaifu;

/**
 * ************************
 * @author tony 3556239829
 */

import com.alibaba.fastjson.JSON;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.CopUtils;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.kspay.AESUtil;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@ResponsePayHandler("XINBAIFU")
public final class XinBaiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

            private static final String RESPONSE_PAY_MSG = "SUCCESS";

            private static final String  Status           = "Status"  ;               //-> "0"
            private static final String  smzfMsgId        = "smzfMsgId"  ;            //-> "HGDG913718063642181632"  平台订单号
            private static final String  totalAmount      = "totalAmount"  ;         //-> "1.00"
            private static final String  merchantCode     = "merchantCode"  ;        //-> "SSSSS00001"
            private static final String  payTime          = "payTime"  ;              //-> "20170929"
            private static final String  isClearOrCancel  = "isClearOrCancel"  ;      //-> "0"  支付状态,0:支付成功,1:支付失败
            private static final String  sign             = "sign"  ;                //-> "119613D987995A0BABFD3D1A183F1766EDAEF82E"
            private static final String  respMsg          = "respMsg"  ;             //-> "TRADE_SUCCESS"
            private static final String  settleDate       = "settleDate"  ;          //-> "null"
            private static final String  extra_para       = "extra_para"  ;          //-> "test"
            private static final String  reqMsgId         =  "reqMsgId" ;            // -> "150668241641433"


    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String orderId = API_RESPONSE_PARAMS.get("orderId");
        if (StringUtils.isBlank(orderId))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[新百付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + orderId);
        return orderId;
    }

    @Override
    protected String buildPaySign(Map<String, String> payParam, String api_key) throws PayException {
        String transData = payParam.get("transData");
        String aesStr = AESUtil.decrypt(transData, channelWrapper.getAPI_KEY()); //AES解密
        String desStr = new String(Base64.decodeBase64(aesStr.getBytes())); //base64_decode 解密
        Map<String, String> payParamNew = HandlerUtil.urlToMap(desStr);
        payParamNew.remove("sign");
        String pay_md5sign = CopUtils.sign(payParamNew, null,channelWrapper.getAPI_KEY()); //验签
        log.debug("[新百付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> payParam, String amount) throws PayException {
        boolean result = false;
        String transData = payParam.get("transData");
        String aesStr = AESUtil.decrypt(transData, channelWrapper.getAPI_KEY()); //AES解密
        String desStr = new String(Base64.decodeBase64(aesStr.getBytes())); //base64_decode 解密
        Map<String, String> payParamNew = HandlerUtil.urlToMap(desStr);
        String Status = payParamNew.get("Status");
        String isClearOrCancel = payParamNew.get("isClearOrCancel");
        String totalAmount = payParamNew.get("totalAmount");
        boolean checkAmount = amount.equalsIgnoreCase(HandlerUtil.getFen(totalAmount));
        if (checkAmount && Status.equalsIgnoreCase("0") && isClearOrCancel.equalsIgnoreCase("0")) {
            result = true;
        } else {
            log.error("[新百付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + isClearOrCancel + " ,支付金额：" + totalAmount + " ，应支付金额：" + amount);
        }
        log.debug("[新百付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + totalAmount + " ,数据库金额：" + amount + ",第三方响应支付成功标志:" + isClearOrCancel + " ,计划成功：0");
        return result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> payParam, String signMd5) {
        boolean result =false;
        String transData = payParam.get("transData");
        String aesStr = AESUtil.decrypt(transData, channelWrapper.getAPI_KEY()); //AES解密
        String desStr = new String(Base64.decodeBase64(aesStr.getBytes())); //base64_decode 解密
        Map<String, String> payParamNew = HandlerUtil.urlToMap(desStr);
        result=signMd5.equalsIgnoreCase(payParamNew.get("sign"));
        log.debug("[新百付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[新百付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}