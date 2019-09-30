package dc.pay.business.mashanfu2;

/**
 * ************************
 * @author tony 3556239829
 */

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RsaUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@ResponsePayHandler("MASHANFU2")
public final class MaShanFu2PayResponseHandler extends PayResponseHandler {
     private final Logger log = LoggerFactory.getLogger(getClass());
     private static final String RESPONSE_PAY_MSG = "0";



     private static final String  orderNo ="orderNo";// "20180827113852",
     private static final String  data ="data";// "aPoUh4lRJ2+O/lea+metcqTaBRl4JoSky4NhUtC7It1NKIqnPIH/DCfFUQLwWAP10lakoDyr+/HiNJa9S+YCR9zFxg5qUGGYYVX5FDEQSr0PbDXzXkFD40mFAgnWZP1hwpNVNyIJbN2yD+bEJXnK6Gd+/fEM1GyL/uXknZuhcuFkcInCKRNg7P+TvI98cIUjRrlXlib9e42xn34ymlqOj+jPD23fSc0soQxYcwKRL6gieNVyoXypnzTKKEyD5JDwaXadLtc7U5cXM+dk7g6jmnZJ6HSUymYR2GBa3ELAcgU28anzjwmKECv1TQ4LYZDh1fRI+m/86tsYeyfMv/23xg==",
     private static final String  merNo ="merNo";// "Mer1530098602429x97",
     private static final String  sign ="sign";// "AAA9B337C68C0CD1EDDF3DA992F9F8CB"


     private static final String  amount = "amount";// "991",
     private static final String  orderNum = "orderNum";// "20180827113852",
     private static final String  payStateCode = "payStateCode";// "10",
     private static final String  goodsName = "goodsName";// "20180827113852",
     private static final String  notifyDate = "notifyDate";// "2018-08-27 11:40:43"


    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String ordernumberR = API_RESPONSE_PARAMS.get(orderNo);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[码闪付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：" + ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        String deTxt = RsaUtil.dencipher(channelWrapper.getAPI_KEY(), params.get(data));
        if(StringUtils.isBlank(deTxt)) throw new PayException("私钥解密回调数据错误，请检查私钥");
        String signMd5 = HandlerUtil.getMD5UpperCase(deTxt+channelWrapper.getAPI_MEMBERID().split("&")[1]);
        log.debug("[码闪付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }



    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean checkResult = false;
        String deTxt = RsaUtil.dencipher(channelWrapper.getAPI_KEY(), api_response_params.get(data));
        JSONObject resJsonObject = JSON.parseObject(deTxt);
        String payStatus = resJsonObject.getString(payStateCode);
        String responseAmount =  resJsonObject.getString(amount);
        //boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        boolean checkAmount =  HandlerUtil.isRightAmount(amountDb,responseAmount,"100");//第三方回调金额差额1元内
        if (checkAmount && payStatus.equalsIgnoreCase("10")) {
            checkResult = true;
        } else {
            log.error("[码闪付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatus + " ,支付金额：" + responseAmount + " ，应支付金额：" + amountDb);
        }
        log.debug("[码闪付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + checkResult + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amountDb + ",第三方响应支付成功标志:" + payStatus + " ,计划成功：1");
        return checkResult;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("[码闪付]-[响应支付]-4.验证MD5签名：" + result);
        return result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[码闪付]-[响应支付]-5.第三方支付确认收到消息返回内容：" + RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}