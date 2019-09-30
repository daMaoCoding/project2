package dc.pay.business.xinjiujiuzhifu;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Mar 28, 2019
 */
@ResponsePayHandler("XINJIUJIUZHIFU")
public final class XinJiuJiuZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //resultCode    是   string  交易标识    交易标识，success/fail   
    private static final String resultCode                   ="resultCode";
    //merchantNo    是   string  商户号 99支付平台分配的商户号   
    private static final String merchantNo                       ="merchantNo";
    //payMoney  是   string  订单金额    订单总金额，单位为分
    private static final String payMoney                          ="payMoney";
    //outTradeNo    是   string  商户订单号   商户系统内部订单号
    private static final String outTradeNo                        ="outTradeNo";
    
    //body    是   string  商品描述    商品简单描述，该字段请按照规范传递
    private static final String body                        ="body";
    //attach  否   string  附加数据    附加数据，在查询API和支付通知中原样返回，可作为自定义参数使用
//    private static final String attach                        ="attach";
    //orderNo 是   string  99支付订单号 99支付订单号
    private static final String orderNo                        ="orderNo";
    //payTime 是   string  支付完成时间  支付完成时间，格式为yyyyMMddHHmmss
    private static final String payTime                        ="payTime";
    //message 是   string  交易状态信息  交易状态信息 如：待支付 支付成功 已结算 部分结算 订单异常等
    private static final String message                        ="message";
    
    private static final String data                        ="data";
    
    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "SUCCESS";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        
        String partnerR = JSON.parseObject(API_RESPONSE_PARAMS.get(data)).getString(merchantNo);
        String ordernumberR = JSON.parseObject(API_RESPONSE_PARAMS.get(data)).getString(outTradeNo);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[新99支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        StringBuilder signStr = new StringBuilder();
        signStr.append(body+"=").append(JSON.parseObject(api_response_params.get(data)).getString(body)).append("&");
        signStr.append(merchantNo+"=").append(JSON.parseObject(api_response_params.get(data)).getString(merchantNo)).append("&");
        signStr.append(message+"=").append(JSON.parseObject(api_response_params.get(data)).getString(message)).append("&");
        signStr.append(orderNo+"=").append(JSON.parseObject(api_response_params.get(data)).getString(orderNo)).append("&");
        signStr.append(outTradeNo+"=").append(JSON.parseObject(api_response_params.get(data)).getString(outTradeNo)).append("&");
        signStr.append(payMoney+"=").append(JSON.parseObject(api_response_params.get(data)).getString(payMoney)).append("&");
        signStr.append(payTime+"=").append(JSON.parseObject(api_response_params.get(data)).getString(payTime)).append("&");
        signStr.append(resultCode+"=").append(JSON.parseObject(api_response_params.get(data)).getString(resultCode)).append("&");
        signStr.append(key+"=").append(api_key);
        String paramsStr = signStr.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[新99支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //resultCode    是   string  交易标识    交易标识，success/fail   
        String payStatusCode = JSON.parseObject(api_response_params.get(data)).getString(resultCode);
        String responseAmount = JSON.parseObject(api_response_params.get(data)).getString(payMoney);
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("success")) {
            my_result = true;
        } else {
            log.error("[新99支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[新99支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：success");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = JSON.parseObject(api_response_params.get(data)).getString(signature).equalsIgnoreCase(signMd5);
        log.debug("[新99支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[新99支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}