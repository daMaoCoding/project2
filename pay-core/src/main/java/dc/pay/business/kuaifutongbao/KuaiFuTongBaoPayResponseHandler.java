package dc.pay.business.kuaifutongbao;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayResponseHandler;
import dc.pay.config.annotation.ResponsePayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.DigestUtil;
import dc.pay.utils.HandlerUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Aug 1, 2018
 */
@ResponsePayHandler("KUAIFUTONGBAO")
public final class KuaiFuTongBaoPayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //参数名称          参数含义                       参数长度             参数说明        顺序
    //p1_MerId          商户编号                       Max(11)             4位数字，是商户在[API支付平台]系统的唯一身份标识请登录商户后台查看        1
    //r0_Cmd            业务类型                       Max(20)             固定值“Buy”        2
    //r1_Code           支付结果                                           固定值 “1”, 代表支付成功        3
    //r2_TrxId          [API支付平台]交易流水号        Max(50)             [API支付平台]平台产生的交易流水号，每笔订单唯一        4
    //r3_Amt            支付金额                       Max(20)             单位：元，精确到分，保留小数点后两位；商户收到该返回数据后，一定用自己数据库中存储的金额与该金额进行比较        5
    //r4_Cur            交易币种                       Max(10)             返回时是“RMB”        6
    //r5_Pid            商品名称                       Max(20)             [API支付平台]返回商户设置的商品名称；此参数如用到中文，请注意转码        7
    //r6_Order          商户订单号                     Max(50)             [API支付平台]返回商户订单号        8
    //r7_Uid            [API支付平台]会员ID            Max(50)             如果用户使用的[API支付平台]会员进行支付则返回该用户的[API支付平台]会员ID；  反之为“”        9
    //r8_MP             商户扩展信息                   Max(200)            此参数如用到中文，请注意转码        10
    //r9_BType          交易结果返回类型               Max(1)              为“1”： 浏览器重定向        为“2”： 服务器点对点通讯        11
    //rb_BankId         支付通道编码                                       返回用户所使用的支付通道编码该返回参数不参与到hmac校验，范例中没有收录，可根据您的需要自行添加        
    //rp_PayDate        支付成功时间                                       该返回参数不参与到hmac校验，范例中没有收录，可根据您的需要自行添加        
    //rq_CardNo         神州行充值卡序列号                                 若用户使用神州行卡支付，返回用户所使用的神州行卡序列号；该返回参数不参与到hmac校验，范例中没有收录，可根据您的需要自行添加        
    //hmac              签名数据                       Max(32)             产生hmac需要两个参数，并调用相关API.
    private static final String p1_MerId                   ="p1_MerId";
    private static final String r0_Cmd                     ="r0_Cmd";
    private static final String r1_Code                    ="r1_Code";
    private static final String r2_TrxId                   ="r2_TrxId";
    private static final String r3_Amt                     ="r3_Amt";
    private static final String r4_Cur                     ="r4_Cur";
    private static final String r5_Pid                     ="r5_Pid";
    private static final String r6_Order                   ="r6_Order";
    private static final String r7_Uid                     ="r7_Uid";
    private static final String r8_MP                      ="r8_MP";
    private static final String r9_BType                   ="r9_BType";
//    private static final String rb_BankId                  ="rb_BankId";
//    private static final String rp_PayDate                 ="rp_PayDate";
//    private static final String rq_CardNo                  ="rq_CardNo";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="hmac";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(p1_MerId);
        String ordernumberR = API_RESPONSE_PARAMS.get(r6_Order);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[快付通宝]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
        return ordernumberR;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        StringBuilder signStr = new StringBuilder();
        signStr.append(api_response_params.get(p1_MerId));
        signStr.append(api_response_params.get(r0_Cmd));
        signStr.append(api_response_params.get(r1_Code));
        signStr.append(api_response_params.get(r2_TrxId));
        signStr.append(api_response_params.get(r3_Amt));
        signStr.append(api_response_params.get(r4_Cur));
        signStr.append(api_response_params.get(r5_Pid));
        signStr.append(api_response_params.get(r6_Order));
        signStr.append(api_response_params.get(r7_Uid));
        signStr.append(api_response_params.get(r8_MP));
        signStr.append(api_response_params.get(r9_BType));
        String paramsStr =signStr.toString();
        String signMd5 = DigestUtil.hmacSign(paramsStr, channelWrapper.getAPI_KEY());
        log.debug("[快付通宝]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //r1_Code           支付结果                                           固定值 “1”, 代表支付成功        3
        String payStatusCode = api_response_params.get(r1_Code);
        String responseAmount = HandlerUtil.getFen(api_response_params.get(r3_Amt));
        //db_amount数据库存入的是分     第三方返回的responseAmount是元
        boolean checkAmount = db_amount.equalsIgnoreCase(responseAmount);
        //1代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("1")) {
            my_result = true;
        } else {
            log.error("[快付通宝]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[快付通宝]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：1");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[快付通宝]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[快付通宝]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}