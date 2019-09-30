package dc.pay.business.xinhuiwang;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
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
 * 
 * @author andrew
 * Aug 6, 2019
 */
@ResponsePayHandler("XINHUIWANG")
public final class XinHuiWangPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //编号 字段名称 数据类型 字段说明 必填 备注
    //1. mercId char(15) 原订单商户编号 Y 由我司分配
    private static final String mercId                ="mercId";
    //2. mercOrderId varchar(24) 商户原订单编号 Y
    private static final String mercOrderId                ="mercOrderId";
    //3. orderStatus char(10) 支付状态 Y 取值请见 3.2.2 章节
    private static final String orderStatus                ="orderStatus";
    //4. createdTime varchar(19) 订单创建时间 Y 示例：2018-11-23 14:52:31
//    private static final String createdTime                ="createdTime";
    //5. paidTime varchar(19) 用户支付时间 Y 示例：2018-11-23 17:20:48
//    private static final String paidTime                ="paidTime";
    //6. orderId varchar(20) 平台系统流水号 Y
//    private static final String orderId                ="orderId";
    //7. txnAmt double 交易金额 Y 交易金额，单位元
    private static final String txnAmt                ="txnAmt";
    //8. txnFee double 交易手续费 Y 交易手续费，单位元
//    private static final String txnFee                ="txnFee";
    //9. sign varchar(32) 数据签名 Y 用于判断报文数据合法性
//    private static final String sign                ="sign";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
       //String api_KEY = channelWrapper.getAPI_KEY();
       //if (null == api_KEY || !api_KEY.contains("-") || api_KEY.split("-").length != 2) {
       //    log.error("[新汇旺]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //    throw new PayException("[新汇旺]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //}
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(mercId);
        String ordernumberR = API_RESPONSE_PARAMS.get(mercOrderId);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[新汇旺]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        //Map<String, String> jsonToMap = handlerUtil.jsonToMap(api_response_params.get(data));
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        try {
            for (int i = 0; i < paramKeys.size(); i++) {
                if (!signature.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                    signSrc.append(paramKeys.get(i)).append("=").append(URLEncoder.encode(api_response_params.get(paramKeys.get(i)), "utf-8")).append("&");
                }
            }
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            log.error("[新汇旺]-[响应支付]-2.1.发送支付请求，及获取支付请求参数：" + JSON.toJSONString(api_response_params) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(JSON.toJSONString(api_response_params));
        }
        
        //删除最后一个字符
        signSrc.deleteCharAt(signSrc.length()-1);
        signSrc.append(api_key);
        String paramsStr = signSrc.toString();
//        System.out.println("签名源串=========>"+paramsStr);
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[新汇旺]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //3. orderStatus varchar(10) 支付状态 Y        created：订单已创建未支付;        paid：支付成功;        expired: 订单超时未支付;
        String payStatusCode = api_response_params.get(orderStatus);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(txnAmt));

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        // 1 代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("paid")) {
            my_result = true;
        } else {
            log.error("[新汇旺]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[新汇旺]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：paid");
        return my_result;
    }
    
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[新汇旺]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[新汇旺]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}