package dc.pay.business.mangguo;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.business.caifubao.StringUtil;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * ************************
 *
 * @author tony 3556239829
 */
@ResponsePayHandler("MANGGUO")
public class MangGuoPayResponseHandler extends PayResponseHandler {
    private static final Logger log = LoggerFactory.getLogger(MangGuoPayResponseHandler.class);

    private static final String code           ="code";           ///: "0000",
    private static final String msg            ="msg";            ///: "成功.",
    private static final String order_sn       ="order_sn";       ///: "201712101617063935164",
    private static final String down_sn        ="down_sn";        ///: "MANGGUO_WAP_WX-rGyNP",
    private static final String status         ="status";         ///: "2",
    private static final String amount         ="amount";         ///: "1.00",
    private static final String fee            ="fee";           ///: "0.03",
    private static final String trans_time     ="trans_time";    ///: "20171210161842",
    private static final String sign           ="sign";          ///: "b0eb7223c60f51e0f4172bd1eeda9272"
    private static final String SIGN = "sign";
    private static final String RETURNCODE_SUCCESS = "2";
    private static final String RESPONSE_PAY_MSG = "SUCCESS";


    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        String orderNum;
        if (null != API_RESPONSE_PARAMS && !API_RESPONSE_PARAMS.isEmpty()) {
            orderNum = API_RESPONSE_PARAMS.get(down_sn);
            if (StringUtils.isBlank(orderNum))
                throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        } else {
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_PARAM_ERROR);
        }
        log.debug("[芒果]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + orderNum);
        return orderNum;
    }
    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        if(!channelWrapper.getAPI_MEMBERID().contains("&") ||channelWrapper.getAPI_MEMBERID().split("&").length!=2 || StringUtil.isBlank(channelWrapper.getAPI_MEMBERID().split("&")[1])){
            throw new PayException("[芒果]-[响应支付]-2.生成Md5签名出错，商户号格式错误：商户号&交易私钥(来自第三方网站)");
        }
        String pay_md5sign = null;
        List<String> paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder sb = new StringBuilder();
        //	协议参数不参与签名，sign、code、msg参数不参与签名
        for (int i = 0; i < paramKeys.size(); i++) {
            if(StringUtil.isBlank(api_response_params.get(paramKeys.get(i)))|| "member_code".equalsIgnoreCase(paramKeys.get(i))  || "sign".equalsIgnoreCase(paramKeys.get(i))  || "code".equalsIgnoreCase(paramKeys.get(i))  || "msg".equalsIgnoreCase(paramKeys.get(i)) )
                continue;
            sb.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
        }
        sb.append("key="+channelWrapper.getAPI_MEMBERID().split("&")[1]);//
        pay_md5sign = HandlerUtil.getMD5UpperCase(sb.toString()).toLowerCase();
        log.debug("[芒果]-[响应支付]-2.响应内容生成md5完成：" + pay_md5sign);
        return pay_md5sign;
    }
    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amount) throws PayException {
        boolean result = false;

        String payStatusCode = api_response_params.get(status);
        String responseAmount = api_response_params.get("amount");
        boolean checkAmount = amount.equalsIgnoreCase(HandlerUtil.getFen(responseAmount));
        if (checkAmount && payStatusCode.equalsIgnoreCase(RETURNCODE_SUCCESS)) {
            result = true;
        } else {
            log.error("[芒果]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + amount);
        }
        log.debug("[芒果]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return result;
    }
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(SIGN).equalsIgnoreCase(signMd5);
        log.debug("[芒果]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }
    @Override
    protected String responseSuccess() {
        log.debug("[芒果]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}