package dc.pay.business.pbcomzhifu;

import java.util.Map;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;

/**
 * @author Cobby
 * Mar 6, 2019
 */
@ResponsePayHandler("PBCOMZHIFU")
public final class PbcomZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

/*    回调说明如下：
    post 请求 json格式示例如下 key来自收款平台发放
    {
        "data":base64({
            "mid":"10000",//下单参数原样返回
            "type":"0",//订单类型 (0非固定金额订单 1固定金额订单) 下单参数原样返回
            "oid":"3",//商户订单号 下单参数原样返回
            "amt":"2",//订单金额 下单参数原样返回
            "way":"3",//交易方式 (1微信支付,2支付宝支付,3微信WAP,4支付宝WAP) 下单参数原样返回
            "back":"4",//支付返回商户地址 下单参数原样返回
            "notify":"5",//支付成功通知商户地址 下单参数原样返回
            "remark":"6"//备注 下单参数原样返回
        "order_no:"7"//平台订单号  新加参数
    }),
        "sign":md5(data+key)
    }*/
    private static final String mid                ="mid";
    private static final String oid                ="oid";
    private static final String amt                ="amt";
    private static final String data               ="data";

    //signature    数据签名    32    是    　
    private static final String signature  ="sign";
    private static final String RESPONSE_PAY_MSG = "{\"error_code\":0}";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        String respData = new String(Base64.decodeBase64(API_RESPONSE_PARAMS.get(data)));
        if (!respData.contains("{") || !respData.contains("}")) {
            log.error("[pbcom支付]-[响应支付]-1.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(API_RESPONSE_PARAMS) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(JSON.toJSONString(API_RESPONSE_PARAMS));
        }
        JSONObject respObject = JSONObject.parseObject(respData);
        String partnerR = respObject.getString(mid);
        String ordernumberR = respObject.getString(oid);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[pbcom支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        String paramsStr = api_response_params.get(data)+channelWrapper.getAPI_KEY();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[pbcom支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        String respData = new String(Base64.decodeBase64(API_RESPONSE_PARAMS.get(data)));
        if (!respData.contains("{") || !respData.contains("}")) {
            log.error("[pbcom支付]-[响应支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(API_RESPONSE_PARAMS) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(JSON.toJSONString(API_RESPONSE_PARAMS));
        }
        JSONObject respObject = JSONObject.parseObject(respData);
        boolean my_result = false;
        String responseAmount = HandlerUtil.getFen(respObject.getString(amt));

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        if (checkAmount ) {
            my_result = true;
        } else {
            log.error("[pbcom支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[pbcom支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount );
        return my_result;
    }

    
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[pbcom支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[pbcom支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}