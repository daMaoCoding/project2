package dc.pay.business.xinjuhezhifu3;

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
 * Aug 13, 2019
 */
@ResponsePayHandler("XINJUHEZHIFU3")
public final class XinJuHeZhiFu3PayResponseHandler extends PayResponseHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //2.3.2 通知参数 字段名 变量名 必填 类型 示例值 描述 
    //支付订单号 payOrderId 是 Stri ng( 30) P2016042721060400049 0 支付中心生成的订 单号 
//    private static final String payOrderId                ="payOrderId";
    //商户ID mchId 是 Stri ng( 30) 20001222 支付中心分配的商 户号 
    private static final String mchId                ="mchId";
    //应用ID appId 是 Stri ng( 32) 0ae8be35ff634e2abe94f 5f32f6d5c4f 该商户创建的应用 对应的 ID 
//    private static final String appId                ="appId";
    //支付产品ID productId 是 int 8001 支付产品 ID 
//    private static final String productId                ="productId";
    //商户订单号 mchOrderNo 是 Stri ng( 30) 20160427210604000490 商户生成的订单号
    private static final String mchOrderNo                ="mchOrderNo";
//    支付金额 amount 是 int 100 支付金额,单位分
    private static final String amount                ="amount";
    //状态 status 是 int 1 支付状态,0-订单生 成,1-支付中,2-支付 成功,3-业务处理完 成
    private static final String status                ="status";
    //渠道订单 channelOrderNo 否 Stri ng( wx2016081611532915ae 15beab0167893571 三方支付渠道订单 号
//    private static final String channelOrderNo                ="channelOrderNo";
    //渠道数据包 channelAttach 否 Stri ng {“bank_type”:”CMB_ DEBIT”,”trade_type”: ”pay.weixin.micropay” } 支付渠道数据包
//    private static final String channelAttach                ="channelAttach";
    //支付成功时间 paySuccTime 是 lon g 精确到毫秒
    private static final String paySuccTime                ="paySuccTime";
//    通知类型 backType 是 int 1 通知类型，1-前台 通知，2-后台通知
//    private static final String backType                ="backType";
    //签名 sign 是 Stri ng( 32) C380BEC2BFD727A4B68 45133519F3AD6 签名值，详见签名 算法
//    private static final String sign                 ="sign";
    
    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    private static final String RESPONSE_PAY_MSG = "success";

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
       //String api_KEY = channelWrapper.getAPI_KEY();
       //if (null == api_KEY || !api_KEY.contains("-") || api_KEY.split("-").length != 2) {
       //    log.error("[新聚合支付3]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //    throw new PayException("[新聚合支付3]-[响应支付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
       //}
        if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
        String partnerR = API_RESPONSE_PARAMS.get(mchId);
        String ordernumberR = API_RESPONSE_PARAMS.get(mchOrderNo);
        if (StringUtils.isBlank(partnerR) || StringUtils.isBlank(ordernumberR))
            throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("[新聚合支付3]-[响应支付]-1.获取支付通道响应信息中的订单号完成：{}" ,ordernumberR);
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
        log.debug("[新聚合支付3]-[响应支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5) );
        return signMd5;
    }

    @Override
    protected boolean checkPayStatusAndMount(Map<String, String> api_response_params, String db_amount) throws PayException {
        boolean my_result = false;
        //状态 status 是 int 1 支付状态,0-订单生 成,1-支付中,2-支付 成功,3-业务处理完 成
        String payStatusCode = api_response_params.get(status);
        String responseAmount = api_response_params.get(amount);

        //偏差大于1元，要意见反馈里备注下，业主要知道，用了对不上账就不是我们的问题了：并在特殊通道写明后，上线前通知我平台客服
        boolean checkAmount =  HandlerUtil.isAllowAmountt(db_amount,responseAmount,"100");//我平台默认允许一元偏差

        // 1 代表第三方支付成功
        if (checkAmount && payStatusCode.equalsIgnoreCase("2")) {
            my_result = true;
        } else {
            log.error("[新聚合支付3]-[响应支付]金额及状态验证错误,订单号：" + channelWrapper.getAPI_ORDER_ID() + ",第三方支付状态：" + payStatusCode + " ,支付金额：" + responseAmount + " ，应支付金额：" + db_amount);
        }
        log.debug("[新聚合支付3]-[响应支付]-3.验证第三方支付响应支付状态&验证第三方支付金额与数据库订单支付金额完成,验证结果：" + my_result + " ,金额验证：" + checkAmount + " ,responseAmount=" + responseAmount + " ,数据库金额：" + db_amount + ",第三方响应支付成功标志:" + payStatusCode + " ,计划成功：2");
        return my_result;
    }

    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean my_result = api_response_params.get(signature).equalsIgnoreCase(signMd5);
        log.debug("[新聚合支付3]-[响应支付]-4.验证MD5签名：{}", my_result);
        return my_result;
    }

    @Override
    protected String responseSuccess() {
        log.debug("[新聚合支付3]-[响应支付]-5.第三方支付确认收到消息返回内容：{}", RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }
}