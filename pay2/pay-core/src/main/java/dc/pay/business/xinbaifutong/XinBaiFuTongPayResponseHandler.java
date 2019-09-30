package dc.pay.business.xinbaifutong;

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
import dc.pay.utils.RsaUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@ResponsePayHandler("XINBAIFUTONG")
public final class XinBaiFuTongPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String merchantId = API_RESPONSE_PARAMS.get("merchantId");
        String outOrderId = API_RESPONSE_PARAMS.get("outOrderId");
        String respType = API_RESPONSE_PARAMS.get("respType");

        if (StringUtils.isBlank(outOrderId) ||StringUtils.isBlank(merchantId)||StringUtils.isBlank(respType))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[新百付通]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + outOrderId);
        return outOrderId;
    }



    @Override
    protected String buildPaySign(Map<String, String> payParam, String api_key) throws PayException {
        log.debug("[新百付通]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString("wait....."));
        return "pay_md5sign";
    }




    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> payParam, String amount) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(payParam);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
                if("sign".equalsIgnoreCase(paramKeys.get(i).toString()))
                    continue;
            sb.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i)));
            if(i<paramKeys.size()-1){
                sb.append("&");
            }
        }
        try {
            boolean sign = RsaUtil.validateSignByPublicKey2(sb.toString(), channelWrapper.getAPI_PUBLIC_KEY(), payParam.get("sign").toString());
            if(!sign)
                new PayException("[新百付通]-[响应支付]-3.验证第三方支付签名出错："+sign+","+sb.toString()+","+channelWrapper.getAPI_PUBLIC_KEY());
        } catch (Exception e) {
           throw new PayException("[新百付通]-[响应支付]-3.验证第三方支付签名异常："+JSON.toJSONString(payParam),e);
        }

        String respType = payParam.get("respType");
        String transAmt = payParam.get("transAmt");
        String respCode = payParam.get("respCode");
        boolean result = false;
        boolean checkAmount = amount.equalsIgnoreCase(HandlerUtil.getFen(transAmt));
        if (checkAmount && respCode.equalsIgnoreCase("00") && !"E".equalsIgnoreCase(respType)) {
            result = true;
        } else {
            log.error("[新百付通]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + respCode + " ,支付金额：" + HandlerUtil.getFen(transAmt) + " ，应支付金额：" + amount);
        }
        log.debug("[新百付通]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + HandlerUtil.getFen(transAmt) + " ,数据库金额：" + amount + ",第三方响应支付成功标志:" + respCode + " ,计划成功：00");
        return result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
       // boolean result = api_response_params.get("sign").equalsIgnoreCase(signMd5);
        //log.debug("[新百付通]-[响应支付]-4.验证MD5签名：" + result);
       // return result;
        return true;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[新百付通]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}