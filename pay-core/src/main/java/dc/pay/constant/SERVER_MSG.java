package dc.pay.constant;

/**
 * ************************
 * 服务器返回信息
 * @author tony 3556239829
 */
public enum SERVER_MSG {
    DECRYPT_API_KEY_ERROR("解密商户API-Key出错"),
    ENCRYPT_API_KEY_ERROR("加密商户API-Key出错"),
    SUCCESS("服务器执行成功"),
    NOT200("处理请求超时"),
    TIMEOUT("服务器执行超时。"),
    PROPERROR("加载配置文件出错。"),
    REQUEST_PAY_ERROR("发送支付请求失败"),
    REQUEST_PAY_PARSE_RESULT_ERROR("获取请求支付结果错误"),
    REQUEST_PAY_CHANNEL_RESULT_ERROR("通道返回结果异常"),
    REQUEST_PAY_CHANNEL_TOTAL_MONEY_ERROR("通道额度已满"),
    REQUEST_PAY_INFO__ERROR("请求支付参数不正确"),
    REQUEST_DAIFU_INFO__ERROR("请求代付参数不正确"),
    REQUEST_DAIFU_QUERY_BALANCE_INFO_ERROR("查询代付余额参数不正确"),
    REQUEST_PAY_AMOUNT__ERROR("支付金额不正确"),
    REQUEST_PAY_RESULT__ERROR("请求支付结果格式不正确"),
    REQUEST_PAY_QR_ERROR("解析二维码错误"),
    REQUEST_PAY_BUILDSIGN_ERROR("加密签名错误"),
    REQUEST_PAY_GET_REQPAYINFO_ID_ERROR("请求支付订单号不一致"),
    REQUEST_PAY_VALIDATE_REQPAYINFO_DOMAIN_ERROR("请求支付回调域名验证错误"),
    REQUEST_PAY_RESULT_VERIFICATION_ERROR("验证请求支付结果不完整"),
    REQUEST_PAY_GETDB_REQPAYINFO_ERROR("获取订单支付信息失败"),
    REQUEST_PAY_GETDB_REQDAIFUINFO_ERROR("获取订单代付信息失败"),
    RESPONSE_PAY_RESULT_EMPTY_ERROR("支付通道返回数据为空,或签名方式不对"),
    RESPONSE_DAIFU_RESULT_EMPTY_ERROR("代付第三方回调数据为空"),
    RESPONSE_PAY_RESULT_ERROR_APIKEY("请检查商户私钥,解密第三方回调数据出错"),
    RESPONSE_PAY_RESULT_ERROR_SIGN("第三方回调数据验签失败"),
    RESPONSE_PAY_RESULT_ERROR_STATE("第三方回调数据支付状态不正确"),
    RESPONSE_PAY_SIGN_METHOD_ERROR("签名(MD5/RSA/..)不对,请登陆第三方后台修改"),
    RESPONSE_PAY_SENDDB_ERROR("发送支付结果通知失败"),
    RESPONSE_PAY_RESULT_ERROR("支付通道返回数据处理错误"),
    RESPONSE_PAY_RESULT_PARAM_ERROR("支付通道返回数据错误"),
    RESPONSE_DAIFU_RESULT_AMOUNT_ERROR("验证金额失败"),
    RESPONSE_DAIFU_RESULT_STATUS_ERROR("验证订单状态失败"),
    RESPONSE_DAIFU_RESULT_SIGN_ERROR("验证签名失败"),

    RESPONSE_PAY_VALDATA_REQPAYINFO_ERROR("订单状态不正确，订单已经支付"),
    ORDER_HAS_BEN_PAYED("该订单已支付(可能强制入款)："),
    ORDER_HAS_BEN_DAIFUED("该订单已代付成功(可能强制确认代付成功)："),
    RESPONSE_PAY_VALDATA_SIGN_ERROR("订单签名验证失败！"),


    RESPONSE_PAY_HANDLER_ORDERID_ERROR(" 回调数据中，订单号获取错误。"),
    RESPONSE_PAY_HANDLER_GETREQPAYINFO_ERROR("回调数据中，无法找到订单支付信息"),
    RESPONSE_PAY_HANDLER_DECAPIKEY_ERROR("回调数据中，解密API-KEY出错"),
    RESPONSE_PAY_HANDLER_BUILDMD5_ERROR("回调数据中，md5生成出错："),
    RESPONSE_PAY_HANDLER_BUILDMD5EMPTY_ERROR("md5生成结果空。"),
    RESPONSE_PAY_HANDLER_CHECKAMOUNTANDSTATUS_ERROR("回调数据中，验证订单状态/金额出错："),
    RESPONSE_PAY_HANDLER_CHECKSIGN_ERROR("回调数据中，验证签名错误："),



    ;









    String msg;
    SERVER_MSG(String msg) {
        this.msg = msg;
    }
    public String getMsg() {
        return msg;
    }
}