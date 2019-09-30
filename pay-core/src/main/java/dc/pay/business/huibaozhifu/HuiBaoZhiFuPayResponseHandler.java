package dc.pay.business.huibaozhifu;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.business.ruijietong.RuiJieTongUtil;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RsaUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Mar 15, 2019
 */
@ResponsePayHandler("HUIBAOZHIFU")
public final class HuiBaoZhiFuPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //字段 ⻓度 可否为空 注释
    //input_charset 10 否 编码格式:UTF-8
//    private static final String input_charset                ="input_charset";
    //sign_type 3 否 签名⽅式:SHA1WITHRSA
//    private static final String sign_type                ="sign_type";
    //sign 256 否 待验签字符串
//    private static final String sign                ="sign";
    //request_time 20 可 YYMMDDHHmmss
//    private static final String request_time                ="request_time";
    //content 1024 否 业务参数待解密密⽂
//    private static final String content                ="content";
    //out_trade_no 64 否 商户平台订单号
    private static final String out_trade_no                ="out_trade_no";
    //status 3 否 订单状态：1.交易完成；2.交易失败
    private static final String status                ="status";
    
    //字段     ⻓度   可否 为空    注释
    //trade_id 64 否 平台提供的交易流⽔号
//    private static final String trade_id                 ="trade_id";
    //out_trad_no 40 否 原始商户订单
//    private static final String out_trad_no                 ="out_trad_no";
    //amount_str 40 否 ⾦额
    private static final String amount_str                 ="amount_str";
    //amount_fee 40 否 ⼿续费
//    private static final String amount_fee                 ="amount_fee";
    //status 40 否 状态：0.处理中；1.已完成；2.失败
//    private static final String status                 ="status";
    //for_trade_id 40 可 对应交易流⽔号(还款)
//    private static final String for_trade_id                 ="for_trade_id";
    //business_type 2 否 业务类型:0.代付;1.代扣;2.充值;3.退款;4.取现;5.移动快捷⽀付;6.移动    快捷退款;7.⽹关⽀付;8.微信⽀付;9.退票;10.调账;11.⽀付宝;99.未知
//    private static final String business_type                 ="business_type";
    //create_time 40 否 创建时间
//    private static final String create_time                 ="create_time";
    //modified_time 40 可 交易时间
//    private static final String modified_time                 ="modified_time";
    //remark 256 可 备注单
//    private static final String remark                 ="remark";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="signature";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
       //String api_KEY = channelWrapper.getAPI_KEY();
       //if (null == api_KEY || !api_KEY.contains("-") || api_KEY.split("-").length != 2) {
       //    log.error("[汇宝支付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //    throw new PayException("[汇宝支付]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //}
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
//        String partnerR = API_RESPONSE_PARAMS.get(merchno);
        String ordernumberR = API_RESPONSE_PARAMS.get(out_trade_no);
        if (StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[汇宝支付]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        boolean my_result = false; 
        try {
            byte[] resultf = RuiJieTongUtil.decryptByPrivateKey(org.apache.shiro.codec.Base64.decode(URLDecoder.decode(api_response_params.get("content"))),api_key.split("-")[1]);
//            my_result = RSACoderUtil.verify(new String(resultf, RuiJieTongUtil.CHARSET).getBytes(RuiJieTongUtil.CHARSET), channelWrapper.getAPI_PUBLIC_KEY(), URLDecoder.decode(api_response_params.get("sign")));
            my_result = RsaUtil.validateSignByPublicKey(new String(resultf, RuiJieTongUtil.CHARSET), channelWrapper.getAPI_PUBLIC_KEY(), URLDecoder.decode(api_response_params.get("sign")),"SHA1withRSA");  
        } catch (Exception e) {
            e.printStackTrace();
        }
        log.debug("[汇宝支付]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(my_result) );
        return String.valueOf(my_result);
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        JSONObject jsonObject = null;
        try {
            byte[] resultf = RuiJieTongUtil.decryptByPrivateKey(org.apache.shiro.codec.Base64.decode(URLDecoder.decode(api_response_params.get("content"))),channelWrapper.getAPI_KEY().split("-")[1]);
            jsonObject = JSONObject.parseObject( new String(resultf, RuiJieTongUtil.CHARSET));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        
        boolean my_result = false;
        //status 40 否 状态：0.处理中；1.已完成；2.失败
        String payStatusCode = jsonObject.getString(status);
        String responseAmount = HandlerUtil.getFen(jsonObject.getString(amount_str));

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        //boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("1")) {
            my_result = true;
        } else {
            log.error("[汇宝支付]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[汇宝支付]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        Boolean my_result = new Boolean(signMd5);
        log.debug("[汇宝支付]-[响应支付]-4.验证MD5签名：{}", my_result.booleanValue());
        return my_result.booleanValue();
    }

    @Override
    protected String responseSuccess() {
        log.debug("[汇宝支付]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}