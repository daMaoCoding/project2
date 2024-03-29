package dc.pay.business.hmizhifu;

import java.util.List;
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
import dc.pay.utils.MapUtils;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Jul 5, 2019
 */
@ResponsePayHandler("HMIZHIFU")
public final class HMiZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //序号  变量名 参数名 是否必填    说明
    //1   appid        商户ID    Y   平台分配商户号
    private static final String appid                ="appid";
    //2   order_no        订单号 Y   上送订单号唯一, 字符长度20
    private static final String order_no                ="order_no";
    //3   amount        商户订单金额  Y   商品金额(单位分)
//    private static final String amount                ="amount";
    //4   platform_order_no        平台流水订单号 N   
//    private static final String platform_order_no                ="platform_order_no";
    //5   attach  附加字段    Y   
//    private static final String attach                ="attach";
    //6   pay_time    交易时间    Y   
//    private static final String pay_time                ="pay_time";
    //7   pay_status        交易状态        N   -1支付中   0交易失败 1交易成功
    private static final String pay_status                ="pay_status";
    //9   actual_amount        实付金额    Y   商品金额(单位分)
    private static final String actual_amount                ="actual_amount";
    //8   sign        MD5签名        Y   请看MD5签名字段格式
//    private static final String sign                ="sign";
    
    private static final String key        ="secret";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "SUCCESS";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
       //String api_KEY = channelWrapper.getAPI_KEY();
       //if (null == api_KEY || !api_KEY.contains("-") || api_KEY.split("-").length != 2) {
       //    log.error("[H米支付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //    throw new PayException("[H米支付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //}
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(appid);
        String ordernumberR = API_RESPONSE_PARAMS.get(order_no);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[H米支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        //Map<String, String> jsonToMap = handlerUtil.jsonToMap(api_response_params.get(data));
        //或者直接取出数值类型
        //String responseAmount = JSON.parseObject(api_response_params.get(data)).getString(payMoney);
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
//            if (!signature.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
            if (!signature.equals(paramKeys.get(i))) {
                signSrc.append(paramKeys.get(i)).append("=").append(StringUtils.isBlank(api_response_params.get(paramKeys.get(i))) ? "" : api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        signSrc.append(key +"="+ channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        System.out.println("签名源串=========>"+paramsStr);
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[H米支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }
    
    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //7 pay_status        交易状态        N   -1支付中   0交易失败 1交易成功
        String payStatusCode = api_response_params.get(pay_status);
        String responseAmount = api_response_params.get(actual_amount);

        //tony(Tony) 01-17 15:09:28
        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        //boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("1")) {
            my_result = true;
        } else {
            log.error("[H米支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[H米支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[H米支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[H米支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}