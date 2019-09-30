package dc.pay.business.xinfubao;

/**
 * ************************
 *
 * @author tony 3556239829
 */

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@ResponsePayHandler("XINFUBAO")
public final class XinFuBaoPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(XinFuBaoPayResponseHandler.class);

    private static final String RESPONSE_PAY_MSG = "SUCCESS";

    private static final String signData = "signData";          //: "90B0A967B0EC5F1EADEDF8C1EC4A4CCC",
    private static final String versionId = "versionId";        //: "1.0",
    private static final String orderAmount = "orderAmount";    //: "1",
    private static final String transType = "transType";        //: "008",
    private static final String asynNotifyUrl = "asynNotifyUrl";//: "http://45.76.13.45:8080/respPayWeb/XINFUBAO_BANK_WEBWAPAPP_WX_SM/",
    private static final String payTime = "payTime";            //: "20171015151243",
    private static final String orderStatus = "orderStatus";    //: "01",
    private static final String signType = "signType";          //: "MD5",
    private static final String merId = "merId";                //"100519455",
    private static final String payId = "payId";                //"919460102388326400",
    private static final String prdOrdNo = "prdOrdNo";          //: "XINFUBAO_WX_SM-iyU7I"


    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String memberId = API_RESPONSE_PARAMS.get(merId);
        String orderId = API_RESPONSE_PARAMS.get(prdOrdNo);
        if (StringUtils.isBlank(memberId) || StringUtils.isBlank(orderId))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[信付宝]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}", orderId);
        return orderId;
    }

    @Override
    protected String buildPaySign(Map<String, String> payParam, String api_key) throws PayException {
        SortedMap<String, String> payParams = Maps.newTreeMap();
        payParams.putAll(payParam);
        String pay_md5sign = XinFuBaoPayUtil.createSign(payParams,channelWrapper.getAPI_KEY()); //把数组所有元素，按照“参数=参数值”的模式用“&”字符拼接成字符串
        log.debug("[信付宝]-[请求支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amount) throws PayException {
        boolean result = false;
        String payStatusCode = api_response_params.get(orderStatus);
        String responseAmount = api_response_params.get(orderAmount);
        boolean checkAmount = amount.equalsIgnoreCase(responseAmount);
        if (checkAmount && payStatusCode.equalsIgnoreCase("01")) {
            result = true;
        } else {
            log.error("[信付宝]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + amount);
        }
        log.debug("[信付宝]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(signData).equalsIgnoreCase(signMd5);
        log.debug("[信付宝]-[响应支付]-4.验证MD5签名：{}", result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[信付宝]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}