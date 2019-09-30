package dc.pay.business.jinniuzhifu;

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
 * @author XXXXXXX
 * June 1, 2019
 */
@ResponsePayHandler("JINZUANZHIFU")
public final class JinZuanZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //POST 参数说明：
    //参数 参数名称 类型(长度) 可否为空 参数说明 样例
    //公共基础参数（放入请求 url）
    //merchantId 接入商户标识    String 否 商户在金钻支付上的    接入商户标识
    private static final String merchantId                ="merchantId";
    //timestamp 请求时间 String 否 格式：unix_timestamp，精确到毫秒
    private static final String timestamp                ="timestamp";
    //signatureMethod 签名类型 String 否 签名类型，目前支持HmacSHA256
    private static final String signatureMethod                ="signatureMethod";
    //signatureVersion 签名算法版    本 Int 否 签名算法版本，目前支持 1
    private static final String signatureVersion                ="signatureVersion";
    //signature 签名 String 否 签名信息，算法参见第    2.3.1 节
//    private static final String signature                ="signature";
    
    //订单业务结果（放入请求 body）
    //orderId 金钻商户的    订单编号    String 否
    private static final String orderId                ="orderId";
    //status 订单状态 Int 否 订单状态
    private static final String status                ="status";
    //jOrderId 商户的订单号 String 否
    private static final String jOrderId                ="jOrderId";
    //notifyUrl 商户创建订单时设置的回调 Url    String 是 商户在调用“创建订单接口”时传入的回调 Url
    private static final String notifyUrl                ="notifyUrl";
    //orderType 订单类型 Int 否 1：充值订单
    private static final String orderType                ="orderType";
    //amount 订单金额 Double 否
    private static final String amount                ="amount";
    //currency 货币类型 String 否 目前全为 cny
    private static final String currency                ="currency";
    //actualAmount 实际支付金 Double 否 单位：元,与 amount 可能会额 不一样，就是说下单金额与    实际金额可能不一样，最终支付以这个金额为准
    private static final String actualAmount                ="actualAmount";
    //fee 产生的费用 Double 否 单位：元
    private static final String fee                ="fee";
    //payWay 支付方式 String 否 见附录“支付类型“
    private static final String payWay                ="payWay";
    //payTime 支付时间 Long 否 Unix_timestamp
    private static final String payTime                ="payTime";
    //jExtra 附加字段 可空 商户提交支付时的 jExtra    字段原样返回
    private static final String jExtra                ="jExtra";
    
    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="signature";

    private static final String RESPONSE_PAY_MSG = jsonResponsePayMsg("{\"code\":0,\"message\":\"ok\",\"data\":{}}");

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
       //String api_KEY = channelWrapper.getAPI_KEY();
       //if (null == api_KEY || !api_KEY.contains("-") || api_KEY.split("-").length != 2) {
       //    log.error("[金钻支付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //    throw new PayException("[金钻支付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //}
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(merchantId);
        String ordernumberR = API_RESPONSE_PARAMS.get(jOrderId);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[金钻支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        //Map<String, String> jsonToMap = handlerUtil.jsonToMap(api_response_params.get(data));
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!signature.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        signSrc.append(key +"="+ channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        System.out.println("签名源串=========>"+paramsStr);
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[金钻支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //status       订单状态 说明        1 已创建        2 已支付        3 完成        4 取消
        String payStatusCode = api_response_params.get(status);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(amount));

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        // 1 代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("2")) {
            my_result = true;
        } else {
            log.error("[金钻支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[金钻支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：2");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[金钻支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[金钻支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}