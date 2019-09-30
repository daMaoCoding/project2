package dc.pay.business.taijizhifu;

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
 * Aug 1, 2019
 */
@ResponsePayHandler("TAIJIZHIFU")
public final class TaiJiZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //1）异步通知报文
    //以下所有报文字段皆会回调，若该字段无值，则带入空字符串。
    //参数名称    参数含义    格式  出现要求    备注
    //respCode    响应码 N4  M   详见附录响应码
//    private static final String respCode                ="respCode";
    //respMsg 响应码描述   ANS1..128   O   详见附录响应码
//    private static final String respMsg                ="respMsg";
    //secpVer 安全协议版本  AN3..16 R   icp3-1.1
//    private static final String secpVer                ="secpVer";
    //secpMode    安全协议类型  AN4..8  R   perm(固定密钥) | dyna（需要签到）
//    private static final String secpMode                ="secpMode";
    //macKeyId    密钥识别    ANS1..16    R   密钥编号，由平台提供，现与商户号相同
//    private static final String macKeyId                ="macKeyId";
    //orderDate   下单日期    N8  R   YYYYMMdd
//    private static final String orderDate                ="orderDate";
    //orderTime   下单时间    N6  R   HHmmss
//    private static final String orderTime                ="orderTime";
    //merId   商户号 AN1..15 R   由平台分配的商户号
    private static final String merId                ="merId";
    //extInfo 额外信息    ANS1..512   M   
//    private static final String extInfo                ="extInfo";
    //orderId 商户订单号   AN1..15 R   商户系统产生并上送
    private static final String orderId                ="orderId";
    //txnId   交易流水号（交易凭证号）    AN8..32 M   平台产生的唯一交易流水号
//    private static final String txnId                ="txnId";
    //txnAmt  交易金额    N1..12  R   单位为分，实际交易金额
    private static final String txnAmt                ="txnAmt";
    //currencyCode    交易币种    NS3 R   默认156
//    private static final String currencyCode                ="currencyCode";
    //txnStatus   交易状态    N2  M   当respCode为0000或1101时返回此域    交易状态    01---处理中    10---交易成功    20---交易失败    30---其他状态（需联系管理人员）
    private static final String txnStatus                ="txnStatus";
    //txnStatusDesc   交易状态说明  ANS1..128   M   交易状态辅助说明
//    private static final String txnStatusDesc                ="txnStatusDesc";
    //timeStamp   时间戳 N14 N14 目前报文时间，格式：YYYYMMddHHmmss
//    private static final String timeStamp                ="timeStamp";
    //mac 签名      R   
//    private static final String mac                ="mac";

    private static final String key        ="k";
    //signature    数据签名    32    是    　
    private static final String signature  ="mac";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
       //String api_KEY = channelWrapper.getAPI_KEY();
       //if (null == api_KEY || !api_KEY.contains("-") || api_KEY.split("-").length != 2) {
       //    log.error("[太极支付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //    throw new PayException("[太极支付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //}
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(merId);
        String ordernumberR = API_RESPONSE_PARAMS.get(orderId);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[太极支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }
    
    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        //Map<String, String> jsonToMap = handlerUtil.jsonToMap(api_response_params.get(data));
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!signature.equals(paramKeys.get(i))) {
                signSrc.append(paramKeys.get(i)).append("=").append(StringUtils.isBlank(api_response_params.get(paramKeys.get(i))) ? "" : api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        signSrc.append(key +"="+ channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[太极支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //txnStatus 交易状态    N2  M   当respCode为0000或1101时返回此域        交易状态        01---处理中        10---交易成功        20---交易失败        30---其他状态（需联系管理人员）
        String payStatusCode = api_response_params.get(txnStatus);
        String responseAmount = api_response_params.get(txnAmt);

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        // 1 代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("10")) {
            my_result = true;
        } else {
            log.error("[太极支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[太极支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：10");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[太极支付]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[太极支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}