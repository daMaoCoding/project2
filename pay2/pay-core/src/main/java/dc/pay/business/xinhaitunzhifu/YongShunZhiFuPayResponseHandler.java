package dc.pay.business.xinhaitunzhifu;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.TreeMap;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.business.tongyinfu.MD5Util;
import dc.pay.business.tongyinfu.SignUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;

import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;

/**
 * @author cobby
 * Jan 17, 2019
 */
@ResponsePayHandler("YONGSHUNZHIFU")
public final class YongShunZhiFuPayResponseHandler extends PayResponseHandler {
        private final Logger log = LoggerFactory.getLogger(getClass());

    //参数名              描述               属性              请求        说明
    //orgid               机构号             Str-max32          M          平台分配机构号
    //merno               商户号             Str-max32          M          平台分配商户号
    //amount              交易金额           Str-max32          M          订单交易金额
    //goods_info          商品信息           Str-max32          M          订单商品信息
    //trade_date          订单交易时间       Str-max255         M          订单交易时间，格式：yyyy-MM-dd HH:mm:ss
    //trade_status        交易状态           Int-1              M          0-成功支付 1-未支付
    //order_id            订单号             Str-max32          M          对应交易类上送的订单号
    //plat_order_id       平台订单号         Str-max32          M          平台的订单号
    //timestamp           请求时间戳         Str-max14          M          格式:yyyyMMddHHmmss
    //sign_data           数据签名           Str-max32          M          签名规则与请求的签名规则一致
    private static final String merno                         ="merno";
    private static final String amount                        ="amount";
    private static final String trade_status                  ="trade_status";
    private static final String order_id                      ="order_id";

    //signature    数据签名    32    是    　
    private static final String signature  ="sign_data";

    private static final String RESPONSE_PAY_MSG = "{\"responseCode\": \"0000\"}";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(merno);
        String ordernumberR = API_RESPONSE_PARAMS.get(order_id);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[永顺支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> params, String api_key) throws PayException {
        Map<String, String> map = new TreeMap<>(params);
        map.remove(signature);
        String paramsStr = SignUtil.getSignData(map)+api_key;
        String signMd5 = null;
        try {
            signMd5 = MD5Util.getMD5(paramsStr.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        log.debug("[永顺支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }


    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean my_result = false;
        //trade_status        交易状态           Int-1              M          0-成功支付 1-未支付
        String payStatusCode = api_response_params.get(trade_status);
        String responseAmount = api_response_params.get(amount);
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("0")) {
            my_result = true;
        } else {
            log.error("[永顺支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + amountDb);
        }
        log.debug("[永顺支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + amountDb + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：0");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[永顺支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[永顺支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}