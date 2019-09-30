package dc.pay.business.huihe;

/**
 * ************************
 *
 * @author tony 3556239829
 */

import com.alibaba.fastjson.JSON;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@ResponsePayHandler("HUIHE")
public final class HuiHePayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(HuiHePayResponseHandler.class);

    private static final String RESPONSE_PAY_MSG = "SUCCESS";


    private static final String AppId = "AppId";            //: "201709221733369623",
    private static final String OutTradeNo = "OutTradeNo";   //: "HUIHE_WX_SM-ChMYq",
    private static final String Code = "Code";               //: "0",
    private static final String Sign = "Sign";               //: "6C991AAF5F362EFCB55564B58E79FA40",
    private static final String SignType = "SignType";       //: "MD5",
    private static final String TotalAmount = "TotalAmount"; //: "0.01",
    private static final String TradeNo = "TradeNo";         //: "2017101517034436939497"


    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String memberId = API_RESPONSE_PARAMS.get(AppId);
        String orderId = API_RESPONSE_PARAMS.get(OutTradeNo);
        if (StringUtils.isBlank(memberId) || StringUtils.isBlank(orderId))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[汇合]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}", orderId);
        return orderId;
    }

    @Override
    protected String buildPaySign(Map<String, String> payParam, String api_key) throws PayException {
        String prestr = HuiHePayUtil.createLinkString(payParam); //把数组所有元素，按照“参数=参数值”的模式用“&”字符拼接成字符串
        String pay_md5sign =HuiHePayUtil.sign(prestr, channelWrapper.getAPI_KEY(), "UTF-8");
        log.debug("[汇合]-[请求支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amount) throws PayException {
        boolean result = false;
        String payStatusCode = api_response_params.get(Code);
        String responseAmount = api_response_params.get(TotalAmount);
        responseAmount = HandlerUtil.getFen(responseAmount);
        boolean checkAmount = amount.equalsIgnoreCase(responseAmount);
        if (checkAmount && payStatusCode.equalsIgnoreCase("0")) {
            result = true;
        } else {
            log.error("[汇合]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + amount);
        }
        log.debug("[汇合]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(Sign).equalsIgnoreCase(signMd5);
        log.debug("[汇合]-[响应支付]-4.验证MD5签名：{}", result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[汇合]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}